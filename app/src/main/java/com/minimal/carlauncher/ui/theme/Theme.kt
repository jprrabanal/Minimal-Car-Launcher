package com.minimal.carlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private fun createColorScheme(colors: CarColors, isDark: Boolean) = if (isDark) {
    darkColorScheme(
        primary = colors.accentCyan,
        onPrimary = colors.bg,
        primaryContainer = colors.surfaceVariant,
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentGreen,
        onSecondary = colors.bg,
        tertiary = colors.accentAmber,
        background = colors.bg,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.border
    )
} else {
    lightColorScheme(
        primary = colors.accentCyan,
        onPrimary = Color.White,
        primaryContainer = colors.surfaceVariant,
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentGreen,
        onSecondary = Color.White,
        tertiary = colors.accentAmber,
        background = colors.bg,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.border
    )
}

@Composable
fun CarLauncherTheme(
    palette: MinimalPalette = MinimalPalette.DEFAULT,
    isNightDimmed: Boolean = false,
    content: @Composable () -> Unit
) {
    // The launcher always uses the soft dark Minimal colors, darker at night
    val colors = minimalCarColors(palette).let { if (isNightDimmed) it.dimmedForNight() else it }
    val colorScheme = createColorScheme(colors, colors.isDark)

    // The launcher uses fixed, screen-filling layouts. A large system font size
    // (common on head units and phones) would push text out of its cards, so
    // text is never scaled above 100%.
    val density = LocalDensity.current
    val cappedDensity = Density(density.density, density.fontScale.coerceAtMost(1f))

    CompositionLocalProvider(
        LocalCarColors provides colors,
        LocalDensity provides cappedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
