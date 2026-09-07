package com.example.data.model

import kotlin.math.min

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

enum class MeasurementMode(val label: String, val minPoints: Int, val description: String) {
    DISTANCE("Distancia", 2, "Mide la longitud entre dos o más puntos"),
    AREA("Área", 3, "Calcula la superficie delimitada por 3 o más puntos"),
    VOLUME("Volumen", 3, "Calcula el volumen ingresando la altura o profundidad")
}

enum class UnitSystem(val label: String) {
    METRIC("Métrico (m/cm/L)"),
    IMPERIAL("Imperial (ft/in/gal)")
}

enum class PlaneType(val displayName: String, val iconRes: String) {
    FLOOR("Piso / Suelo", "ic_floor"),
    TABLE("Mesa / Escritorio", "ic_table"),
    WALL("Pared / Vertical", "ic_wall"),
    AIR("Aire / Libre", "ic_air")
}

enum class CalibrationPreset(val displayName: String, val lengthMeters: Double) {
    CREDIT_CARD("Tarjeta de crédito (8.56 cm)", 0.0856),
    A4_PAPER("Hoja A4 (29.7 cm)", 0.297),
    ONE_METER_RULER("Regla de 1 metro (1.00 m)", 1.00),
    CUSTOM("Medida personalizada", 0.0)
}

/** How the active [scaleFactor] correction was (or should be) obtained. */
enum class CalibrationMethod(val displayName: String) {
    REFERENCE_OBJECT("Objeto de referencia"),
    KNOWN_MEASUREMENT("Medida real conocida")
}

/**
 * Typical bulk density (metric tons per cubic meter) for common construction
 * and aggregate materials, used to convert a measured volume into an
 * estimated weight ("Reporte en toneladas"). Values are general references —
 * real density varies with moisture, compaction and particle size, so this
 * is informational only and should be verified against the actual material.
 */
enum class MaterialType(val displayName: String, val densityTonPerCubicMeter: Double) {
    NONE("Sin especificar", 0.0),
    DRY_SAND("Arena seca", 1.6),
    WET_SAND("Arena húmeda", 1.9),
    GRAVEL("Ripio / Grava", 1.75),
    CRUSHED_STONE("Piedra partida", 1.6),
    TOPSOIL("Tierra vegetal", 1.5),
    CLAY("Arcilla", 1.9),
    ASPHALT_MILLINGS("Fresado asfáltico (RAP)", 1.9),
    MULCH("Mulch / Corteza", 0.4),
    COMPOST("Compost", 0.7)
}

/**
 * Method used to obtain a saved measurement.
 */
enum class MeasurementMethod {
    MANUAL_TAP,
    AR_POINT_CLOUD
}

/**
 * A point in real-world AR space (meters), as tracked by ARCore's
 * visual-inertial odometry. Y is up, matching ARCore's world coordinate frame.
 */
data class WorldPoint(
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Confidence bucket for a coverage ratio (0f..1f), mirroring the
 * "Surface/Toe Coverage Confidence" indicators of professional stockpile
 * volume scanners.
 */
enum class ConfidenceLevel(val displayName: String) {
    LOW("Baja"),
    MEDIUM("Media"),
    HIGH("Alta");

    companion object {
        fun fromRatio(ratio: Float): ConfidenceLevel = when {
            ratio >= 0.85f -> HIGH
            ratio >= 0.55f -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Result of a grid-sampled volumetric scan of a stockpile/pile using the
 * ARCore point cloud + depth to reconstruct the surface above a base plane.
 */
data class ArVolumeResult(
    val volumeCubicMeters: Double,
    val baseAreaSquareMeters: Double,
    val maxHeightMeters: Double,
    val surfaceCoverageConfidence: Float, // ratio of grid cells with a valid depth sample
    val toeCoverageConfidence: Float, // ratio of toe/boundary anchors still well tracked
    val gridResolution: Int,
    val toePoints: List<WorldPoint>,
    // Row-major height-above-base (meters) per sampled grid cell, 0f where no
    // depth was recovered. Transient/in-memory only (not persisted to Room) —
    // used to render the topographic contour/heatmap preview right after a scan.
    val heightGrid: List<List<Float>> = emptyList()
) {
    val surfaceCoverageLevel: ConfidenceLevel get() = ConfidenceLevel.fromRatio(surfaceCoverageConfidence)
    val toeCoverageLevel: ConfidenceLevel get() = ConfidenceLevel.fromRatio(toeCoverageConfidence)

    /** Combined confidence: the weaker of the two coverage signals, since either alone can hide a bad scan. */
    val overallConfidence: Float get() = min(surfaceCoverageConfidence, toeCoverageConfidence)
    val overallConfidenceLevel: ConfidenceLevel get() = ConfidenceLevel.fromRatio(overallConfidence)
}
