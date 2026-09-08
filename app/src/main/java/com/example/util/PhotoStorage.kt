package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Saves a snapshot of the live camera/AR view alongside a measurement, the
 * way SR Measure attaches a photo/video to every saved pile scan. Files live
 * in app-internal storage (no runtime storage permission needed) and are
 * exposed for sharing via the `com.example.fileprovider` FileProvider.
 */
object PhotoStorage {
    private const val DIR_NAME = "measurement_photos"
    private const val MAX_DIMENSION_PX = 1600

    fun photosDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /**
     * Downscales [bitmap] (a raw GLSurfaceView/PreviewView capture, which can be
     * several megapixels) to a sane on-disk size, compresses it to JPEG and
     * saves it to internal storage. Returns the absolute path, or null on failure.
     */
    fun savePhoto(context: Context, bitmap: Bitmap): String? {
        return try {
            val scaled = downscale(bitmap, MAX_DIMENSION_PX)
            val file = File(photosDir(context), "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Decodes a small thumbnail for [path] sized to roughly [reqSizePx], using
     * [BitmapFactory.Options.inSampleSize] so list rows never pay the cost of
     * decoding a full-resolution photo just to show a 40-something dp preview.
     */
    fun decodeThumbnail(path: String, reqSizePx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            val smallestSide = min(bounds.outWidth, bounds.outHeight)
            while (smallestSide / (sampleSize * 2) >= reqSizePx) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
}
