package com.example.lessiontpnl.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lessiontpnl.data.device.BleDeviceConnector
import com.example.lessiontpnl.data.device.SimulatedDeviceConnector
import com.example.lessiontpnl.data.repository.MeasurementRepository
import com.example.lessiontpnl.domain.device.DeviceConnector
import com.example.lessiontpnl.domain.model.BleDiscoveredDevice
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.ConnectorMode
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.FatigueSessionPlot
import com.example.lessiontpnl.domain.model.FatigueRiskThresholds
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import com.example.lessiontpnl.domain.model.TimeRangeFilter
import com.example.lessiontpnl.util.BoxPlotCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Información formateada de presentación para cada tarjeta de métrica.
 */
data class MetricDisplayInfo(
    val type: MetricType,
    val valueText: String,
    val unitText: String,
    val statusText: String,
    val descriptionText: String,
    val sourceText: String,
    val timestampText: String,
    val isAvailable: Boolean,
    val isError: Boolean
)

/**
 * Estado general de la pantalla principal.
 */
data class MainUiState(
    val connectorMode: ConnectorMode = ConnectorMode.ESP32_BLE,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val metricsMap: Map<MetricType, MetricDisplayInfo> = emptyMap(),
    val selectedChartMetric: MetricType = MetricType.MUSCLE_FATIGUE,
    val selectedTimeRange: TimeRangeFilter = TimeRangeFilter.ALL,
    val chartRecords: List<MeasurementRecord> = emptyList(),
    val historyRecords: List<MeasurementRecord> = emptyList(),
    val historyError: String? = null,
    val sessionActionError: String? = null,
    val riskThresholds: FatigueRiskThresholds = FatigueRiskThresholds(),
    val showTechnicalDialog: Boolean = false,
    val showScannerDialog: Boolean = false,
    val scannedDevices: List<BleDiscoveredDevice> = emptyList(),
    val activeSession: FatigueSession = FatigueSession("default", System.currentTimeMillis()),
    val sessionPlots: List<FatigueSessionPlot> = emptyList()
)

/**
 * ViewModel principal enfocado en recepción BLE de señal EMG real, gestión de sesiones
 * de grabación con valores mínimos/máximos y cálculo del gráfico de caja y bigotes.
 */
