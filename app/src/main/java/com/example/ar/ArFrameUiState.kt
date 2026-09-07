package com.example.ar

import androidx.compose.ui.geometry.Offset

/** Coarse tracking status surfaced to the UI, decoupled from ARCore's own enum. */
enum class ArTrackingStatus {
    INITIALIZING,
    TRACKING,
    LOST
}

/**
 * Snapshot of the AR session state published once per rendered frame so the
 * Compose overlay can draw the live point cloud / toe polygon without ever
 * touching ARCore types directly.
 */
data class ArFrameUiState(
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.INITIALIZING,
    val planeDetected: Boolean = false,
    val pointCloudScreenPoints: List<Offset> = emptyList(),
    val toePointsScreen: List<Offset> = emptyList(),
    val statusMessage: String = "Iniciando cámara AR…"
)

/** Terminal/blocking states for the AR camera host (permission, install, hardware support). */
sealed interface ArAvailability {
    data object Checking : ArAvailability
    data object Ready : ArAvailability
    data object NeedsCameraPermission : ArAvailability
    data object NeedsInstall : ArAvailability
    data class Unsupported(val reason: String) : ArAvailability
}
