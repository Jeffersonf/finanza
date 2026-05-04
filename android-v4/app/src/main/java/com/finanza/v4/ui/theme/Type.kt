package com.finanza.v4.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finanza.v4.R

val DmSans = FontFamily(
    Font(R.font.dm_sans_300, FontWeight.Light),
    Font(R.font.dm_sans_400, FontWeight.Normal),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold),
    Font(R.font.dm_sans_800, FontWeight.ExtraBold)
)

val Syne = FontFamily(
    Font(R.font.syne_600, FontWeight.SemiBold),
    Font(R.font.syne_700, FontWeight.Bold),
    Font(R.font.syne_800, FontWeight.ExtraBold)
)

val FinanzaTypography = Typography(
    displayMedium = Typography().displayMedium.copy(
        fontFamily = Syne,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = Syne,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = Syne,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.sp
    )
)
