package ru.tomilo.lib.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = TomiloPrimary,
    onPrimary = TomiloOnPrimary,
    primaryContainer = TomiloPrimaryDim,
    onPrimaryContainer = TomiloOnPrimary,
    secondary = TomiloSurface2,
    onSecondary = TomiloText,
    secondaryContainer = TomiloSurface2,
    onSecondaryContainer = TomiloText,
    background = TomiloBg,
    onBackground = TomiloText,
    surface = TomiloSurface,
    onSurface = TomiloText,
    surfaceVariant = TomiloSurface2,
    onSurfaceVariant = TomiloMuted,
    outline = TomiloBorder,
    outlineVariant = TomiloBorder.copy(alpha = 0.6f),
    error = TomiloDanger,
    onError = Color.White,
    surfaceContainerHighest = TomiloSurface3,
    surfaceContainerHigh = TomiloSurface2,
    surfaceContainer = TomiloSurface,
    surfaceContainerLow = Color(0xFF0D0F11),
    surfaceContainerLowest = Color(0xFF050607),
    inverseSurface = TomiloText,
    inverseOnSurface = TomiloBg,
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5FD9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF0A1B5C),
    background = Color(0xFFF4F5F8),
    onBackground = Color(0xFF12141A),
    surface = Color.White,
    onSurface = Color(0xFF12141A),
    surfaceVariant = Color(0xFFEBEDF2),
    onSurfaceVariant = Color(0xFF5C6578),
    outline = Color(0xFFD0D4DE),
    error = TomiloDanger,
)

private val TomiloShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun TomiloTheme(
    darkTheme: Boolean = true, // Ink: тёмная читалка по умолчанию
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TomiloTypography,
        shapes = TomiloShapes,
        content = content,
    )
}
