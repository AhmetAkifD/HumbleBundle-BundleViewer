package com.ahmet.bundleviewer

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * humblebundle.com is a Next.js app. Page data is streamed as a React Server Components
 * "flight" payload inside `self.__next_f.push([1, "..."])` scripts, so we decode that
 * payload and pull the JSON objects we need out of it.
 */
object BundleScraper {
    private const val BASE_URL = "https://www.humblebundle.com"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchBundles(): List<BundleSummary> = withContext(Dispatchers.IO) {
        val flight = extractFlight(download("$BASE_URL/bundles"))
        val bundles = linkedMapOf<String, BundleSummary>()

        // Every bundle card on the listing is an Algolia search hit.
        for (match in LIST_HIT_REGEX.findAll(flight)) {
            val hit = parseObjectAt(flight, match.range.first).optJSONObject("value") ?: continue
            val summary = parseSummary(hit) ?: continue
            bundles.putIfAbsent(summary.machineName, summary)
        }
        if (bundles.isEmpty()) throw IOException("Sayfa yapısı tanınmadı (bundle bulunamadı)")
        bundles.values.toList()
    }

    suspend fun fetchDetail(bundleUrl: String): BundleDetail = withContext(Dispatchers.IO) {
        val flight = extractFlight(download(bundleUrl))
        val start = flight.indexOf(DETAIL_MARKER)
        if (start < 0) throw IOException("Sayfa yapısı tanınmadı (bundlePage bulunamadı)")
        val page = parseObjectAt(flight, start).getJSONObject("bundlePage")

        val unlocked = mutableSetOf<String>()
        val tiers = page.optJSONArray("tiers")?.objects().orEmpty()
            .sortedBy { it.optJSONObject("price")?.optDouble("amount") ?: Double.MAX_VALUE }
            .map { tier ->
                // Items repeated from another tier are sent as "$e:...:bundlePage:tiers:0:bundleItems:1".
                val items = tier.optJSONArray("bundleItems")?.let { array ->
                    (0 until array.length()).mapNotNull { i ->
                        when (val item = array.opt(i)) {
                            is JSONObject -> item
                            is String -> resolvePageRef(page, item)
                            else -> null
                        }
                    }
                }.orEmpty()
                BundleTier(
                    price = tier.optJSONObject("price")?.toMoney(),
                    totalValue = tier.optJSONObject("totalMsrpValue")?.toMoney(),
                    newItems = items.filter { unlocked.add(it.optString("id")) }.map(::parseItem),
                    totalItemCount = items.size,
                )
            }

        BundleDetail(
            name = page.optString("title"),
            headline = page.optStringOrNull("headline"),
            description = (page.optStringOrNull("details") ?: page.optStringOrNull("description"))
                ?.let { resolveText(flight, it) }
                ?.let(::stripHtml)
                ?.ifBlank { null },
            logoUrl = page.optStringOrNull("logoImageUrl"),
            imageUrl = page.optStringOrNull("thumbnailImageUrl"),
            value = page.optJSONObject("value")?.toMoney(),
            startDate = parseDate(page.optStringOrNull("startDate")),
            endDate = parseDate(page.optStringOrNull("endDate")),
            keyExpirationDate = parseDate(page.optStringOrNull("keyExpirationDate")),
            charities = page.optJSONArray("supportingCharities")?.objects().orEmpty()
                .mapNotNull { it.optStringOrNull("name") },
            tiers = tiers,
        )
    }

    private fun parseSummary(hit: JSONObject): BundleSummary? {
        val category = BundleCategory.fromKey(hit.optString("category")) ?: return null
        return BundleSummary(
            machineName = hit.optStringOrNull("productKey") ?: return null,
            name = hit.optString("title"),
            category = category,
            url = absoluteUrl(hit.optString("url")),
            imageUrl = hit.optString("thumbnail"),
            logoUrl = hit.optStringOrNull("titleImageUrl"),
            price = hit.optJSONObject("displayPrice")?.optJSONObject("currentPrice")?.toMoney(),
            value = hit.optJSONObject("value")?.toMoney(),
            itemCount = hit.optInt("itemsCount"),
            itemTitles = hit.optJSONArray("includedItemTitles")?.strings().orEmpty(),
            endDate = parseDate(hit.optStringOrNull("endDate")),
        )
    }

