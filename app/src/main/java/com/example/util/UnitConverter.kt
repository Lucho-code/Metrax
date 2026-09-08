package com.example.util

import java.util.Locale
import kotlin.math.pow

/**
 * Supported measurement units for real-time toggling and conversions.
 */
enum class MeasurementUnit(
    val id: String,
    val displayName: String,
    val lengthSymbol: String,
    val areaSymbol: String,
    val volumeSymbol: String,
    val metersMultiplier: Double // Multiplier to convert 1 meter to this unit
) {
    METERS(
        id = "m",
        displayName = "Metros (m)",
        lengthSymbol = "m",
        areaSymbol = "m²",
        volumeSymbol = "m³",
        metersMultiplier = 1.0
    ),
    CENTIMETERS(
        id = "cm",
        displayName = "Centímetros (cm)",
        lengthSymbol = "cm",
        areaSymbol = "cm²",
        volumeSymbol = "cm³",
        metersMultiplier = 100.0
    ),
    FEET(
        id = "ft",
        displayName = "Pies (ft)",
        lengthSymbol = "ft",
        areaSymbol = "ft²",
        volumeSymbol = "ft³",
        metersMultiplier = 3.28084
    ),
    INCHES(
        id = "in",
        displayName = "Pulgadas (in)",
        lengthSymbol = "in",
        areaSymbol = "in²",
        volumeSymbol = "in³",
        metersMultiplier = 39.3701
    );

    /**
     * Gets next unit in rotation for real-time toggling in UI components.
     */
    fun next(): MeasurementUnit {
        val values = entries
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}

/**
 * Utility for converting and formatting length, area, and volume values in real-time
 * between meters, centimeters, feet, and inches.
 */
object UnitConverter {

    /**
     * Converts a length in meters to the specified target unit.
     */
    fun convertLength(lengthMeters: Double, targetUnit: MeasurementUnit): Double {
        return lengthMeters * targetUnit.metersMultiplier
    }

    /**
     * Converts a surface area in square meters (m²) to the specified target unit squared.
     */
    fun convertArea(areaSquareMeters: Double, targetUnit: MeasurementUnit): Double {
        val areaMultiplier = targetUnit.metersMultiplier.pow(2)
        return areaSquareMeters * areaMultiplier
    }

    /**
     * Converts a volume in cubic meters (m³) to the specified target unit cubed.
     */
    fun convertVolume(volumeCubicMeters: Double, targetUnit: MeasurementUnit): Double {
        val volumeMultiplier = targetUnit.metersMultiplier.pow(3)
        return volumeCubicMeters * volumeMultiplier
    }

    /**
     * Formats a length value (given in meters) to a formatted string using the target unit.
     */
    fun formatLength(
        lengthMeters: Double,
        targetUnit: MeasurementUnit,
        decimals: Int = 2
    ): String {
        val converted = convertLength(lengthMeters, targetUnit)
        return String.format(Locale.US, "%.${decimals}f %s", converted, targetUnit.lengthSymbol)
    }

    /**
     * Formats an area value (given in square meters) to a formatted string using the target unit.
     */
    fun formatArea(
        areaSquareMeters: Double,
        targetUnit: MeasurementUnit,
        decimals: Int = 2
    ): String {
        val converted = convertArea(areaSquareMeters, targetUnit)
        return String.format(Locale.US, "%.${decimals}f %s", converted, targetUnit.areaSymbol)
    }

    /**
     * Formats a volume value (given in cubic meters) to a formatted string using the target unit.
     */
    fun formatVolume(
        volumeCubicMeters: Double,
        targetUnit: MeasurementUnit,
        decimals: Int = 2
    ): String {
        val converted = convertVolume(volumeCubicMeters, targetUnit)
        return String.format(Locale.US, "%.${decimals}f %s", converted, targetUnit.volumeSymbol)
    }
}

/**
 * Stateful manager class to hold the active measurement unit and perform real-time toggling.
 */
class UnitToggleManager(initialUnit: MeasurementUnit = MeasurementUnit.METERS) {

    var currentUnit: MeasurementUnit = initialUnit
        private set

    /**
     * Toggles to the next measurement unit in sequence (Meters -> Centimeters -> Feet -> Inches).
     * @return the newly selected unit.
     */
    fun toggleNext(): MeasurementUnit {
        currentUnit = currentUnit.next()
        return currentUnit
    }

    /**
     * Sets a specific measurement unit.
     */
    fun setUnit(unit: MeasurementUnit) {
        currentUnit = unit
    }

    /**
     * Real-time formatted length for the current active unit.
     */
    fun formatLength(lengthMeters: Double, decimals: Int = 2): String {
        return UnitConverter.formatLength(lengthMeters, currentUnit, decimals)
    }

    /**
     * Real-time formatted area for the current active unit.
     */
    fun formatArea(areaSquareMeters: Double, decimals: Int = 2): String {
        return UnitConverter.formatArea(areaSquareMeters, currentUnit, decimals)
    }

    /**
     * Real-time formatted volume for the current active unit.
     */
    fun formatVolume(volumeCubicMeters: Double, decimals: Int = 2): String {
        return UnitConverter.formatVolume(volumeCubicMeters, currentUnit, decimals)
    }
}
