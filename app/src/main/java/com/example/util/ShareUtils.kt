package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Builds a share Intent for a measurement summary, attaching the saved photo (if any) via FileProvider. */
object ShareUtils {
    fun shareMeasurement(context: Context, subject: String, text: String, photoPath: String?) {
        val photoUri = photoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else null
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (photoUri != null) "image/jpeg" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            if (photoUri != null) {
                putExtra(Intent.EXTRA_STREAM, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Compartir medición"))
    }
}
