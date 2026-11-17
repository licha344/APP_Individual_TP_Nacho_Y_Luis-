package com.example.lessiontpnl.data.device

import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue

/**
 * Analizador y reensamblador defensivo de paquetes JSON para la característica BLE del ESP32.
 * Deserializa las notificaciones del sensor EMG V3 (señal bruta, activación % y amplitud en mV).
 */
class BlePayloadParser {

    private val accumulator = StringBuilder()
    private val maximumPayloadLength = 4096

    /**
     * Acumula fragmentos recibidos por notificación BLE y extrae registros de medición válidos.
     */
    @Synchronized
    fun processNotificationBytes(bytes: ByteArray): List<MeasurementRecord> {
        val chunk = String(bytes, Charsets.UTF_8)
        accumulator.append(chunk)
        if (accumulator.length > maximumPayloadLength) {
            accumulator.setLength(0)
            return emptyList()
        }

        val records = mutableListOf<MeasurementRecord>()
        var fullJsonString = findAndExtractCompleteJson()

        while (fullJsonString != null) {
            val parsedRecords = parseJson(fullJsonString)
            records.addAll(parsedRecords)
            fullJsonString = findAndExtractCompleteJson()
        }

        return records
    }

    /**
     * Parsea la cadena JSON de notificaciones recibidas desde la placa ESP32 y el sensor EMG V3.
     */
    fun parseJson(jsonString: String): List<MeasurementRecord> {
        val records = mutableListOf<MeasurementRecord>()
        val payload = jsonString.trim()
        if (!payload.startsWith("{") || !payload.endsWith("}")) return records
        val numericFields = parseNumericFields(payload) ?: return records
        val now = System.currentTimeMillis()

        fun getIntField(key: String): Int? {
            return numericFields[key]?.toIntOrNull()
        }

        fun getDoubleField(key: String): Double? {
            return numericFields[key]?.toDoubleOrNull()
        }

        // 1. Procesar Métrica Principal: Activación Muscular (EMG %)
        val emgRaw = getIntField("emg")
        val fatiguePercent = getIntField("fatigue")

        if (emgRaw != null && emgRaw >= 0 && fatiguePercent != null && fatiguePercent in 0..100) {
            // Métrica 1: Activación Muscular (%)
            records.add(
                MeasurementRecord(
                    metricType = MetricType.MUSCLE_FATIGUE,
                    value = MetricValue.MuscleFatigueEmg(
                        emgRaw = emgRaw,
                        activationPercent = fatiguePercent,
                        isDemoData = false
                    ),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )

            // Métrica 2: Señal EMG Raw (unidades ADC 0-4095)
            records.add(
                MeasurementRecord(
                    metricType = MetricType.EMG_RAW,
                    value = MetricValue.EmgRawValue(rawValue = emgRaw),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )

            // Métrica 3: Amplitud Pico EMG calculada en milivoltios (mV)
            val peakMv = (emgRaw.toDouble() / 4095.0) * 3300.0
            records.add(
                MeasurementRecord(
                    metricType = MetricType.EMG_PEAK,
                    value = MetricValue.EmgPeakValue(peakMv = Math.round(peakMv * 10.0) / 10.0),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )
        }

        // 2. Métricas secundarias opcionales/fijas si están presentes en la trama del ESP32
        val hr = getIntField("hr")
        if (hr != null) {
            records.add(
                MeasurementRecord(
                    metricType = MetricType.HEART_RATE,
                    value = MetricValue.HeartRate(hr),
                    timestamp = now,
                    source = DataSource.DEMO_ESP32
                )
            )
        }

        val temp = getDoubleField("temp")
        if (temp != null) {
            records.add(
                MeasurementRecord(
                    metricType = MetricType.BODY_TEMPERATURE,
                    value = MetricValue.BodyTemperature(temp),
                    timestamp = now,
                    source = DataSource.DEMO_ESP32
                )
            )
        }

        val sys = getIntField("sys")
        val dia = getIntField("dia")
        if (sys != null && dia != null) {
            records.add(
                MeasurementRecord(
                    metricType = MetricType.BLOOD_PRESSURE,
                    value = MetricValue.BloodPressure(sys, dia),
                    timestamp = now,
                    source = DataSource.DEMO_ESP32
                )
            )
        }

        return records
    }

    private fun parseNumericFields(payload: String): Map<String, String>? {
        val body = payload.substring(1, payload.length - 1)
        val fieldPattern = Regex("""\s*"([A-Za-z][A-Za-z0-9_]*)"\s*:\s*(-?\d+(?:\.\d+)?)\s*""")
        val fields = mutableMapOf<String, String>()
        var index = 0

        fun skipWhitespace() {
            while (index < body.length && body[index].isWhitespace()) index++
        }

        skipWhitespace()
        if (index == body.length) return null
        while (index < body.length) {
            val match = fieldPattern.matchAt(body, index) ?: return null
            val key = match.groupValues[1]
            if (fields.put(key, match.groupValues[2]) != null) return null
            index = match.range.last + 1
            skipWhitespace()
            if (index == body.length) break
            if (body[index] != ',') return null
            index++
            skipWhitespace()
            if (index == body.length) return null
        }
        return fields
    }

    @Synchronized
    fun resetBuffer() {
        accumulator.setLength(0)
    }

    private fun findAndExtractCompleteJson(): String? {
        val start = accumulator.indexOf("{")
        if (start < 0) {
            accumulator.setLength(0)
            return null
        }

        val end = accumulator.indexOf("}", start)
        if (end > start) {
            val jsonString = accumulator.substring(start, end + 1)
            accumulator.delete(0, end + 1)
            return jsonString
        }

        if (start > 0) {
            accumulator.delete(0, start)
        }

        return null
    }
}
