package com.ahmet.bundleviewer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HumbleRed = Color(0xFFE5484D)
val UrgentOrange = Color(0xFFFF9F43)
val NewGreen = Color(0xFF3DD68C)

private val colors = darkColorScheme(
    primary = HumbleRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1A1C),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFB8C4D6),
    secondaryContainer = Color(0xFF2B3240),
    onSecondaryContainer = Color(0xFFDDE5F2),
    background = Color(0xFF121418),
    onBackground = Color(0xFFE6E8EC),
    surface = Color(0xFF121418),
    onSurface = Color(0xFFE6E8EC),
    surfaceContainer = Color(0xFF1C1F25),
    surfaceContainerHigh = Color(0xFF242830),
    surfaceContainerHighest = Color(0xFF2D323B),
    onSurfaceVariant = Color(0xFF9BA3AF),
    outlineVariant = Color(0xFF353B45),
)

@Composable
fun BundleViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
