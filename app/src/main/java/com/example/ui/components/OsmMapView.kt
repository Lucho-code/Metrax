package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

private const val ZOOM = 16
private const val TILE_PX = 256
private const val GRID = 3 // fetch a 3x3 patch of tiles so the point is never near a tile seam
private const val CROP_PX = 480 // final square crop, centered exactly on the point

/**
 * Small embedded location map with no API key: stitches a patch of standard
 * OpenStreetMap raster tiles around (lat, lon), crops it centered on the
 * point, and overlays a pin — the way a measurement's saved location is
 * shown on SR Measure's detail screen. Requires network; shows a spinner
 * while loading and a quiet fallback if the fetch fails (e.g. offline).
 */
@Composable
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    var mapBitmap by remember(latitude, longitude) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(latitude, longitude) { mutableStateOf(false) }

    LaunchedEffect(latitude, longitude) {
        mapBitmap = null
        failed = false
        val result = buildCenteredMap(latitude, longitude)
        if (result == null) failed = true else mapBitmap = result
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = mapBitmap
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Mapa de la ubicación de la medición",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            }
            failed -> {
                Text(
                    text = "No se pudo cargar el mapa (revisá tu conexión)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
    }
}

private suspend fun buildCenteredMap(lat: Double, lon: Double): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val n = 2.0.pow(ZOOM)
        val latRad = Math.toRadians(lat)
        val xTileF = (lon + 180.0) / 360.0 * n
        val yTileF = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val centerTileX = floor(xTileF).toInt()
        val centerTileY = floor(yTileF).toInt()
        val half = GRID / 2

        val patchSize = GRID * TILE_PX
        val composed = Bitmap.createBitmap(patchSize, patchSize, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(composed)

        for (dy in -half..half) {
            for (dx in -half..half) {
                val tile = fetchTile(centerTileX + dx, centerTileY + dy, ZOOM) ?: continue
                canvas.drawBitmap(tile, ((dx + half) * TILE_PX).toFloat(), ((dy + half) * TILE_PX).toFloat(), null)
            }
        }

        // Pixel position of (lat, lon) within the composed patch.
        val pointX = ((xTileF - (centerTileX - half)) * TILE_PX).toInt()
        val pointY = ((yTileF - (centerTileY - half)) * TILE_PX).toInt()

        val half2 = CROP_PX / 2
        val left = (pointX - half2).coerceIn(0, patchSize - CROP_PX)
        val top = (pointY - half2).coerceIn(0, patchSize - CROP_PX)
        Bitmap.createBitmap(composed, left, top, CROP_PX, CROP_PX)
    } catch (e: Exception) {
        null
    }
}

private fun fetchTile(x: Int, y: Int, z: Int): Bitmap? {
    var connection: HttpURLConnection? = null
    return try {
        val url = URL("https://tile.openstreetmap.org/$z/$x/$y.png")
        connection = (url.openConnection() as HttpURLConnection).apply {
            // OpenStreetMap's tile usage policy requires a descriptive User-Agent
            // identifying the app, not a generic/default client string.
            setRequestProperty("User-Agent", "MetrajeInstanteApp/1.0 (Android; contact via app store listing)")
            connectTimeout = 8000
            readTimeout = 8000
        }
        connection.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
