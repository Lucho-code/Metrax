package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/** A best-effort (lat, lon) fix, in decimal degrees. */
data class LatLon(val latitude: Double, val longitude: Double)

/**
 * Best-effort "where was this measurement taken" tagging, the way SR Measure
 * shows a location map on a measurement's detail screen. Uses only the
 * framework [LocationManager] (no Play Services / Fused Location dependency)
 * and never triggers a fresh GPS fix — it just reads whatever last-known fix
 * any enabled provider already has cached, so it's instant and safe to call
 * synchronously at save time.
 */
object LocationUtil {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Returns the freshest last-known location across all enabled providers, or null if unavailable/denied. */
    fun lastKnownLocation(context: Context): LatLon? {
        if (!hasLocationPermission(context)) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        return try {
            locationManager.allProviders
                .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { LatLon(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }
}
