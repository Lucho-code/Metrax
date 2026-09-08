package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.BoundingBox3D
import com.example.data.model.PointCloudColorMap
import com.example.data.model.PointCloudPoint
import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import java.util.Locale
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
     * Estimates footprint area from an unordered 3D point cloud by projecting
     * points onto their best-fit plane (Newell's normal) and computing the
     * convex hull area there. Point-cloud samples arrive in scan order, not
     * perimeter order, so polygonArea3D (which needs ordered vertices) would
     * give a near-random result if fed the raw cloud directly.
     */
    fun pointCloudFootprintArea(points: List<Point3D>): Double {
        if (points.size < 3) return 0.0

        var nx = 0.0; var ny = 0.0; var nz = 0.0
        var cx = 0.0; var cy = 0.0; var cz = 0.0
        for (i in points.indices) {
            val cur = points[i]
            val next = points[(i + 1) % points.size]
            nx += (cur.y - next.y) * (cur.z + next.z)
            ny += (cur.z - next.z) * (cur.x + next.x)
            nz += (cur.x - next.x) * (cur.y + next.y)
            cx += cur.x; cy += cur.y; cz += cur.z
        }
        val n = points.size
        cx /= n; cy /= n; cz /= n

        var len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len < 1e-9) { nx = 0.0; ny = 1.0; nz = 0.0; len = 1.0 }
        nx /= len; ny /= len; nz /= len

        // Build an orthonormal basis (ux,uy,uz)/(vx,vy,vz) spanning the plane.
        val (ax, ay, az) = if (kotlin.math.abs(ny) < 0.9) Triple(0.0, 1.0, 0.0) else Triple(1.0, 0.0, 0.0)
        var ux = ny * az - nz * ay
        var uy = nz * ax - nx * az
        var uz = nx * ay - ny * ax
        val ulen = sqrt(ux * ux + uy * uy + uz * uz)
        ux /= ulen; uy /= ulen; uz /= ulen
        val vx = ny * uz - nz * uy
        val vy = nz * ux - nx * uz
        val vz = nx * uy - ny * ux

        val projected = points.map { p ->
            val dx = p.x - cx; val dy = p.y - cy; val dz = p.z - cz
            Pair(dx * ux + dy * uy + dz * uz, dx * vx + dy * vy + dz * vz)
        }

        val hull = convexHull2D(projected)
        if (hull.size < 3) return 0.0

        var area = 0.0
        for (i in hull.indices) {
            val (x1, y1) = hull[i]
            val (x2, y2) = hull[(i + 1) % hull.size]
            area += x1 * y2 - x2 * y1
        }
        return kotlin.math.abs(area) / 2.0
    }

    /**
     * Andrew's monotone chain convex hull on 2D points.
     */
    private fun convexHull2D(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        val sorted = points.distinct().sortedWith(compareBy({ it.first }, { it.second }))
        if (sorted.size < 3) return sorted

        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)

        val lower = mutableListOf<Pair<Double, Double>>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
                lower.removeAt(lower.size - 1)
            }
            lower.add(p)
        }
        val upper = mutableListOf<Pair<Double, Double>>()
        for (p in sorted.reversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
                upper.removeAt(upper.size - 1)
            }
            upper.add(p)
        }
        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)
        return lower + upper
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
                String.format(Locale.US, "%.2f m", meters)
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
                String.format(Locale.US, "%.2f m²", squareMeters)
            }
        } else {
            val squareFeet = squareMeters * 10.7639
            if (squareFeet < 1.0) {
                val squareInches = squareMeters * 1550.0
                "${squareInches.roundToInt()} in²"
            } else {
                String.format(Locale.US, "%.2f ft²", squareFeet)
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
                String.format(Locale.US, "%.1f L (%.3f m³)", liters, cubicMeters)
            } else {
                String.format(Locale.US, "%.2f m³ (%.0f L)", cubicMeters, liters)
            }
        } else {
            val cubicFeet = cubicMeters * 35.3147
            val gallons = cubicMeters * 264.172
            if (cubicFeet < 0.1) {
                val cubicInches = cubicMeters * 61023.7
                "${cubicInches.roundToInt()} in³"
            } else {
                String.format(Locale.US, "%.2f ft³ (%.1f gal)", cubicFeet, gallons)
            }
        }
    }

    /**
     * Calculates axis-aligned 3D bounding box for a point cloud.
     */
    fun calculateBoundingBox(points: List<PointCloudPoint>): BoundingBox3D? {
        if (points.isEmpty()) return null
        var minX = points[0].x
        var maxX = points[0].x
        var minY = points[0].y
        var maxY = points[0].y
        var minZ = points[0].z
        var maxZ = points[0].z

        for (pt in points) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
            if (pt.z < minZ) minZ = pt.z
            if (pt.z > maxZ) maxZ = pt.z
        }

        return BoundingBox3D(minX, maxX, minY, maxY, minZ, maxZ)
    }

    fun distance3D(a: PointCloudPoint, b: PointCloudPoint): Double {
        val dx = (b.x - a.x).toDouble()
        val dy = (b.y - a.y).toDouble()
        val dz = (b.z - a.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Returns color representation for a point in point cloud based on selected color map.
     */
    fun getColorForPoint(point: PointCloudPoint, colorMap: PointCloudColorMap): Color {
        val hue = point.colorHue.coerceIn(0f, 1f)
        val conf = point.confidence.coerceIn(0.2f, 1f)
        return when (colorMap) {
            PointCloudColorMap.HEATMAP -> {
                // Rainbow heatmap from Red (near/low) -> Yellow -> Green -> Cyan -> Blue (far/high)
                when {
                    hue < 0.2f -> {
                        val t = hue / 0.2f
                        Color(red = 1f, green = t * 0.8f, blue = 0.2f, alpha = conf)
                    }
                    hue < 0.4f -> {
                        val t = (hue - 0.2f) / 0.2f
                        Color(red = 1f - t * 0.8f, green = 1f, blue = 0.1f, alpha = conf)
                    }
                    hue < 0.7f -> {
                        val t = (hue - 0.4f) / 0.3f
                        Color(red = 0.1f, green = 1f - t * 0.3f, blue = 0.4f + t * 0.6f, alpha = conf)
                    }
                    else -> {
                        val t = (hue - 0.7f) / 0.3f
                        Color(red = t * 0.8f, green = 0.2f, blue = 1f, alpha = conf)
                    }
                }
            }
            PointCloudColorMap.NEON_CYAN -> {
                Color(red = 0.0f, green = 0.94f, blue = 1.0f, alpha = conf)
            }
            PointCloudColorMap.SPECTRUM -> {
                Color(red = 0.0f, green = 1.0f - hue * 0.4f, blue = 0.6f + hue * 0.4f, alpha = conf)
            }
            PointCloudColorMap.MONOCHROME -> {
                Color(red = 1.0f, green = 0.73f, blue = 0.2f, alpha = conf) // High-precision amber
            }
        }
    }

    /**
     * Generates Stanford PLY 3D Point Cloud ASCII file format.
     */
    fun generatePlyPointCloud(points: List<PointCloudPoint>): String {
        val sb = StringBuilder()
        sb.append("ply\n")
        sb.append("format ascii 1.0\n")
        sb.append("comment Metrax 3D LiDAR Point Cloud Scan\n")
        sb.append("element vertex ${points.size}\n")
        sb.append("property float x\n")
        sb.append("property float y\n")
        sb.append("property float z\n")
        sb.append("property uchar red\n")
        sb.append("property uchar green\n")
        sb.append("property uchar blue\n")
        sb.append("end_header\n")

        for (pt in points) {
            val col = getColorForPoint(pt, PointCloudColorMap.HEATMAP)
            val r = (col.red * 255).toInt().coerceIn(0, 255)
            val g = (col.green * 255).toInt().coerceIn(0, 255)
            val b = (col.blue * 255).toInt().coerceIn(0, 255)
            sb.append("${pt.x} ${pt.y} ${pt.z} $r $g $b\n")
        }
        return sb.toString()
    }

    /**
     * Generates Wavefront OBJ Point Cloud format.
     */
    fun generateObjPointCloud(points: List<PointCloudPoint>): String {
        val sb = StringBuilder()
        sb.append("# Metrax 3D LiDAR Scan\n")
        sb.append("# Vertices: ${points.size}\n")
        for (pt in points) {
            sb.append("v ${pt.x} ${pt.y} ${pt.z}\n")
        }
        return sb.toString()
    }
}
