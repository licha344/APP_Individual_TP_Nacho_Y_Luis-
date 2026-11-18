package com.example.lessiontpnl.data.device

import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue

/**
 * Analizador y reensamblador defensivo de notificaciones BLE para la placa ESP32 (ESP32 LESSION).
 * Procesa el protocolo oficial Versión 1 (`t_ms,adc\n`) enviado a 50 Hz desde la característica TX (6E400003),
 * manteniendo soporte retrocompatible para tramas JSON.
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

        // 1. Intentar procesar líneas completas en formato oficial "t_ms,adc\n"
        var lineEndIndex = accumulator.indexOf("\n")
        while (lineEndIndex >= 0) {
            val rawLine = accumulator.substring(0, lineEndIndex).trim()
            accumulator.delete(0, lineEndIndex + 1)

            if (rawLine.isNotEmpty()) {
                val parsedRecords = parseLine(rawLine)
                records.addAll(parsedRecords)
            }
            lineEndIndex = accumulator.indexOf("\n")
        }

        // 2. Si el acumulador no contenía saltos de línea pero contenía JSON completo "{...}"
        val jsonString = findAndExtractCompleteJson()
        if (jsonString != null) {
            records.addAll(parseJson(jsonString))
        }

        return records
    }

    /**
     * Parsea una línea en el formato oficial Protocolo V1: `t_ms,adc` (ej. "12340,2048").
     * t_ms: milisegundos desde el arranque del ESP32.
     * adc: entero de 0 a 4095 (12-bit ADC).
     */
    fun parseLine(line: String): List<MeasurementRecord> {
        val records = mutableListOf<MeasurementRecord>()
        val parts = line.split(",")
        if (parts.size != 2) return records

        val timestampMs = parts[0].trim().toLongOrNull()
        val adcValue = parts[1].trim().toIntOrNull()

        if (timestampMs != null && timestampMs >= 0 && adcValue != null && adcValue in 0..4095) {
            val now = System.currentTimeMillis()

            // Mapeo lineal del ADC 12 bits (0..4095) a porcentaje de activación (0..100 %)
            val activationPercent = ((adcValue.toDouble() / 4095.0) * 100.0).toInt().coerceIn(0, 100)
            val peakMv = Math.round(((adcValue.toDouble() / 4095.0) * 3300.0) * 10.0) / 10.0

            // 1. Métrica: Activación Muscular (%)
            records.add(
                MeasurementRecord(
                    metricType = MetricType.MUSCLE_FATIGUE,
                    value = MetricValue.MuscleFatigueEmg(
                        emgRaw = adcValue,
                        activationPercent = activationPercent,
                        isDemoData = false
                    ),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )

            // 2. Métrica: Señal EMG Raw (cuentas ADC 0..4095)
            records.add(
                MeasurementRecord(
                    metricType = MetricType.EMG_RAW,
                    value = MetricValue.EmgRawValue(rawValue = adcValue),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )

            // 3. Métrica: Amplitud Pico EMG (mV)
            records.add(
                MeasurementRecord(
                    metricType = MetricType.EMG_PEAK,
                    value = MetricValue.EmgPeakValue(peakMv = peakMv),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )
        }

        return records
    }

    /**
     * Parsea cadenas JSON para retrocompatibilidad.
     */
    fun parseJson(jsonString: String): List<MeasurementRecord> {
        val records = mutableListOf<MeasurementRecord>()
        val payload = jsonString.trim()
        if (!payload.startsWith("{") || !payload.endsWith("}")) return records
        val numericFields = parseNumericFields(payload) ?: return records
        val now = System.currentTimeMillis()

        fun getIntField(key: String): Int? = numericFields[key]?.toIntOrNull()
        fun getDoubleField(key: String): Double? = numericFields[key]?.toDoubleOrNull()

        val emgRaw = getIntField("emg")
        val fatiguePercent = getIntField("fatigue")

        if (emgRaw != null && emgRaw >= 0 && fatiguePercent != null && fatiguePercent in 0..100) {
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
            records.add(
                MeasurementRecord(
                    metricType = MetricType.EMG_RAW,
                    value = MetricValue.EmgRawValue(rawValue = emgRaw),
                    timestamp = now,
                    source = DataSource.PHYSICAL_DEVICE
                )
            )
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
