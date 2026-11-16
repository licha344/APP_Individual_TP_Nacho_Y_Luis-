package com.example.lessiontpnl

import com.example.lessiontpnl.data.device.SimulatedDeviceConnector
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedDeviceConnectorTest {

    @Test
    fun `connection state transitions correctly on connect and disconnect`() = runBlocking {
        val connector = SimulatedDeviceConnector()

        assertEquals(ConnectionState.Disconnected, connector.connectionState.value)

        connector.connect()

        assertEquals(ConnectionState.Connected, connector.connectionState.value)

        connector.disconnect()

        assertEquals(ConnectionState.Disconnected, connector.connectionState.value)
    }

    @Test
    fun `trigger error transitions state to Error`() = runBlocking {
        val connector = SimulatedDeviceConnector()
        connector.connect()

        val errorMessage = "Fallo de red de prueba"
        connector.triggerSimulatedError(errorMessage)

        val currentState = connector.connectionState.value
        assertTrue(currentState is ConnectionState.Error)
        assertEquals(errorMessage, (currentState as ConnectionState.Error).message)
    }

    @Test
    fun `muscle fatigue is never simulated as numeric data in simulated mode`() = runBlocking {
        val connector = SimulatedDeviceConnector()
        connector.connect()

        val receivedRecord = connector.measurementFlow.first { it.metricType == MetricType.MUSCLE_FATIGUE }

        assertEquals(MetricType.MUSCLE_FATIGUE, receivedRecord.metricType)
        assertTrue(receivedRecord.value is MetricValue.MuscleFatigueUnavailable)

        connector.disconnect()
    }
}
