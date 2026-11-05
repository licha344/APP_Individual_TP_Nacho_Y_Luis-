package com.example.lessiontpnl

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lessiontpnl.data.repository.MeasurementRepository
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementPersistenceTest {

    @Test
    fun fatigueSessionAndLinkedMeasurementsSurviveRepositoryRecreation() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val temporaryFiles = File(targetContext.cacheDir, "history-test-${UUID.randomUUID()}")
        check(temporaryFiles.mkdirs())
        val isolatedContext = object : ContextWrapper(targetContext) {
            override fun getFilesDir(): File = temporaryFiles
        }

        try {
            val repository = MeasurementRepository(isolatedContext)
            val session = FatigueSession(
                sessionId = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                minPercent = 35,
                maxPercent = 70,
                sampleCount = 2,
                isActive = false,
                source = DataSource.PHYSICAL_DEVICE,
                samples = listOf(70, 35)
            )
            val record = MeasurementRecord(
                metricType = MetricType.MUSCLE_FATIGUE,
                value = MetricValue.MuscleFatigueEmg(500, 35),
                source = DataSource.PHYSICAL_DEVICE,
                sessionId = session.sessionId
            )

            repository.addRecord(record)
            repository.saveFatigueSession(session)
            val historyFile = File(temporaryFiles, "measurement_history.json")
            withTimeout(5_000) {
                while (!historyFile.exists() ||
                    !historyFile.readText().contains(session.sessionId) ||
                    !historyFile.readText().contains(record.id)
                ) {
                    delay(25)
                }
            }

            val restoredRepository = MeasurementRepository(isolatedContext)
            withTimeout(5_000) { restoredRepository.isInitialized.first { it } }

            assertEquals(session.minPercent, restoredRepository.fatigueSessions.value.single().minPercent)
            assertEquals(session.maxPercent, restoredRepository.fatigueSessions.value.single().maxPercent)
            assertEquals(session.samples, restoredRepository.fatigueSessions.value.single().samples)
            assertEquals(session.sessionId, restoredRepository.allRecords.value.single().sessionId)
        } finally {
            temporaryFiles.deleteRecursively()
        }
    }
}
