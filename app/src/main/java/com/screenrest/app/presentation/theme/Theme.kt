package com.screenrest.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode

val LocalThemeColorPalette = staticCompositionLocalOf { getThemeColorPalette(ThemeColor.TEAL) }

private fun buildLightColorScheme(p: ThemeColorPalette) = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primaryContainerLight,
    onPrimaryContainer = p.primaryVariant,
    secondary = p.secondary,
    onSecondary = Color.White,
    secondaryContainer = p.secondaryContainerLight,
    onSecondaryContainer = p.primaryVariant,
    background = p.backgroundLight,
    onBackground = p.textPrimary,
    surface = p.surfaceLight,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceVariantLight,
    onSurfaceVariant = p.textSecondary,
    error = Error,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Error,
    outline = p.outlineLight
)

private fun buildDarkColorScheme(p: ThemeColorPalette) = darkColorScheme(
    primary = p.primaryLight,
    onPrimary = p.primaryVariant,
    primaryContainer = p.cardDark,
    onPrimaryContainer = p.textOnDark,
    secondary = p.secondary,
    onSecondary = p.primaryVariant,
    secondaryContainer = p.surfaceVariantDark,
    onSecondaryContainer = p.textOnDark,
    background = p.backgroundDark,
    onBackground = p.textOnDark,
    surface = p.surfaceDark,
    onSurface = p.textOnDark,
    surfaceVariant = p.surfaceVariantDark,
    onSurfaceVariant = p.textMuted,
    error = Error,
    errorContainer = ErrorDark,
    onErrorContainer = Color(0xFFFFB4AB),
    outline = p.outlineDark
)

@Composable
fun ScreenRestTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: ThemeColor = ThemeColor.TEAL,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val palette = getThemeColorPalette(themeColor)
    val colorScheme = if (darkTheme) buildDarkColorScheme(palette) else buildLightColorScheme(palette)

    CompositionLocalProvider(LocalThemeColorPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
