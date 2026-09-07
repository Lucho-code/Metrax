package com.example.ar

import com.example.data.model.ArVolumeResult
import com.example.data.model.WorldPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure geometry engine that turns real-world toe/boundary points (from
 * ARCore anchors) plus a mesh of depth-sampled surface points into a volume
 * estimate. Has no dependency on ARCore types so it can be unit tested
 * directly; the ARCore-specific screen projection + hit-testing that
 * produces the sample grid lives in [com.example.ar.ArVolumeRenderer].
 *
 * Technique: the caller samples a uniform grid of *screen* pixels covering
 * the pile and hit-tests each one, producing a 2D mesh of world-space
 * surface points (row-major, null where no depth was recovered). For every
 * 2x2 cell of that mesh where all four corners were resolved, we measure the
 * true horizontal (X,Z) area of that quad via the shoelace formula and
 * multiply by its average height above the base plane — a Riemann sum over
 * *actually measured* world-space cells, which stays correct under
 * perspective (unlike assuming a uniform world-space grid).
 */
object VolumeCalculator {

    /** Shoelace formula projected onto the horizontal (X,Z) plane. */
    fun polygonAreaXZ(points: List<WorldPoint>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += (a.x.toDouble() * b.z.toDouble()) - (b.x.toDouble() * a.z.toDouble())
        }
        return abs(sum) / 2.0
    }

    /** Average elevation (Y) of the toe points, used as the base reference plane. */
    fun baseElevation(points: List<WorldPoint>): Float {
        if (points.isEmpty()) return 0f
        return (points.sumOf { it.y.toDouble() } / points.size).toFloat()
    }

    private fun quadAreaXZ(p0: WorldPoint, p1: WorldPoint, p2: WorldPoint, p3: WorldPoint): Double {
        // Shoelace over the 4 corners in mesh (row/col) winding order.
        val xs = doubleArrayOf(p0.x.toDouble(), p1.x.toDouble(), p2.x.toDouble(), p3.x.toDouble())
        val zs = doubleArrayOf(p0.z.toDouble(), p1.z.toDouble(), p2.z.toDouble(), p3.z.toDouble())
        var sum = 0.0
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            sum += xs[i] * zs[j] - xs[j] * zs[i]
        }
        return abs(sum) / 2.0
    }

    /**
     * Integrates volume over a row-major mesh of depth-sampled world points
     * (null where the hit-test found no surface). [baseElevationY] is the
     * reference ground plane; heights below it are clamped to zero.
     */
    fun integrateGridVolume(
        mesh: List<List<WorldPoint?>>,
        baseElevationY: Float
    ): GridIntegrationResult {
        var volume = 0.0
        var maxHeight = 0.0
        var validQuads = 0
        var totalQuads = 0

        if (mesh.size < 2) return GridIntegrationResult(0.0, 0.0, 0, 0)

        for (row in 0 until mesh.size - 1) {
            val current = mesh[row]
            val next = mesh[row + 1]
            val cols = min(current.size, next.size)
            if (cols < 2) continue

            for (col in 0 until cols - 1) {
                totalQuads++
                val p0 = current[col]
                val p1 = current[col + 1]
                val p2 = next[col + 1]
                val p3 = next[col]
                if (p0 == null || p1 == null || p2 == null || p3 == null) continue

                validQuads++
                val area = quadAreaXZ(p0, p1, p2, p3)
                val avgHeight = listOf(p0, p1, p2, p3)
                    .map { max(0.0, (it.y - baseElevationY).toDouble()) }
                    .average()
                maxHeight = max(maxHeight, listOf(p0, p1, p2, p3).maxOf { max(0.0, (it.y - baseElevationY).toDouble()) })
                volume += area * avgHeight
            }
        }

        return GridIntegrationResult(volume, maxHeight, validQuads, totalQuads)
    }

    /**
     * Combines a toe-point base area/elevation with a depth-sampled surface
     * mesh into a full [ArVolumeResult], including confidence indicators.
     */
    fun buildResult(
        toePoints: List<WorldPoint>,
        mesh: List<List<WorldPoint?>>,
        toeTrackingRatio: Float,
        gridResolution: Int
    ): ArVolumeResult? {
        if (toePoints.size < 3) return null
        val baseArea = polygonAreaXZ(toePoints)
        if (baseArea <= 0.0) return null
        val baseY = baseElevation(toePoints)

        val integration = integrateGridVolume(mesh, baseY)
        if (integration.totalQuadCells == 0) return null

        val surfaceCoverage = integration.validQuadCells.toFloat() / integration.totalQuadCells.toFloat()

        return ArVolumeResult(
            volumeCubicMeters = integration.volume,
            baseAreaSquareMeters = baseArea,
            maxHeightMeters = integration.maxHeight,
            surfaceCoverageConfidence = surfaceCoverage.coerceIn(0f, 1f),
            toeCoverageConfidence = toeTrackingRatio.coerceIn(0f, 1f),
            gridResolution = gridResolution,
            toePoints = toePoints
        )
    }

    /** Ray-casting point-in-polygon test, generic over any 2D (a,b) coordinate pair. */
    fun isInsidePolygon(x: Float, y: Float, polygon: List<Pair<Float, Float>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val (xi, yi) = polygon[i]
            val (xj, yj) = polygon[j]
            val intersects = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    /** Clamps a requested grid resolution to a sane, performance-safe range. */
    fun clampGridResolution(requested: Int): Int = min(40, max(6, requested))

    data class GridIntegrationResult(
        val volume: Double,
        val maxHeight: Double,
        val validQuadCells: Int,
        val totalQuadCells: Int
    )
}
