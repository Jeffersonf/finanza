package com.finanza.v4.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
fun FinanzaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinanzaDarkColors,
        typography = FinanzaTypography,
        content = content
    )
}
