package com.example.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Saves a snapshot of the live camera/AR view alongside a measurement, the
 * way SR Measure attaches a photo/video to every saved pile scan. Files live
 * in app-internal storage (no runtime storage permission needed) and are
 * exposed for sharing via the `com.example.fileprovider` FileProvider.
 */
object PhotoStorage {
    private const val DIR_NAME = "measurement_photos"

    fun photosDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** Compresses [bitmap] to JPEG and saves it to internal storage. Returns the absolute path, or null on failure. */
    fun savePhoto(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(photosDir(context), "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
