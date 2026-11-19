package com.example.lessiontpnl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import com.example.lessiontpnl.domain.model.TimeRangeFilter
import com.example.lessiontpnl.ui.theme.PaletteLavender
import com.example.lessiontpnl.ui.theme.PaletteLightLavender
import com.example.lessiontpnl.ui.theme.PaletteLime
import com.example.lessiontpnl.ui.theme.PaletteMutedPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente gráfico interactivo para la evolución temporal de métricas.
 * Soporta Ritmo Cardíaco (bpm), Temperatura Corporal (°C), Presión Arterial (serie doble Sistólica/Diastólica en mmHg)
 * y métricas de activación/señal EMG V3.
 */
@Composable
fun MetricGraph(
    selectedMetric: MetricType,
    selectedTimeRange: TimeRangeFilter,
    records: List<MeasurementRecord>,
    onSelectMetric: (MetricType) -> Unit,
    onSelectTimeRange: (TimeRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Estructuras para trazado simple o doble (Presión Arterial)
    val isBloodPressure = selectedMetric == MetricType.BLOOD_PRESSURE

    val bpPoints = if (isBloodPressure) {
        records.mapNotNull { record ->
            val v = record.value as? MetricValue.BloodPressure ?: return@mapNotNull null
            Triple(record.timestamp, v.systolic.toDouble(), v.diastolic.toDouble())
        }
    } else emptyList()

    val singlePoints = if (!isBloodPressure) {
        records.mapNotNull { record ->
            val yVal = when (val v = record.value) {
                is MetricValue.HeartRate -> v.bpm.toDouble()
                is MetricValue.BodyTemperature -> v.celsius
                is MetricValue.MuscleFatigueEmg -> v.activationPercent.toDouble()
                is MetricValue.EmgRawValue -> v.rawValue.toDouble()
                is MetricValue.EmgPeakValue -> v.peakMv
                else -> null
            }
            if (yVal != null) record.timestamp to yVal else null
        }
    } else emptyList()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Título de la sección gráfica con estilo Neón
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Gráficos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Evolución Gráfica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${selectedMetric.displayName} (${selectedMetric.unit})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de métricas en Chips deslizables
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricType.entries.forEach { metric ->
                    FilterChip(
                        selected = (metric == selectedMetric),
                        onClick = { onSelectMetric(metric) },
                        label = { Text(text = metric.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Selector de Rango de Tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rango:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                TimeRangeFilter.entries.forEach { range ->
                    FilterChip(
                        selected = (range == selectedTimeRange),
                        onClick = { onSelectTimeRange(range) },
                        label = { Text(text = range.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Renderizado del Gráfico en Canvas o Estado Vacío
            val hasData = if (isBloodPressure) bpPoints.isNotEmpty() else singlePoints.isNotEmpty()

            if (!hasData) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No hay mediciones registradas para ${selectedMetric.displayName} en el rango seleccionado.\nConecte el dispositivo o inicie la recepción para visualizar la curva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isBloodPressure) {
                // GRAFICO DOBLE SERIE: PRESIÓN ARTERIAL (SISTÓLICA Y DIASTÓLICA)
                val sysColor = PaletteLightLavender
                val diaColor = PaletteLime
                val gridColor = PaletteMutedPurple

                val allY = bpPoints.flatMap { listOf(it.second, it.third) }
                val minY = (allY.minOrNull() ?: 60.0) * 0.9
                val maxY = (allY.maxOrNull() ?: 140.0) * 1.1

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(color = sysColor, label = "Sistólica (mmHg)")
                            LegendDot(color = diaColor, label = "Diastólica (mmHg)")
                        }
                        Text(
                            text = "Escala: ${String.format(Locale.getDefault(), "%.0f-%.0f", minY, maxY)} mmHg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Cuadrícula horizontal
                        val steps = 3
                        for (i in 0..steps) {
                            val y = height * i / steps
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        fun valueToY(yVal: Double): Float {
                            val norm = ((yVal - minY) / (maxY - minY)).toFloat()
                            return height - (norm * height)
                        }

                        val sysOffsets = bpPoints.mapIndexed { idx, item ->
                            val x = if (bpPoints.size == 1) width / 2f else (idx.toFloat() / (bpPoints.size - 1)) * width
                            Offset(x, valueToY(item.second))
                        }

                        val diaOffsets = bpPoints.mapIndexed { idx, item ->
                            val x = if (bpPoints.size == 1) width / 2f else (idx.toFloat() / (bpPoints.size - 1)) * width
                            Offset(x, valueToY(item.third))
                        }

                        // Trazar Serie Sistólica
                        drawSeries(pathColor = sysColor, points = sysOffsets)
                        // Trazar Serie Diastólica
                        drawSeries(pathColor = diaColor, points = diaOffsets)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Eje X: Tiempos de inicio y fin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = timeFormat.format(Date(bpPoints.first().first)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (bpPoints.size > 1) {
                            Text(
                                text = timeFormat.format(Date(bpPoints.last().first)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // GRAFICO SERIE SIMPLE (Ritmo Cardíaco, Temperatura Corporal, EMG)
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = PaletteMutedPurple

                val minY = (singlePoints.minOf { it.second } * 0.95).coerceAtLeast(0.0)
                val maxY = (singlePoints.maxOf { it.second } * 1.05).coerceAtLeast(minY + 1.0)

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Máx: ${String.format(Locale.getDefault(), "%.1f", maxY)} ${selectedMetric.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mín: ${String.format(Locale.getDefault(), "%.1f", minY)} ${selectedMetric.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        val steps = 3
                        for (i in 0..steps) {
                            val y = height * i / steps
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val points = if (singlePoints.size == 1) {
                            listOf(Offset(width / 2f, height / 2f))
                        } else {
                            singlePoints.mapIndexed { index, pair ->
                                val x = (index.toFloat() / (singlePoints.size - 1)) * width
                                val normalizedY = ((pair.second - minY) / (maxY - minY)).toFloat()
                                val y = height - (normalizedY * height)
                                Offset(x, y)
                            }
                        }

                        drawSeries(pathColor = lineColor, points = points)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = timeFormat.format(Date(singlePoints.first().first)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (singlePoints.size > 1) {
                            Text(
                                text = timeFormat.format(Date(singlePoints.last().first)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSeries(
    pathColor: Color,
    points: List<Offset>
) {
    if (points.isEmpty()) return

    if (points.size > 1) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = pathColor.copy(alpha = 0.3f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = pathColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    points.forEach { point ->
        drawCircle(
            color = pathColor,
            radius = 5.dp.toPx(),
            center = point
        )
        drawCircle(
            color = PaletteLime,
            radius = 2.dp.toPx(),
            center = point
        )
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = color,
            modifier = Modifier.height(6.dp).width(14.dp)
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
