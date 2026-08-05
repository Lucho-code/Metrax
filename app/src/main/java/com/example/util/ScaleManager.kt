package com.example.util

import kotlin.math.hypot

/**
 * Class representing a 2D point in screen pixel coordinates.
 */
data class PixelPoint(val x: Float, val y: Float) {
    /**
     * Calculates Euclidean distance in pixels to another point.
     */
    fun distanceTo(other: PixelPoint): Double {
        val dx = (other.x - x).toDouble()
        val dy = (other.y - y).toDouble()
        return hypot(dx, dy)
    }
}

/**
 * Units supported for setting real-world distance.
 */
enum class DistanceUnit(val displayName: String, val toMetersMultiplier: Double) {
    METERS("Metros (m)", 1.0),
    CENTIMETERS("Centímetros (cm)", 0.01),
    MILLIMETERS("Milímetros (mm)", 0.001),
    INCHES("Pulgadas (in)", 0.0254),
    FEET("Pies (ft)", 0.3048)
}

/**
 * Utility class for scaling calculations.
 * Allows defining a reference segment in pixel coordinates and assigning a known real-world
 * distance to calculate scale factors (meters/pixel) and convert measurements.
 */
class ScaleManager(
    private var referencePointA: PixelPoint? = null,
    private var referencePointB: PixelPoint? = null,
    private var knownRealDistanceMeters: Double = 0.0
) {

    /**
     * Sets the reference segment using two pixel points.
     */
    fun setReferenceSegment(startPx: PixelPoint, endPx: PixelPoint) {
        this.referencePointA = startPx
        this.referencePointB = endPx
    }

    /**
     * Sets the reference segment by pixel coordinates.
     */
    fun setReferenceSegment(x1: Float, y1: Float, x2: Float, y2: Float) {
        setReferenceSegment(PixelPoint(x1, y1), PixelPoint(x2, y2))
    }

    /**
     * Sets both the reference segment and known real distance in a single call.
     */
    fun configureReference(
        startPx: PixelPoint,
        endPx: PixelPoint,
        realDistance: Double,
        unit: DistanceUnit = DistanceUnit.METERS
    ) {
        setReferenceSegment(startPx, endPx)
        setKnownRealDistance(realDistance, unit)
    }

    /**
     * Sets the known real-world distance corresponding to the reference segment.
     * @param realDistance magnitude of distance
     * @param unit unit of measurement (default: METERS)
     */
    fun setKnownRealDistance(realDistance: Double, unit: DistanceUnit = DistanceUnit.METERS) {
        require(realDistance >= 0.0) { "La distancia real debe ser un valor no negativo" }
        this.knownRealDistanceMeters = realDistance * unit.toMetersMultiplier
    }

    /**
     * Returns the length of the reference segment in pixels.
     */
    fun getReferencePixelLength(): Double {
        val a = referencePointA ?: return 0.0
        val b = referencePointB ?: return 0.0
        return a.distanceTo(b)
    }

    /**
     * Returns the configured known real distance in meters.
     */
    fun getKnownRealDistanceMeters(): Double = knownRealDistanceMeters

    /**
     * Calculates the scale factor in meters per pixel (m/px).
     * Formula: scaleFactor = knownRealDistanceMeters / referencePixelLength
     * @return scale factor in m/px, or 0.0 if pixel length or real distance is 0
     */
    fun calculateScaleFactorMetersPerPixel(): Double {
        val pxLength = getReferencePixelLength()
        if (pxLength <= 0.0 || knownRealDistanceMeters <= 0.0) return 0.0
        return knownRealDistanceMeters / pxLength
    }

    /**
     * Calculates the scale factor in pixels per meter (px/m).
     * @return scale factor in px/m, or 0.0 if scale is invalid
     */
    fun calculateScaleFactorPixelsPerMeter(): Double {
        val mPerPx = calculateScaleFactorMetersPerPixel()
        if (mPerPx <= 0.0) return 0.0
        return 1.0 / mPerPx
    }

    /**
     * Checks if a valid scale factor exists.
     */
    fun isValidScale(): Boolean {
        return calculateScaleFactorMetersPerPixel() > 0.0
    }

    /**
     * Converts a distance in pixels to real meters using the scale factor.
     */
    fun pixelsToMeters(pixelDistance: Double): Double {
        val scale = calculateScaleFactorMetersPerPixel()
        return pixelDistance * scale
    }

    /**
     * Converts distance between two pixel points to real meters.
     */
    fun pixelSegmentToMeters(p1: PixelPoint, p2: PixelPoint): Double {
        return pixelsToMeters(p1.distanceTo(p2))
    }

    /**
     * Converts a 2D pixel area (px²) to real square meters (m²).
     */
    fun pixelAreaToSquareMeters(pixelArea: Double): Double {
        val scale = calculateScaleFactorMetersPerPixel()
        return pixelArea * (scale * scale)
    }

    /**
     * Converts real meters to pixels using current scale factor.
     */
    fun metersToPixels(meters: Double): Double {
        val pxPerM = calculateScaleFactorPixelsPerMeter()
        return meters * pxPerM
    }

    /**
     * Resets the calibration points and real distance.
     */
    fun reset() {
        referencePointA = null
        referencePointB = null
        knownRealDistanceMeters = 0.0
    }

    companion object {
        /**
         * Static helper to compute scale factor (m/px) directly from pixel length and known distance in meters.
         */
        fun computeScaleFactor(pixelLength: Double, knownRealDistanceMeters: Double): Double {
            if (pixelLength <= 0.0 || knownRealDistanceMeters <= 0.0) return 0.0
            return knownRealDistanceMeters / pixelLength
        }

        /**
         * Static helper to compute scale factor (m/px) directly from two pixel points and known distance in meters.
         */
        fun computeScaleFactor(p1: PixelPoint, p2: PixelPoint, knownRealDistanceMeters: Double): Double {
            return computeScaleFactor(p1.distanceTo(p2), knownRealDistanceMeters)
        }

        /**
         * Static helper to convert pixel distance to meters given a known scale factor (m/px).
         */
        fun convertPixelsToMeters(pixelDistance: Double, scaleFactorMetersPerPixel: Double): Double {
            return pixelDistance * scaleFactorMetersPerPixel
        }

        /**
         * Static helper to convert pixel area (px²) to square meters (m²) given scale factor (m/px).
         */
        fun convertPixelAreaToSquareMeters(pixelArea: Double, scaleFactorMetersPerPixel: Double): Double {
            return pixelArea * (scaleFactorMetersPerPixel * scaleFactorMetersPerPixel)
        }
    }
}
