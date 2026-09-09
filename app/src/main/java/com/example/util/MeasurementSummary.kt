package com.example.util

import com.example.data.db.MeasurementEntity
import com.example.data.model.ConfidenceLevel
import com.example.data.model.MeasurementMode
import com.example.data.model.UnitSystem

/** Shareable plain-text summary of a saved measurement, used by both the history list and the detail screen. */
fun buildMeasurementSummaryText(item: MeasurementEntity, unitSystem: UnitSystem): String {
    val modeLabel = when (item.mode) {
        MeasurementMode.DISTANCE.name -> "Distancia"
        MeasurementMode.AREA.name -> "Área"
        MeasurementMode.VOLUME.name -> "Volumen"
        else -> item.mode
    }

    val formattedVal = when (item.mode) {
        MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
        MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
        MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
        else -> "${item.value}"
    }

    val extraInfo = if (item.mode == MeasurementMode.VOLUME.name && item.heightValue > 0) {
        "\n• Altura/Profundidad: ${GeometryUtils.formatLength(item.heightValue, unitSystem)}"
    } else ""

    val arInfo = if (item.method == "AR_POINT_CLOUD") {
        val coverage = item.surfaceCoverageConfidence?.let { "${(it * 100).toInt()}%" } ?: "N/D"
        "\n🔬 Método: ARCore Depth + Nube de puntos\n📡 Cobertura de superficie: $coverage"
    } else ""

    val tonnageInfo = item.tonnage?.let {
        "\n⚖️ Peso estimado: ${GeometryUtils.formatTonnage(it, unitSystem)} (${item.materialType ?: ""})"
    } ?: ""

    val locationInfo = if (item.latitude != null && item.longitude != null) {
        "\n📍 Ubicación: %.5f, %.5f".format(item.latitude, item.longitude)
    } else ""

    return "📏 Metrax - $modeLabel\n" +
            "📌 Título: ${item.title}\n" +
            "📊 Resultado: $formattedVal$extraInfo$tonnageInfo\n" +
            "🌐 Superficie: ${item.planeType}$arInfo\n" +
            "📐 Escala/Calibración: ${item.calibrationLabel}$locationInfo"
}

/** Combined confidence for a saved AR measurement: the weaker of its two coverage signals. */
fun overallConfidenceLevel(item: MeasurementEntity): ConfidenceLevel? {
    val surface = item.surfaceCoverageConfidence ?: return null
    val toe = item.toeCoverageConfidence ?: surface
    return ConfidenceLevel.fromRatio(minOf(surface, toe))
}
