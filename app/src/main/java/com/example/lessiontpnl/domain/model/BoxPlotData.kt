package com.example.lessiontpnl.domain.model

/**
 * Representa los cálculos estadísticos de un gráfico de Caja y Bigotes (Box Plot).
 */
data class BoxPlotData(
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val iqr: Double,
    val outliers: List<Double> = emptyList(),
    val sampleCount: Int,
    val unit: String = "%",
    val lowerWhisker: Double = min,
    val upperWhisker: Double = max
)

data class FatigueSessionPlot(
    val sessionId: String,
    val startTime: Long,
    val source: DataSource?,
    val data: BoxPlotData
)

/**
 * Modelo para registrar una sesión de monitoreo con extremos de fatiga/activación EMG.
 */
data class FatigueSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val currentPercent: Int? = null,
    val minPercent: Int? = null,
    val maxPercent: Int? = null,
    val sampleCount: Int = 0,
    val isActive: Boolean = false,
    val source: DataSource? = null,
    val samples: List<Int> = emptyList()
)
