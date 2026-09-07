package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar.ArAvailability
import com.example.ar.ArFrameUiState
import com.example.ar.ArVolumeRenderer
import com.example.data.db.AppDatabase
import com.example.data.db.MeasurementEntity
import com.example.data.model.ArVolumeResult
import com.example.data.model.MeasurementMethod
import com.example.data.model.MeasurementMode
import com.example.data.model.PlaneType
import com.example.data.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bridges the GL-thread [ArVolumeRenderer] (real ARCore point cloud, plane
 * detection and depth-based volume scanning) with Compose UI state, and
 * persists finished AR volume scans through the same [MeasurementRepository]
 * used by the manual-tap measuring flow.
 */
class ArMeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository =
        MeasurementRepository(AppDatabase.getDatabase(application).measurementDao())

    private var renderer: ArVolumeRenderer? = null

    private val _uiState = MutableStateFlow(ArFrameUiState())
    val uiState: StateFlow<ArFrameUiState> = _uiState.asStateFlow()

    private val _availability = MutableStateFlow<ArAvailability>(ArAvailability.Checking)
    val availability: StateFlow<ArAvailability> = _availability.asStateFlow()

    private val _arResult = MutableStateFlow<ArVolumeResult?>(null)
    val arResult: StateFlow<ArVolumeResult?> = _arResult.asStateFlow()

    private val _isComputing = MutableStateFlow(false)
    val isComputing: StateFlow<Boolean> = _isComputing.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    fun onRendererReady(instance: ArVolumeRenderer) {
        renderer = instance
    }

    fun onAvailabilityChanged(availability: ArAvailability) {
        _availability.value = availability
    }

    fun onUiStateUpdate(state: ArFrameUiState) {
        _uiState.value = state
    }

    fun onToePointsChanged() {
        _arResult.value = null
    }

    fun onVolumeResult(result: ArVolumeResult) {
        _arResult.value = result
        _isComputing.value = false
    }

    fun onTap(x: Float, y: Float) {
        renderer?.postTap(x, y)
    }

    fun undo() {
        renderer?.postUndo()
    }

    fun reset() {
        renderer?.postReset()
        _arResult.value = null
    }

    fun computeVolume(gridResolution: Int = 20) {
        if (renderer == null) return
        _isComputing.value = true
        renderer?.postComputeVolume(gridResolution)
    }

    fun setShowSaveDialog(show: Boolean) {
        _showSaveDialog.value = show
    }

    fun setCalibrationMode(active: Boolean) {
        renderer?.postSetCalibrationMode(active)
    }

    fun resetCalibrationPoints() {
        renderer?.postResetCalibrationPoints()
    }

    fun applyCalibration(trueDistanceMeters: Double) {
        renderer?.postApplyCalibration(trueDistanceMeters)
    }

    fun saveMeasurement(title: String) {
        val result = _arResult.value ?: return

        val pointsJson = JSONArray().apply {
            result.toePoints.forEach { point ->
                put(
                    JSONObject().apply {
                        put("x", point.x)
                        put("y", point.y)
                        put("z", point.z)
                    }
                )
            }
        }.toString()

        val entity = MeasurementEntity(
            mode = MeasurementMode.VOLUME.name,
            value = result.volumeCubicMeters,
            heightValue = result.maxHeightMeters,
            scaleFactor = _uiState.value.lengthCorrectionFactor,
            title = title.ifBlank { "Volumen AR (nube de puntos)" },
            pointsJson = pointsJson,
            planeType = PlaneType.FLOOR.name,
            method = MeasurementMethod.AR_POINT_CLOUD.name,
            surfaceCoverageConfidence = result.surfaceCoverageConfidence,
            toeCoverageConfidence = result.toeCoverageConfidence
        )

        viewModelScope.launch {
            repository.insert(entity)
            _showSaveDialog.value = false
            _arResult.value = null
            renderer?.postReset()
        }
    }
}
