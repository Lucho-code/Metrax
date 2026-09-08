package com.example.util

import com.example.data.model.MaterialType
import com.example.data.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryUtilsTest {

    @Test
    fun formatTonnage_metricShowsMetricTons() {
        assertEquals("2.50 t", GeometryUtils.formatTonnage(2.5, UnitSystem.METRIC))
    }

    @Test
    fun formatTonnage_imperialConvertsToUsShortTons() {
        // 2.5 metric tons * 1.10231 = 2.755775 US short tons
        assertEquals("2.76 ton (US)", GeometryUtils.formatTonnage(2.5, UnitSystem.IMPERIAL))
    }

    @Test
    fun formatTonnage_zeroIsZero() {
        assertEquals("0.00 t", GeometryUtils.formatTonnage(0.0, UnitSystem.METRIC))
    }

    @Test
    fun materialType_noneHasZeroDensity() {
        assertEquals(0.0, MaterialType.NONE.densityTonPerCubicMeter, 1e-9)
    }

    @Test
    fun materialType_everyRealMaterialHasPositiveDensity() {
        MaterialType.values()
            .filter { it != MaterialType.NONE }
            .forEach { material ->
                assertTrue(
                    "${material.name} should have a positive density",
                    material.densityTonPerCubicMeter > 0.0
                )
            }
    }

    @Test
    fun tonnageCalculation_volumeTimesDensity() {
        // Mirrors the calculation in MeasurementViewModel.calculateCurrentTonnage /
        // ArMeasurementViewModel.saveMeasurement: tons = volume(m3) * density(t/m3).
        val volumeCubicMeters = 12.0
        val tonnage = volumeCubicMeters * MaterialType.DRY_SAND.densityTonPerCubicMeter
        assertEquals(19.2, tonnage, 1e-9)
    }
}
