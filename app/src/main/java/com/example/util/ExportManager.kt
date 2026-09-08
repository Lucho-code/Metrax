package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.MeasurementEntity
import com.example.data.model.MeasurementMode
import com.example.data.model.UnitSystem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting measurement data into various tabular and graphical formats:
 * - CSV Table (Excel compatible)
 * - JSON Data
 * - Markdown / Plain Text Table
 * - Interactive HTML Report with Built-in Charts & Tables
 * - Vector SVG Chart (Bar & Pie breakdown of measurements and surface distribution)
 */
object ExportManager {

    /**
     * Generates a CSV table string representing the measurement items.
     */
    fun generateCsvTable(items: List<MeasurementEntity>, unitSystem: UnitSystem): String {
        val sb = StringBuilder()
        sb.append("ID,Título,Modo,Valor,Unidad,Altura,Superficie,Fecha\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (item in items) {
            val formattedVal = when (item.mode) {
                MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
                MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
                MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
                else -> "${item.value}"
            }

            val unitSymbol = when (item.mode) {
                MeasurementMode.DISTANCE.name -> if (unitSystem == UnitSystem.METRIC) "m" else "ft"
                MeasurementMode.AREA.name -> if (unitSystem == UnitSystem.METRIC) "m²" else "ft²"
                MeasurementMode.VOLUME.name -> if (unitSystem == UnitSystem.METRIC) "m³" else "ft³"
                else -> ""
            }

            val heightStr = if (item.heightValue > 0) {
                GeometryUtils.formatLength(item.heightValue, unitSystem)
            } else "-"

            val dateStr = dateFormat.format(Date(item.createdAt))
            val titleClean = "\"${item.title.replace("\"", "\"\"")}\""

