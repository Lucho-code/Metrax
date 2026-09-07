package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persistent stockpile/site ("Pile"), grouping repeated measurements of the
 * same physical material over time — e.g. re-scanning the same sand pile
 * weekly to track consumption/reposition, the way SR Measure's "Piles" tab
 * does. A [MeasurementEntity] optionally points here via `pileId`.
 */
@Entity(tableName = "piles")
data class PileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val materialType: String? = null, // MaterialType enum name, default material for measurements of this pile
    val notes: String = ""
)
