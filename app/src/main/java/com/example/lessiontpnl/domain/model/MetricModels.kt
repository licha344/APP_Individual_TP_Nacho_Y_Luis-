package com.example.lessiontpnl.domain.model

import com.example.lessiontpnl.config.ProvisionalDataConfig
import java.util.UUID

/**
 * Tipos de métricas físicas monitoreadas por la aplicación.
 */
enum class MetricType(
    val displayName: String,
    val unit: String,
    val description: String
) {
    HEART_RATE(
        displayName = "Ritmo Cardíaco",
        unit = "bpm",
        description = "Frecuencia del pulso cardíaco expresada en latidos por minuto. Permite monitorear el esfuerzo físico."
    ),
    BODY_TEMPERATURE(
        displayName = "Temperatura Corporal",
        unit = ProvisionalDataConfig.BODY_TEMP_UNIT,
        description = "Temperatura corporal en grados Celsius (°C). Monitorea la termorregulación."
    ),
    BLOOD_PRESSURE(
        displayName = "Presión Arterial",
        unit = ProvisionalDataConfig.BLOOD_PRESSURE_UNIT,
        description = "Presión ejercida por la sangre en las arterias (Sistólica / Diastólica)."
    ),
    MUSCLE_FATIGUE(
        displayName = "Activación Muscular (EMG)",
        unit = ProvisionalDataConfig.MUSCLE_FATIGUE_SCALE,
        description = "Estimación del nivel de activación neuromuscular basada en lecturas del sensor EMG. No representa una medición clínica ni diagnóstico médico."
    )
}

/**
 * Modo de conexión seleccionado por el usuario.
 */
enum class ConnectorMode(val displayName: String) {
    SIMULATOR("Simulador"),
    ESP32_BLE("ESP32 BLE")
}

/**
 * Origen de la medición registrada.
 */
enum class DataSource(val label: String) {
    SIMULATED("Simulado"),
    PHYSICAL_DEVICE("ESP32 BLE"),
    DEMO_ESP32("Demo ESP32 (Fijo)")
}

/**
 * Representación tipada de los valores de las métricas.
 */
sealed class MetricValue {
    data class HeartRate(val bpm: Int) : MetricValue()
    data class BodyTemperature(val celsius: Double) : MetricValue()
    data class BloodPressure(val systolic: Int, val diastolic: Int) : MetricValue()
    data class MuscleFatigueEmg(
        val emgRaw: Int,
        val activationPercent: Int,
        val isDemoData: Boolean = false
    ) : MetricValue()
    data class MuscleFatigueUnavailable(
        val reason: String = ProvisionalDataConfig.MUSCLE_FATIGUE_UNAVAILABLE_REASON
    ) : MetricValue()
    data class Error(val message: String) : MetricValue()
    object NoData : MetricValue()
}

/**
 * Registro de medición con marca de tiempo y origen.
 */
data class MeasurementRecord(
    val id: String = UUID.randomUUID().toString(),
    val metricType: MetricType,
    val value: MetricValue,
    val timestamp: Long = System.currentTimeMillis(),
    val source: DataSource = DataSource.SIMULATED,
    val sessionId: String? = null
)

/**
 * Estados de la conexión con el dispositivo de monitoreo.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object PermissionRequired : ConnectionState()
    object BluetoothDisabled : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    val label: String
        get() = when (this) {
            is Disconnected -> "Desconectado"
            is Scanning -> "Buscando ESP32..."
            is Connecting -> "Conectando..."
            is Connected -> "Conectado"
            is PermissionRequired -> "Permiso requerido"
            is BluetoothDisabled -> "Bluetooth apagado"
            is Error -> "Error de conexión"
        }
}

/**
 * Filtro de rango de tiempo para la visualización gráfica de métricas.
 */
enum class TimeRangeFilter(val label: String) {
    ALL("Todas"),
    LAST_5_MIN("5 min"),
    LAST_15_MIN("15 min"),
    LAST_HOUR("1 hora")
}
