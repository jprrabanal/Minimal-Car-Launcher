package com.minimal.carlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CarColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentAmber: Color,
    val accentGreen: Color,
    val accentZLink: Color,
    val accentBlue: Color,
    val accentRed: Color,
    val isDark: Boolean
)

// Luxury automotive obsidian backdrop with deep velvet undertone (Night Mode)
val DarkCarColors = CarColors(
    bg = Color(0xFF0A0E17),
    surface = Color(0xFF131926),
    surfaceVariant = Color(0xFF1C2436),
    border = Color(0xFF263248),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    accentCyan = Color(0xFF00D2EE),
    accentAmber = Color(0xFFF59E0B),
    accentGreen = Color(0xFF10B981),
    accentZLink = Color(0xFF10B981),
    accentBlue = Color(0xFF38BDF8),
    accentRed = Color(0xFFF43F5E),
    isDark = true
)

/**
 * Soft dark color themes for the Minimal home screen. None of them use pure
 * black or pure white: medium-dark backgrounds and slightly dimmed text are
 * easier on the eyes when driving at night.
 */
enum class MinimalPalette(
    val label: String,
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val accent: Color
) {
    SLATE("Slate", Color(0xFF1E2430), Color(0xFF272E3B), Color(0xFF323A49), Color(0xFF424C5E), Color(0xFF7DD3FC)),
    MIDNIGHT("Midnight Blue", Color(0xFF1A2233), Color(0xFF222C40), Color(0xFF2C384F), Color(0xFF3B4964), Color(0xFF93C5FD)),
    GRAPHITE("Graphite", Color(0xFF242526), Color(0xFF2E2F31), Color(0xFF393A3D), Color(0xFF4A4B4F), Color(0xFFD4D4D8)),
    FOREST("Forest", Color(0xFF1C2622), Color(0xFF24302B), Color(0xFF2E3B35), Color(0xFF3D4C45), Color(0xFF86EFAC)),
    PLUM("Plum", Color(0xFF251E2C), Color(0xFF2E2637), Color(0xFF393042), Color(0xFF4A3F55), Color(0xFFD8B4FE)),
    MOCHA("Mocha", Color(0xFF29231F), Color(0xFF332B26), Color(0xFF3E3530), Color(0xFF4F443D), Color(0xFFFDBA74));

    companion object {
        val DEFAULT = SLATE

        fun fromName(name: String?): MinimalPalette =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

fun minimalCarColors(palette: MinimalPalette): CarColors = DarkCarColors.copy(
    bg = palette.bg,
    surface = palette.surface,
    surfaceVariant = palette.surfaceVariant,
    border = palette.border,
    textPrimary = Color(0xFFE6E9EF),
    textSecondary = Color(0xFFA9B1BE),
    textMuted = Color(0xFF7D8696),
    accentCyan = palette.accent
)

val LocalCarColors = staticCompositionLocalOf { DarkCarColors }

// Dynamic Composable Color Accessors:
// Seamlessly delegates to LocalCarColors.current so all existing composables adapt dynamically
val CarBg: Color
    @Composable
    get() = LocalCarColors.current.bg

val CarSurface: Color
    @Composable
    get() = LocalCarColors.current.surface

val CarSurfaceVariant: Color
    @Composable
    get() = LocalCarColors.current.surfaceVariant

val CarBorder: Color
    @Composable
    get() = LocalCarColors.current.border

val AccentCyan: Color
    @Composable
    get() = LocalCarColors.current.accentCyan

val AccentAmber: Color
    @Composable
    get() = LocalCarColors.current.accentAmber

val AccentGreen: Color
    @Composable
    get() = LocalCarColors.current.accentGreen

val AccentZLink: Color
    @Composable
    get() = LocalCarColors.current.accentZLink

val AccentBlue: Color
    @Composable
    get() = LocalCarColors.current.accentBlue

val AccentRed: Color
    @Composable
    get() = LocalCarColors.current.accentRed

val TextPrimary: Color
    @Composable
    get() = LocalCarColors.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalCarColors.current.textSecondary

val TextMuted: Color
    @Composable
    get() = LocalCarColors.current.textMuted
