package com.example.lessiontpnl

import com.example.lessiontpnl.data.repository.MeasurementRepository
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementRepositoryTest {

    @Test
    fun `addRecord stores records and updates latest map`() {
        val repository = MeasurementRepository()

        val record1 = MeasurementRecord(
            metricType = MetricType.HEART_RATE,
            value = MetricValue.HeartRate(80),
            timestamp = 1000L,
            source = DataSource.SIMULATED
        )

        val record2 = MeasurementRecord(
            metricType = MetricType.BODY_TEMPERATURE,
            value = MetricValue.BodyTemperature(36.6),
            timestamp = 1005L,
            source = DataSource.SIMULATED
        )

        repository.addRecord(record1)
        repository.addRecord(record2)

        assertEquals(2, repository.allRecords.value.size)
        assertEquals(record1, repository.latestRecordsByMetric.value[MetricType.HEART_RATE])
        assertEquals(record2, repository.latestRecordsByMetric.value[MetricType.BODY_TEMPERATURE])
    }

    @Test
    fun `clearHistory empties records list and latest map`() {
        val repository = MeasurementRepository()

        val record = MeasurementRecord(
            metricType = MetricType.HEART_RATE,
            value = MetricValue.HeartRate(85)
        )
        repository.addRecord(record)

        assertEquals(1, repository.allRecords.value.size)

        repository.clearHistory()

        assertTrue(repository.allRecords.value.isEmpty())
        assertTrue(repository.latestRecordsByMetric.value.isEmpty())
    }

    @Test
    fun `stores completed fatigue session and clears it with history`() {
        val repository = MeasurementRepository()
        val session = FatigueSession(
            sessionId = "session-1",
            startTime = 100L,
            minPercent = 25,
            maxPercent = 75,
            sampleCount = 2,
            source = DataSource.SIMULATED,
            samples = listOf(75, 25)
        )

        repository.saveFatigueSession(session)

        assertEquals(session, repository.fatigueSessions.value.single())
        repository.clearHistory()
        assertTrue(repository.fatigueSessions.value.isEmpty())
    }
}
