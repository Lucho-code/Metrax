package com.example.data.model

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
