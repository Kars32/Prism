package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PrismDarkColorScheme = darkColorScheme(
    primary = PrismCyan,
    onPrimary = PrismObsidian,
    primaryContainer = PrismCyanContainer,
    onPrimaryContainer = PrismCyan,
    secondary = PrismViolet,
    onSecondary = PrismObsidian,
    secondaryContainer = PrismVioletContainer,
    onSecondaryContainer = PrismViolet,
    tertiary = PrismEmerald,
    onTertiary = PrismObsidian,
    tertiaryContainer = PrismEmeraldContainer,
    onTertiaryContainer = PrismEmerald,
    background = PrismObsidian,
    onBackground = PrismAlabaster,
    surface = PrismObsidian,
    onSurface = PrismAlabaster,
    surfaceVariant = PrismSurface,
    onSurfaceVariant = PrismPewter,
    surfaceContainerLow = PrismSurface,
    surfaceContainer = PrismSurface,
    surfaceContainerHigh = PrismSurfaceElevated,
    outline = PrismOutline,
    outlineVariant = PrismOutlineSubtle,
    error = PrismRose,
    onError = PrismObsidian
)

private val PrismShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun PrismTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PrismDarkColorScheme,
        typography = PrismTypography,
        shapes = PrismShapes,
        content = content
    )
}
