package com.finanza.v4.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FinanzaGreen = Color(0xFFC8F55A)
val FinanzaMint = Color(0xFF5AF5C8)
val FinanzaPurple = Color(0xFFA78BFA)
val FinanzaRed = Color(0xFFF5705A)
val FinanzaAmber = Color(0xFFF5C85A)
val FinanzaBg = Color(0xFF08090D)
val FinanzaBg2 = Color(0xFF0D0F16)
val FinanzaSurface = Color(0xFF12151E)
val FinanzaSurface2 = Color(0xFF1C2030)
val FinanzaBorder = Color(0x17FFFFFF)
val FinanzaText = Color(0xFFF0F3FF)
val FinanzaText2 = Color(0xC7F0F3FF)
val FinanzaMuted = Color(0x7AF0F3FF)
val FinanzaLightBg = Color(0xFFF4F7F2)
val FinanzaLightSurface = Color(0xDDFDFEF8)
val FinanzaLightSurface2 = Color(0xFFE9F0E4)
val FinanzaLightText = Color(0xFF182016)
val FinanzaLightMuted = Color(0xA3182016)

private val FinanzaDarkColors: ColorScheme = darkColorScheme(
    primary = FinanzaGreen,
    secondary = FinanzaMint,
    tertiary = FinanzaPurple,
    background = FinanzaBg,
    surface = FinanzaSurface,
    surfaceVariant = FinanzaSurface2,
    surfaceContainer = FinanzaSurface,
    surfaceContainerHigh = FinanzaSurface2,
    surfaceContainerHighest = FinanzaSurface2,
    outline = FinanzaBorder,
    outlineVariant = FinanzaBorder.copy(alpha = .55f),
    onPrimary = FinanzaBg,
    onSecondary = FinanzaBg,
    onTertiary = FinanzaBg,
    onBackground = FinanzaText,
    onSurface = FinanzaText,
    onSurfaceVariant = FinanzaMuted,
    error = FinanzaRed,
    onError = FinanzaBg
)

@Composable
fun FinanzaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF4F7D00),
        secondary = Color(0xFF007B62),
        tertiary = Color(0xFF6D55B7),
        background = FinanzaLightBg,
        surface = FinanzaLightSurface,
        surfaceVariant = FinanzaLightSurface2,
        surfaceContainer = FinanzaLightSurface,
        surfaceContainerHigh = Color(0xFFFDFEF8),
        surfaceContainerHighest = Color(0xFFFFFFFF),
        outline = Color(0x24273322),
        outlineVariant = Color(0x18273322),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = FinanzaLightText,
        onSurface = FinanzaLightText,
        onSurfaceVariant = FinanzaLightMuted,
        error = Color(0xFFB82E1D),
        onError = Color.White
    )
    MaterialTheme(
        colorScheme = if (darkTheme) FinanzaDarkColors else lightColors,
        typography = FinanzaTypography,
        content = content
    )
}
