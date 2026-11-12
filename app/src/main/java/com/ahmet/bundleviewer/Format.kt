package com.ahmet.bundleviewer

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr-TR")

fun Money.format(): String {
    val format = NumberFormat.getCurrencyInstance(TURKISH)
    runCatching { format.currency = Currency.getInstance(currency) }
    format.maximumFractionDigits = if (amount >= 1000 || amount % 1.0 == 0.0) 0 else 2
    format.minimumFractionDigits = format.maximumFractionDigits
    return format.format(amount)
}

fun timeLeft(end: Instant?): Duration? = end?.let { Duration.between(Instant.now(), it) }

fun Duration.formatRemaining(): String = when {
    isNegative -> "Sona erdi"
    toDays() >= 1 -> "${toDays()} gün kaldı"
    toHours() >= 1 -> "${toHours()} saat kaldı"
    else -> "${toMinutes().coerceAtLeast(1)} dakika kaldı"
}

/** Less than two days left. */
fun Duration.isUrgent(): Boolean = toDays() < 2

fun LocalDateTime.formatClock(): String = format(DateTimeFormatter.ofPattern("HH:mm"))

fun Instant.formatDate(): String =
    DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH).format(atZone(ZoneId.systemDefault()))

fun String.toPlatformLabel(): String = when (lowercase()) {
    "windows" -> "Windows"
    "mac" -> "Mac"
    "linux" -> "Linux"
    "android" -> "Android"
    "steam" -> "Steam"
    "gog" -> "GOG"
    "epic" -> "Epic"
    "drm-free", "download" -> "DRM'siz"
    "other_key" -> "Anahtar"
    "pdf", "epub", "mobi", "cbz", "mp3", "flac", "wav", "stl" -> uppercase()
    else -> replace('_', ' ').replaceFirstChar { it.uppercase() }
}
