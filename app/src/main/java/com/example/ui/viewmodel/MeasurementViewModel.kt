package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.MeasurementEntity
import com.example.data.model.BoundingBox3D
import com.example.data.model.CalibrationPreset
import com.example.data.model.MeasurementMode
import com.example.data.model.PlaneType
import com.example.data.model.PointCloudColorMap
import com.example.data.model.PointCloudPoint
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

data class MaterialPreset(val name: String, val densityKgPerM3: Float)

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

    // 3D LiDAR Point Cloud State
    private val _pointCloud = MutableStateFlow<List<PointCloudPoint>>(emptyList())
    val pointCloud: StateFlow<List<PointCloudPoint>> = _pointCloud.asStateFlow()

    private val _pointCloudColorMap = MutableStateFlow(PointCloudColorMap.HEATMAP)
    val pointCloudColorMap: StateFlow<PointCloudColorMap> = _pointCloudColorMap.asStateFlow()

    private val _is3DViewerMode = MutableStateFlow(false)
    val is3DViewerMode: StateFlow<Boolean> = _is3DViewerMode.asStateFlow()

    private val _selectedCloudPoints = MutableStateFlow<List<PointCloudPoint>>(emptyList())
    val selectedCloudPoints: StateFlow<List<PointCloudPoint>> = _selectedCloudPoints.asStateFlow()

    private val _selectedPlane = MutableStateFlow(PlaneType.FLOOR)
    val selectedPlane: StateFlow<PlaneType> = _selectedPlane.asStateFlow()

    private val _unitSystem = MutableStateFlow(UnitSystem.METRIC)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    // Height/Depth parameter in meters for Volume calculations
    private val _heightMeters = MutableStateFlow(1.0)
    val heightMeters: StateFlow<Double> = _heightMeters.asStateFlow()

    val availableMaterials = listOf(
        MaterialPreset("Madera", 600f),
        MaterialPreset("Asfalto", 700f),
        MaterialPreset("Agua", 1000f),
        MaterialPreset("Yeso", 1200f),
        MaterialPreset("Bloque de hormigón", 1400f),
        MaterialPreset("Cemento", 1440f),
        MaterialPreset("Ladrillos", 1600f), // Promedio de 1500-1700
        MaterialPreset("Arena", 1650f),
        MaterialPreset("Grava", 1800f),
        MaterialPreset("Arcilla", 1800f),
        MaterialPreset("Hormigón simple (PCC)", 2400f),
        MaterialPreset("Hormigón armado (RCC)", 2500f),
        MaterialPreset("Acero", 7850f)
    )

    private val _selectedMaterial = MutableStateFlow(availableMaterials.first())
    val selectedMaterial: StateFlow<MaterialPreset> = _selectedMaterial.asStateFlow()

    /**
     * Overrides the density of the currently selected material with a
     * user-entered value, keeping its name.
     */
    fun setCustomDensity(densityKgPerM3: Float) {
        if (densityKgPerM3 <= 0f) return
        _selectedMaterial.value = _selectedMaterial.value.copy(densityKgPerM3 = densityKgPerM3)
    }

    private val _scaleFactor = MutableStateFlow(1.0)
    val scaleFactor: StateFlow<Double> = _scaleFactor.asStateFlow()

    val boundingBox3D: StateFlow<BoundingBox3D?> = _pointCloud.combine(_scaleFactor) { cloud, _ ->
        GeometryUtils.calculateBoundingBox(cloud)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
    
    fun setMaterial(material: MaterialPreset) {
        _selectedMaterial.value = material
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

    fun setPointCloudColorMap(colorMap: PointCloudColorMap) {
        _pointCloudColorMap.value = colorMap
    }

    fun toggle3DViewerMode() {
        _is3DViewerMode.value = !_is3DViewerMode.value
    }

    fun set3DViewerMode(enabled: Boolean) {
        _is3DViewerMode.value = enabled
    }

    fun addPointCloudPoints(newPoints: List<PointCloudPoint>) {
        if (newPoints.isEmpty()) return
        val current = _pointCloud.value
        // Cap point cloud at 4000 points to keep smooth 60fps rendering and memory efficiency
        val combined = (current + newPoints).takeLast(4000)
        _pointCloud.value = combined
    }

    fun clearPointCloud() {
        _pointCloud.value = emptyList()
        _selectedCloudPoints.value = emptyList()
    }

    fun selectCloudPointForMeasure(point: PointCloudPoint) {
        val current = _selectedCloudPoints.value
        if (current.size >= 2) {
            _selectedCloudPoints.value = listOf(point)
        } else {
            _selectedCloudPoints.value = current + point
        }
    }

    fun clearSelectedCloudPoints() {
        _selectedCloudPoints.value = emptyList()
    }

    fun generateSimulatedPointCloud(type: String = "BOX") {
        val list = mutableListOf<PointCloudPoint>()
        val rnd = java.util.Random()
        val sizeX = 0.85f + rnd.nextFloat() * 0.35f
        val sizeY = 0.55f + rnd.nextFloat() * 0.3f
        val sizeZ = 0.70f + rnd.nextFloat() * 0.35f
        val centerX = 0f
        val centerY = -0.15f
        val centerZ = -1.25f

        val pointsPerFace = 160
        for (i in 0 until pointsPerFace) {
            // Front & Back faces
            val u = (rnd.nextFloat() - 0.5f) * sizeX
            val v = (rnd.nextFloat() - 0.5f) * sizeY
            val j1 = (rnd.nextFloat() - 0.5f) * 0.02f
            val j2 = (rnd.nextFloat() - 0.5f) * 0.02f
            list.add(PointCloudPoint(centerX + u, centerY + v, centerZ + sizeZ / 2f + j1, 0.95f, 1.1f, ((v + sizeY / 2f) / sizeY)))
            list.add(PointCloudPoint(centerX + u, centerY + v, centerZ - sizeZ / 2f + j2, 0.88f, 1.5f, ((v + sizeY / 2f) / sizeY)))

            // Top & Bottom faces
            val u2 = (rnd.nextFloat() - 0.5f) * sizeX
            val w2 = (rnd.nextFloat() - 0.5f) * sizeZ
            val j3 = (rnd.nextFloat() - 0.5f) * 0.02f
            val j4 = (rnd.nextFloat() - 0.5f) * 0.02f
            list.add(PointCloudPoint(centerX + u2, centerY + sizeY / 2f + j3, centerZ + w2, 0.96f, 1.25f, 0.95f))
            list.add(PointCloudPoint(centerX + u2, centerY - sizeY / 2f + j4, centerZ + w2, 0.90f, 1.35f, 0.05f))

            // Left & Right faces
            val v3 = (rnd.nextFloat() - 0.5f) * sizeY
            val w3 = (rnd.nextFloat() - 0.5f) * sizeZ
            val j5 = (rnd.nextFloat() - 0.5f) * 0.02f
            val j6 = (rnd.nextFloat() - 0.5f) * 0.02f
            list.add(PointCloudPoint(centerX + sizeX / 2f + j5, centerY + v3, centerZ + w3, 0.92f, 1.3f, ((v3 + sizeY / 2f) / sizeY)))
            list.add(PointCloudPoint(centerX - sizeX / 2f + j6, centerY + v3, centerZ + w3, 0.92f, 1.3f, ((v3 + sizeY / 2f) / sizeY)))
        }

        // Add reference ground points
        for (i in 0 until 140) {
            val fx = (rnd.nextFloat() - 0.5f) * 1.5f
            val fz = centerZ + (rnd.nextFloat() - 0.5f) * 1.5f
            list.add(PointCloudPoint(centerX + fx, centerY - sizeY / 2f - 0.02f, fz, 0.75f, 1.4f, 0.02f))
        }

        _pointCloud.value = list
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
        _pointCloud.value = emptyList()
        _selectedCloudPoints.value = emptyList()
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
        // Volume mode collects an unordered point cloud (scan order, not
        // perimeter order), so its footprint needs a convex-hull area
        // instead of the ordered-vertex shoelace formula used for AREA mode.
        val baseArea = if (_mode.value == MeasurementMode.VOLUME) {
            GeometryUtils.pointCloudFootprintArea(currentPoints)
        } else {
            GeometryUtils.polygonArea3D(currentPoints)
        }
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
                if (_pointCloud.value.isNotEmpty()) {
                    val box = GeometryUtils.calculateBoundingBox(_pointCloud.value)
                    if (box != null) {
                        (box.volume.toDouble() * scale * scale * scale)
                    } else 0.0
                } else {
                    val area = calculateCurrentArea()
                    GeometryUtils.calculateVolume(area, _heightMeters.value)
                }
            }
        }
    }

    fun saveMeasurement(title: String) {
        val valCalculated = calculateCurrentValue()
        if (valCalculated <= 0.0) return

        val isCloud = _mode.value == MeasurementMode.VOLUME && _pointCloud.value.isNotEmpty()
        val jsonArray = JSONArray()

        if (isCloud) {
            val cloud = _pointCloud.value
            val step = (cloud.size / 120).coerceAtLeast(1)
            for (i in 0 until cloud.size step step) {
                val pt = cloud[i]
                val obj = JSONObject()
                obj.put("x", pt.x)
                obj.put("y", pt.y)
                obj.put("z", pt.z)
                obj.put("confidence", pt.confidence)
                obj.put("colorHue", pt.colorHue)
                jsonArray.put(obj)
            }
        } else {
            _points.value.forEach { pt ->
                val obj = JSONObject()
                obj.put("x", pt.x)
                obj.put("y", pt.y)
                obj.put("z", pt.z)
                jsonArray.put(obj)
            }
        }

        val defaultTitle = if (title.isBlank()) {
            when (_mode.value) {
                MeasurementMode.DISTANCE -> "Distancia (${_selectedPlane.value.displayName})"
                MeasurementMode.AREA -> "Área (${_selectedPlane.value.displayName})"
                MeasurementMode.VOLUME -> if (isCloud) "Nube 3D (${_pointCloud.value.size} pts)" else "Volumen (${_selectedPlane.value.displayName})"
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
            _pointCloud.value = emptyList()
            _selectedCloudPoints.value = emptyList()
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
