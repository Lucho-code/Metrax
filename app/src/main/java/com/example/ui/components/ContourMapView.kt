package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ar.VolumeCalculator

private val BAND_COLORS = listOf(
    Color(0xFF1D4ED8), // deep blue (lowest)
    Color(0xFF0EA5E9),
    Color(0xFF22D3EE),
    Color(0xFF4ADE80),
    Color(0xFFFACC15),
    Color(0xFFF97316),
    Color(0xFFEF4444)  // red (highest)
)
private val NO_DATA_COLOR = Color(0xFFB91C1C)

/**
 * Color-banded height heatmap of a scanned pile (a topographic "contour map"
 * of discrete elevation bands, from blue/low to red/high), built from the
 * grid mesh used for the AR volume integration. Cells with no recovered
 * depth sample ([VolumeCalculator.NO_DATA_HEIGHT]) render as a hatched red
 * "area not seen by camera", matching SR Measure's coverage legend.
 */
@Composable
fun ContourMapView(
    heightGrid: List<List<Float>>,
    maxHeightMeters: Double,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.4f)
        ) {
            if (heightGrid.size < 2 || maxHeightMeters <= 0.0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sin datos suficientes para el mapa de contornos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val rows = heightGrid.size
                    val cols = heightGrid.maxOf { it.size }
                    if (cols < 2) return@Canvas
                    val cellWidth = size.width / cols
                    val cellHeight = size.height / rows
                    val maxHeight = maxHeightMeters.toFloat().coerceAtLeast(0.001f)
                    val hatchEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    for (row in 0 until rows) {
                        val rowData = heightGrid[row]
                        for (col in rowData.indices) {
                            val height = rowData[col]
                            val topLeft = Offset(col * cellWidth, (rows - 1 - row) * cellHeight)
                            val cellSize = Size(cellWidth, cellHeight)
                            if (height == VolumeCalculator.NO_DATA_HEIGHT) {
                                drawRect(color = NO_DATA_COLOR.copy(alpha = 0.35f), topLeft = topLeft, size = cellSize)
                                drawRect(
                                    color = NO_DATA_COLOR,
                                    topLeft = topLeft,
                                    size = cellSize,
                                    style = Stroke(width = 1f, pathEffect = hatchEffect)
                                )
                            } else {
                                val ratio = (height / maxHeight).coerceIn(0f, 1f)
                                val bandIndex = (ratio * (BAND_COLORS.size - 1)).toInt().coerceIn(0, BAND_COLORS.size - 1)
                                drawRect(color = BAND_COLORS[bandIndex], topLeft = topLeft, size = cellSize)
                            }
                        }
                    }
                }
            }
        }
        if (showLegend) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(14.dp).padding(1.dp)) {
                    Surface(color = NO_DATA_COLOR.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize()) {}
                }
                Text(
                    text = "Áreas no vistas por la cámara",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }
}
