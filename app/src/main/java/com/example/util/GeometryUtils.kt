package com.example.util

import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import kotlin.math.roundToInt
import kotlin.math.sqrt

object GeometryUtils {

    /**
     * Calculates Euclidean distance in 3D space between points a and b.
     * Assumes coordinates in meters.
     */
    fun distance3D(a: Point3D, b: Point3D): Double {
        val dx = (b.x - a.x).toDouble()
        val dy = (b.y - a.y).toDouble()
        val dz = (b.z - a.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates area of a 3D planar polygon using Newell's algorithm.
     * Assumes points are coplanar on detected surface plane.
     */
    fun polygonArea3D(points: List<Point3D>): Double {
        if (points.size < 3) return 0.0

        var nx = 0.0
        var ny = 0.0
        var nz = 0.0

        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]

            nx += (current.y - next.y) * (current.z + next.z)
            ny += (current.z - next.z) * (current.x + next.x)
            nz += (current.x - next.x) * (current.y + next.y)
        }

        return sqrt(nx * nx + ny * ny + nz * nz) / 2.0
    }

    /**
     * Calculates volume from surface area and height/depth dimension.
     */
    fun calculateVolume(areaSquareMeters: Double, heightMeters: Double): Double {
        if (areaSquareMeters <= 0.0 || heightMeters <= 0.0) return 0.0
        return areaSquareMeters * heightMeters
    }

    /**
     * Formats distance length in metric (m/cm) or imperial (ft/in).
     */
    fun formatLength(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        return if (unitSystem == UnitSystem.METRIC) {
            if (meters < 1.0) {
                "${(meters * 100).roundToInt()} cm"
            } else {
                String.format("%.2f m", meters)
            }
        } else {
            val totalInches = meters * 39.3701
            val feet = (totalInches / 12).toInt()
            val inches = (totalInches % 12).roundToInt()
            if (feet == 0) {
                "$inches in"
            } else if (inches == 0) {
                "$feet ft"
            } else {
                "$feet ft $inches in"
            }
        }
    }

    /**
     * Formats surface area in metric (m²/cm²) or imperial (ft²/in²).
     */
    fun formatArea(squareMeters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        return if (unitSystem == UnitSystem.METRIC) {
            if (squareMeters < 1.0) {
                "${(squareMeters * 10000).roundToInt()} cm²"
            } else {
                String.format("%.2f m²", squareMeters)
            }
        } else {
            val squareFeet = squareMeters * 10.7639
            if (squareFeet < 1.0) {
                val squareInches = squareMeters * 1550.0
                "${squareInches.roundToInt()} in²"
            } else {
                String.format("%.2f ft²", squareFeet)
            }
        }
    }

    /**
     * Formats volume in metric (m³, cm³, Litros) or imperial (ft³, in³, Galones).
     */
    fun formatVolume(cubicMeters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        return if (unitSystem == UnitSystem.METRIC) {
            val liters = cubicMeters * 1000.0
            if (cubicMeters < 0.01) {
                val cm3 = cubicMeters * 1000000.0
                "${cm3.roundToInt()} cm³"
            } else if (cubicMeters < 1.0) {
                String.format("%.1f L (%.3f m³)", liters, cubicMeters)
            } else {
                String.format("%.2f m³ (%.0f L)", cubicMeters, liters)
            }
        } else {
            val cubicFeet = cubicMeters * 35.3147
            val gallons = cubicMeters * 264.172
            if (cubicFeet < 0.1) {
                val cubicInches = cubicMeters * 61023.7
                "${cubicInches.roundToInt()} in³"
            } else {
                String.format("%.2f ft³ (%.1f gal)", cubicFeet, gallons)
            }
        }
    }

    /**
     * Formats an estimated weight in metric tons (t) or US short tons,
     * for the "Reporte en toneladas" material-density conversion.
     */
    fun formatTonnage(metricTons: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        return if (unitSystem == UnitSystem.METRIC) {
            String.format("%.2f t", metricTons)
        } else {
            val shortTons = metricTons * 1.10231
            String.format("%.2f ton (US)", shortTons)
        }
    }
}
