package com.ahmet.bundleviewer

import java.time.LocalDateTime

enum class BundleCategory(val key: String, val label: String) {
    GAMES("games", "Oyun"),
    BOOKS("books", "Kitap"),
    SOFTWARE("software", "Yazılım");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** A bundle tile as listed on humblebundle.com/bundles. */
data class BundleSummary(
    val machineName: String,
    val name: String,
    val category: BundleCategory,
    val url: String,
    val imageUrl: String,
    val author: String,
    val blurb: String,
    /** e.g. "₺246.94 or more", taken from the "Pay X or more" highlight. */
    val priceText: String?,
    /** e.g. "US$494 Value" */
    val valueText: String?,
    /** e.g. "45 books" */
    val itemCountText: String?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
)

data class BundleItem(
    val name: String,
    val type: String,
    val imageUrl: String?,
    val msrp: Money?,
    val platforms: List<String>,
)

data class Money(val amount: Double, val currency: String)

data class BundleTier(
    val header: String,
    val price: Money?,
    /** Items first unlocked at this tier (not already in a cheaper tier). */
    val newItems: List<BundleItem>,
    val totalItemCount: Int,
)

data class BundleDetail(
    val name: String,
    val description: String,
    val totalMsrp: Money?,
    /** Cheapest tier first. */
    val tiers: List<BundleTier>,
)
