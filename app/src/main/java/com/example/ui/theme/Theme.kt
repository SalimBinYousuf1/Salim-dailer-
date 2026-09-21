package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalLiquidGlassOpacity = staticCompositionLocalOf { 0.75f }
val LocalIsTrueBlack = staticCompositionLocalOf { true }

private val OledDarkColorScheme = darkColorScheme(
    primary = AppleGreen,
    onPrimary = Color.White,
    primaryContainer = AppleGreenDark,
    onPrimaryContainer = Color.White,
    secondary = AppleBlue,
    onSecondary = Color.White,
    error = AppleRed,
    onError = Color.White,
    background = OledBlack,
    onBackground = TextPrimaryDark,
    surface = OledBlack,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurface1,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkSurface3
)

private val LightColorScheme = lightColorScheme(
    primary = AppleGreen,
    onPrimary = Color.White,
    primaryContainer = AppleGreen,
    onPrimaryContainer = Color.White,
    secondary = AppleBlueLight,
    onSecondary = Color.White,
    error = AppleRed,
    onError = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = TextPrimaryLight,
    surface = Color(0xFFFFFFFF),
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFE5E5EA),
    outlineVariant = Color(0xFFF2F2F7)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Apple White as requested
    trueBlack: Boolean = false,
    glassOpacity: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        if (trueBlack) OledDarkColorScheme else OledDarkColorScheme.copy(
            background = Color(0xFF101216),
            surface = Color(0xFF14171E)
        )
    } else {
        LightColorScheme
    }

    CompositionLocalProvider(
        LocalLiquidGlassOpacity provides glassOpacity,
        LocalIsTrueBlack provides trueBlack
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

