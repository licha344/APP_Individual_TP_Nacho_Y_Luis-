package com.example.lessiontpnl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.domain.model.FatigueSessionPlot
import com.example.lessiontpnl.ui.theme.PaletteLavender
import com.example.lessiontpnl.ui.theme.PaletteLime
import com.example.lessiontpnl.util.CsvExporter
import java.util.Locale

@Composable
fun BoxPlotChart(
    sessionPlots: List<FatigueSessionPlot>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gráficos de Caja y Bigotes por Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (sessionPlots.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Todavía no hay sesiones con mediciones de fatiga para graficar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val plotWidth = maxWidth.coerceIn(280.dp, 420.dp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        sessionPlots.forEach { plot ->
                            SessionBoxPlot(plot, Modifier.width(plotWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionBoxPlot(
    plot: FatigueSessionPlot,
    modifier: Modifier = Modifier
) {
    val data = plot.data
    val lineColor = MaterialTheme.colorScheme.primary
    val medianColor = PaletteLavender
    val boxFill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val range = (data.max - data.min).coerceAtLeast(1.0)
    val scaleMin = (data.min - range * 0.08).coerceAtLeast(0.0)
    val scaleMax = data.max + range * 0.08

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = CsvExporter.formatFullDate(plot.startTime),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = "${plot.source?.label ?: "Origen no disponible"} · N = ${data.sampleCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                val centerX = size.width / 2f
                val boxWidth = size.width * 0.42f
                val leftX = centerX - boxWidth / 2f
                val rightX = centerX + boxWidth / 2f
                fun y(value: Double): Float =
                    size.height - ((value - scaleMin) / (scaleMax - scaleMin)).toFloat() * size.height

                for (i in 0..4) {
                    val gridY = size.height * i / 4
                    drawLine(gridColor, Offset(0f, gridY), Offset(size.width, gridY), 1.dp.toPx())
                }

                val yQ1 = y(data.q1)
                val yQ3 = y(data.q3)
                val yMedian = y(data.median)
                val yLow = y(data.lowerWhisker)
                val yHigh = y(data.upperWhisker)
                drawLine(lineColor, Offset(centerX, yQ1), Offset(centerX, yLow), 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(lineColor, Offset(centerX, yQ3), Offset(centerX, yHigh), 2.dp.toPx(), cap = StrokeCap.Round)
                val capHalf = boxWidth * 0.18f
                drawLine(lineColor, Offset(centerX - capHalf, yLow), Offset(centerX + capHalf, yLow), 2.dp.toPx())
                drawLine(lineColor, Offset(centerX - capHalf, yHigh), Offset(centerX + capHalf, yHigh), 2.dp.toPx())

                val top = minOf(yQ1, yQ3)
                val height = (yQ1 - yQ3).toFloat().let { kotlin.math.abs(it).coerceAtLeast(2f) }
                drawRoundRect(
                    color = boxFill,
                    topLeft = Offset(leftX, top),
                    size = Size(boxWidth, height),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(leftX, top),
                    size = Size(boxWidth, height),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(medianColor, Offset(leftX, yMedian), Offset(rightX, yMedian), 3.dp.toPx())
                data.outliers.forEach { outlier ->
                    drawCircle(PaletteLime, 4.dp.toPx(), Offset(centerX, y(outlier)))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatLabel("Mín", data.min, data.unit)
                StatLabel("Q1", data.q1, data.unit)
                StatLabel("Mediana", data.median, data.unit, isAccent = true)
                StatLabel("Q3", data.q3, data.unit)
                StatLabel("Máx", data.max, data.unit)
            }
            if (data.outliers.isNotEmpty()) {
                Text(
                    text = "Atípicos: ${data.outliers.joinToString { formatValue(it) }} ${data.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatLabel(
    title: String,
    value: Double,
    unit: String,
    isAccent: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAccent) PaletteLime else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${formatValue(value)} $unit",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) PaletteLime else MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatValue(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
