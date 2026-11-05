package com.example.lessiontpnl.util

import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.MetricValue
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidad para la exportación del historial y mediciones a archivos CSV estandarizados
 * con codificación UTF-8 BOM (\uFEFF) para garantizar la compatibilidad de tildes y caracteres en español
 * en Microsoft Excel, Google Sheets e iWork Numbers.
 */
object CsvExporter {

    private val localeEs = Locale.forLanguageTag("es-ES")
    private val zoneId = ZoneId.systemDefault()

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", localeEs)
    private val fullDateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, HH:mm", localeEs)

    /**
     * Formatea una marca de tiempo en la fecha ISO legible: "2026-11-03 14:35:10"
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(zoneId))
    }

    /**
     * Formatea una marca de tiempo en el formato completo: "martes, 3 de noviembre de 2026, 14:35"
     */
    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Instant.ofEpochMilli(timestamp).atZone(zoneId))
    }

    /**
     * Genera el contenido CSV estandarizado en español con Byte Order Mark (BOM).
     */
    /**
     * Los campos de sesión se dejan vacíos cuando la medición no pertenece a una sesión de fatiga.
     * El orden de columnas es fijo y las filas salen de la más reciente a la más antigua.
     */
    fun generateCsvContent(
        records: List<MeasurementRecord>,
        sessions: List<FatigueSession> = emptyList()
    ): String {
        val sb = StringBuilder()
        
        // BOM UTF-8 (\uFEFF) para visualización correcta de acentos en hojas de cálculo
        sb.append("\uFEFF")

        sb.append("fecha_hora,id_sesion,origen_datos,metrica,valor,fatiga_minima,fatiga_maxima,unidad,id_registro,detalle\r\n")
        val sessionsById = sessions.associateBy { it.sessionId }

        // Ordenar registros de más reciente a más antiguo por fecha y hora
        val sortedRecords = records.sortedWith(
            compareByDescending<MeasurementRecord> { it.timestamp }.thenBy { it.id }
        )

        for (record in sortedRecords) {
            val dateStr = escapeCsv(formatDateTime(record.timestamp))
            val idStr = escapeCsv(record.id)
            val sourceStr = escapeCsv(record.source.label)
            val metricName = escapeCsv(record.metricType.displayName)
            val (valueStr, unitStr, detailStr) = extractFormattedValueUnitAndDetail(record)
            val session = record.sessionId?.let(sessionsById::get)
            val sessionId = escapeCsv(record.sessionId.orEmpty())
            val minimum = session?.minPercent?.toString().orEmpty()
            val maximum = session?.maxPercent?.toString().orEmpty()
            
            val valEscaped = escapeCsv(valueStr)
            val unitEscaped = escapeCsv(unitStr)
            val detailEscaped = escapeCsv(detailStr)

            sb.append(
                "$dateStr,$sessionId,$sourceStr,$metricName,$valEscaped,$minimum,$maximum," +
                    "$unitEscaped,$idStr,$detailEscaped\r\n"
            )
        }

        return sb.toString()
    }

    /**
     * Escribe el contenido CSV directamente en un flujo de salida (OutputStream).
     */
    fun writeCsvToStream(
        records: List<MeasurementRecord>,
        outputStream: OutputStream,
        sessions: List<FatigueSession> = emptyList()
    ) {
        val content = generateCsvContent(records, sessions)
        outputStream.write(content.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    private fun extractFormattedValueUnitAndDetail(record: MeasurementRecord): Triple<String, String, String> {
        return when (val v = record.value) {
            is MetricValue.HeartRate -> Triple("${v.bpm}", record.metricType.unit, "Frecuencia Cardíaca")
            is MetricValue.BodyTemperature -> Triple(String.format(Locale.ROOT, "%.1f", v.celsius), record.metricType.unit, "Temperatura Corporal")
            is MetricValue.BloodPressure -> Triple("${v.systolic} / ${v.diastolic}", record.metricType.unit, "Presión Arterial")
            is MetricValue.MuscleFatigueEmg -> Triple("${v.activationPercent}", record.metricType.unit, "Raw EMG: ${v.emgRaw}")
            is MetricValue.MuscleFatigueUnavailable -> Triple("Sin datos", record.metricType.unit, v.reason)
            is MetricValue.Error -> Triple("Error", record.metricType.unit, v.message)
            is MetricValue.NoData -> Triple("Sin datos", record.metricType.unit, "No disponible")
        }
    }

    private fun escapeCsv(field: String): String {
        var result = field.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("\r")) {
            result = "\"$result\""
        }
        return result
    }
}
