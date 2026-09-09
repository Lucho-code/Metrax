package com.example.util

import org.json.JSONArray

/** Parses a [MeasurementEntity.heightGridJson]-shaped JSON array-of-arrays-of-floats back into a grid. */
fun parseHeightGrid(json: String?): List<List<Float>> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val rows = JSONArray(json)
        (0 until rows.length()).map { rowIndex ->
            val row = rows.getJSONArray(rowIndex)
            (0 until row.length()).map { colIndex -> row.getDouble(colIndex).toFloat() }
        }
    } catch (e: Exception) {
        emptyList()
    }
}
