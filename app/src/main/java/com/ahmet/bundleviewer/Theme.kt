package com.ahmet.bundleviewer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette taken from the 2026 humblebundle.com redesign (slate surfaces, violet accent).
val HumbleViolet = Color(0xFF7C3BED)
val HumbleVioletLight = Color(0xFFA78BFA)
val UrgentOrange = Color(0xFFFB923C)
val CardBorder = Color(0xFF344256)

private val colors = darkColorScheme(
    primary = HumbleViolet,
    onPrimary = Color(0xFFF8FAFC),
    primaryContainer = Color(0xFF2E1A5C),
    onPrimaryContainer = Color(0xFFDDD6FE),
    secondary = HumbleVioletLight,
    secondaryContainer = Color(0xFF2A3549),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0F1729),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F1729),
    onSurface = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFF1D283A),
    surfaceContainerHigh = Color(0xFF243247),
    surfaceContainerHighest = Color(0xFF2C3B52),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CardBorder,
    outlineVariant = CardBorder,
)

@Composable
fun BundleViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
