package com.kaval.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KavalColors {
    val Background = Color(0xFF0B1220)
    val Surface = Color(0xFF121C2D)
    val SurfaceLight = Color(0xFFEAF2FF)
    val Emergency = Color(0xFFD92D20)
    val Warning = Color(0xFFF59E0B)
    val Safe = Color(0xFF12B76A)
    val Trust = Color(0xFF2E90FA)
    val Muted = Color(0xFF98A2B3)
    val Text = Color(0xFFF8FAFC)
}

private val DarkScheme = darkColorScheme(
    primary = KavalColors.Trust,
    secondary = KavalColors.Safe,
    tertiary = KavalColors.Warning,
    error = KavalColors.Emergency,
    background = KavalColors.Background,
    surface = KavalColors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF111827),
    onBackground = KavalColors.Text,
    onSurface = KavalColors.Text
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF175CD3),
    secondary = Color(0xFF087443),
    tertiary = Color(0xFFB54708),
    error = KavalColors.Emergency,
    background = Color(0xFFF3F7FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

@Composable
fun KavalTheme(
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "Light" -> false
        "System Default" -> isSystemInDarkTheme()
        else -> true
    }
    val scheme: ColorScheme = if (useDark) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = androidx.compose.material3.Typography(), content = content)
}
