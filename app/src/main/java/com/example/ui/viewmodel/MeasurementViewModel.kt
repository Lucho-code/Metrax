package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.MeasurementEntity
import com.example.data.db.PileEntity
import com.example.data.model.CalibrationMethod
import com.example.data.model.CalibrationPreset
import com.example.data.model.MaterialType
import com.example.data.model.MeasurementMode
import com.example.data.model.PlaneType
import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import com.example.data.repository.MeasurementRepository
import com.example.util.GeometryUtils
import kotlinx.coroutines.flow.Flow
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
        val db = AppDatabase.getDatabase(application)
        repository = MeasurementRepository(db.measurementDao(), db.pileDao())
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

    // "Verify against a known real measurement" calibration: measure something whose
    // real-world value you already know, then correct the scale factor from the difference.
    private val _calibrationMethod = MutableStateFlow(CalibrationMethod.REFERENCE_OBJECT)
    val calibrationMethod: StateFlow<CalibrationMethod> = _calibrationMethod.asStateFlow()

    private val _lastCalibrationCorrection = MutableStateFlow<Double?>(null)
    val lastCalibrationCorrection: StateFlow<Double?> = _lastCalibrationCorrection.asStateFlow()

    // Custom real-world length (meters) for the KNOWN_MEASUREMENT calibration source.
    private val _knownLengthMeters = MutableStateFlow(1.0)
    val knownLengthMeters: StateFlow<Double> = _knownLengthMeters.asStateFlow()

    // Dedicated calibration tap flow: independent of the mode's own measurement
    // points, so calibrating never interferes with (or depends on) whatever the
    // user has already tapped to measure. Tap point A, then point B, then apply.
    private val _calibrationTapModeActive = MutableStateFlow(false)
    val calibrationTapModeActive: StateFlow<Boolean> = _calibrationTapModeActive.asStateFlow()

    private val _calibrationTapPoints = MutableStateFlow<List<Point3D>>(emptyList())
    val calibrationTapPoints: StateFlow<List<Point3D>> = _calibrationTapPoints.asStateFlow()

    private val _calibrationTargetLengthMeters = MutableStateFlow(0.0)
    val calibrationTargetLengthMeters: StateFlow<Double> = _calibrationTargetLengthMeters.asStateFlow()

    private val _trackingReady = MutableStateFlow(true)
    val trackingReady: StateFlow<Boolean> = _trackingReady.asStateFlow()

    private val _surfaceFound = MutableStateFlow(true)
    val surfaceFound: StateFlow<Boolean> = _surfaceFound.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    // Material seleccionado para convertir volumen -> toneladas ("Reporte en toneladas").
    // Es un estado compartido: tanto el modo manual como el AR lo leen y lo asignan
    // al guardar una medición de VOLUMEN.
    private val _selectedMaterial = MutableStateFlow(MaterialType.NONE)
    val selectedMaterial: StateFlow<MaterialType> = _selectedMaterial.asStateFlow()

    // Acopio/"Pile" activo: cuando el usuario entra a medir desde el detalle de un
    // acopio, la próxima medición guardada queda asociada a ese acopio y luego se limpia.
    private val _activePileId = MutableStateFlow<Long?>(null)
    val activePileId: StateFlow<Long?> = _activePileId.asStateFlow()

    val pilesList: StateFlow<List<PileEntity>> = repository.allPiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Foto capturada de la cámara al guardar (modo manual), en almacenamiento interno de la app.
    private val _capturedPhotoPath = MutableStateFlow<String?>(null)
    val capturedPhotoPath: StateFlow<String?> = _capturedPhotoPath.asStateFlow()

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
        if (show) _lastCalibrationCorrection.value = null
    }

    fun setCalibrationMethod(method: CalibrationMethod) {
        _calibrationMethod.value = method
    }

    fun setKnownLengthMeters(meters: Double) {
        _knownLengthMeters.value = meters.coerceAtLeast(0.01)
    }

    /**
     * Starts the dedicated calibration tap flow: closes the setup dialog and puts
     * the live camera into "tap point A, then point B" mode, independent of
     * whatever points the active measuring mode already has. The target real
     * length comes from the selected reference preset (or its custom value) or
     * from the typed-in known length, depending on [calibrationMethod].
     * Returns false if no valid target length is available yet.
     */
    fun beginCalibrationTapping(): Boolean {
        val targetLength = when (_calibrationMethod.value) {
            CalibrationMethod.REFERENCE_OBJECT -> if (_calibrationPreset.value == CalibrationPreset.CUSTOM) {
                _customRefMeters.value
            } else {
                _calibrationPreset.value.lengthMeters
            }
            CalibrationMethod.KNOWN_MEASUREMENT -> _knownLengthMeters.value
        }
        if (targetLength <= 0.0) return false

        _calibrationTargetLengthMeters.value = targetLength
        _calibrationTapPoints.value = emptyList()
        _calibrationTapModeActive.value = true
        _showCalibrationDialog.value = false
        return true
    }

    /**
     * Registers a calibration tap. A third tap restarts from point A so the user
     * can always just keep tapping until both points look right.
     */
    fun addCalibrationTapPoint(point: Point3D) {
        if (!_calibrationTapModeActive.value) return
        val current = _calibrationTapPoints.value
        _calibrationTapPoints.value = if (current.size >= 2) listOf(point) else current + point
    }

    fun undoCalibrationTapPoint() {
        if (_calibrationTapPoints.value.isNotEmpty()) {
            _calibrationTapPoints.value = _calibrationTapPoints.value.dropLast(1)
        }
    }

    fun resetCalibrationTapPoints() {
        _calibrationTapPoints.value = emptyList()
    }

    fun cancelCalibrationTapping() {
        _calibrationTapModeActive.value = false
        _calibrationTapPoints.value = emptyList()
    }

    /** What the app currently reads for the tapped segment, for on-screen feedback. */
    fun calibrationTapRawDistance(): Double? {
        val pts = _calibrationTapPoints.value
        if (pts.size < 2) return null
        return GeometryUtils.distance3D(pts[0], pts[1]) * _scaleFactor.value
    }

    /**
     * Applies the calibration: compares the raw (pre-scale) distance between the
     * two tapped points against the known real length and corrects [scaleFactor]
     * by the resulting ratio. Returns true if the correction was applied.
     */
    fun applyCalibrationTap(): Boolean {
        val pts = _calibrationTapPoints.value
        if (pts.size < 2) return false
        val targetLength = _calibrationTargetLengthMeters.value
        if (targetLength <= 0.0) return false

        val rawDistance = GeometryUtils.distance3D(pts[0], pts[1])
        if (rawDistance <= 0.0) return false

        val newScale = targetLength / rawDistance
        if (newScale.isNaN() || newScale.isInfinite() || newScale <= 0.0) return false

        val previousScale = _scaleFactor.value
        _scaleFactor.value = newScale.coerceIn(0.02, 50.0)
        _lastCalibrationCorrection.value = _scaleFactor.value / previousScale
        _calibrationTapModeActive.value = false
        _calibrationTapPoints.value = emptyList()
        return true
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

    fun setMaterial(material: MaterialType) {
        _selectedMaterial.value = material
    }

    fun setActivePile(pileId: Long?) {
        _activePileId.value = pileId
    }

    fun setCapturedPhotoPath(path: String?) {
        _capturedPhotoPath.value = path
    }

    fun measurementsForPile(pileId: Long): Flow<List<MeasurementEntity>> =
        repository.measurementsForPile(pileId)

    fun pileById(pileId: Long): Flow<PileEntity?> = repository.pileById(pileId)

    fun createPile(name: String, material: MaterialType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertPile(
                PileEntity(
                    name = name.trim(),
                    materialType = if (material == MaterialType.NONE) null else material.name
                )
            )
        }
    }

    fun deletePile(pileId: Long) {
        viewModelScope.launch {
            repository.deletePile(pileId)
        }
    }

    /** Toneladas estimadas para el volumen actual, o null si no aplica (modo != VOLUME o sin material). */
    fun calculateCurrentTonnage(): Double? {
        if (_mode.value != MeasurementMode.VOLUME) return null
        val material = _selectedMaterial.value
        if (material == MaterialType.NONE) return null
        val volume = calculateCurrentValue()
        if (volume <= 0.0) return null
        return volume * material.densityTonPerCubicMeter
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

        val tonnage = calculateCurrentTonnage()
        val material = _selectedMaterial.value

        val entity = MeasurementEntity(
            mode = _mode.value.name,
            value = valCalculated,
            heightValue = if (_mode.value == MeasurementMode.VOLUME) _heightMeters.value else 0.0,
            scaleFactor = _scaleFactor.value,
            title = defaultTitle,
            pointsJson = jsonArray.toString(),
            planeType = _selectedPlane.value.name,
            photoPath = _capturedPhotoPath.value,
            materialType = if (tonnage != null) material.name else null,
            tonnage = tonnage,
            pileId = _activePileId.value
        )

        viewModelScope.launch {
            repository.insert(entity)
            _points.value = emptyList()
            _showSaveDialog.value = false
            _capturedPhotoPath.value = null
            _activePileId.value = null
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
