package com.example.lessiontpnl

import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import com.example.lessiontpnl.util.CsvExporter
import java.util.Calendar
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun `formats full date in Spanish including weekday and year`() {
        val timestamp = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 14, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(CsvExporter.formatFullDate(timestamp).contains("martes, 3 de noviembre de 2026, 14:35"))
    }

    @Test
    fun `exports session data with stable Spanish headers and UTF-8 BOM`() {
        val timestamp = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 14, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val record = MeasurementRecord(
            id = "registro-antiguo",
            metricType = MetricType.MUSCLE_FATIGUE,
            value = MetricValue.MuscleFatigueEmg(emgRaw = 700, activationPercent = 80),
            timestamp = timestamp,
            source = DataSource.SIMULATED,
            sessionId = "sesión,1"
        )
        val session = FatigueSession(
            sessionId = "sesión,1",
            startTime = timestamp,
            minPercent = 20,
            maxPercent = 80,
            sampleCount = 2,
            source = DataSource.SIMULATED,
            samples = listOf(20, 80)
        )

        val specialCharactersRecord = MeasurementRecord(
            id = "registro-nuevo",
            metricType = MetricType.MUSCLE_FATIGUE,
            value = MetricValue.Error("presión, \"señal\"\n perdida"),
            timestamp = timestamp + 1,
            source = DataSource.PHYSICAL_DEVICE
        )
        val csv = CsvExporter.generateCsvContent(listOf(record, specialCharactersRecord), listOf(session))

        assertTrue(
            csv.startsWith(
                "\uFEFFfecha_hora,id_sesion,origen_datos,metrica,valor," +
                    "fatiga_minima,fatiga_maxima,unidad,id_registro,detalle\r\n"
            )
        )
        assertTrue(csv.contains("\"sesión,1\",Simulado,Activación Muscular (EMG),80,20,80,% Activación"))
        assertTrue(csv.contains("\"presión, \"\"señal\"\"\n perdida\""))
        assertTrue(csv.indexOf("registro-nuevo") < csv.indexOf("registro-antiguo"))
        assertTrue(!csv.contains("presiA~n"))
    }
}
