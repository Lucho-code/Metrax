package com.example

import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import com.example.util.GeometryUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class GeometryUtilsTest {

    @Test
    fun testDistance3D() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(3f, 4f, 0f)
        assertEquals(5.0, GeometryUtils.distance3D(a, b), 0.0001)
    }

    @Test
    fun testPolygonArea3DSquareOnFloor() {
        // 2m x 2m square on the horizontal (XZ) plane, ordered as a perimeter walk
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(2f, 0f, 0f),
            Point3D(2f, 0f, 2f),
            Point3D(0f, 0f, 2f)
        )
        assertEquals(4.0, GeometryUtils.polygonArea3D(square), 0.0001)
    }

    @Test
    fun testPolygonArea3DTooFewPoints() {
        val twoPoints = listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f))
        assertEquals(0.0, GeometryUtils.polygonArea3D(twoPoints), 0.0001)
    }

    @Test
    fun testCalculateVolume() {
        assertEquals(10.0, GeometryUtils.calculateVolume(5.0, 2.0), 0.0001)
        assertEquals(0.0, GeometryUtils.calculateVolume(0.0, 2.0), 0.0001)
        assertEquals(0.0, GeometryUtils.calculateVolume(5.0, 0.0), 0.0001)
    }

    @Test
    fun testPointCloudFootprintAreaIgnoresScanOrderAndInteriorPoints() {
        // Same 2m x 2m square footprint as above, but points arrive in scan
        // order (scrambled, not walked around the perimeter) plus a couple
        // of interior samples -- as a real ARCore point-cloud scan would
        // produce. The convex hull area must still come out ~4 m² and must
        // not depend on the order points were added.
        val scrambledCloud = listOf(
            Point3D(2f, 0f, 2f),
            Point3D(1f, 0f, 1f),   // interior point, should not affect hull
            Point3D(0f, 0f, 0f),
            Point3D(0.5f, 0f, 1.5f), // interior point
            Point3D(0f, 0f, 2f),
            Point3D(2f, 0f, 0f)
        )
        assertEquals(4.0, GeometryUtils.pointCloudFootprintArea(scrambledCloud), 0.01)
    }

    @Test
    fun testPointCloudFootprintAreaTooFewPoints() {
        val twoPoints = listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f))
        assertEquals(0.0, GeometryUtils.pointCloudFootprintArea(twoPoints), 0.0001)
    }

    @Test
    fun testPointCloudFootprintAreaOnTiltedPlane() {
        // Same square footprint but tilted (non-horizontal plane, as when
        // scanning a pile on uneven ground) -- area should still be ~ side^2
        // regardless of the plane's orientation in 3D space.
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(2f, 1f, 0f),
            Point3D(2f, 1f, 2f),
            Point3D(0f, 0f, 2f)
        )
        // The rectangle has one pair of sides of length 2 (Z axis) and the
        // other pair tilted by (dx=2, dy=1) => length sqrt(5).
        val expected = 2.0 * sqrt(5.0)
        assertEquals(expected, GeometryUtils.pointCloudFootprintArea(square), 0.01)
    }

    @Test
    fun testFormatLengthSwitchesToCentimetersBelowOneMeter() {
        assertEquals("50 cm", GeometryUtils.formatLength(0.5, UnitSystem.METRIC))
        assertEquals("1.50 m", GeometryUtils.formatLength(1.5, UnitSystem.METRIC))
    }

    @Test
    fun testFormatAreaAndVolumeMetric() {
        assertEquals("2.00 m²", GeometryUtils.formatArea(2.0, UnitSystem.METRIC))
        assertTrue(GeometryUtils.formatVolume(0.5, UnitSystem.METRIC).contains("L"))
    }
}
