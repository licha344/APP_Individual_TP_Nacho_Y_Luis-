package com.example.lessiontpnl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema global de la paleta LessionTPNL.
 */
private val PaletteColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = NeonOnPrimary,
    primaryContainer = NeonPrimaryContainer,
    onPrimaryContainer = NeonOnPrimaryContainer,
    secondary = NeonSecondary,
    onSecondary = NeonOnSecondary,
    secondaryContainer = NeonSecondaryContainer,
    onSecondaryContainer = NeonOnSecondaryContainer,
    tertiary = NeonTertiary,
    onTertiary = NeonOnTertiary,
    tertiaryContainer = NeonTertiaryContainer,
    onTertiaryContainer = NeonOnTertiaryContainer,
    error = NeonError,
    onError = NeonOnError,
    errorContainer = NeonErrorContainer,
    onErrorContainer = NeonOnErrorContainer,
    background = NeonBackground,
    onBackground = NeonOnBackground,
    surface = NeonSurface,
    onSurface = NeonOnSurface,
    surfaceVariant = NeonSurfaceVariant,
    onSurfaceVariant = NeonOnSurfaceVariant,
    outline = NeonOutline,
    inverseSurface = NeonOnBackground,
    inverseOnSurface = NeonBackground,
    inversePrimary = NeonSecondary,
    surfaceTint = NeonPrimary
)

@Composable
fun LessionTPNLTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PaletteColorScheme,
        typography = Typography,
        content = content
    )
}
