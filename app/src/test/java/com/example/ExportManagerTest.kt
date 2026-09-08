package com.example

import com.example.data.db.MeasurementEntity
import com.example.data.model.MeasurementMode
import com.example.data.model.UnitSystem
import com.example.util.ExportManager
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    private val sampleItems = listOf(
        MeasurementEntity(
            id = 1,
            title = "Pared Norte",
            mode = MeasurementMode.DISTANCE.name,
            value = 4.5,
            heightValue = 0.0,
            planeType = "WALL",
            createdAt = 1700000000000L
        ),
        MeasurementEntity(
            id = 2,
            title = "Piso Sala",
            mode = MeasurementMode.AREA.name,
            value = 18.2,
            heightValue = 0.0,
            planeType = "FLOOR",
            createdAt = 1700000100000L
        ),
        MeasurementEntity(
            id = 3,
            title = "Zapata Hormigón",
            mode = MeasurementMode.VOLUME.name,
            value = 2.4,
            heightValue = 0.6,
            planeType = "FLOOR",
            createdAt = 1700000200000L
        )
    )

    @Test
    fun testCsvExport() {
        val csv = ExportManager.generateCsvTable(sampleItems, UnitSystem.METRIC)
        assertTrue(csv.contains("ID,Título,Modo,Valor,Unidad,Altura,Superficie,Fecha"))
        assertTrue(csv.contains("Pared Norte"))
        assertTrue(csv.contains("Piso Sala"))
        assertTrue(csv.contains("Zapata Hormigón"))
    }

    @Test
    fun testJsonExport() {
        val json = ExportManager.generateJsonData(sampleItems, UnitSystem.METRIC)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]\n"))
        assertTrue(json.contains("\"title\": \"Pared Norte\""))
        assertTrue(json.contains("\"raw_value\": 18.2"))
    }

    @Test
    fun testMarkdownTableExport() {
        val md = ExportManager.generateMarkdownTable(sampleItems, UnitSystem.METRIC)
        assertTrue(md.contains("# 📊 Reporte de Mediciones - Metrax"))
        assertTrue(md.contains("| ID | Título | Modo | Resultado | Altura | Plano | Fecha |"))
        assertTrue(md.contains("| 1 | Pared Norte | DISTANCE |"))
    }

    @Test
    fun testHtmlReportWithChartsExport() {
        val html = ExportManager.generateHtmlReportWithChartsAndTable(sampleItems, UnitSystem.METRIC)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<svg"))
        assertTrue(html.contains("Pared Norte"))
        assertTrue(html.contains("Comparativa de Magnitudes Medidas"))
    }

    @Test
    fun testSvgChartExport() {
        val svg = ExportManager.generateSvgChart(sampleItems, UnitSystem.METRIC)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("Distancias"))
        assertTrue(svg.contains("Superficies"))
        assertTrue(svg.contains("Volúmenes"))
    }
}
