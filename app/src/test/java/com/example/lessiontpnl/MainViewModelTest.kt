package com.example.lessiontpnl

import com.example.lessiontpnl.data.device.SimulatedDeviceConnector
import com.example.lessiontpnl.data.repository.MeasurementRepository
import com.example.lessiontpnl.domain.device.DeviceConnector
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import com.example.lessiontpnl.domain.model.TimeRangeFilter
import com.example.lessiontpnl.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has disconnected state and default selections`() {
        val connector = SimulatedDeviceConnector()
        val repository = MeasurementRepository()
        val viewModel = MainViewModel(context = null, repository = repository, initialConnector = connector)

        assertEquals(MetricType.MUSCLE_FATIGUE, viewModel.uiState.value.selectedChartMetric)
        assertEquals(TimeRangeFilter.ALL, viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `selecting chart metric updates UI state`() {
        val viewModel = MainViewModel()
        viewModel.selectChartMetric(MetricType.BODY_TEMPERATURE)

        assertEquals(MetricType.BODY_TEMPERATURE, viewModel.uiState.value.selectedChartMetric)
    }

    @Test
    fun `selecting time range updates UI state`() {
        val viewModel = MainViewModel()
        viewModel.selectTimeRange(TimeRangeFilter.LAST_5_MIN)

        assertEquals(TimeRangeFilter.LAST_5_MIN, viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `fatigue session records valid samples only while active and saves its range`() = runTest(testDispatcher) {
        val connector = TestDeviceConnector()
        val repository = MeasurementRepository()
        val viewModel = MainViewModel(context = null, repository = repository, initialConnector = connector)

        connector.emit(fatigueRecord(90))
        viewModel.startRecordingSession()
        connector.emit(fatigueRecord(70))
        connector.emit(fatigueRecord(101))
        connector.emit(fatigueRecord(35))
        viewModel.stopRecordingSession()

        val session = repository.fatigueSessions.value.single()
        assertEquals(35, session.minPercent)
        assertEquals(70, session.maxPercent)
        assertEquals(2, session.sampleCount)
        assertEquals(listOf(70, 35), session.samples)
        assertEquals(DataSource.PHYSICAL_DEVICE, session.source)
        assertEquals(session.sessionId, repository.allRecords.value.first().sessionId)
        assertEquals(3, repository.allRecords.value.size)
        assertEquals(1, viewModel.uiState.value.sessionPlots.size)
    }

    private fun fatigueRecord(percent: Int) = MeasurementRecord(
        metricType = MetricType.MUSCLE_FATIGUE,
        value = MetricValue.MuscleFatigueEmg(emgRaw = 500, activationPercent = percent),
        source = DataSource.PHYSICAL_DEVICE
    )

    private class TestDeviceConnector : DeviceConnector {
        private val mutableConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
        override val connectionState: StateFlow<ConnectionState> = mutableConnectionState
        private val mutableMeasurements = MutableSharedFlow<MeasurementRecord>(extraBufferCapacity = 8)
        override val measurementFlow: SharedFlow<MeasurementRecord> = mutableMeasurements

        suspend fun emit(record: MeasurementRecord) {
            mutableMeasurements.emit(record)
        }

        override suspend fun connect() {
            mutableConnectionState.value = ConnectionState.Connected
        }

        override suspend fun disconnect() {
            mutableConnectionState.value = ConnectionState.Disconnected
        }

        override suspend fun triggerSimulatedError(message: String) {
            mutableConnectionState.value = ConnectionState.Error(message)
        }
    }
}
