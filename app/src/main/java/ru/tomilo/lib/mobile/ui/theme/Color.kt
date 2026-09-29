package ru.tomilo.lib.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Графитовая дизайн-система Tomilo: тёмная тема — настоящий чёрный фон и живой
 * коралловый акцент; светлая — пара по токенам Figma (light-* / *-dark).
 * Значения берутся из [LocalTomiloPalette], поэтому токены (TomiloBg и т.д.)
 * можно читать только из Composable-контекста.
 */
class TomiloPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val primary: Color,
    val primaryDim: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val danger: Color,
    val premium: Color,
    val success: Color,
    val info: Color,
    val pink: Color,
    val glass: Color,
    val glassBorder: Color,
    val paper: Color,
    val ink: Color,
)

val TomiloDarkPalette = TomiloPalette(
    bg = Color(0xFF070809),
    surface = Color(0xFF111315),
    surface2 = Color(0xFF181A1D),
    surface3 = Color(0xFF222529),
    primary = Color(0xFFFF5F57),
    primaryDim = Color(0xFFD94B45),
    primarySoft = Color(0xFFFFC9C5),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFFF5F5F7),
    muted = Color(0xFFA2A3A8),
    border = Color(0xFF2B2E32),
    danger = Color(0xFFFF5F57),
    premium = Color(0xFFE8C07A),
    success = Color(0xFF7CB98A),
    info = Color(0xFFD4A574),
    pink = Color(0xFFE8A0B0),
    glass = Color(0xF2111315),
    glassBorder = Color(0x24FFFFFF),
    paper = Color(0xFFF4F4F6),
    ink = Color(0xFF121315),
)

/** Светлая тема по токенам Figma: light-950/900/850/800/750/400/100 + *-dark. */
val TomiloLightPalette = TomiloPalette(
    bg = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAECEF),
    surface3 = Color(0xFFDFE2E7),
    primary = Color(0xFFE04841),
    primaryDim = Color(0xFFC93A34),
    primarySoft = Color(0xFFF6CFCB),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF1A1C20),
    muted = Color(0xFF686E7C),
    border = Color(0xFFCDD2DA),
    danger = Color(0xFFE04841),
    premium = Color(0xFFC2964A),
    success = Color(0xFF3B7E4C),
    info = Color(0xFF2B477D),
    pink = Color(0xFFC27A8C),
    glass = Color(0xF2FFFFFF),
    glassBorder = Color(0x24000000),
    paper = Color(0xFFF4F4F6),
    ink = Color(0xFF121315),
)

val LocalTomiloPalette = staticCompositionLocalOf { TomiloDarkPalette }

val TomiloBg: Color @Composable get() = LocalTomiloPalette.current.bg
val TomiloSurface: Color @Composable get() = LocalTomiloPalette.current.surface
val TomiloSurface2: Color @Composable get() = LocalTomiloPalette.current.surface2
val TomiloSurface3: Color @Composable get() = LocalTomiloPalette.current.surface3
val TomiloPrimary: Color @Composable get() = LocalTomiloPalette.current.primary
val TomiloPrimaryDim: Color @Composable get() = LocalTomiloPalette.current.primaryDim
val TomiloPrimarySoft: Color @Composable get() = LocalTomiloPalette.current.primarySoft
val TomiloOnPrimary: Color @Composable get() = LocalTomiloPalette.current.onPrimary
val TomiloText: Color @Composable get() = LocalTomiloPalette.current.text
val TomiloMuted: Color @Composable get() = LocalTomiloPalette.current.muted
val TomiloBorder: Color @Composable get() = LocalTomiloPalette.current.border
val TomiloDanger: Color @Composable get() = LocalTomiloPalette.current.danger
val TomiloPremium: Color @Composable get() = LocalTomiloPalette.current.premium
val TomiloSuccess: Color @Composable get() = LocalTomiloPalette.current.success
val TomiloInfo: Color @Composable get() = LocalTomiloPalette.current.info
val TomiloPink: Color @Composable get() = LocalTomiloPalette.current.pink
val TomiloGlass: Color @Composable get() = LocalTomiloPalette.current.glass
val TomiloGlassBorder: Color @Composable get() = LocalTomiloPalette.current.glassBorder
val TomiloPaper: Color @Composable get() = LocalTomiloPalette.current.paper
val TomiloInk: Color @Composable get() = LocalTomiloPalette.current.ink

// Устаревшие токены активной пилюли (оставлены для совместимости, не темизируются).
val TomiloActivePill = Color(0x33E85D4C)
val TomiloActiveBorder = Color(0x66E85D4C)