    private fun parseItem(item: JSONObject) = BundleItem(
        name = item.optString("title"),
        imageUrl = item.optStringOrNull("thumbnailImageUrl"),
        msrp = item.optJSONObject("msrp")?.toMoney(),
        developers = item.optJSONArray("developers")?.objects().orEmpty().mapNotNull { it.optStringOrNull("name") },
        platforms = item.optJSONArray("platforms")?.strings().orEmpty(),
        deliveryMethods = item.optJSONArray("deliveryMethods")?.strings().orEmpty(),
    )

    private fun download(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Boş yanıt")
        }
    }

    /** Joins the string chunks of every `self.__next_f.push([1, "..."])` call. */
    private fun extractFlight(html: String): String {
        val flight = StringBuilder()
        for (match in PUSH_REGEX.findAll(html)) {
            val args = runCatching { JSONArray(match.groupValues[1]) }.getOrNull() ?: continue
            (args.opt(1) as? String)?.let(flight::append)
        }
        if (flight.isEmpty()) throw IOException("Sayfa yapısı tanınmadı (Next.js verisi bulunamadı)")
        return flight.toString()
    }

    /** Parses the single JSON object that starts at [index], ignoring whatever follows it. */
    private fun parseObjectAt(text: String, index: Int): JSONObject =
        JSONTokener(text.substring(index)).nextValue() as JSONObject

    /** Follows a "$e:0:props:...:bundlePage:tiers:0:bundleItems:1" reference inside [page]. */
    private fun resolvePageRef(page: JSONObject, ref: String): JSONObject? {
        val path = ref.substringAfter(":bundlePage:", "").split(':').filter { it.isNotEmpty() }
        if (path.isEmpty()) return null
        var node: Any? = page
        for (key in path) {
            node = when (node) {
                is JSONObject -> node.opt(key)
                is JSONArray -> key.toIntOrNull()?.let(node::opt)
                else -> null
            }
        }
        return node as? JSONObject
    }

    /**
     * Long strings are sent as separate text rows ("6e:T5e6,<text>") and referenced as "$6e".
     * The hex length is in UTF-8 bytes.
     */
    private fun resolveText(flight: String, value: String): String? {
        if (!value.startsWith("$")) return value
        val id = value.removePrefix("$")
        val row = Regex("""(?<![0-9a-z])${Regex.escape(id)}:T([0-9a-f]+),""").find(flight) ?: return null
        var remaining = row.groupValues[1].toInt(16)
        var end = row.range.last + 1
        while (remaining > 0 && end < flight.length) {
            val cp = flight.codePointAt(end)
            remaining -= when {
                cp < 0x80 -> 1
                cp < 0x800 -> 2
                cp < 0x10000 -> 3
                else -> 4
            }
            end += Character.charCount(cp)
        }
        return flight.substring(row.range.last + 1, end)
    }

    private fun absoluteUrl(path: String) = if (path.startsWith("http")) path else BASE_URL + path

    private fun parseDate(value: String?): Instant? =
        value?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun stripHtml(html: String): String =
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

    /** Prices come either as {amount} or as {centAmount, fractionDigits}. */
    private fun JSONObject.toMoney(): Money? {
        val currency = optStringOrNull("currencyCode") ?: "USD"
        if (has("centAmount")) {
            val digits = optInt("fractionDigits", 2)
            return Money(optLong("centAmount") / Math.pow(10.0, digits.toDouble()), currency)
        }
        val amount = optDouble("amount", Double.NaN)
        return if (amount.isNaN()) null else Money(amount, currency)
    }

    /** Treats JSON null, "" and the RSC placeholder "$undefined" as missing. */
    private fun JSONObject.optStringOrNull(key: String): String? {
        if (isNull(key)) return null
        val value = optString(key)
        return if (value.isEmpty() || value == "\$undefined") null else value
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { opt(it) as? String }

    private val PUSH_REGEX = Regex("""self\.__next_f\.push\((\[.*?])\)</script>""", RegexOption.DOT_MATCHES_ALL)
    private val LIST_HIT_REGEX = Regex("""\{"relationTo":"algolia-hit","value":""")
    private const val DETAIL_MARKER = """{"bundlePage":"""
}
