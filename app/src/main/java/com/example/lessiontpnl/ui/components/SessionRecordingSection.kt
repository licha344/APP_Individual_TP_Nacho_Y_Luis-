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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.FatigueRiskThresholds

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
    var activationInput by remember(riskThresholds) {
        mutableStateOf(riskThresholds.activationPercent?.toString().orEmpty())
    }
    var emgInput by remember(riskThresholds) {
        mutableStateOf(riskThresholds.emgRaw?.toString().orEmpty())
    }
    var thresholdInputError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (session.isActive) Color(0xFFFF1744) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fila 1: Título de Sesión e Indicador de Grabación
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 480.dp
                val title: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sesión de Registro de Fatiga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                }

                val status: @Composable () -> Unit = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (session.isActive) Color(0xFF5C0003) else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (session.isActive) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (session.isActive) Color(0xFFFF1744) else Color(0xFF78909C),
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
                                color = if (session.isActive) Color(0xFFFF8A80) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        title()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            status()
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        title()
                        status()
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila 2: Indicadores de Fatiga Extrema (Mínimo, Actual, Máximo)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 480.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtremesCard(
                            title = "Fatiga Mínima",
                            valueText = session.minPercent?.let { "$it %" } ?: "N/D",
                            accentColor = Color(0xFF00E676),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExtremesCard(
                            title = "Activación Actual",
                            valueText = session.currentPercent?.let { "$it %" } ?: "N/D",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExtremesCard(
                            title = "Fatiga Máxima",
                            valueText = session.maxPercent?.let { "$it %" } ?: "N/D",
                            accentColor = Color(0xFFFF1744),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtremesCard(
                            title = "Fatiga Mínima",
                            valueText = session.minPercent?.let { "$it %" } ?: "N/D",
                            accentColor = Color(0xFF00E676),
                            modifier = Modifier.weight(1f)
                        )
                        ExtremesCard(
                            title = "Activación Actual",
                            valueText = session.currentPercent?.let { "$it %" } ?: "N/D",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ExtremesCard(
                            title = "Fatiga Máxima",
                            valueText = session.maxPercent?.let { "$it %" } ?: "N/D",
                            accentColor = Color(0xFFFF1744),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                text = "Origen: ${session.source?.label ?: "sin asignar"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Umbrales de riesgo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Deje un campo vacío para desactivar ese umbral. Se avisará cuando una medición supere el valor configurado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 480.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = activationInput,
                            onValueChange = { activationInput = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Fatiga (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = emgInput,
                            onValueChange = { emgInput = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Señal EMG (unidades del sensor)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = activationInput,
                            onValueChange = { activationInput = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Fatiga (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = emgInput,
                            onValueChange = { emgInput = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Señal EMG (unidades del sensor)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
            thresholdInputError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = {
                    val activation = activationInput.toIntOrNull()
                    val emg = emgInput.toIntOrNull()
                    val invalidInput = (activationInput.isNotBlank() && activation == null) ||
                        (emgInput.isNotBlank() && emg == null)
                    val thresholds = FatigueRiskThresholds(
                        activationPercent = activation,
                        emgRaw = emg
                    )
                    val validationError = when {
                        invalidInput -> "Ingrese valores numéricos válidos."
                        else -> thresholds.validationError()
                    }
                    thresholdInputError = validationError
                    if (validationError == null) onSaveRiskThresholds(thresholds)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar umbrales")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cantidad de muestras de la sesión
            Text(
                text = "Muestras registradas en la sesión: ${session.sampleCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Controles de Sesión: Iniciar / Finalizar / Reiniciar
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
                        Text(text = "Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStopSession,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Finalizar Sesión", fontWeight = FontWeight.Bold)
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

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text(text = "Confirmar Reinicio de Sesión", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Desea reiniciar la sesión actual de fatiga? Los valores mínimos y máximos registrados se restablecerán.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmResetDialog = false
                        onResetSession()
                    }
                ) {
                    Text(text = "Reiniciar", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
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
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valueText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}
