package com.example.lessiontpnl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.ConnectorMode
import com.example.lessiontpnl.ui.theme.NeonGlowBorder

@Composable
fun ConnectionHeader(
    connectorMode: ConnectorMode,
    connectionState: ConnectionState,
    onSelectMode: (ConnectorMode) -> Unit,
    onOpenScanner: () -> Unit,
    onToggleConnection: () -> Unit,
    onRequestPermissions: () -> Unit,
    onTriggerError: () -> Unit,
    onShowTechnicalInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> Color(0xFF00E676) // Verde Neón
            is ConnectionState.Connecting, is ConnectionState.Scanning -> Color(0xFFFFAB00) // Ámbar Neón
            is ConnectionState.Disconnected -> Color(0xFF78909C) // Gris Azulado
            is ConnectionState.PermissionRequired, is ConnectionState.BluetoothDisabled, is ConnectionState.Error -> Color(0xFFFF5252) // Coral Neón
        },
        label = "statusColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonGlowBorder.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fila 1: Título y Selector de Modo (ESP32 BLE vs Modo Demo/Simulador)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 500.dp
                val titleAndStatus: @Composable () -> Unit = {
                    Column {
                    Text(
                        text = "Monitoreo Deportivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estado: ${connectionState.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
                }
                val modeSelector: @Composable () -> Unit = {
                    Row(
                        modifier = if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ConnectorMode.entries.forEach { mode ->
                            FilterChip(
                                selected = (connectorMode == mode),
                                onClick = { onSelectMode(mode) },
                                label = { Text(text = mode.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        titleAndStatus()
                        modeSelector()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        titleAndStatus()
                        modeSelector()
                    }
                }
            }

            // Alerta de estado de error o permisos requeridos
            if (connectionState is ConnectionState.Error || connectionState is ConnectionState.PermissionRequired || connectionState is ConnectionState.BluetoothDisabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF380000),
                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.PermissionRequired -> "Se requieren permisos de Bluetooth para buscar el ESP32; Android antiguo también puede solicitar ubicación."
                                is ConnectionState.BluetoothDisabled -> "El adaptador Bluetooth está apagado. Enciéndalo para buscar sensores."
                                is ConnectionState.Error -> connectionState.message
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 480.dp
                Row(
                    modifier = Modifier.fillMaxWidth().then(
                        if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                val isConnected = connectionState is ConnectionState.Connected
                val isConnectingOrScanning = connectionState is ConnectionState.Connecting || connectionState is ConnectionState.Scanning

                if (connectionState is ConnectionState.PermissionRequired) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = if (compact) Modifier.widthIn(min = 200.dp) else Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Conceder Permisos", fontWeight = FontWeight.Bold)
                    }
                } else if (connectorMode == ConnectorMode.ESP32_BLE) {
                    Button(
                        onClick = onOpenScanner,
                        modifier = if (compact) Modifier.widthIn(min = 200.dp) else Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isConnected) "Cambiar Dispositivo BLE" else "Buscar Dispositivos BLE", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onToggleConnection,
                        modifier = if (compact) Modifier.widthIn(min = 200.dp) else Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected || isConnectingOrScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isConnected || isConnectingOrScanning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isConnectingOrScanning
                    ) {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Desconectar Demo"
                                is ConnectionState.Connecting -> "Conectando..."
                                else -> "Iniciar Modo Demo"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (connectorMode == ConnectorMode.SIMULATOR) {
                    OutlinedButton(
                        onClick = onTriggerError,
                        enabled = isConnected,
                        border = BorderStroke(1.dp, if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simular Error",
                            modifier = Modifier.size(16.dp),
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Probar Error",
                            fontSize = 12.sp,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onShowTechnicalInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detalles Técnicos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                }
            }
        }
    }
}
