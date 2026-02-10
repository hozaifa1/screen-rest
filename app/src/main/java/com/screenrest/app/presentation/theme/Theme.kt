package com.screenrest.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.screenrest.app.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F0ED),
    onPrimaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F5F3),
    onSecondaryContainer = PrimaryVariant,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFEEF3F2),
    onSurfaceVariant = TextSecondary,
    error = Error,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Error,
    outline = Color(0xFFBCC8C7)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF003733),
    primaryContainer = CardDark,
    onPrimaryContainer = TextOnDark,
    secondary = Secondary,
    onSecondary = Color(0xFF003733),
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextOnDark,
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = SurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted,
    error = Error,
    errorContainer = ErrorDark,
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF3D5554)
)

@Composable
fun ScreenRestTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
