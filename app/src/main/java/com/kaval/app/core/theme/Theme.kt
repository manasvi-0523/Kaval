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
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val SurfaceLight = Color(0xFFF7F8FA)
    val Emergency = Color(0xFFD32F2F)
    val Warning = Color(0xFFFF8F00)
    val Safe = Color(0xFF00897B)
    val Trust = Color(0xFF1565C0)
    val Muted = Color(0xFFB0B0B0)
    val Text = Color(0xFFFFFFFF)
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
    primary = KavalColors.Trust,
    secondary = KavalColors.Safe,
    tertiary = KavalColors.Warning,
    error = KavalColors.Emergency,
    background = Color(0xFFFFFFFF),
    surface = KavalColors.SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF212121),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
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
