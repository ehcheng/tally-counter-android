package com.echeng.tally.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// OLED Dark palette
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF111111)
val DarkCard = Color(0xFF1A1A1A)
val DarkCardElevated = Color(0xFF222222)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFC7C7C9)
val TextTertiary = Color(0xFFB1B1B3)

// Accent
val AccentBlue = Color(0xFF007AFF)

// Counter colors — single source of truth for hex + Color values
data class CounterColorOption(val hex: String, val color: Color)

val CounterColorOptions = listOf(
    CounterColorOption("#FFFF3B5C", Color(0xFFFF3B5C)), // Coral Red
    CounterColorOption("#FF007AFF", Color(0xFF007AFF)), // Blue
    CounterColorOption("#FF34C759", Color(0xFF34C759)), // Green
    CounterColorOption("#FFFF9500", Color(0xFFFF9500)), // Orange
    CounterColorOption("#FFAF52DE", Color(0xFFAF52DE)), // Purple
    CounterColorOption("#FFFF2D55", Color(0xFFFF2D55)), // Pink
    CounterColorOption("#FF5AC8FA", Color(0xFF5AC8FA)), // Light Blue
    CounterColorOption("#FFFFCC00", Color(0xFFFFCC00)), // Yellow
    CounterColorOption("#FF00C7BE", Color(0xFF00C7BE)), // Teal
    CounterColorOption("#FFFF6482", Color(0xFFFF6482)), // Salmon
    CounterColorOption("#FF30D158", Color(0xFF30D158)), // Mint
    CounterColorOption("#FFBF5AF2", Color(0xFFBF5AF2)), // Violet
)

// Backwards compat alias
val CounterColors = CounterColorOptions.map { it.color }

/** Parse a hex color string to Compose Color, with fallback. */
fun String.toComposeColor(fallback: Color = AccentBlue): Color =
    try { Color(android.graphics.Color.parseColor(this)) } catch (_: Exception) { fallback }

/** Look up a CounterColorOption by hex, case-insensitive. */
fun findCounterColor(hex: String): CounterColorOption? =
    CounterColorOptions.find { it.hex.equals(hex, ignoreCase = true) }

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF636366),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF8E8E93),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFD1D1D6),
)

@Composable
fun TallyTheme(
    darkTheme: Boolean = true, // OLED dark by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
