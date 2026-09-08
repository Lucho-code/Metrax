package com.example.ar

import com.example.data.model.WorldPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeCalculatorTest {

    @Test
    fun polygonAreaXZ_computesSquareArea() {
        val square = listOf(
            WorldPoint(-1f, 0f, -1f),
            WorldPoint(1f, 0f, -1f),
            WorldPoint(1f, 0f, 1f),
            WorldPoint(-1f, 0f, 1f)
        )
        assertEquals(4.0, VolumeCalculator.polygonAreaXZ(square), 1e-6)
    }

    @Test
    fun baseElevation_averagesY() {
        val points = listOf(
            WorldPoint(0f, 1f, 0f),
            WorldPoint(1f, 2f, 0f),
            WorldPoint(1f, 3f, 1f)
        )
        assertEquals(2.0f, VolumeCalculator.baseElevation(points), 1e-6f)
    }

    @Test
    fun integrateGridVolume_singleFullyValidQuad_computesAreaTimesHeight() {
        val mesh = listOf(
            listOf(WorldPoint(0f, 2f, 0f), WorldPoint(1f, 2f, 0f)),
            listOf(WorldPoint(0f, 2f, 1f), WorldPoint(1f, 2f, 1f))
        )
        val result = VolumeCalculator.integrateGridVolume(mesh, baseElevationY = 0f)

        assertEquals(2.0, result.volume, 1e-6) // 1 m^2 footprint * 2 m height
        assertEquals(2.0, result.maxHeight, 1e-6)
        assertEquals(1, result.validQuadCells)
        assertEquals(1, result.totalQuadCells)
    }

    @Test
    fun integrateGridVolume_missingCornerExcludesQuad() {
        val mesh = listOf(
            listOf(WorldPoint(0f, 2f, 0f), WorldPoint(1f, 2f, 0f)),
            listOf(null, WorldPoint(1f, 2f, 1f))
        )
        val result = VolumeCalculator.integrateGridVolume(mesh, baseElevationY = 0f)

        assertEquals(0.0, result.volume, 1e-6)
        assertEquals(0, result.validQuadCells)
        assertEquals(1, result.totalQuadCells)
    }

    @Test
    fun integrateGridVolume_clampsHeightBelowBaseToZero() {
        // A surface sample below the base plane must not subtract volume.
        val mesh = listOf(
            listOf(WorldPoint(0f, -5f, 0f), WorldPoint(1f, -5f, 0f)),
            listOf(WorldPoint(0f, -5f, 1f), WorldPoint(1f, -5f, 1f))
        )
        val result = VolumeCalculator.integrateGridVolume(mesh, baseElevationY = 0f)

        assertEquals(0.0, result.volume, 1e-6)
        assertEquals(0.0, result.maxHeight, 1e-6)
    }

    @Test
    fun buildResult_combinesToeAreaAndMeshIntegration() {
        val toePoints = listOf(
            WorldPoint(-1f, 0f, -1f),
            WorldPoint(1f, 0f, -1f),
            WorldPoint(1f, 0f, 1f),
            WorldPoint(-1f, 0f, 1f)
        )
        val mesh = listOf(
            listOf(WorldPoint(0f, 1.5f, 0f), WorldPoint(1f, 1.5f, 0f)),
            listOf(WorldPoint(0f, 1.5f, 1f), WorldPoint(1f, 1.5f, 1f))
        )

        val result = VolumeCalculator.buildResult(
            toePoints = toePoints,
            mesh = mesh,
            toeTrackingRatio = 0.75f,
            gridResolution = 20
        )

        checkNotNull(result)
        assertEquals(4.0, result.baseAreaSquareMeters, 1e-6)
        assertEquals(1.5, result.volumeCubicMeters, 1e-6)
        assertEquals(1.0f, result.surfaceCoverageConfidence, 1e-6f)
        assertEquals(0.75f, result.toeCoverageConfidence, 1e-6f)
    }

    @Test
    fun buildResult_returnsNullForDegenerateToePolygon() {
        val collinearToePoints = listOf(
            WorldPoint(0f, 0f, 0f),
            WorldPoint(1f, 0f, 0f),
            WorldPoint(2f, 0f, 0f)
        )
        val mesh = listOf(
            listOf(WorldPoint(0f, 1f, 0f), WorldPoint(1f, 1f, 0f)),
            listOf(WorldPoint(0f, 1f, 1f), WorldPoint(1f, 1f, 1f))
        )
        assertNull(VolumeCalculator.buildResult(collinearToePoints, mesh, 1f, 20))
    }

    @Test
    fun isInsidePolygon_raycastsCorrectly() {
        val square = listOf(0f to 0f, 4f to 0f, 4f to 4f, 0f to 4f)
        assertTrue(VolumeCalculator.isInsidePolygon(2f, 2f, square))
        assertFalse(VolumeCalculator.isInsidePolygon(5f, 5f, square))
    }

    @Test
    fun clampGridResolution_staysWithinSafeBounds() {
        assertEquals(6, VolumeCalculator.clampGridResolution(1))
        assertEquals(40, VolumeCalculator.clampGridResolution(1000))
        assertEquals(20, VolumeCalculator.clampGridResolution(20))
    }

    @Test
    fun distance3D_computesEuclideanDistance() {
        val a = WorldPoint(0f, 0f, 0f)
        val b = WorldPoint(3f, 4f, 0f)
        assertEquals(5.0, VolumeCalculator.distance3D(a, b), 1e-6)
    }

    @Test
    fun lengthCorrectionFactor_computesRatioOfTrueToMeasured() {
        assertEquals(2.0, VolumeCalculator.lengthCorrectionFactor(1.0, 2.0)!!, 1e-6)
        assertEquals(0.5, VolumeCalculator.lengthCorrectionFactor(2.0, 1.0)!!, 1e-6)
        assertNull(VolumeCalculator.lengthCorrectionFactor(0.0, 1.0))
        assertNull(VolumeCalculator.lengthCorrectionFactor(1.0, 0.0))
    }

    @Test
    fun applyLengthCorrection_scalesAreaByFactorSquaredAndVolumeByFactorCubed() {
        val result = com.example.data.model.ArVolumeResult(
            volumeCubicMeters = 10.0,
            baseAreaSquareMeters = 5.0,
            maxHeightMeters = 2.0,
            surfaceCoverageConfidence = 1.0f,
            toeCoverageConfidence = 1.0f,
            gridResolution = 20,
            toePoints = emptyList()
        )

        val corrected = VolumeCalculator.applyLengthCorrection(result, 2.0)

        assertEquals(80.0, corrected.volumeCubicMeters, 1e-6) // 10 * 2^3
        assertEquals(20.0, corrected.baseAreaSquareMeters, 1e-6) // 5 * 2^2
        assertEquals(4.0, corrected.maxHeightMeters, 1e-6) // 2 * 2
    }

    @Test
    fun applyLengthCorrection_scalesHeightGridSoContourMapStaysConsistentWithCorrectedHeight() {
        val result = com.example.data.model.ArVolumeResult(
            volumeCubicMeters = 10.0,
            baseAreaSquareMeters = 5.0,
            maxHeightMeters = 2.0,
            surfaceCoverageConfidence = 1.0f,
            toeCoverageConfidence = 1.0f,
            gridResolution = 20,
            toePoints = emptyList(),
            heightGrid = listOf(listOf(1.0f, 2.0f), listOf(0.5f, 1.5f))
        )

        val corrected = VolumeCalculator.applyLengthCorrection(result, 2.0)

        assertEquals(listOf(listOf(2.0f, 4.0f), listOf(1.0f, 3.0f)), corrected.heightGrid)
    }

    @Test
    fun applyLengthCorrection_isNoOpForFactorOfOne() {
        val result = com.example.data.model.ArVolumeResult(
            volumeCubicMeters = 10.0,
            baseAreaSquareMeters = 5.0,
            maxHeightMeters = 2.0,
            surfaceCoverageConfidence = 1.0f,
            toeCoverageConfidence = 1.0f,
            gridResolution = 20,
            toePoints = emptyList()
        )

        assertEquals(result, VolumeCalculator.applyLengthCorrection(result, 1.0))
    }
}
