package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.MeasurementEntity
import com.example.data.model.MeasurementMode
import com.example.data.model.UnitSystem
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.util.ExportManager
import com.example.util.GeometryUtils

enum class ExportFormat(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val extension: String,
    val mimeType: String
) {
    HTML_REPORT(
        title = "Reporte HTML + Gráficos",
        subtitle = "Tabla estilizada e infografía visual interactiva",
        icon = Icons.Default.Html,
        extension = "html",
        mimeType = "text/html"
    ),
    SVG_CHART(
        title = "Gráfico Vectorial SVG",
        subtitle = "Diagrama comparativo de barras de alta calidad",
        icon = Icons.Default.BarChart,
        extension = "svg",
        mimeType = "image/svg+xml"
    ),
    CSV_TABLE(
        title = "Tabla CSV / Excel",
        subtitle = "Formato tabular estándar para planillas de cálculo",
        icon = Icons.Default.TableChart,
        extension = "csv",
        mimeType = "text/csv"
    ),
    MARKDOWN_TABLE(
        title = "Tabla Markdown / Texto",
        subtitle = "Texto estructurado para compartir en e-mails o chat",
        icon = Icons.Default.Description,
        extension = "md",
        mimeType = "text/markdown"
    ),
    JSON_DATA(
        title = "Datos JSON",
        subtitle = "Estructura técnica completa de mediciones",
        icon = Icons.Default.Code,
        extension = "json",
        mimeType = "application/json"
    )
}

@Composable
fun ExportDialog(
    items: List<MeasurementEntity>,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.HTML_REPORT) }

    val distItems = remember(items) { items.filter { it.mode == MeasurementMode.DISTANCE.name } }
    val areaItems = remember(items) { items.filter { it.mode == MeasurementMode.AREA.name } }
    val volItems = remember(items) { items.filter { it.mode == MeasurementMode.VOLUME.name } }

    val totalDist = remember(distItems) { distItems.sumOf { it.value } }
    val totalArea = remember(areaItems) { areaItems.sumOf { it.value } }
    val totalVol = remember(volItems) { volItems.sumOf { it.value } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = PrimaryOrange
                            )
                        }
                        Column {
                            Text(
                                text = "Exportar Mediciones",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Formatos de tablas y gráficos (${items.size} registros)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_export_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Visual Graphic Chart Summary Box
                    Text(
                        text = "📊 Vista Previa de Gráfico",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    ComposeBarChartPreview(
                        totalDist = totalDist,
                        totalArea = totalArea,
                        totalVol = totalVol,
                        distCount = distItems.size,
                        areaCount = areaItems.size,
                        volCount = volItems.size,
                        unitSystem = unitSystem
                    )

                    // 2. Select Format Section
                    Text(
                        text = "📁 Selección de Formato",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    ExportFormat.entries.forEach { format ->
                        val isSelected = format == selectedFormat
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = format }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.2f
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = format.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = format.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = format.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = ".${format.extension}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Code/Text Content Preview
                    Text(
                        text = "📝 Vista Previa de Contenido",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val previewText = remember(selectedFormat, items, unitSystem) {
                        when (selectedFormat) {
                            ExportFormat.CSV_TABLE -> ExportManager.generateCsvTable(items, unitSystem)
                            ExportFormat.MARKDOWN_TABLE -> ExportManager.generateMarkdownTable(items, unitSystem)
                            ExportFormat.JSON_DATA -> ExportManager.generateJsonData(items, unitSystem)
                            ExportFormat.SVG_CHART -> ExportManager.generateSvgChart(items, unitSystem)
                            ExportFormat.HTML_REPORT -> "<!-- Documento HTML completo con gráficas SVG y tabla de ${items.size} filas de mediciones -->"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = previewText.take(400) + if (previewText.length > 400) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val content = when (selectedFormat) {
                                ExportFormat.CSV_TABLE -> ExportManager.generateCsvTable(items, unitSystem)
                                ExportFormat.MARKDOWN_TABLE -> ExportManager.generateMarkdownTable(items, unitSystem)
                                ExportFormat.JSON_DATA -> ExportManager.generateJsonData(items, unitSystem)
                                ExportFormat.SVG_CHART -> ExportManager.generateSvgChart(items, unitSystem)
                                ExportFormat.HTML_REPORT -> ExportManager.generateHtmlReportWithChartsAndTable(items, unitSystem)
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Exportación ${selectedFormat.title}", content))
                            Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar")
                    }

                    Button(
                        onClick = {
                            val filename = "Mediciones_Metrax.${selectedFormat.extension}"
                            val content = when (selectedFormat) {
                                ExportFormat.CSV_TABLE -> ExportManager.generateCsvTable(items, unitSystem)
                                ExportFormat.MARKDOWN_TABLE -> ExportManager.generateMarkdownTable(items, unitSystem)
                                ExportFormat.JSON_DATA -> ExportManager.generateJsonData(items, unitSystem)
                                ExportFormat.SVG_CHART -> ExportManager.generateSvgChart(items, unitSystem)
                                ExportFormat.HTML_REPORT -> ExportManager.generateHtmlReportWithChartsAndTable(items, unitSystem)
                            }
                            ExportManager.shareExportFile(context, filename, selectedFormat.mimeType, content)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_share_export_file"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeBarChartPreview(
    totalDist: Double,
    totalArea: Double,
    totalVol: Double,
    distCount: Int,
    areaCount: Int,
    volCount: Int,
    unitSystem: UnitSystem
) {
    val maxVal = maxOf(totalDist, totalArea, totalVol, 1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bar 1: Distancias
            BarRowItem(
                label = "Distancias ($distCount)",
                formattedValue = GeometryUtils.formatLength(totalDist, unitSystem),
                ratio = (totalDist / maxVal).toFloat(),
                color = PrimaryOrange
            )

            // Bar 2: Áreas
            BarRowItem(
                label = "Superficies ($areaCount)",
                formattedValue = GeometryUtils.formatArea(totalArea, unitSystem),
                ratio = (totalArea / maxVal).toFloat(),
                color = SecondaryCyan
            )

            // Bar 3: Volúmenes
            BarRowItem(
                label = "Volúmenes ($volCount)",
                formattedValue = GeometryUtils.formatVolume(totalVol, unitSystem),
                ratio = (totalVol / maxVal).toFloat(),
                color = AccentEmerald
            )
        }
    }
}

@Composable
private fun BarRowItem(
    label: String,
    formattedValue: String,
    ratio: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            Text(text = formattedValue, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0.04f, 1.0f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
