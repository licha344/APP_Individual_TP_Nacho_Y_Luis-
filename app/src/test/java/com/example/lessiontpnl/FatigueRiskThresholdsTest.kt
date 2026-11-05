package com.example.lessiontpnl

import com.example.lessiontpnl.domain.model.FatigueRiskThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FatigueRiskThresholdsTest {
    @Test
    fun blankThresholdsAreValidAndDoNotReportRisk() {
        val thresholds = FatigueRiskThresholds()

        assertNull(thresholds.validationError())
        assertEquals(emptyList<String>(), thresholds.exceededParameters(100, 500))
    }

    @Test
    fun validatesFatigueAndEmgRanges() {
        assertEquals(
            "El umbral de fatiga debe estar entre 0 y 100 %.",
            FatigueRiskThresholds(101, null).validationError()
        )
        assertEquals(
            "El umbral EMG debe ser cero o mayor.",
            FatigueRiskThresholds(null, -1).validationError()
        )
        assertNull(FatigueRiskThresholds(100, 0).validationError())
    }

    @Test
    fun reportsEachParameterOnlyWhenItExceedsItsThreshold() {
        val thresholds = FatigueRiskThresholds(70, 300)

        assertEquals(emptyList<String>(), thresholds.exceededParameters(70, 300))
        assertEquals(
            listOf("fatiga: 71 % (umbral 70 %)"),
            thresholds.exceededParameters(71, 300)
        )
        assertEquals(
            listOf("fatiga: 71 % (umbral 70 %)", "señal EMG: 301 unidades (umbral 300 unidades)"),
            thresholds.exceededParameters(71, 301)
        )
    }
}
