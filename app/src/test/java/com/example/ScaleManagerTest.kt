package com.example

import com.example.util.DistanceUnit
import com.example.util.PixelPoint
import com.example.util.ScaleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleManagerTest {

    @Test
    fun testScaleCalculation() {
        val calculator = ScaleManager()
        
        // Reference segment: 100 pixels long
        val p1 = PixelPoint(0f, 0f)
        val p2 = PixelPoint(100f, 0f)
        
        // Known distance: 2 meters
        calculator.configureReference(p1, p2, 2.0, DistanceUnit.METERS)
        
        // Scale factor should be 2.0 / 100.0 = 0.02 meters/pixel
        val scaleMetersPerPx = calculator.calculateScaleFactorMetersPerPixel()
        assertEquals(0.02, scaleMetersPerPx, 0.0001)
        
        // Scale in pixels per meter: 100 / 2 = 50.0 px/m
        val pxPerMeter = calculator.calculateScaleFactorPixelsPerMeter()
        assertEquals(50.0, pxPerMeter, 0.0001)
        
        assertTrue(calculator.isValidScale())
        
        // Convert 250 pixels -> meters
        val distanceMeters = calculator.pixelsToMeters(250.0)
        assertEquals(5.0, distanceMeters, 0.0001)
        
        // Convert 10,000 px² area -> square meters: 10000 * (0.02^2) = 10000 * 0.0004 = 4.0 m²
        val areaSqMeters = calculator.pixelAreaToSquareMeters(10000.0)
        assertEquals(4.0, areaSqMeters, 0.0001)
        
        // Convert meters to pixels: 10 meters -> 500 pixels
        val pixels = calculator.metersToPixels(10.0)
        assertEquals(500.0, pixels, 0.0001)
    }

    @Test
    fun testUnitConversionInScale() {
        val calculator = ScaleManager()
        val p1 = PixelPoint(0f, 0f)
        val p2 = PixelPoint(200f, 0f) // 200 px
        
        // Known distance: 50 cm = 0.5 meters
        calculator.configureReference(p1, p2, 50.0, DistanceUnit.CENTIMETERS)
        
        // Scale factor: 0.5 / 200 = 0.0025 m/px
        assertEquals(0.0025, calculator.calculateScaleFactorMetersPerPixel(), 0.0001)
    }

    @Test
    fun testResetAndInvalidScale() {
        val calculator = ScaleManager()
        assertFalse(calculator.isValidScale())
        assertEquals(0.0, calculator.calculateScaleFactorMetersPerPixel(), 0.0001)
        
        calculator.setReferenceSegment(PixelPoint(0f, 0f), PixelPoint(50f, 0f))
        calculator.setKnownRealDistance(1.0, DistanceUnit.METERS)
        assertTrue(calculator.isValidScale())
        
        calculator.reset()
        assertFalse(calculator.isValidScale())
    }
}
