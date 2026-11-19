package com.example.lessiontpnl.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Familia tipográfica Arial con fallback automático a SansSerif del sistema Android.
 */
val ArialFontFamily: FontFamily = try {
    FontFamily(Typeface.create("Arial", Typeface.NORMAL))
} catch (_: Exception) {
    FontFamily.SansSerif
}

private fun TextStyle.withArial(): TextStyle = this.copy(fontFamily = ArialFontFamily)

private val DefaultTypography = Typography()

/**
 * Configuración global de la tipografía Arial en Material3 para toda la aplicación LessionTPNL.
 * Conserva intactos los tamaños, pesos (Bold, Medium, Normal, ExtraBold) y espaciados originales.
 */
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
