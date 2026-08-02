package ru.tomilo.lib.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = TomiloPrimary,
    onPrimary = TomiloOnPrimary,
    secondary = TomiloSurface2,
    onSecondary = TomiloText,
    background = TomiloBg,
    onBackground = TomiloText,
    surface = TomiloSurface,
    onSurface = TomiloText,
    surfaceVariant = TomiloSurface2,
    onSurfaceVariant = TomiloMuted,
    outline = TomiloBorder,
    error = TomiloDanger,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D63D8),
    onPrimary = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1A1D24),
    surface = Color.White,
    onSurface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF5C6578),
    outline = Color(0xFFD5D9E2),
    error = TomiloDanger,
)

@Composable
fun TomiloTheme(
    darkTheme: Boolean = true, // default dark — matches site reading focus
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TomiloTypography,
        content = content,
    )
}
