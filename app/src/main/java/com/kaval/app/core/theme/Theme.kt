package com.kaval.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kaval.app.domain.model.AppearanceSettings

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

private val HighContrastScheme = darkColorScheme(
    primary = Color(0xFF7CC7FF),
    secondary = Color(0xFF32D583),
    tertiary = Color(0xFFFFC24A),
    error = Color(0xFFFF453A),
    background = Color.Black,
    surface = Color(0xFF101010),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun KavalTheme(
    settings: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val useDark = when (settings.themeMode) {
        "Light" -> false
        "System Default" -> isSystemInDarkTheme()
        else -> true
    }
    val scheme: ColorScheme = when (settings.themeMode) {
        "Emergency High Contrast" -> HighContrastScheme
        "Light" -> LightScheme
        else -> if (useDark) DarkScheme else LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = androidx.compose.material3.Typography(), content = content)
}
