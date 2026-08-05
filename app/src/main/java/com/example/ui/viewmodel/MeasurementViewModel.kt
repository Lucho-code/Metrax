package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.MeasurementEntity
import com.example.data.model.CalibrationPreset
import com.example.data.model.MeasurementMode
import com.example.data.model.PlaneType
import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import com.example.data.repository.MeasurementRepository
import com.example.util.GeometryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository

    init {
        val dao = AppDatabase.getDatabase(application).measurementDao()
        repository = MeasurementRepository(dao)
    }

    private val _mode = MutableStateFlow(MeasurementMode.DISTANCE)
    val mode: StateFlow<MeasurementMode> = _mode.asStateFlow()

    private val _points = MutableStateFlow<List<Point3D>>(emptyList())
    val points: StateFlow<List<Point3D>> = _points.asStateFlow()

    private val _selectedPlane = MutableStateFlow(PlaneType.FLOOR)
    val selectedPlane: StateFlow<PlaneType> = _selectedPlane.asStateFlow()

    private val _unitSystem = MutableStateFlow(UnitSystem.METRIC)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    // Height/Depth parameter in meters for Volume calculations
    private val _heightMeters = MutableStateFlow(1.0)
    val heightMeters: StateFlow<Double> = _heightMeters.asStateFlow()

    // Calibration Scale Factor (1.0 = standard default 180px per meter)
    private val _scaleFactor = MutableStateFlow(1.0)
    val scaleFactor: StateFlow<Double> = _scaleFactor.asStateFlow()

    private val _calibrationPreset = MutableStateFlow(CalibrationPreset.CREDIT_CARD)
    val calibrationPreset: StateFlow<CalibrationPreset> = _calibrationPreset.asStateFlow()

    private val _customRefMeters = MutableStateFlow(1.0)
    val customRefMeters: StateFlow<Double> = _customRefMeters.asStateFlow()

    private val _showCalibrationDialog = MutableStateFlow(false)
    val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()

    private val _trackingReady = MutableStateFlow(true)
    val trackingReady: StateFlow<Boolean> = _trackingReady.asStateFlow()

    private val _surfaceFound = MutableStateFlow(true)
    val surfaceFound: StateFlow<Boolean> = _surfaceFound.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _historyFilter = MutableStateFlow("ALL") // ALL, DISTANCE, AREA, VOLUME
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val historyList: StateFlow<List<MeasurementEntity>> = combine(
        repository.allMeasurements,
        _historyFilter,
        _searchQuery
    ) { items, filter, query ->
        items.filter { item ->
            val matchesFilter = when (filter) {
                "DISTANCE" -> item.mode == MeasurementMode.DISTANCE.name
                "AREA" -> item.mode == MeasurementMode.AREA.name
                "VOLUME" -> item.mode == MeasurementMode.VOLUME.name
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.planeType.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMode(newMode: MeasurementMode) {
        if (_mode.value != newMode) {
            _mode.value = newMode
            _points.value = emptyList()
        }
    }

    fun setPlane(plane: PlaneType) {
        _selectedPlane.value = plane
    }

    fun toggleUnitSystem() {
        _unitSystem.value = if (_unitSystem.value == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }

    fun setHeightMeters(height: Double) {
        _heightMeters.value = height.coerceAtLeast(0.01)
    }

    fun setScaleFactor(factor: Double) {
        _scaleFactor.value = factor.coerceIn(0.1, 10.0)
    }

    fun setCalibrationPreset(preset: CalibrationPreset) {
        _calibrationPreset.value = preset
    }

    fun setCustomRefMeters(meters: Double) {
        _customRefMeters.value = meters.coerceAtLeast(0.01)
    }

    fun setShowCalibrationDialog(show: Boolean) {
        _showCalibrationDialog.value = show
    }

    /**
     * Calibrates scale using first 2 points distance against selected reference object length.
     */
    fun calibrateScaleFromPoints() {
        val currentPoints = _points.value
        if (currentPoints.size >= 2) {
            val pixelDist = GeometryUtils.distance3D(currentPoints[0], currentPoints[1])
            val targetRefMeters = if (_calibrationPreset.value == CalibrationPreset.CUSTOM) {
                _customRefMeters.value
            } else {
                _calibrationPreset.value.lengthMeters
            }

            if (pixelDist > 0 && targetRefMeters > 0) {
                // Adjust scale factor based on reference measurement
                val newScale = targetRefMeters / pixelDist
                _scaleFactor.value = newScale
                _showCalibrationDialog.value = false
            }
        }
    }

    fun addPoint(point: Point3D) {
        _points.value = _points.value + point
    }

    fun undoLastPoint() {
        if (_points.value.isNotEmpty()) {
            _points.value = _points.value.dropLast(1)
        }
    }

    fun resetPoints() {
        _points.value = emptyList()
    }

    fun setShowSaveDialog(show: Boolean) {
        _showSaveDialog.value = show
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Calculates area in square meters using points scaled by scaleFactor.
     */
    fun calculateCurrentArea(): Double {
        val currentPoints = _points.value
        if (currentPoints.size < 3) return 0.0
        val baseArea = GeometryUtils.polygonArea3D(currentPoints)
        return baseArea * _scaleFactor.value * _scaleFactor.value
    }

    /**
     * Calculates distance, area, or volume based on current mode and points.
     */
    fun calculateCurrentValue(): Double {
        val currentPoints = _points.value
        val scale = _scaleFactor.value

        return when (_mode.value) {
            MeasurementMode.DISTANCE -> {
                if (currentPoints.size >= 2) {
                    var total = 0.0
                    for (i in 0 until currentPoints.size - 1) {
                        total += GeometryUtils.distance3D(currentPoints[i], currentPoints[i + 1])
                    }
                    total * scale
                } else 0.0
            }
            MeasurementMode.AREA -> {
                calculateCurrentArea()
            }
            MeasurementMode.VOLUME -> {
                val area = calculateCurrentArea()
                GeometryUtils.calculateVolume(area, _heightMeters.value)
            }
        }
    }

    fun saveMeasurement(title: String) {
        val valCalculated = calculateCurrentValue()
        if (valCalculated <= 0.0) return

        val jsonArray = JSONArray()
        _points.value.forEach { pt ->
            val obj = JSONObject()
            obj.put("x", pt.x)
            obj.put("y", pt.y)
            obj.put("z", pt.z)
            jsonArray.put(obj)
        }

        val defaultTitle = if (title.isBlank()) {
            when (_mode.value) {
                MeasurementMode.DISTANCE -> "Distancia (${_selectedPlane.value.displayName})"
                MeasurementMode.AREA -> "Área (${_selectedPlane.value.displayName})"
                MeasurementMode.VOLUME -> "Volumen (${_selectedPlane.value.displayName})"
            }
        } else title

        val entity = MeasurementEntity(
            mode = _mode.value.name,
            value = valCalculated,
            heightValue = if (_mode.value == MeasurementMode.VOLUME) _heightMeters.value else 0.0,
            scaleFactor = _scaleFactor.value,
            title = defaultTitle,
            pointsJson = jsonArray.toString(),
            planeType = _selectedPlane.value.name
        )

        viewModelScope.launch {
            repository.insert(entity)
            _points.value = emptyList()
            _showSaveDialog.value = false
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
