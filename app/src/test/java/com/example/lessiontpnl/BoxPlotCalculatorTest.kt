package com.example.lessiontpnl

import com.example.lessiontpnl.util.BoxPlotCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BoxPlotCalculatorTest {

    @Test
    fun `calculates a plot as soon as one confirmed sample exists`() {
        val samples = listOf(10.0)
        val result = BoxPlotCalculator.calculate(samples)
        assertNotNull(result)
        result!!
        assertEquals(10.0, result.min, 0.01)
        assertEquals(10.0, result.q1, 0.01)
        assertEquals(10.0, result.median, 0.01)
        assertEquals(10.0, result.q3, 0.01)
        assertEquals(10.0, result.max, 0.01)
    }

    @Test
    fun `calculates min max median Q1 Q3 correctly for even sample size`() {
        val samples = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0)
        val result = BoxPlotCalculator.calculate(samples)

        assertNotNull(result)
        result!!

        assertEquals(10.0, result.min, 0.01)
        assertEquals(80.0, result.max, 0.01)
        assertEquals(45.0, result.median, 0.01)
        assertEquals(27.5, result.q1, 0.01)
        assertEquals(62.5, result.q3, 0.01)
        assertEquals(35.0, result.iqr, 0.01)
        assertEquals(8, result.sampleCount)
    }

    @Test
    fun `detects outliers correctly using 1 point 5 IQR rule`() {
        // Muestras normales entre 20 y 40, con un valor atípico de 150.0
        val samples = listOf(20.0, 22.0, 25.0, 28.0, 30.0, 32.0, 35.0, 150.0)
        val result = BoxPlotCalculator.calculate(samples)

        assertNotNull(result)
        result!!

        assertEquals(1, result.outliers.size)
        assertEquals(150.0, result.outliers.first(), 0.01)
        assertEquals(20.0, result.lowerWhisker, 0.01)
        assertEquals(35.0, result.upperWhisker, 0.01)
    }
}
