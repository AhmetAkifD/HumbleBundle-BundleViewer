package com.ahmet.bundleviewer

import java.time.Instant

enum class BundleCategory(val key: String, val label: String) {
    GAMES("games", "Oyun"),
    BOOKS("books", "Kitap"),
    SOFTWARE("software", "Yazılım");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

data class Money(val amount: Double, val currency: String)

/** A bundle tile as listed on humblebundle.com/bundles. */
data class BundleSummary(
    val machineName: String,
    val name: String,
    val category: BundleCategory,
    val url: String,
    val imageUrl: String,
    /** Transparent bundle logo drawn over the cover art, if any. */
    val logoUrl: String?,
    /** Minimum price ("Pay what you want" starting point). */
    val price: Money?,
    val value: Money?,
    val itemCount: Int,
    val itemTitles: List<String>,
    val endDate: Instant?,
)

data class BundleItem(
    val name: String,
    val imageUrl: String?,
    val msrp: Money?,
    val developers: List<String>,
    val platforms: List<String>,
    /** How the item is delivered, e.g. "steam". */
    val deliveryMethods: List<String>,
)

data class BundleTier(
    val price: Money?,
    /** Total retail value of everything in this tier. */
    val totalValue: Money?,
    /** Items first unlocked at this tier (not already in a cheaper tier). */
    val newItems: List<BundleItem>,
    val totalItemCount: Int,
)

data class BundleDetail(
    val name: String,
    val headline: String?,
    val description: String?,
    val logoUrl: String?,
    val imageUrl: String?,
    val value: Money?,
    val startDate: Instant?,
    val endDate: Instant?,
    val keyExpirationDate: Instant?,
    val charities: List<String>,
    /** Cheapest tier first. */
    val tiers: List<BundleTier>,
)
