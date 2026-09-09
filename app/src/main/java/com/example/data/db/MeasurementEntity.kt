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
    val toeCoverageConfidence: Float? = null, // AR_POINT_CLOUD only: ratio of boundary anchors well tracked
    val photoPath: String? = null, // absolute path to a saved JPEG snapshot of the scene, in app-internal storage
    val materialType: String? = null, // MaterialType enum name, null = sin material asignado
    val tonnage: Double? = null, // volume * material density at save time, in metric tons
    val pileId: Long? = null, // FK to PileEntity.id, null = sin acopio/sitio asignado
    val latitude: Double? = null, // best-effort last-known GPS location at save time, null if unavailable/denied
    val longitude: Double? = null,
    val calibrationLabel: String = "Sin calibrar", // human-readable scaling/calibration method active at save time
    val heightGridJson: String? = null // AR_POINT_CLOUD only: serialized height grid for the contour map preview on the detail screen
)
