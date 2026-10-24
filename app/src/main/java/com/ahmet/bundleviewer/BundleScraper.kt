package com.ahmet.bundleviewer

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Humble Bundle pages embed their data as JSON inside <script type="application/json"> tags,
 * so we read that instead of scraping the rendered HTML.
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
        val html = download("$BASE_URL/bundles")
        val data = extractJson(html, "landingPage-json-data").getJSONObject("data")
        val seen = mutableSetOf<String>()
        val bundles = mutableListOf<BundleSummary>()

        for (category in BundleCategory.entries) {
            val mosaic = data.optJSONObject(category.key)?.optJSONArray("mosaic") ?: continue
            for (section in mosaic.objects()) {
                val products = section.optJSONArray("products") ?: continue
                for (p in products.objects()) {
                    val machineName = p.optString("machine_name")
                    if (machineName.isEmpty() || !seen.add(machineName)) continue
                    bundles += parseSummary(p, category)
                }
            }
        }
        bundles
    }

    suspend fun fetchDetail(bundleUrl: String): BundleDetail = withContext(Dispatchers.IO) {
        val html = download(bundleUrl)
        val data = extractJson(html, "webpack-bundle-page-data").getJSONObject("bundleData")
        val basic = data.getJSONObject("basic_data")
        val display = data.getJSONObject("tier_display_data")
        val pricing = data.optJSONObject("tier_pricing_data")
        val itemData = data.getJSONObject("tier_item_data")

        // tier_order lists the most expensive tier first.
        val order = data.getJSONArray("tier_order").strings().reversed()
        val unlocked = mutableSetOf<String>()
        val tiers = order.mapNotNull { id ->
            val tier = display.optJSONObject(id) ?: return@mapNotNull null
            val names = tier.optJSONArray("tier_item_machine_names")?.strings().orEmpty()
            val newItems = names.filter { unlocked.add(it) }
                .mapNotNull { itemData.optJSONObject(it)?.let(::parseItem) }
            BundleTier(
                header = tier.optString("header"),
                price = pricing?.optJSONObject(id)?.optJSONObject("price|money")?.toMoney(),
                newItems = newItems,
                totalItemCount = names.size,
            )
        }

        BundleDetail(
            name = basic.optString("human_name"),
            description = stripHtml(basic.optString("detailed_marketing_blurb")),
            totalMsrp = basic.optJSONObject("msrp|money")?.toMoney(),
            tiers = tiers,
        )
    }

    private fun parseSummary(p: JSONObject, category: BundleCategory): BundleSummary {
        val heroHeadings = p.optJSONArray("hero_highlights")?.objects()
            ?.map { it.optString("heading") }.orEmpty()
        val hover = p.optJSONArray("hover_highlights")?.strings().orEmpty()
        val price = heroHeadings.firstNotNullOfOrNull { PRICE_REGEX.find(it)?.groupValues?.get(1) }

        return BundleSummary(
            machineName = p.getString("machine_name"),
            name = p.optString("tile_name").ifEmpty { p.optString("tile_short_name") },
            category = category,
            url = absoluteUrl(p.optString("product_url")),
            imageUrl = p.optString("tile_image").ifEmpty { p.optString("high_res_tile_image") },
            author = p.optString("author"),
            blurb = stripHtml(p.optString("short_marketing_blurb").ifEmpty { p.optString("marketing_blurb") }),
            priceText = price,
            valueText = hover.firstOrNull { it.endsWith("Value", ignoreCase = true) },
            itemCountText = hover.firstOrNull { !it.endsWith("Value", ignoreCase = true) },
            startDate = parseDate(p.optString("start_date|datetime")),
            endDate = parseDate(p.optString("end_date|datetime")),
        )
    }

    private fun parseItem(item: JSONObject): BundleItem {
        val icons = item.optJSONObject("availability_icons")
        val humanNames = icons?.optJSONObject("human_names")
        val platforms = icons?.optJSONArray("platform_icons")?.strings().orEmpty()
            .map { humanNames?.optString(it)?.ifEmpty { null } ?: it.removePrefix("hb-") }
        return BundleItem(
            name = item.optString("human_name"),
            type = item.optString("item_content_type"),
            imageUrl = item.optJSONObject("resolved_paths")?.let { paths ->
                listOf("front_page_art_imgix_retina", "preview_image", "featured_image")
                    .firstNotNullOfOrNull { paths.optStringOrNull(it) }
            },
            msrp = item.optJSONObject("msrp_price|money")?.toMoney(),
            platforms = platforms,
        )
    }

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

    private fun extractJson(html: String, scriptId: String): JSONObject {
        val regex = Regex("""<script id="$scriptId" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val json = regex.find(html)?.groupValues?.get(1)
            ?: throw IOException("Sayfa yapısı tanınmadı ($scriptId bulunamadı)")
        return JSONObject(json)
    }

    private fun absoluteUrl(path: String) = if (path.startsWith("http")) path else BASE_URL + path

    private fun parseDate(value: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(value) }.getOrNull()

    private fun stripHtml(html: String): String =
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

    private fun JSONObject.toMoney(): Money? {
        val amount = optDouble("amount", Double.NaN)
        if (amount.isNaN()) return null
        return Money(amount, optString("currency", "USD"))
    }

    /** optString() turns JSON null into the string "null"; this returns real null instead. */
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifEmpty { null }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }

    private val PRICE_REGEX = Regex("""Pay (.+?) (?:or more|to unlock)""", RegexOption.IGNORE_CASE)
}
