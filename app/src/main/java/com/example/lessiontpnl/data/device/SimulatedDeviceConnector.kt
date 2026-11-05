package com.example.lessiontpnl.data.device

import com.example.lessiontpnl.config.ProvisionalDataConfig
import com.example.lessiontpnl.domain.device.DeviceConnector
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Fuente de datos simulados para pruebas y desarrollo.
 * Genera mediciones periódicas para Ritmo Cardíaco, Temperatura Corporal, Presión Arterial
 * y Activación Muscular EMG. Alimenta el mismo flujo de procesamiento y gráficos de caja y bigotes
 * que el dispositivo real, etiquetando los registros explícitamente como "Simulado".
 */
class SimulatedDeviceConnector(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) : DeviceConnector {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _measurementFlow = MutableSharedFlow<MeasurementRecord>()
    override val measurementFlow: SharedFlow<MeasurementRecord> = _measurementFlow.asSharedFlow()

    private var simulationJob: Job? = null
    private var stepCount = 0

    override suspend fun connect() {
        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
            return
        }

        _connectionState.value = ConnectionState.Connecting
        delay(ProvisionalDataConfig.CONNECTION_DELAY_MS)
        _connectionState.value = ConnectionState.Connected

        startSimulationLoop()
    }

    override suspend fun disconnect() {
        simulationJob?.cancel()
        simulationJob = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun triggerSimulatedError(message: String) {
        simulationJob?.cancel()
        simulationJob = null
        _connectionState.value = ConnectionState.Error(message)
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                stepCount++

                // 1. Ritmo Cardíaco simulado (oscilación suave de 70 a 135 bpm)
                val baseHeartRate = 75 + (stepCount % 20) * 3 + Random.nextInt(-2, 3)
                emitRecord(
                    MetricType.HEART_RATE,
                    MetricValue.HeartRate(baseHeartRate.coerceIn(60, 180))
                )

                // 2. Temperatura Corporal simulada (oscilación de 36.3°C a 37.4°C)
                val baseTemp = 36.4 + (stepCount % 10) * 0.1 + Random.nextDouble(-0.05, 0.05)
                val formattedTemp = (Math.round(baseTemp * 10.0) / 10.0).coerceIn(35.5, 40.0)
                emitRecord(
                    MetricType.BODY_TEMPERATURE,
                    MetricValue.BodyTemperature(formattedTemp)
                )

                // 3. Presión Arterial simulada (Sistólica 115-130, Diastólica 75-85 mmHg)
                val systolic = 115 + (stepCount % 8) * 2 + Random.nextInt(-1, 2)
                val diastolic = 75 + (stepCount % 6) * 2 + Random.nextInt(-1, 2)
                emitRecord(
                    MetricType.BLOOD_PRESSURE,
                    MetricValue.BloodPressure(systolic, diastolic)
                )

                // 4. Activación Muscular EMG simulada (Mismo flujo que el sensor real, marcado como Simulado)
                val baseEmg = 600 + (stepCount % 15) * 120 + Random.nextInt(-50, 50)
                val fatiguePercent = (baseEmg / 30).coerceIn(10, 95)
                emitRecord(
                    MetricType.MUSCLE_FATIGUE,
                    MetricValue.MuscleFatigueEmg(
                        emgRaw = baseEmg,
                        activationPercent = fatiguePercent,
                        isDemoData = true
                    )
                )

                delay(ProvisionalDataConfig.SIMULATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun emitRecord(metricType: MetricType, value: MetricValue) {
        val record = MeasurementRecord(
            metricType = metricType,
            value = value,
            timestamp = System.currentTimeMillis(),
            source = DataSource.SIMULATED
        )
        _measurementFlow.emit(record)
    }
}
