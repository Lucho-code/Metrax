package com.example

import com.example.util.MeasurementUnit
import com.example.util.UnitConverter
import com.example.util.UnitToggleManager
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun testLengthConversions() {
        val lengthMeters = 2.5

        // Meters
        assertEquals(2.5, UnitConverter.convertLength(lengthMeters, MeasurementUnit.METERS), 0.0001)
        assertEquals("2.50 m", UnitConverter.formatLength(lengthMeters, MeasurementUnit.METERS))

        // Centimeters (2.5 m = 250 cm)
        assertEquals(250.0, UnitConverter.convertLength(lengthMeters, MeasurementUnit.CENTIMETERS), 0.0001)
        assertEquals("250.00 cm", UnitConverter.formatLength(lengthMeters, MeasurementUnit.CENTIMETERS))

        // Feet (2.5 m * 3.28084 ≈ 8.2021 ft)
        assertEquals(8.2021, UnitConverter.convertLength(lengthMeters, MeasurementUnit.FEET), 0.001)
        assertEquals("8.20 ft", UnitConverter.formatLength(lengthMeters, MeasurementUnit.FEET))

        // Inches (2.5 m * 39.3701 ≈ 98.42525 in)
        assertEquals(98.42525, UnitConverter.convertLength(lengthMeters, MeasurementUnit.INCHES), 0.01)
        assertEquals("98.43 in", UnitConverter.formatLength(lengthMeters, MeasurementUnit.INCHES))
    }

    @Test
    fun testAreaConversions() {
        val areaSquareMeters = 10.0 // 10 m²

        // Meters: 10 m²
        assertEquals(10.0, UnitConverter.convertArea(areaSquareMeters, MeasurementUnit.METERS), 0.0001)
        assertEquals("10.00 m²", UnitConverter.formatArea(areaSquareMeters, MeasurementUnit.METERS))

        // Centimeters: 10 m² * 10,000 = 100,000 cm²
        assertEquals(100000.0, UnitConverter.convertArea(areaSquareMeters, MeasurementUnit.CENTIMETERS), 0.0001)
        assertEquals("100000.00 cm²", UnitConverter.formatArea(areaSquareMeters, MeasurementUnit.CENTIMETERS))

        // Feet: 10 m² * (3.28084)² ≈ 107.6391 ft²
        assertEquals(107.6391, UnitConverter.convertArea(areaSquareMeters, MeasurementUnit.FEET), 0.01)
        assertEquals("107.64 ft²", UnitConverter.formatArea(areaSquareMeters, MeasurementUnit.FEET))

        // Inches: 10 m² * (39.3701)² ≈ 15500.05 in²
        assertEquals(15500.05, UnitConverter.convertArea(areaSquareMeters, MeasurementUnit.INCHES), 0.1)
        assertEquals("15500.05 in²", UnitConverter.formatArea(areaSquareMeters, MeasurementUnit.INCHES))
    }

    @Test
    fun testVolumeConversions() {
        val volumeCubicMeters = 2.0 // 2 m³

        // Meters: 2 m³
        assertEquals(2.0, UnitConverter.convertVolume(volumeCubicMeters, MeasurementUnit.METERS), 0.0001)
        assertEquals("2.00 m³", UnitConverter.formatVolume(volumeCubicMeters, MeasurementUnit.METERS))

        // Centimeters: 2 m³ * 1,000,000 = 2,000,000 cm³
        assertEquals(2000000.0, UnitConverter.convertVolume(volumeCubicMeters, MeasurementUnit.CENTIMETERS), 0.0001)
        assertEquals("2000000.00 cm³", UnitConverter.formatVolume(volumeCubicMeters, MeasurementUnit.CENTIMETERS))

        // Feet: 2 m³ * (3.28084)³ ≈ 70.6293 ft³
        assertEquals(70.6293, UnitConverter.convertVolume(volumeCubicMeters, MeasurementUnit.FEET), 0.01)
        assertEquals("70.63 ft³", UnitConverter.formatVolume(volumeCubicMeters, MeasurementUnit.FEET))
    }

    @Test
    fun testUnitToggleManagerRealTimeCycle() {
        val manager = UnitToggleManager(MeasurementUnit.METERS)
        assertEquals(MeasurementUnit.METERS, manager.currentUnit)

        // Toggle sequence: METERS -> CENTIMETERS -> FEET -> INCHES -> METERS
        assertEquals(MeasurementUnit.CENTIMETERS, manager.toggleNext())
        assertEquals("100.00 cm", manager.formatLength(1.0))

        assertEquals(MeasurementUnit.FEET, manager.toggleNext())
        assertEquals("3.28 ft²", manager.formatArea(0.3048 * 0.3048 * 3.28))

        assertEquals(MeasurementUnit.INCHES, manager.toggleNext())
        assertEquals(MeasurementUnit.METERS, manager.toggleNext())
    }
}
