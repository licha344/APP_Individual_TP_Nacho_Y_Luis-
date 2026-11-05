package com.example.lessiontpnl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de Color Neón Azul y Negro.
 */
private val NeonDarkColorScheme = darkColorScheme(
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
    outline = NeonOutline
)

@Composable
fun LessionTPNLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Aplicar el tema oscuro Neón Azul y Negro como el tema principal estilizado
    MaterialTheme(
        colorScheme = NeonDarkColorScheme,
        typography = Typography,
        content = content
    )
}
