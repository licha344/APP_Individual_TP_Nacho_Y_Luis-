package com.example.lessiontpnl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.config.ProvisionalDataConfig

@Composable
fun TechnicalInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = "Detalles Técnicos e Integración",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "1. Decisiones Provisionales Centralizadas",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Configuración: Archivo ProvisionalDataConfig.kt\n" +
                            "• Temperatura: ${ProvisionalDataConfig.BODY_TEMP_UNIT}\n" +
                            "• Presión Arterial: ${ProvisionalDataConfig.BLOOD_PRESSURE_UNIT} (Sistólica/Diastólica)\n" +
                            "• Fatiga Muscular: Estado '${ProvisionalDataConfig.MUSCLE_FATIGUE_STATUS}' | Escala: '${ProvisionalDataConfig.MUSCLE_FATIGUE_SCALE}'\n" +
                            "• Intervalo Simulación: ${ProvisionalDataConfig.SIMULATION_INTERVAL_MS} ms",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Estrategia de Persistencia",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Se implementó un repositorio en memoria (MeasurementRepository) utilizando StateFlow y CopyOnWriteArrayList. Registra las mediciones con marcas de tiempo (System.currentTimeMillis()). Se eligió esta opción por ser la solución reactiva más ligera y segura para hilos en Kotlin sin modificar la arquitectura base.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3. Información Faltante para Dispositivo Físico",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Para conectar un hardware real (Bluetooth Low Energy / GATT) faltan:\n" +
                            "a) UUIDs de Servicio GATT y Características específicas del fabricante.\n" +
                            "b) Protocolo de parseo de tramas binarias (byte arrays).\n" +
                            "c) Permisos de ubicación/Bluetooth en AndroidManifest.xml.\n" +
                            "d) Especificación del sensor EMG para Fatiga Muscular (unidad, algoritmo de análisis espectral y escala acordada).",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Entendido",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
