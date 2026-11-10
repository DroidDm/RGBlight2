package com.rgblight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val IosTypography = Typography(
    displayLarge   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 17.sp, letterSpacing = (-0.2).sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 15.sp, letterSpacing = (-0.1).sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp, letterSpacing = 0.sp)
)