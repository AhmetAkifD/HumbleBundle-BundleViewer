package com.ahmet.bundleviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object BundleScraper {
    suspend fun fetchBundles(): List<BundleItem> = withContext(Dispatchers.IO) {
        val url = "https://www.humblebundle.com/bundles"
        val bundles = mutableListOf<BundleItem>()
        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val tileHolders = document.select("div.tile-holder.js-tile-holder.one-third")
            
            for (element in tileHolders) {
                val anchor = element.selectFirst("a") ?: continue
                val detailUrl = anchor.attr("href")
                
                // Info block
                val infoSection = element.selectFirst(".info-section")
                val name = infoSection?.selectFirst(".name")?.text() ?: "Unknown Bundle"
                val timeLeft = infoSection?.selectFirst(".timer-field")?.text() ?: "Unknown Time"
                
                // Image
                val imgElement = element.selectFirst("img.tile-image")
                var imageUrl = imgElement?.attr("data-src") ?: ""
                if (imageUrl.isEmpty()) {
                    imageUrl = imgElement?.attr("src") ?: ""
                }
                
                // Text blurb
                val blurb = element.selectFirst(".marketing-blurb")?.text() ?: ""
                
                // We add to list
                bundles.add(BundleItem(
                    name = name,
                    imageUrl = imageUrl,
                    marketingBlurb = blurb,
                    timeLeft = timeLeft,
                    detailUrl = detailUrl
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace() // In a real app we'd log this properly
        }
        bundles
    }
}