class MainViewModel(
    context: Context? = null,
    val repository: MeasurementRepository = MeasurementRepository(context),
    initialConnector: DeviceConnector? = null
) : ViewModel() {

    private val appContext = context?.applicationContext
    private val _connectorMode = MutableStateFlow(
        if (initialConnector is SimulatedDeviceConnector) ConnectorMode.SIMULATOR else ConnectorMode.ESP32_BLE
    )

    private var activeDeviceConnector: DeviceConnector = initialConnector ?: if (appContext != null) {
        BleDeviceConnector(appContext)
    } else {
        SimulatedDeviceConnector()
    }

    private val _selectedChartMetric = MutableStateFlow(MetricType.MUSCLE_FATIGUE)
    private val _selectedTimeRange = MutableStateFlow(TimeRangeFilter.ALL)
    private val _showTechnicalDialog = MutableStateFlow(false)
    private val _showScannerDialog = MutableStateFlow(false)
    private val _sessionActionError = MutableStateFlow<String?>(null)
    private val _riskThresholds = MutableStateFlow(loadRiskThresholds())
    private val _riskAlerts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val riskAlerts: SharedFlow<String> = _riskAlerts.asSharedFlow()
    private val activeRiskParameters = mutableSetOf<String>()

    private val _activeSession = MutableStateFlow(newEmptySession())
    private var sessionStartedByUser = false

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var connectorJobs = mutableListOf<Job>()

    init {
        bindConnector(activeDeviceConnector)
        viewModelScope.launch {
            repository.isInitialized.first { it }
            if (!sessionStartedByUser) {
                _activeSession.value = repository.fatigueSessions.value
                    .firstOrNull { it.isActive }
                    ?: repository.fatigueSessions.value.firstOrNull()
                    ?: newEmptySession()
            }
        }
    }

    fun selectConnectorMode(mode: ConnectorMode, context: Context) {
        if (_connectorMode.value == mode) return

        if (_activeSession.value.isActive) stopRecordingSession()
        viewModelScope.launch {
            activeDeviceConnector.disconnect()

            val newConnector: DeviceConnector = when (mode) {
                ConnectorMode.SIMULATOR -> SimulatedDeviceConnector()
                ConnectorMode.ESP32_BLE -> BleDeviceConnector(context.applicationContext)
            }

            _connectorMode.value = mode
            activeDeviceConnector = newConnector
            bindConnector(newConnector)
        }
    }

    fun openScannerDialog(context: Context) {
        if (activeDeviceConnector is BleDeviceConnector) {
            (activeDeviceConnector as BleDeviceConnector).startInteractiveScan()
        } else {
            val bleConnector = BleDeviceConnector(context.applicationContext)
            _connectorMode.value = ConnectorMode.ESP32_BLE
            activeDeviceConnector = bleConnector
            bindConnector(bleConnector)
            bleConnector.startInteractiveScan()
        }
        _showScannerDialog.value = true
    }

    fun closeScannerDialog() {
        if (activeDeviceConnector is BleDeviceConnector) {
            (activeDeviceConnector as BleDeviceConnector).cancelInteractiveScan()
        }
        _showScannerDialog.value = false
    }

    fun connectToDeviceAddress(macAddress: String) {
        if (activeDeviceConnector is BleDeviceConnector) {
            (activeDeviceConnector as BleDeviceConnector).connectToAddress(macAddress)
        }
    }

    fun saveRiskThresholds(thresholds: FatigueRiskThresholds): String? {
        val error = thresholds.validationError()
        if (error != null) {
            _sessionActionError.value = error
            return error
        }
        _riskThresholds.value = thresholds
        appContext?.getSharedPreferences(RISK_PREFERENCES, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_ACTIVATION, thresholds.activationPercent ?: DISABLED_THRESHOLD)
            ?.putInt(KEY_EMG, thresholds.emgRaw ?: DISABLED_THRESHOLD)
            ?.apply()
        activeRiskParameters.clear()
        _sessionActionError.value = "Umbrales de riesgo guardados."
        return null
    }

    fun toggleConnection() {
        viewModelScope.launch {
            when (activeDeviceConnector.connectionState.value) {
                is ConnectionState.Connected, is ConnectionState.Connecting, is ConnectionState.Scanning -> {
                    activeDeviceConnector.disconnect()
                }
                is ConnectionState.Disconnected, is ConnectionState.Error, is ConnectionState.PermissionRequired, is ConnectionState.BluetoothDisabled -> {
                    activeDeviceConnector.connect()
                }
            }
        }
    }

    fun startRecordingSession() {
        _sessionActionError.value = null
        if (_activeSession.value.isActive) return
        if (_uiState.value.connectionState !is ConnectionState.Connected) {
            _sessionActionError.value = "Conecte el dispositivo o inicie la simulación antes de grabar una sesión."
            return
        }

        val now = System.currentTimeMillis()
        val source = if (_connectorMode.value == ConnectorMode.SIMULATOR) {
            DataSource.SIMULATED
        } else {
            DataSource.PHYSICAL_DEVICE
        }
        val session = FatigueSession(
            sessionId = UUID.randomUUID().toString(),
            startTime = now,
            isActive = true,
            source = source
        )
        activeRiskParameters.clear()
        sessionStartedByUser = true
        _activeSession.value = session
        repository.saveFatigueSession(session)
    }

    fun stopRecordingSession() {
        val session = _activeSession.value
        if (!session.isActive) return
        activeRiskParameters.clear()
        val completed = session.copy(endTime = System.currentTimeMillis(), isActive = false)
        _activeSession.value = completed
        if (completed.sampleCount == 0) {
            repository.removeFatigueSession(completed.sessionId)
            _sessionActionError.value = "La sesión no contiene mediciones de fatiga y no se guardó."
        } else {
            repository.saveFatigueSession(completed)
        }
    }

    fun resetSession() {
        val previous = _activeSession.value
        activeRiskParameters.clear()
        if (previous.sampleCount > 0) {
            repository.saveFatigueSession(previous.copy(endTime = System.currentTimeMillis(), isActive = false))
        } else if (previous.isActive) {
            repository.removeFatigueSession(previous.sessionId)
        }
        val source = if (_connectorMode.value == ConnectorMode.SIMULATOR) DataSource.SIMULATED else DataSource.PHYSICAL_DEVICE
        val next = newEmptySession(
            isActive = previous.isActive && _uiState.value.connectionState is ConnectionState.Connected,
            source = source.takeIf { previous.isActive && _uiState.value.connectionState is ConnectionState.Connected }
        )
        _activeSession.value = next
        sessionStartedByUser = true
        if (next.isActive) repository.saveFatigueSession(next)
        _sessionActionError.value = null
    }

    fun triggerSimulatedError() {
        viewModelScope.launch {
            activeDeviceConnector.triggerSimulatedError("Simulación manual de falla de conexión")
        }
    }

    fun selectChartMetric(metricType: MetricType) {
        _selectedChartMetric.value = metricType
    }

    fun selectTimeRange(timeRangeFilter: TimeRangeFilter) {
        _selectedTimeRange.value = timeRangeFilter
    }

    fun showTechnicalDialog(show: Boolean) {
        _showTechnicalDialog.value = show
    }

    fun clearHistory() {
        repository.clearHistory()
        _activeSession.value = newEmptySession()
        sessionStartedByUser = true
        _sessionActionError.value = null
    }

    private fun bindConnector(connector: DeviceConnector) {
        connectorJobs.forEach { it.cancel() }
        connectorJobs.clear()

        val bleScannedFlow = if (connector is BleDeviceConnector) {
            connector.scannedDevices
        } else {
            MutableStateFlow(emptyList())
        }

        // 1. Escuchar mediciones emitidas
        val job1 = viewModelScope.launch {
            connector.measurementFlow.collect { record ->
                processRecordForSession(record)?.let(repository::addRecord)
            }
        }

        // 2. Combinar estado reactivo de la interfaz
        val job2 = viewModelScope.launch {
            val historyDataFlow = combine(
                repository.allRecords,
                repository.storageError,
                repository.fatigueSessions,
                _sessionActionError
            ) { records, error, sessions, actionError ->
                HistorySnapshot(records, error, sessions, actionError)
            }

            val deviceAndMetricsFlow = combine(
                _connectorMode,
                connector.connectionState,
                repository.latestRecordsByMetric,
                _selectedChartMetric,
                _selectedTimeRange
            ) { mode, connection, latestMap, chartMetric, timeRange ->
                val metricsMap = MetricType.entries.associateWith { metricType ->
                    val record = latestMap[metricType]
                    createDisplayInfo(metricType, record, connection)
                }
                Triple(mode, connection, metricsMap) to (chartMetric to timeRange)
            }

            val dialogAndSessionFlow = combine(
                _showTechnicalDialog,
                _showScannerDialog,
                bleScannedFlow,
                _activeSession,
                _riskThresholds
            ) { showTech, showScanner, scannedDevs, session, thresholds ->
                SessionUiSnapshot(showTech, showScanner, scannedDevs, session, thresholds)
            }

            combine(
                deviceAndMetricsFlow,
                historyDataFlow,
                dialogAndSessionFlow
            ) { devAndMetrics, historyData, dialogAndSession ->
                val (tripleMode, pairChart) = devAndMetrics
                val (mode, connection, metricsMap) = tripleMode
                val (chartMetric, timeRange) = pairChart
                val (allRecords, historyError, sessions, actionError) = historyData
                val (showTech, showScanner, scannedDevs, session, thresholds) = dialogAndSession
                val filteredChartRecords = filterRecordsForChart(allRecords, chartMetric, timeRange)
                val plots = sessions.mapNotNull { savedSession ->
                    val data = BoxPlotCalculator.calculate(savedSession.samples.map(Int::toDouble), "%")
                    data?.let {
                        FatigueSessionPlot(
                            sessionId = savedSession.sessionId,
                            startTime = savedSession.startTime,
                            source = savedSession.source,
                            data = it
                        )
                    }
                }

                MainUiState(
                    connectorMode = mode,
                    connectionState = connection,
                    metricsMap = metricsMap,
                    selectedChartMetric = chartMetric,
                    selectedTimeRange = timeRange,
                    chartRecords = filteredChartRecords,
                    historyRecords = allRecords,
                    historyError = historyError,
                    sessionActionError = actionError,
                    riskThresholds = thresholds,
                    showTechnicalDialog = showTech,
                    showScannerDialog = showScanner,
                    scannedDevices = scannedDevs,
                    activeSession = session,
                    sessionPlots = plots
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }

        connectorJobs.add(job1)
        connectorJobs.add(job2)
    }

    private data class SessionUiSnapshot(
        val showTech: Boolean,
        val showScanner: Boolean,
        val scannedDevices: List<BleDiscoveredDevice>,
        val session: FatigueSession,
        val thresholds: FatigueRiskThresholds
    )
    private data class HistorySnapshot(
        val records: List<MeasurementRecord>,
        val error: String?,
        val sessions: List<FatigueSession>,
        val actionError: String?
    )

    private fun processRecordForSession(record: MeasurementRecord): MeasurementRecord? {
        if (record.metricType != MetricType.MUSCLE_FATIGUE) return record
        val value = record.value as? MetricValue.MuscleFatigueEmg ?: return record
        if (value.activationPercent !in 0..100 || value.emgRaw < 0) return null
        val session = _activeSession.value
        if (!session.isActive || session.source != record.source) return record

        val thresholds = _riskThresholds.value
        val exceededKeys = buildSet {
            if (thresholds.activationPercent?.let { value.activationPercent > it } == true) add("fatiga")
            if (thresholds.emgRaw?.let { value.emgRaw > it } == true) add("EMG")
        }
        val newlyExceeded = exceededKeys - activeRiskParameters
        activeRiskParameters.clear()
        activeRiskParameters.addAll(exceededKeys)
        if (newlyExceeded.isNotEmpty()) {
            val newDescriptions = thresholds.exceededParameters(
                value.activationPercent,
                value.emgRaw
            ).filter { description ->
                (description.startsWith("fatiga:") && "fatiga" in newlyExceeded) ||
                    (description.startsWith("señal EMG:") && "EMG" in newlyExceeded)
            }
            _riskAlerts.tryEmit(
                "Riesgo detectado: ${newDescriptions.joinToString(", ")}."
            )
        }

        val taggedRecord = record.copy(sessionId = session.sessionId)
        val samples = session.samples + value.activationPercent
        val updated = session.copy(
            currentPercent = value.activationPercent,
            minPercent = samples.minOrNull(),
            maxPercent = samples.maxOrNull(),
            sampleCount = samples.size,
            samples = samples
        )
        _activeSession.value = updated
        repository.saveFatigueSession(updated)
        return taggedRecord
    }

    private fun newEmptySession(
        isActive: Boolean = false,
        source: DataSource? = null
    ) = FatigueSession(
        sessionId = UUID.randomUUID().toString(),
        startTime = System.currentTimeMillis(),
        isActive = isActive,
        source = source
    )

    private fun loadRiskThresholds(): FatigueRiskThresholds {
        val preferences = appContext?.getSharedPreferences(RISK_PREFERENCES, Context.MODE_PRIVATE)
            ?: return FatigueRiskThresholds()
        return FatigueRiskThresholds(
            activationPercent = preferences.getInt(KEY_ACTIVATION, DISABLED_THRESHOLD)
                .takeIf { it != DISABLED_THRESHOLD },
            emgRaw = preferences.getInt(KEY_EMG, DISABLED_THRESHOLD)
                .takeIf { it != DISABLED_THRESHOLD }
        )
    }

    private fun createDisplayInfo(
        metricType: MetricType,
        record: MeasurementRecord?,
        connectionState: ConnectionState
    ): MetricDisplayInfo {
        val sourceLabel = record?.source?.label ?: if (_connectorMode.value == ConnectorMode.SIMULATOR) {
            DataSource.SIMULATED.label
        } else {
            DataSource.PHYSICAL_DEVICE.label
        }
        if (connectionState is ConnectionState.Error) {
            return MetricDisplayInfo(
                type = metricType,
                valueText = "Error",
                unitText = metricType.unit,
                statusText = "Error de sensor",
                descriptionText = metricType.description,
                sourceText = sourceLabel,
                timestampText = timeFormat.format(Date()),
                isAvailable = false,
                isError = true
            )
        }

        if (connectionState is ConnectionState.PermissionRequired) {
            return MetricDisplayInfo(
                type = metricType,
                valueText = "Sin permiso",
                unitText = metricType.unit,
                statusText = "Permiso requerido",
                descriptionText = metricType.description,
                sourceText = sourceLabel,
                timestampText = "N/A",
                isAvailable = false,
                isError = true
            )
        }

        if (connectionState is ConnectionState.BluetoothDisabled) {
            return MetricDisplayInfo(
                type = metricType,
                valueText = "BT Apagado",
                unitText = metricType.unit,
                statusText = "Bluetooth apagado",
                descriptionText = metricType.description,
                sourceText = sourceLabel,
                timestampText = "N/A",
                isAvailable = false,
                isError = true
            )
        }

        if (record == null || connectionState is ConnectionState.Disconnected || connectionState is ConnectionState.Connecting || connectionState is ConnectionState.Scanning) {
            val status = when (connectionState) {
                is ConnectionState.Scanning -> "Buscando..."
                is ConnectionState.Connecting -> "Conectando..."
                else -> "Sin datos"
            }
            return MetricDisplayInfo(
                type = metricType,
                valueText = "Sin datos",
                unitText = metricType.unit,
                statusText = status,
                descriptionText = metricType.description,
                sourceText = sourceLabel,
                timestampText = "N/A",
                isAvailable = false,
                isError = false
            )
        }

        return when (val v = record.value) {
            is MetricValue.HeartRate -> MetricDisplayInfo(
                type = metricType,
                valueText = "${v.bpm}",
                unitText = metricType.unit,
                statusText = if (record.source == DataSource.DEMO_ESP32) "Demo (Fijo)" else "Disponible",
                descriptionText = metricType.description + if (record.source == DataSource.DEMO_ESP32) " (Atención: Dato fijo de demostración de firmware ESP32)." else "",
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = true,
                isError = false
            )
            is MetricValue.BodyTemperature -> MetricDisplayInfo(
                type = metricType,
                valueText = String.format(Locale.getDefault(), "%.1f", v.celsius),
                unitText = metricType.unit,
                statusText = if (record.source == DataSource.DEMO_ESP32) "Demo (Fijo)" else "Disponible",
                descriptionText = metricType.description + if (record.source == DataSource.DEMO_ESP32) " (Atención: Dato fijo de demostración de firmware ESP32)." else "",
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = true,
                isError = false
            )
            is MetricValue.BloodPressure -> MetricDisplayInfo(
                type = metricType,
                valueText = "${v.systolic} / ${v.diastolic}",
                unitText = metricType.unit,
                statusText = if (record.source == DataSource.DEMO_ESP32) "Demo (Fijo)" else "Disponible",
                descriptionText = metricType.description + if (record.source == DataSource.DEMO_ESP32) " (Atención: Dato fijo de demostración de firmware ESP32)." else "",
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = true,
                isError = false
            )
            is MetricValue.MuscleFatigueEmg -> MetricDisplayInfo(
                type = metricType,
                valueText = "${v.activationPercent} %",
                unitText = metricType.unit,
                statusText = "EMG: ${v.emgRaw}",
                descriptionText = metricType.description,
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = true,
                isError = false
            )
            is MetricValue.MuscleFatigueUnavailable -> MetricDisplayInfo(
                type = metricType,
                valueText = "Sin datos",
                unitText = metricType.unit,
                statusText = "No disponible",
                descriptionText = metricType.description,
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = false,
                isError = false
            )
            is MetricValue.Error -> MetricDisplayInfo(
                type = metricType,
                valueText = "Error",
                unitText = metricType.unit,
                statusText = "Error",
                descriptionText = metricType.description,
                sourceText = record.source.label,
                timestampText = timeFormat.format(Date(record.timestamp)),
                isAvailable = false,
                isError = true
            )
            is MetricValue.NoData -> MetricDisplayInfo(
                type = metricType,
                valueText = "Sin datos",
                unitText = metricType.unit,
                statusText = "Sin datos",
                descriptionText = metricType.description,
                sourceText = record.source.label,
                timestampText = "N/A",
                isAvailable = false,
                isError = false
            )
        }
    }

    private fun filterRecordsForChart(
        allRecords: List<MeasurementRecord>,
        metricType: MetricType,
        timeRangeFilter: TimeRangeFilter
    ): List<MeasurementRecord> {
        val now = System.currentTimeMillis()
        val windowMs = when (timeRangeFilter) {
            TimeRangeFilter.ALL -> Long.MAX_VALUE
            TimeRangeFilter.LAST_5_MIN -> 5 * 60 * 1000L
            TimeRangeFilter.LAST_15_MIN -> 15 * 60 * 1000L
            TimeRangeFilter.LAST_HOUR -> 60 * 60 * 1000L
        }

        return allRecords.filter { record ->
            record.metricType == metricType && (now - record.timestamp <= windowMs)
        }
    }

    private companion object {
        const val RISK_PREFERENCES = "fatigue_risk_preferences"
        const val KEY_ACTIVATION = "activation_percent"
        const val KEY_EMG = "emg_raw"
        const val DISABLED_THRESHOLD = -1
    }
}
