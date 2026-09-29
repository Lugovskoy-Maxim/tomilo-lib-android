package ru.tomilo.lib.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private fun darkScheme(p: TomiloPalette, accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = p.onPrimary,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = p.surface2,
    onSecondary = p.text,
    secondaryContainer = p.surface2,
    onSecondaryContainer = p.text,
    background = p.bg,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surface2,
    onSurfaceVariant = p.muted,
    outline = p.border,
    outlineVariant = p.border.copy(alpha = 0.6f),
    error = p.danger,
    onError = Color.White,
    surfaceContainerHighest = p.surface3,
    surfaceContainerHigh = p.surface2,
    surfaceContainer = p.surface,
    surfaceContainerLow = Color(0xFF0D0F11),
    surfaceContainerLowest = Color(0xFF050607),
    inverseSurface = p.text,
    inverseOnSurface = p.bg,
    scrim = Color.Black,
)

private fun lightScheme(p: TomiloPalette, accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = p.onPrimary,
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = accent,
    secondary = p.surface2,
    onSecondary = p.text,
    secondaryContainer = p.surface2,
    onSecondaryContainer = p.text,
    background = p.bg,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surface2,
    onSurfaceVariant = p.muted,
    outline = p.border,
    outlineVariant = p.border.copy(alpha = 0.7f),
    error = p.danger,
    onError = Color.White,
    surfaceContainerHighest = p.surface3,
    surfaceContainerHigh = p.surface2,
    surfaceContainer = p.surface,
    surfaceContainerLow = Color(0xFFF0F1F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF303236),
    inverseOnSurface = Color(0xFFF4F5F7),
    scrim = Color.Black,
)

private val TomiloShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

val LocalTomiloAccent = compositionLocalOf { TomiloDarkPalette.primary }

/**
 * Корневая тема приложения.
 * [darkTheme] решается на уровне MainActivity по выбору пользователя
 * (системная/тёмная/светлая), поэтому здесь флаг используется как есть.
 */
@Composable
fun TomiloTheme(
    accentColor: Color? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) TomiloDarkPalette else TomiloLightPalette
    val activePrimary = accentColor ?: palette.primary
    val colors: ColorScheme = remember(palette, activePrimary) {
        if (darkTheme) darkScheme(palette, activePrimary) else lightScheme(palette, activePrimary)
    }
    val systemDensity = LocalDensity.current
    // Preserve the user's accessibility setting while capping extreme scales that
    // can make tightly composed cards and controls unusable.
    val appFontScale = systemDensity.fontScale.coerceIn(1f, 1.3f)
    val appDensity = remember(systemDensity.density, appFontScale) {
        Density(density = systemDensity.density, fontScale = appFontScale)
    }
    CompositionLocalProvider(
        LocalTomiloPalette provides palette,
        LocalTomiloAccent provides activePrimary,
        LocalDensity provides appDensity,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = TomiloTypography,
            shapes = TomiloShapes,
            content = content,
        )
    }
}