            sb.append("${item.id},$titleClean,${item.mode},\"$formattedVal\",$unitSymbol,\"$heightStr\",${item.planeType},\"$dateStr\"\n")
        }

        return sb.toString()
    }

    /**
     * Generates a formatted JSON string of all measurements.
     */
    fun generateJsonData(items: List<MeasurementEntity>, unitSystem: UnitSystem): String {
        val sb = StringBuilder()
        sb.append("[\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

        for ((index, item) in items.withIndex()) {
            val formattedVal = when (item.mode) {
                MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
                MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
                MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
                else -> "${item.value}"
            }

            sb.append("  {\n")
            sb.append("    \"id\": ${item.id},\n")
            sb.append("    \"title\": \"${escapeJson(item.title)}\",\n")
            sb.append("    \"mode\": \"${item.mode}\",\n")
            sb.append("    \"raw_value\": ${item.value},\n")
            sb.append("    \"formatted_value\": \"$formattedVal\",\n")
            sb.append("    \"height_value\": ${item.heightValue},\n")
            sb.append("    \"plane_type\": \"${item.planeType}\",\n")
            sb.append("    \"created_at\": \"${dateFormat.format(Date(item.createdAt))}\"\n")
            sb.append("  }${if (index < items.size - 1) "," else ""}\n")
        }

        sb.append("]\n")
        return sb.toString()
    }

    /**
     * Generates a clean Markdown table format for emails or documentation.
     */
    fun generateMarkdownTable(items: List<MeasurementEntity>, unitSystem: UnitSystem): String {
        val sb = StringBuilder()
        sb.append("# 📊 Reporte de Mediciones - Metrax\n\n")
        sb.append("| ID | Título | Modo | Resultado | Altura | Plano | Fecha |\n")
        sb.append("|:---|:-------|:-----|:----------|:-------|:------|:------|\n")

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (item in items) {
            val formattedVal = when (item.mode) {
                MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
                MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
                MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
                else -> "${item.value}"
            }

            val heightStr = if (item.heightValue > 0) GeometryUtils.formatLength(item.heightValue, unitSystem) else "-"
            val dateStr = dateFormat.format(Date(item.createdAt))
            val title = if (item.title.isBlank()) "Sin título" else item.title

            sb.append("| ${item.id} | $title | ${item.mode} | **$formattedVal** | $heightStr | ${item.planeType} | $dateStr |\n")
        }

        val totalDist = items.filter { it.mode == MeasurementMode.DISTANCE.name }.sumOf { it.value }
        val totalArea = items.filter { it.mode == MeasurementMode.AREA.name }.sumOf { it.value }
        val totalVol = items.filter { it.mode == MeasurementMode.VOLUME.name }.sumOf { it.value }

        sb.append("\n### 📈 Resumen General:\n")
        sb.append("- **Distancia Total Trazada:** ${GeometryUtils.formatLength(totalDist, unitSystem)}\n")
        sb.append("- **Superficie Total Medida:** ${GeometryUtils.formatArea(totalArea, unitSystem)}\n")
        sb.append("- **Volumen Total Cúbico:** ${GeometryUtils.formatVolume(totalVol, unitSystem)}\n")

        return sb.toString()
    }

    /**
     * Generates a full HTML document containing styled data tables and visual SVG bar & distribution graphs.
     */
    fun generateHtmlReportWithChartsAndTable(items: List<MeasurementEntity>, unitSystem: UnitSystem): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        val distItems = items.filter { it.mode == MeasurementMode.DISTANCE.name }
        val areaItems = items.filter { it.mode == MeasurementMode.AREA.name }
        val volItems = items.filter { it.mode == MeasurementMode.VOLUME.name }

        val totalDist = distItems.sumOf { it.value }
        val totalArea = areaItems.sumOf { it.value }
        val totalVol = volItems.sumOf { it.value }

        val svgChart = generateSvgChart(items, unitSystem, width = 700, height = 300)

        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reporte de Mediciones - Metrax</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 20px; margin: 0; }
                    .container { max-width: 900px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    h1 { color: #f97316; margin-bottom: 5px; font-size: 28px; }
                    .subtitle { color: #94a3b8; font-size: 14px; margin-bottom: 25px; }
                    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 30px; }
                    .card { background: #334155; padding: 18px; border-radius: 12px; border-left: 4px solid #f97316; }
                    .card.cyan { border-left-color: #06b6d4; }
                    .card.emerald { border-left-color: #10b981; }
                    .card-title { font-size: 12px; text-transform: uppercase; color: #94a3b8; font-weight: bold; }
                    .card-value { font-size: 22px; font-weight: bold; margin-top: 5px; color: #ffffff; }
                    .chart-section { background: #0f172a; padding: 20px; border-radius: 12px; text-align: center; margin-bottom: 30px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; background: #0f172a; border-radius: 8px; overflow: hidden; }
                    th { background: #f97316; color: #ffffff; text-align: left; padding: 12px 15px; font-size: 14px; }
                    td { padding: 12px 15px; border-bottom: 1px solid #334155; font-size: 14px; color: #e2e8f0; }
                    tr:hover { background: #1e293b; }
                    .badge { display: inline-block; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: bold; text-transform: uppercase; }
                    .badge-distance { background: rgba(249, 115, 22, 0.2); color: #f97316; }
                    .badge-area { background: rgba(6, 182, 212, 0.2); color: #06b6d4; }
                    .badge-volume { background: rgba(16, 185, 129, 0.2); color: #10b981; }
                    .footer { text-align: center; margin-top: 30px; color: #64748b; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📏 Metrax</h1>
                    <div class="subtitle">Reporte técnico de mediciones en obra • Generado el $generatedDate</div>
                    
                    <div class="metrics-grid">
                        <div class="card">
                            <div class="card-title">Distancia Total</div>
                            <div class="card-value">${GeometryUtils.formatLength(totalDist, unitSystem)}</div>
                        </div>
                        <div class="card cyan">
                            <div class="card-title">Superficie Total</div>
                            <div class="card-value">${GeometryUtils.formatArea(totalArea, unitSystem)}</div>
                        </div>
                        <div class="card emerald">
                            <div class="card-title">Volumen Total</div>
                            <div class="card-value">${GeometryUtils.formatVolume(totalVol, unitSystem)}</div>
                        </div>
                    </div>

                    <div class="chart-section">
                        <h3 style="color: #f8fafc; margin-top: 0;">Gráfico Comparativo de Mediciones</h3>
                        $svgChart
                    </div>

                    <h2 style="color: #f8fafc; font-size: 20px;">Tabla Detallada de Mediciones</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Título</th>
                                <th>Modo</th>
                                <th>Resultado</th>
                                <th>Plano</th>
                                <th>Fecha</th>
                            </tr>
                        </thead>
                        <tbody>
        """.trimIndent())

        for (item in items) {
            val badgeClass = when (item.mode) {
                MeasurementMode.DISTANCE.name -> "badge-distance"
                MeasurementMode.AREA.name -> "badge-area"
                else -> "badge-volume"
            }

            val formattedVal = when (item.mode) {
                MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
                MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
                MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
                else -> "${item.value}"
            }

            val title = if (item.title.isBlank()) "Medición #${item.id}" else item.title
            val dateStr = dateFormat.format(Date(item.createdAt))

            sb.append("""
                <tr>
                    <td>#${item.id}</td>
                    <td><strong>$title</strong></td>
                    <td><span class="badge $badgeClass">${item.mode}</span></td>
                    <td><strong>$formattedVal</strong></td>
                    <td>${item.planeType}</td>
                    <td>$dateStr</td>
                </tr>
            """.trimIndent())
        }

        sb.append("""
                        </tbody>
                    </table>
                    
                    <div class="footer">
                        Metrax — Sistema Profesional de Medición AR para Ingeniería y Arquitectura
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }

    /**
     * Generates a stand-alone SVG Vector Graphic Chart representing measurement counts, modes, and surface distributions.
     */
    fun generateSvgChart(
        items: List<MeasurementEntity>,
        unitSystem: UnitSystem,
        width: Int = 800,
        height: Int = 400
    ): String {
        val distCount = items.count { it.mode == MeasurementMode.DISTANCE.name }
        val areaCount = items.count { it.mode == MeasurementMode.AREA.name }
        val volCount = items.count { it.mode == MeasurementMode.VOLUME.name }

        val totalDist = items.filter { it.mode == MeasurementMode.DISTANCE.name }.sumOf { it.value }
        val totalArea = items.filter { it.mode == MeasurementMode.AREA.name }.sumOf { it.value }
        val totalVol = items.filter { it.mode == MeasurementMode.VOLUME.name }.sumOf { it.value }

        val maxVal = maxOf(totalDist, totalArea, totalVol, 1.0)

        // Heights proportional to max
        val barHeightDist = ((totalDist / maxVal) * 180).toInt().coerceAtLeast(10)
        val barHeightArea = ((totalArea / maxVal) * 180).toInt().coerceAtLeast(10)
        val barHeightVol = ((totalVol / maxVal) * 180).toInt().coerceAtLeast(10)

        val distFormatted = GeometryUtils.formatLength(totalDist, unitSystem)
        val areaFormatted = GeometryUtils.formatArea(totalArea, unitSystem)
        val volFormatted = GeometryUtils.formatVolume(totalVol, unitSystem)

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="100%" height="100%" style="background-color: #0f172a; font-family: sans-serif; border-radius: 12px;">
                <!-- Header Title -->
                <text x="30" y="40" fill="#f8fafc" font-size="18" font-weight="bold">Comparativa de Magnitudes Medidas</text>
                <text x="30" y="60" fill="#94a3b8" font-size="12">Total de ítems registrados: ${items.size}</text>

                <!-- Grid Background Lines -->
                <line x1="80" y1="260" x2="720" y2="260" stroke="#334155" stroke-width="2"/>
                <line x1="80" y1="200" x2="720" y2="200" stroke="#1e293b" stroke-dasharray="4"/>
                <line x1="80" y1="140" x2="720" y2="140" stroke="#1e293b" stroke-dasharray="4"/>
                <line x1="80" y1="80" x2="720" y2="80" stroke="#1e293b" stroke-dasharray="4"/>

                <!-- Bar 1: Distancias -->
                <rect x="150" y="${260 - barHeightDist}" width="80" height="$barHeightDist" rx="6" fill="#f97316"/>
                <text x="190" y="${250 - barHeightDist}" fill="#f97316" font-size="13" font-weight="bold" text-anchor="middle">$distFormatted</text>
                <text x="190" y="285" fill="#f8fafc" font-size="14" font-weight="bold" text-anchor="middle">Distancias</text>
                <text x="190" y="305" fill="#94a3b8" font-size="12" text-anchor="middle">$distCount registros</text>

                <!-- Bar 2: Áreas -->
                <rect x="360" y="${260 - barHeightArea}" width="80" height="$barHeightArea" rx="6" fill="#06b6d4"/>
                <text x="400" y="${250 - barHeightArea}" fill="#06b6d4" font-size="13" font-weight="bold" text-anchor="middle">$areaFormatted</text>
                <text x="400" y="285" fill="#f8fafc" font-size="14" font-weight="bold" text-anchor="middle">Superficies</text>
                <text x="400" y="305" fill="#94a3b8" font-size="12" text-anchor="middle">$areaCount registros</text>

                <!-- Bar 3: Volúmenes -->
                <rect x="570" y="${260 - barHeightVol}" width="80" height="$barHeightVol" rx="6" fill="#10b981"/>
                <text x="610" y="${250 - barHeightVol}" fill="#10b981" font-size="13" font-weight="bold" text-anchor="middle">$volFormatted</text>
                <text x="610" y="285" fill="#f8fafc" font-size="14" font-weight="bold" text-anchor="middle">Volúmenes</text>
                <text x="610" y="305" fill="#94a3b8" font-size="12" text-anchor="middle">$volCount registros</text>

                <!-- Legend Footer -->
                <rect x="30" y="345" width="740" height="40" rx="8" fill="#1e293b"/>
                <circle cx="60" cy="365" r="6" fill="#f97316"/>
                <text x="75" y="369" fill="#e2e8f0" font-size="12">Longitud</text>

                <circle cx="280" cy="365" r="6" fill="#06b6d4"/>
                <text x="295" y="369" fill="#e2e8f0" font-size="12">Superficie</text>

                <circle cx="500" cy="365" r="6" fill="#10b981"/>
                <text x="515" y="369" fill="#e2e8f0" font-size="12">Volumen Cúbico</text>
            </svg>
        """.trimIndent()
    }

    /**
     * Shares plain text or formatted data string using Android share sheet.
     */
    fun shareTextData(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir $title"))
    }

    /**
     * Writes content to a file in cache directory and shares it using FileProvider.
     */
    fun shareExportFile(context: Context, filename: String, mimeType: String, content: String) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, filename)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Exportación: $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir archivo $filename"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text sharing
            shareTextData(context, content, filename)
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
