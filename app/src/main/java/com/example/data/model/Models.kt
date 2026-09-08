package com.example.data.model

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

data class PointCloudPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Float = 1.0f,
    val depth: Float = 1.0f,
    val colorHue: Float = 0.5f
)

data class BoundingBox3D(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0.01f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0.01f)
    val depth: Float get() = (maxZ - minZ).coerceAtLeast(0.01f)
    val volume: Float get() = width * height * depth
    val center: Point3D get() = Point3D(
        (minX + maxX) / 2f,
        (minY + maxY) / 2f,
        (minZ + maxZ) / 2f
    )
}

enum class PointCloudColorMap(val displayName: String) {
    HEATMAP("LiDAR Térmico"),
    NEON_CYAN("Neón Cyber"),
    SPECTRUM("Espectro 3D"),
    MONOCHROME("Técnico")
}

enum class MeasurementMode(val label: String, val minPoints: Int, val description: String) {
    DISTANCE("Distancia", 2, "Mide la longitud entre dos o más puntos"),
    AREA("Área", 3, "Calcula la superficie delimitada por 3 o más puntos"),
    VOLUME("Escaneo 3D / Nube", 1, "Escaneo 3D con nube de puntos LiDAR, dimensiones y volumen")
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
