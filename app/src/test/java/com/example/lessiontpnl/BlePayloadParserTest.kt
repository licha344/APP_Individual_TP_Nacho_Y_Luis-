package com.example.lessiontpnl

import com.example.lessiontpnl.data.device.BlePayloadParser
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePayloadParserTest {

    @Test
    fun `parseLine correctly parses official ESP32 LESSION protocol t_ms,adc format`() {
        val parser = BlePayloadParser()
        val line = "12340,2048"

        val records = parser.parseLine(line)

        assertEquals(3, records.size)

        // 1. Métrica: Activación Muscular (%)
        val emgRecord = records.first { it.metricType == MetricType.MUSCLE_FATIGUE }
        assertEquals(DataSource.PHYSICAL_DEVICE, emgRecord.source)
        assertTrue(emgRecord.value is MetricValue.MuscleFatigueEmg)
        val emgVal = emgRecord.value as MetricValue.MuscleFatigueEmg
        assertEquals(2048, emgVal.emgRaw)
        assertEquals(50, emgVal.activationPercent) // (2048/4095) * 100 = 50%

        // 2. Métrica: Señal EMG Raw
        val rawRecord = records.first { it.metricType == MetricType.EMG_RAW }
        assertEquals(2048, (rawRecord.value as MetricValue.EmgRawValue).rawValue)

        // 3. Métrica: Amplitud Pico EMG (mV)
        val peakRecord = records.first { it.metricType == MetricType.EMG_PEAK }
        assertEquals(1650.0, (peakRecord.value as MetricValue.EmgPeakValue).peakMv, 1.0)
    }

    @Test
    fun `processNotificationBytes handles line delimited notifications from ESP32 LESSION`() {
        val parser = BlePayloadParser()
        val notificationBytes = "12340,2048\n12360,2500\n".toByteArray(Charsets.UTF_8)

        val records = parser.processNotificationBytes(notificationBytes)

        assertEquals(6, records.size) // 3 métricas por cada línea recibida
    }

    @Test
    fun `parseJson correctly extracts muscle fatigue EMG and tags demo HR temp BP data`() {
        val parser = BlePayloadParser()
        val jsonPayload = """{"emg":1250,"fatigue":45,"hr":76,"temp":36.6,"sys":120,"dia":80}"""

        val records = parser.parseJson(jsonPayload)

        assertEquals(6, records.size)

        // 1. Verificar Métrica de Fatiga/Activación Muscular EMG
        val emgRecord = records.first { it.metricType == MetricType.MUSCLE_FATIGUE }
        assertEquals(DataSource.PHYSICAL_DEVICE, emgRecord.source)
        assertTrue(emgRecord.value is MetricValue.MuscleFatigueEmg)
        val emgVal = emgRecord.value as MetricValue.MuscleFatigueEmg
        assertEquals(1250, emgVal.emgRaw)
        assertEquals(45, emgVal.activationPercent)

        // 2. Verificar Métrica EMG Raw
        val rawRecord = records.first { it.metricType == MetricType.EMG_RAW }
        assertEquals(DataSource.PHYSICAL_DEVICE, rawRecord.source)
        assertEquals(1250, (rawRecord.value as MetricValue.EmgRawValue).rawValue)

        // 3. CUMPLIMIENTO PROTOCOLO: Verificar que HR, Temp y Presión se etiquetan como DEMO_ESP32
        val hrRecord = records.first { it.metricType == MetricType.HEART_RATE }
        assertEquals(DataSource.DEMO_ESP32, hrRecord.source)
        assertEquals(76, (hrRecord.value as MetricValue.HeartRate).bpm)

        val tempRecord = records.first { it.metricType == MetricType.BODY_TEMPERATURE }
        assertEquals(DataSource.DEMO_ESP32, tempRecord.source)
        assertEquals(36.6, (tempRecord.value as MetricValue.BodyTemperature).celsius, 0.01)

        val bpRecord = records.first { it.metricType == MetricType.BLOOD_PRESSURE }
        assertEquals(DataSource.DEMO_ESP32, bpRecord.source)
        val bpVal = bpRecord.value as MetricValue.BloodPressure
        assertEquals(120, bpVal.systolic)
        assertEquals(80, bpVal.diastolic)
    }

    @Test
    fun `processNotificationBytes reassembles JSON chunks split across notifications`() {
        val parser = BlePayloadParser()

        val chunk1 = """{"emg":1250,"fatig""".toByteArray(Charsets.UTF_8)
        val chunk2 = """ue":45,"hr":76,"temp":36.6,"sys":120,"dia":80}""".toByteArray(Charsets.UTF_8)

        val recordsFromChunk1 = parser.processNotificationBytes(chunk1)
        assertTrue(recordsFromChunk1.isEmpty()) // Aún incompleto

        val recordsFromChunk2 = parser.processNotificationBytes(chunk2)
        assertEquals(6, recordsFromChunk2.size) // Reensamblado exitosamente
    }

    @Test
    fun `processNotificationBytes handles malformed JSON without crashing`() {
        val parser = BlePayloadParser()
        val malformedChunk = "not_json_data_without_brackets".toByteArray(Charsets.UTF_8)

        val records = parser.processNotificationBytes(malformedChunk)
        assertTrue(records.isEmpty())
    }

    @Test
    fun `does not create partial or out of range fatigue readings`() {
        val parser = BlePayloadParser()

        assertTrue(parser.parseJson("""{"emg":1200}""").none { it.metricType == MetricType.MUSCLE_FATIGUE })
        assertTrue(parser.parseJson("""{"emg":1200,"fatigue":101}""").none { it.metricType == MetricType.MUSCLE_FATIGUE })
        assertTrue(parser.parseJson("""{"emg":-1,"fatigue":40}""").none { it.metricType == MetricType.MUSCLE_FATIGUE })
        assertTrue(parser.parseJson("""{"emg":1200,"fatigue":40 garbage}""").isEmpty())
    }
}
