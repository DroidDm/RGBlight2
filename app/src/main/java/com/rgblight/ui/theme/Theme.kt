package com.rgblight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rgblight.data.ThemeMode

data class IosColors(
    val bg: Color, val surface: Color, val surfaceSecondary: Color,
    val textPrimary: Color, val textSecondary: Color, val separator: Color,
    val accent: Color, val red: Color, val green: Color
)

private val LightScheme = lightColorScheme(
    primary = IosAccentLight, onPrimary = Color.White,
    background = IosBgLight, onBackground = IosTextLight,
    surface = IosSurfaceLight, onSurface = IosTextLight,
    surfaceVariant = IosSurfaceSecLight, onSurfaceVariant = IosTextSecLight,
    outline = IosSeparatorLight, error = IosRedLight
)
private val DarkScheme = darkColorScheme(
    primary = IosAccentDark, onPrimary = Color.White,
    background = IosBgDark, onBackground = IosTextDark,
    surface = IosSurfaceDark, onSurface = IosTextDark,
    surfaceVariant = IosSurfaceSecDark, onSurfaceVariant = IosTextSecDark,
    outline = IosSeparatorDark, error = IosRedDark
)

@Composable
fun RgbLightTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val ios = if (dark) IosColors(
        IosBgDark, IosSurfaceDark, IosSurfaceSecDark, IosTextDark, IosTextSecDark,
        IosSeparatorDark, IosAccentDark, IosRedDark, IosGreenDark
    ) else IosColors(
        IosBgLight, IosSurfaceLight, IosSurfaceSecLight, IosTextLight, IosTextSecLight,
        IosSeparatorLight, IosAccentLight, IosRedLight, IosGreenLight
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as android.app.Activity).window
            w.statusBarColor = android.graphics.Color.TRANSPARENT
            w.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(w, view).isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalIosColors provides ios) {
        MaterialTheme(colorScheme = scheme, typography = IosTypography, content = content)
    }
}

val LocalIosColors = staticCompositionLocalOf {
    IosColors(
        IosBgLight, IosSurfaceLight, IosSurfaceSecLight, IosTextLight, IosTextSecLight,
        IosSeparatorLight, IosAccentLight, IosRedLight, IosGreenLight
    )
}

@Composable
fun iosColors(): IosColors = LocalIosColors.current