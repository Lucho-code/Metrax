package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String, // DISTANCE, AREA, VOLUME
    val value: Double, // meters, square meters, or cubic meters
    val heightValue: Double = 0.0, // height / depth in meters for volume
    val scaleFactor: Double = 1.0, // scale factor calibration
    val title: String = "",
    val pointsJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val planeType: String = "FLOOR",
    val method: String = "MANUAL_TAP", // MANUAL_TAP or AR_POINT_CLOUD
    val surfaceCoverageConfidence: Float? = null, // AR_POINT_CLOUD only: ratio of surface grid with valid depth
    val toeCoverageConfidence: Float? = null // AR_POINT_CLOUD only: ratio of boundary anchors well tracked
)
