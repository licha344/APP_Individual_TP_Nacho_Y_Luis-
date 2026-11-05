package com.example.lessiontpnl.util

import com.example.lessiontpnl.domain.model.BoxPlotData

/**
 * Calculador estadístico estricto para gráficos de Caja y Bigotes (Box Plot).
 * Calcula el mínimo, primer cuartil (Q1), mediana (Q2), tercer cuartil (Q3), máximo, rango intercuartílico (IQR)
 * e identifica valores atípicos (outliers) utilizando la regla estándar 1.5 * IQR.
 */
object BoxPlotCalculator {

    fun calculate(values: List<Double>, unit: String = "%"): BoxPlotData? {
        if (values.isEmpty() || values.any { !it.isFinite() }) {
            return null
        }

        val sorted = values.sorted()
        val n = sorted.size

        val minVal = sorted.first()
        val maxVal = sorted.last()

        val median = calculateMedian(sorted)
        val q1 = calculatePercentile(sorted, 0.25)
        val q3 = calculatePercentile(sorted, 0.75)
        val iqr = q3 - q1

        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr

        val outliers = sorted.filter { it < lowerFence || it > upperFence }
        val nonOutliers = sorted.filterNot { it < lowerFence || it > upperFence }

        return BoxPlotData(
            min = minVal,
            q1 = q1,
            median = median,
            q3 = q3,
            max = maxVal,
            iqr = iqr,
            outliers = outliers,
            sampleCount = n,
            unit = unit,
            lowerWhisker = nonOutliers.first(),
            upperWhisker = nonOutliers.last()
        )
    }

    private fun calculateMedian(sortedValues: List<Double>): Double {
        val size = sortedValues.size
        return if (size % 2 == 0) {
            (sortedValues[size / 2 - 1] + sortedValues[size / 2]) / 2.0
        } else {
            sortedValues[size / 2]
        }
    }

    private fun calculatePercentile(sortedValues: List<Double>, percentile: Double): Double {
        val size = sortedValues.size
        if (size == 1) return sortedValues[0]

        val index = percentile * (size - 1)
        val lowerIndex = index.toInt()
        val fraction = index - lowerIndex

        return if (lowerIndex + 1 < size) {
            sortedValues[lowerIndex] + fraction * (sortedValues[lowerIndex + 1] - sortedValues[lowerIndex])
        } else {
            sortedValues[lowerIndex]
        }
    }
}
