package com.example.lessiontpnl.domain.device

import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.MeasurementRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de abstracción para la conexión con el dispositivo de monitoreo.
 * Permite desacoplar el origen de datos (simulador vs. dispositivo físico por Bluetooth/GATT).
 */
interface DeviceConnector {
    val connectionState: StateFlow<ConnectionState>
    val measurementFlow: Flow<MeasurementRecord>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun triggerSimulatedError(message: String = "Simulación de error en la comunicación de sensores")
}
