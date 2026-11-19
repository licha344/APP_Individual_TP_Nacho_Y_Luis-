package com.example.lessiontpnl.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ArialFontFamily = FontFamily(Typeface.create("Arial", Typeface.NORMAL))

private fun TextStyle.withArial() = copy(fontFamily = ArialFontFamily)

private val DefaultTypography = Typography()

// Keep Material's existing sizes and weights while applying one font family globally.
val Typography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.withArial(),
    displayMedium = DefaultTypography.displayMedium.withArial(),
    displaySmall = DefaultTypography.displaySmall.withArial(),
    headlineLarge = DefaultTypography.headlineLarge.withArial(),
    headlineMedium = DefaultTypography.headlineMedium.withArial(),
    headlineSmall = DefaultTypography.headlineSmall.withArial(),
    titleLarge = DefaultTypography.titleLarge.withArial(),
    titleMedium = DefaultTypography.titleMedium.withArial(),
    titleSmall = DefaultTypography.titleSmall.withArial(),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontFamily = ArialFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.withArial(),
    bodySmall = DefaultTypography.bodySmall.withArial(),
    labelLarge = DefaultTypography.labelLarge.withArial(),
    labelMedium = DefaultTypography.labelMedium.withArial(),
    labelSmall = DefaultTypography.labelSmall.withArial()
)