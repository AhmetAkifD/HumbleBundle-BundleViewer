package com.ahmet.bundleviewer

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr-TR")

fun Money.format(): String {
    val format = NumberFormat.getCurrencyInstance(TURKISH)
    runCatching { format.currency = Currency.getInstance(currency) }
    format.maximumFractionDigits = if (amount >= 1000) 0 else 2
    return format.format(amount)
}

/** Humble Bundle dates are in UTC. */
private fun nowUtc(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

fun BundleSummary.timeLeft(): Duration? = endDate?.let { Duration.between(nowUtc(), it) }

fun BundleSummary.isNew(): Boolean =
    startDate?.let { Duration.between(it, nowUtc()).toDays() < 3 } ?: false

fun Duration.formatRemaining(): String = when {
    isNegative -> "Sona erdi"
    toDays() >= 1 -> "${toDays()} gün kaldı"
    toHours() >= 1 -> "${toHours()} saat kaldı"
    else -> "${toMinutes().coerceAtLeast(1)} dakika kaldı"
}

fun LocalDateTime.formatClock(): String = format(DateTimeFormatter.ofPattern("HH:mm"))

fun String.toTurkishItemType(): String = when (lowercase()) {
    "game" -> "Oyun"
    "ebook", "book", "comic" -> "Kitap"
    "software" -> "Yazılım"
    "audio" -> "Ses"
    "video" -> "Video"
    else -> replaceFirstChar { it.uppercase() }
}
