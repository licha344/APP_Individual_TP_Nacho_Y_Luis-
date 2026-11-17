package com.example.lessiontpnl.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.ui.components.BleDeviceScannerDialog
import com.example.lessiontpnl.ui.components.BoxPlotChart
import com.example.lessiontpnl.ui.components.ConnectionHeader
import com.example.lessiontpnl.ui.components.HistorySection
import com.example.lessiontpnl.ui.components.MetricCard
import com.example.lessiontpnl.ui.components.MetricGraph
import com.example.lessiontpnl.ui.components.SessionRecordingSection
import com.example.lessiontpnl.ui.components.TechnicalInfoDialog
import com.example.lessiontpnl.ui.viewmodel.MainViewModel
import com.example.lessiontpnl.ui.viewmodel.MainViewModelFactory
import com.example.lessiontpnl.util.BlePermissionHelper
import com.example.lessiontpnl.util.CsvExporter
import com.example.lessiontpnl.util.RiskNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(LocalContext.current.applicationContext))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingRiskMessage by remember { mutableStateOf<String?>(null) }

    val riskPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val message = pendingRiskMessage
        pendingRiskMessage = null
        if (message != null) {
            if (granted && RiskNotificationHelper.notificationsEnabled(context)) {
                RiskNotificationHelper.show(context, message)
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("$message (notificaciones desactivadas o permiso denegado)")
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.riskAlerts.collect { message ->
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !RiskNotificationHelper.hasRuntimePermission(context)
            ) {
                pendingRiskMessage = message
                riskPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (RiskNotificationHelper.notificationsEnabled(context)) {
                RiskNotificationHelper.show(context, message)
            } else {
                snackbarHostState.showSnackbar("$message (notificaciones desactivadas)")
            }
        }
    }

    LaunchedEffect(uiState.historyError) {
        uiState.historyError?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.sessionActionError) {
        uiState.sessionActionError?.let { snackbarHostState.showSnackbar(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val recordsToExport = uiState.historyRecords
            val sessionsToExport = viewModel.repository.fatigueSessions.value
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val outputStream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Android no pudo abrir el archivo seleccionado")
                        outputStream.use {
                            CsvExporter.writeCsvToStream(recordsToExport, it, sessionsToExport)
                        }
                    }
                    snackbarHostState.showSnackbar("Historial exportado correctamente.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        "No se pudo exportar el historial: ${e.localizedMessage ?: "error de escritura"}"
                    )
                }
            }
        }
    }

    // Launcher para solicitud de permisos en tiempo de ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.openScannerDialog(context)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Monitoreo Físico de Entrenamiento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // US 2 & BLE ESP32: Encabezado de Conexión y Selector de Conector
            item {
                ConnectionHeader(
                    connectorMode = uiState.connectorMode,
                    connectionState = uiState.connectionState,
                    onSelectMode = { mode -> viewModel.selectConnectorMode(mode, context) },
                    onOpenScanner = {
                        if (!BlePermissionHelper.hasPermissions(context)) {
                            permissionLauncher.launch(BlePermissionHelper.getRequiredPermissions())
                        } else {
                            viewModel.openScannerDialog(context)
                        }
                    },
                    onToggleConnection = { viewModel.toggleConnection() },
                    onRequestPermissions = {
                        permissionLauncher.launch(BlePermissionHelper.getRequiredPermissions())
                    },
                    onTriggerError = { viewModel.triggerSimulatedError() },
                    onShowTechnicalInfo = { viewModel.showTechnicalDialog(true) }
                )
            }

            // US 1 & US 4: Organización y Comprensión de las Métricas (Grid 2x2 en filas)
            item {
                Column {
                    Text(
                        text = "Métricas en Tiempo Real",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val columns = if (maxWidth < 600.dp) 1 else 3
                        val metrics = listOf(
                            MetricType.MUSCLE_FATIGUE,
                            MetricType.EMG_RAW,
                            MetricType.EMG_PEAK
                        )
                        if (columns == 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                metrics.forEach { metric ->
                                    uiState.metricsMap[metric]?.let { displayInfo ->
                                        MetricCard(displayInfo = displayInfo, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                metrics.forEach { metric ->
                                    uiState.metricsMap[metric]?.let { displayInfo ->
                                        MetricCard(displayInfo = displayInfo, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FASE 5: Registro de Sesión con Niveles Mínimo y Máximo
            item {
                SessionRecordingSection(
                    session = uiState.activeSession,
                    onStartSession = { viewModel.startRecordingSession() },
                    onStopSession = { viewModel.stopRecordingSession() },
                    onResetSession = { viewModel.resetSession() },
                    isConnected = uiState.connectionState is ConnectionState.Connected,
                    riskThresholds = uiState.riskThresholds,
                    onSaveRiskThresholds = { viewModel.saveRiskThresholds(it) }
                )
            }

            // FASE 6: Gráfico de Caja y Bigotes (Box Plot)
            item {
                BoxPlotChart(
                    sessionPlots = uiState.sessionPlots
                )
            }

            // US 5: Seguimiento Gráfico del Progreso Lineal
            item {
                MetricGraph(
                    selectedMetric = uiState.selectedChartMetric,
                    selectedTimeRange = uiState.selectedTimeRange,
                    records = uiState.chartRecords,
                    onSelectMetric = { metric -> viewModel.selectChartMetric(metric) },
                    onSelectTimeRange = { range -> viewModel.selectTimeRange(range) }
                )
            }

            // Historial Persistente y Exportación CSV
            item {
                HistorySection(
                    records = uiState.historyRecords,
                    onExportCsv = { exportLauncher.launch("historial_mediciones.csv") },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
            }
        }

        // Diálogo de Búsqueda Interactiva de Dispositivos BLE
        if (uiState.showScannerDialog) {
            BleDeviceScannerDialog(
                devices = uiState.scannedDevices,
                isScanning = uiState.connectionState is ConnectionState.Scanning,
                connectionState = uiState.connectionState,
                onSelectDevice = { device -> viewModel.connectToDeviceAddress(device.address) },
                onDismiss = { viewModel.closeScannerDialog() }
            )
        }

        // Diálogo Informativo Técnico
        if (uiState.showTechnicalDialog) {
            TechnicalInfoDialog(
                onDismiss = { viewModel.showTechnicalDialog(false) }
            )
        }
    }
}
