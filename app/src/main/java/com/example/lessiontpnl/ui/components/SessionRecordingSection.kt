package com.example.lessiontpnl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.domain.model.FatigueRiskThresholds
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.ui.theme.PaletteDeepPurple
import com.example.lessiontpnl.ui.theme.PaletteLime
import com.example.lessiontpnl.ui.theme.PaletteMutedPurple
import com.example.lessiontpnl.ui.theme.PalettePurple

/**
 * Sección simplificada de registro de fatiga muscular (Mínima y Máxima) en tiempo real
 * alimentada por las mediciones recibidas desde la placa ESP32 y el sensor EMG V3.
 */
@Composable
fun SessionRecordingSection(
    session: FatigueSession,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onResetSession: () -> Unit,
    isConnected: Boolean,
    riskThresholds: FatigueRiskThresholds,
    onSaveRiskThresholds: (FatigueRiskThresholds) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmResetDialog by remember { mutableStateOf(false) }
    var showThresholdsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (session.isActive) PaletteLime else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Encabezado: Título, Estado y Botón de Ajustes de Umbrales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Registro EMG V3 - Placa ESP32",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (session.isActive) PalettePurple else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (session.isActive) PaletteLime else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (session.isActive) PaletteLime else PaletteMutedPurple,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    session.isActive && isConnected -> "GRABANDO"
                                    session.isActive -> "PAUSADA"
                                    else -> "INACTIVA"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PaletteLime
                            )
                        }
                    }

                    IconButton(onClick = { showThresholdsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar Umbrales",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjetas de Métricas de Fatiga (Mínima, Actual en Vivo, Máxima)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 480.dp
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtremesCard(
                            title = "Fatiga Mínima",
                            valueText = session.minPercent?.let { "$it %" } ?: "N/D",
                            accentColor = PaletteLime,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExtremesCard(
                            title = "Activación Actual (EMG V3)",
                            valueText = session.currentPercent?.let { "$it %" } ?: "N/D",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExtremesCard(
                            title = "Fatiga Máxima",
                            valueText = session.maxPercent?.let { "$it %" } ?: "N/D",
                            accentColor = PaletteLime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtremesCard(
                            title = "Fatiga Mínima",
                            valueText = session.minPercent?.let { "$it %" } ?: "N/D",
                            accentColor = PaletteLime,
                            modifier = Modifier.weight(1f)
                        )
                        ExtremesCard(
                            title = "Activación Actual (EMG V3)",
                            valueText = session.currentPercent?.let { "$it %" } ?: "N/D",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ExtremesCard(
                            title = "Fatiga Máxima",
                            valueText = session.maxPercent?.let { "$it %" } ?: "N/D",
                            accentColor = PaletteLime,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info de origen y lecturas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Origen: Placa ESP32 | Sensor EMG V3",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Muestras: ${session.sampleCount}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botones Principales de Control: Iniciar / Detener / Reiniciar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!session.isActive) {
                    Button(
                        onClick = onStartSession,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Iniciar Registro ESP32 EMG V3", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStopSession,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaletteLime,
                            contentColor = PaletteDeepPurple
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Finalizar y Guardar Sesión", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showConfirmResetDialog = true },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(text = "Reiniciar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    // Diálogo Modal Limpio para Configuración de Umbrales de Riesgo
    if (showThresholdsDialog) {
        ThresholdsDialog(
            riskThresholds = riskThresholds,
            onSave = { thresholds ->
                onSaveRiskThresholds(thresholds)
                showThresholdsDialog = false
            },
            onDismiss = { showThresholdsDialog = false }
        )
    }

    // Diálogo Modal para Confirmar Reinicio
    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text(text = "Confirmar Reinicio de Sesión", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Desea reiniciar los parámetros de fatiga registrados? Los valores Mínimo y Máximo se restablecerán para una nueva medición.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmResetDialog = false
                        onResetSession()
                    }
                ) {
                    Text(text = "Reiniciar", color = PaletteLime, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ExtremesCard(
    title: String,
    valueText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valueText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ThresholdsDialog(
    riskThresholds: FatigueRiskThresholds,
    onSave: (FatigueRiskThresholds) -> Unit,
    onDismiss: () -> Unit
) {
    var activationInput by remember(riskThresholds) {
        mutableStateOf(riskThresholds.activationPercent?.toString().orEmpty())
    }
    var emgInput by remember(riskThresholds) {
        mutableStateOf(riskThresholds.emgRaw?.toString().orEmpty())
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Configurar Umbrales de Riesgo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Configure los límites de alerta para recibir una notificación cuando la señal EMG V3 o fatiga los supere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = activationInput,
                    onValueChange = { activationInput = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fatiga Máxima (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = emgInput,
                    onValueChange = { emgInput = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Señal EMG Raw Máxima") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activation = activationInput.toIntOrNull()
                    val emg = emgInput.toIntOrNull()
                    val thresholds = FatigueRiskThresholds(
                        activationPercent = activation,
                        emgRaw = emg
                    )
                    val validationError = thresholds.validationError()
                    if (validationError != null) {
                        errorMsg = validationError
                    } else {
                        onSave(thresholds)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
