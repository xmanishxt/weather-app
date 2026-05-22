package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PolarCyan,
    onPrimary = BlackBg,
    primaryContainer = DarkSurfaceCard,
    onPrimaryContainer = PureWhite,
    secondary = IceBlue,
    onSecondary = BlackBg,
    tertiary = SolarGold,
    onTertiary = BlackBg,
    background = BlackBg,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = WarmText,
    outline = GrayBorder,
    error = PyroRed,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme per client request
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium Black alignment
    content: @Composable () -> Unit,
) {
    // Keep it explicitly aligned to our custom high-fidelity Minimalist Black Theme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
