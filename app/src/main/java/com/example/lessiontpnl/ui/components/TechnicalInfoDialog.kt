package com.example.lessiontpnl.ui.components

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
                text = "Detalles del Firmware ESP32 LESSION",
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
                    text = "1. Configuración Periférico BLE GATT",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Nombre anunciado: ${ProvisionalDataConfig.ESP32_DEVICE_NAME}\n" +
                            "• Servicio GATT (UART): ${ProvisionalDataConfig.ESP32_SERVICE_UUID}\n" +
                            "• Característica TX (ESP32->App): ${ProvisionalDataConfig.ESP32_CHARACTERISTIC_UUID} (READ + NOTIFY)\n" +
                            "• Característica RX (App->ESP32): ${ProvisionalDataConfig.ESP32_RX_CHARACTERISTIC_UUID} (WRITE)\n" +
                            "• Descriptor CCCD: ${ProvisionalDataConfig.CCCD_DESCRIPTOR_UUID}",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Protocolo de Comunicación (Versión 1)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Frecuencia: 50 Hz (notificación cada 20 ms)\n" +
                            "• Formato exacto: t_ms,adc\\n (Texto ASCII sin JSON)\n" +
                            "• t_ms: milisegundos desde el arranque del ESP32 (uint32_t)\n" +
                            "• adc: 0 a 4095 (12 bits ADC, promedio de 4 lecturas de A0)\n" +
                            "• Ejemplo trama: 12340,2048\\n",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3. Comandos Opcionales (App -> ESP32 RX)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• START: Reanuda transmisión de notificaciones.\n" +
                            "• STOP: Pausa la transmisión (LED azul en ESP32).",
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
