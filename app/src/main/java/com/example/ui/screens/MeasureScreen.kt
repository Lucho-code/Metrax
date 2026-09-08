package com.example.ui.screens

import android.widget.Toast
import android.opengl.Matrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalibrationPreset
import com.example.data.model.MeasurementMode
import com.example.data.model.PlaneType
import com.example.data.model.Point3D
import com.example.data.model.UnitSystem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import io.github.sceneview.ar.ARScene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.SphereNode
import com.google.ar.core.Config
import com.google.ar.core.PointCloud
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils
import com.example.util.LocalAppStrings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.geometry.Size
import com.example.data.model.BoundingBox3D
import com.example.data.model.PointCloudColorMap
import com.example.data.model.PointCloudPoint
import com.example.ui.components.PointCloudViewer3D
import com.example.ui.components.ScanGuidanceOverlay
import androidx.compose.material.icons.filled.HelpOutline
import android.content.Context
import kotlin.math.sqrt

private fun projectPoint3DToScreen(
    point: Point3D,
    frame: com.google.ar.core.Frame?,
    width: Float,
    height: Float
): Offset? {
    if (frame == null) {
        val focal = width * 0.85f
        val depth = (-point.z).coerceAtLeast(0.1f)
        val sx = width / 2f + (point.x / depth) * focal
        val sy = height / 2f - (point.y / depth) * focal
        return Offset(sx, sy)
    }
    val camera = frame.camera ?: return null
    if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) return null

    val viewMatrix = FloatArray(16)
    val projMatrix = FloatArray(16)
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

    val worldPos = floatArrayOf(point.x, point.y, point.z, 1.0f)
    val viewPos = FloatArray(4)
    Matrix.multiplyMV(viewPos, 0, viewMatrix, 0, worldPos, 0)

    if (viewPos[2] > -0.05f) return null

    val clipPos = FloatArray(4)
    Matrix.multiplyMV(clipPos, 0, projMatrix, 0, viewPos, 0)

    if (clipPos[3] == 0f) return null

    val ndcX = clipPos[0] / clipPos[3]
    val ndcY = clipPos[1] / clipPos[3]

    val screenX = (ndcX + 1.0f) / 2.0f * width
    val screenY = (1.0f - ndcY) / 2.0f * height

    return Offset(screenX, screenY)
}

private fun projectPointCloudPointToScreen(
    point: PointCloudPoint,
    frame: com.google.ar.core.Frame?,
    width: Float,
    height: Float
): Offset? {
    if (frame == null) {
        val focal = width * 0.85f
        val depth = (-point.z).coerceAtLeast(0.1f)
        val sx = width / 2f + (point.x / depth) * focal
        val sy = height / 2f - (point.y / depth) * focal
        return Offset(sx, sy)
    }
    val camera = frame.camera ?: return null
    if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) return null

    val viewMatrix = FloatArray(16)
    val projMatrix = FloatArray(16)
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

    val worldPos = floatArrayOf(point.x, point.y, point.z, 1.0f)
    val viewPos = FloatArray(4)
    Matrix.multiplyMV(viewPos, 0, viewMatrix, 0, worldPos, 0)

    if (viewPos[2] > -0.05f) return null

    val clipPos = FloatArray(4)
    Matrix.multiplyMV(clipPos, 0, projMatrix, 0, viewPos, 0)

    if (clipPos[3] == 0f) return null

    val ndcX = clipPos[0] / clipPos[3]
    val ndcY = clipPos[1] / clipPos[3]

    val screenX = (ndcX + 1.0f) / 2.0f * width
    val screenY = (1.0f - ndcY) / 2.0f * height

    return Offset(screenX, screenY)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val selectedPlane by viewModel.selectedPlane.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val heightMeters by viewModel.heightMeters.collectAsStateWithLifecycle()
    val scaleFactor by viewModel.scaleFactor.collectAsStateWithLifecycle()
    val calibrationPreset by viewModel.calibrationPreset.collectAsStateWithLifecycle()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsStateWithLifecycle()
    val showSaveDialog by viewModel.showSaveDialog.collectAsStateWithLifecycle()

    val pointCloud by viewModel.pointCloud.collectAsStateWithLifecycle()
    val pointCloudColorMap by viewModel.pointCloudColorMap.collectAsStateWithLifecycle()
    val is3DViewerMode by viewModel.is3DViewerMode.collectAsStateWithLifecycle()
    val boundingBox3D by viewModel.boundingBox3D.collectAsStateWithLifecycle()

    var saveTitleInput by remember { mutableStateOf("") }
    var heightSliderValue by remember(heightMeters) { mutableFloatStateOf(heightMeters.toFloat()) }
    var customRefInput by remember { mutableStateOf("1.00") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    // Interactive 3D Scan Guidance State (loaded automatically on first camera launch)
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("metrax_ar_prefs", Context.MODE_PRIVATE)
    }
    var showScanGuide by remember {
        mutableStateOf(!sharedPrefs.getBoolean("has_completed_scan_guide", false))
    }
    val textMeasurer = rememberTextMeasurer()

    val calculatedValue = viewModel.calculateCurrentValue()
    val currentArea = viewModel.calculateCurrentArea()

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(strings.deleteConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, strings.clearAll, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 1. Scale Calibration Dialog
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCalibrationDialog(false) },
            title = {
                Text(
                    text = "Calibración de escala",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Seleccioná un objeto de referencia conocido colocado en la toma, o ingresá su dimensión manual:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    CalibrationPreset.values().forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (preset == calibrationPreset) PrimaryAmber.copy(alpha = 0.2f)
                                    else Color.DarkGray.copy(alpha = 0.2f)
                                )
                                .clickable { viewModel.setCalibrationPreset(preset) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (preset == calibrationPreset) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (preset == calibrationPreset) PrimaryAmber else Color.White
                            )
                        }
                    }

                    if (calibrationPreset == CalibrationPreset.CUSTOM) {
                        OutlinedTextField(
                            value = customRefInput,
                            onValueChange = {
                                customRefInput = it
                                it.toDoubleOrNull()?.let { m -> viewModel.setCustomRefMeters(m) }
                            },
                            label = { Text("Longitud de referencia en metros (ej. 1.50)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (points.size >= 2) {
                        Button(
                            onClick = {
                                viewModel.calibrateScaleFromPoints()
                                Toast.makeText(context, "Escala calibrada con éxito", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Calibrar usando los 2 primeros puntos", color = Color.White)
                        }
                    } else {
                        Text(
                            text = "💡 Colocá al menos 2 puntos sobre el objeto de referencia en la cámara para aplicar la calibración.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowCalibrationDialog(false) }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // 2. Save Measurement Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSaveDialog(false) },
            title = {
                Text(
                    text = strings.saveMeasurement,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when (mode) {
                            MeasurementMode.DISTANCE -> "${strings.modeDistanceTitle.split(" ")[0]}: " + GeometryUtils.formatLength(calculatedValue, unitSystem)
                            MeasurementMode.AREA -> "${strings.modeAreaTitle.split(" ")[0]}: " + GeometryUtils.formatArea(calculatedValue, unitSystem)
                            MeasurementMode.VOLUME -> "${strings.modeVolumeTitle.split(" ")[0]}: " + GeometryUtils.formatVolume(calculatedValue, unitSystem) +
                                    "\n(${strings.baseArea}: ${GeometryUtils.formatArea(currentArea, unitSystem)}, ${strings.heightText}: ${GeometryUtils.formatLength(heightMeters, unitSystem)})"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = when (mode) {
                                MeasurementMode.DISTANCE -> PrimaryAmber
                                MeasurementMode.AREA -> SecondaryCyan
                                MeasurementMode.VOLUME -> AccentEmerald
                            }
                        )
                    )
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text(strings.saveTitle) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_save_title")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMeasurement(saveTitleInput)
                        saveTitleInput = ""
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (mode) {
                            MeasurementMode.DISTANCE -> PrimaryAmber
                            MeasurementMode.AREA -> SecondaryCyan
                            MeasurementMode.VOLUME -> AccentEmerald
                        }
                    ),
                    modifier = Modifier.testTag("btn_confirm_save")
                ) {
                    Text(strings.save, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSaveDialog(false) }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 1. Live Camera Feed Layer with ARCore Sceneview
        var childNodes by remember { mutableStateOf(listOf<Node>()) }
        var isScanning by remember { mutableStateOf(false) }
        var arSceneView by remember { mutableStateOf<io.github.sceneview.ar.ARSceneView?>(null) }
        var latestFrame by remember { mutableStateOf<com.google.ar.core.Frame?>(null) }
        var currentCamPose by remember { mutableStateOf<com.google.ar.core.Pose?>(null) }
        var cameraTrajectory by remember { mutableStateOf<List<Point3D>>(emptyList()) }
        var isArInstalled by remember { mutableStateOf<Boolean?>(null) }
        var countdownValue by remember { mutableStateOf<Int?>(null) }

        androidx.compose.runtime.LaunchedEffect(countdownValue) {
            if (countdownValue != null) {
                if (countdownValue!! > 0) {
                    kotlinx.coroutines.delay(1000)
                    countdownValue = countdownValue!! - 1
                } else {
                    isScanning = true
                    countdownValue = null
                }
            }
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            try {
                context.packageManager.getPackageInfo("com.google.ar.core", 0)
                isArInstalled = true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                isArInstalled = false
            }
        }

        if (isArInstalled == true) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    io.github.sceneview.ar.ARSceneView(context).apply {
                        planeRenderer.isVisible = true
                        
                        configureSession { session, config ->
                            config.depthMode = com.google.ar.core.Config.DepthMode.AUTOMATIC
                            config.planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                            config.lightEstimationMode = com.google.ar.core.Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        }

                    onSessionUpdated = { session, frame ->
                        latestFrame = frame
                        val pose = frame.camera.pose
                        currentCamPose = pose

                        // Record real-time motion trajectory as user moves phone
                        val newPoint = Point3D(pose.tx(), pose.ty(), pose.tz())
                        val lastPoint = cameraTrajectory.lastOrNull()
                        if (lastPoint == null || GeometryUtils.distance3D(lastPoint, newPoint) > 0.02) {
                            cameraTrajectory = (cameraTrajectory + newPoint).takeLast(35)
                        }

                        if (isScanning && mode == MeasurementMode.VOLUME) {
                            try {
                                val pointCloudAr = frame.acquirePointCloud()
                                val pointsBuffer = pointCloudAr.points
                                val numPoints = pointsBuffer.limit() / 4
                                
                                if (numPoints > 0) {
                                    val newPoints = ArrayList<PointCloudPoint>()
                                    val step = (numPoints / 50).coerceIn(1, 10)
                                    for (i in 0 until numPoints step step) {
                                        val x = pointsBuffer.get(i * 4)
                                        val y = pointsBuffer.get(i * 4 + 1)
                                        val z = pointsBuffer.get(i * 4 + 2)
                                        val confidence = pointsBuffer.get(i * 4 + 3)
                                        
                                        if (confidence > 0.2f) {
                                            val dx = x - pose.tx()
                                            val dy = y - pose.ty()
                                            val dz = z - pose.tz()
                                            val depth = sqrt(dx * dx + dy * dy + dz * dz)
                                            val hue = ((depth - 0.3f) / 2.5f).coerceIn(0f, 1f)
                                            newPoints.add(PointCloudPoint(x, y, z, confidence, depth, hue))
                                        }
                                    }
                                    if (newPoints.isNotEmpty()) {
                                        viewModel.addPointCloudPoints(newPoints)
                                    }
                                }
                                pointCloudAr.release()
                            } catch (e: Exception) { }
                        }
                    }

                    setOnTouchListener { _, motionEvent ->
                        if (motionEvent.action == android.view.MotionEvent.ACTION_UP && mode != MeasurementMode.VOLUME) {
                            latestFrame?.let { frame ->
                                val hitResults = frame.hitTest(motionEvent.x, motionEvent.y)
                                val hit = hitResults.firstOrNull { it.trackable is com.google.ar.core.Plane }
                                if (hit != null) {
                                    val pose = hit.hitPose
                                    viewModel.addPoint(Point3D(pose.tx(), pose.ty(), pose.tz()))
                                    
                                    try {
                                        val anchorNode = io.github.sceneview.ar.node.AnchorNode(engine, hit.createAnchor())
                                        val sphereNode = io.github.sceneview.node.SphereNode(
                                            engine = engine,
                                            radius = 0.025f,
                                            center = io.github.sceneview.math.Position(0f, 0f, 0f)
                                        )
                                        anchorNode.addChildNode(sphereNode)
                                        addChildNode(anchorNode)
                                    } catch (e: Exception) { }
                                }
                            }
                        }
                        false
                    }
                }
            },
            update = { view ->
                arSceneView = view
            }
        )
        } else if (isArInstalled == false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B14))
            ) {
                // Background subtle simulation spatial grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 45.dp.toPx()
                    val cols = (size.width / step).toInt()
                    val rows = (size.height / step).toInt()
                    for (x in 0..cols) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.3f),
                            start = Offset(x * step, 0f),
                            end = Offset(x * step, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (y in 0..rows) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.3f),
                            start = Offset(0f, y * step),
                            end = Offset(size.width, y * step),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = PrimaryAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Modo Simulación LiDAR (ARCore no detectado)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryAmber
                        )
                    }
                }
            }
        }

        // 2. Center Reticle & HUD Canvas Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Touch gesture handled by Sceneview or Marcar Punto button
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val reticleRadius = 20.dp.toPx()
            val tickLen = 8.dp.toPx()

            // Outer reticle circle
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = reticleRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center precision dot
            drawCircle(
                color = PrimaryAmber,
                radius = 3.5.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // Crosshair ticks
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX - reticleRadius - tickLen, centerY),
                end = Offset(centerX - reticleRadius + 3.dp.toPx(), centerY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX + reticleRadius - 3.dp.toPx(), centerY),
                end = Offset(centerX + reticleRadius + tickLen, centerY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX, centerY - reticleRadius - tickLen),
                end = Offset(centerX, centerY - reticleRadius + 3.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX, centerY + reticleRadius - 3.dp.toPx()),
                end = Offset(centerX, centerY + reticleRadius + tickLen),
                strokeWidth = 1.5.dp.toPx()
            )

            // --- REAL-TIME AR OVERLAY & MEASUREMENT PATH OVERLAY ---

            // 1. Draw Real-Time Camera Trajectory Path Trail
            if (cameraTrajectory.size >= 2) {
                val trajectoryPath = Path()
                var firstPointDrawn = false
                cameraTrajectory.forEach { pt ->
                    val proj = projectPoint3DToScreen(pt, latestFrame, size.width, size.height)
                    if (proj != null) {
                        if (!firstPointDrawn) {
                            trajectoryPath.moveTo(proj.x, proj.y)
                            firstPointDrawn = true
                        } else {
                            trajectoryPath.lineTo(proj.x, proj.y)
                        }
                    }
                }
                if (firstPointDrawn) {
                    drawPath(
                        path = trajectoryPath,
                        color = SecondaryCyan.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }
            }

            // 2. Project Marked 3D Points to Screen Offsets
            val projectedPoints = points.map { pt ->
                pt to projectPoint3DToScreen(pt, latestFrame, size.width, size.height)
            }

            val themeColor = when (mode) {
                MeasurementMode.DISTANCE -> PrimaryAmber
                MeasurementMode.AREA -> SecondaryCyan
                MeasurementMode.VOLUME -> AccentEmerald
            }

            // 3. Draw Lines and Segment Labels between Consecutive Marked Points
            // (skipped in VOLUME mode: points there are an unordered point
            // cloud, not ordered corner taps, so connecting them draws a
            // scribble instead of a measurement path)
            if (mode != MeasurementMode.VOLUME && projectedPoints.size >= 2) {
                for (i in 0 until projectedPoints.size - 1) {
                    val (p1, screen1) = projectedPoints[i]
                    val (p2, screen2) = projectedPoints[i + 1]

                    if (screen1 != null && screen2 != null) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.5f),
                            start = screen1,
                            end = screen2,
                            strokeWidth = 6.dp.toPx()
                        )
                        drawLine(
                            color = themeColor,
                            start = screen1,
                            end = screen2,
                            strokeWidth = 3.5.dp.toPx()
                        )

                        val segDist = GeometryUtils.distance3D(p1, p2) * scaleFactor
                        val labelText = GeometryUtils.formatLength(segDist, unitSystem)

                        val midX = (screen1.x + screen2.x) / 2f
                        val midY = (screen1.y + screen2.y) / 2f
                        val textResult = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        val paddingPx = 8.dp.toPx()
                        val badgeWidth = textResult.size.width + paddingPx * 2
                        val badgeHeight = textResult.size.height + paddingPx

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.8f),
                            topLeft = Offset(midX - badgeWidth / 2f, midY - badgeHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawRoundRect(
                            color = themeColor,
                            topLeft = Offset(midX - badgeWidth / 2f, midY - badgeHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(midX - textResult.size.width / 2f, midY - textResult.size.height / 2f)
                        )
                    }
                }

                // Closing segment: connects the last point back to the first
                // once there are enough points to form a closed surface, so
                // the polygon visibly closes instead of dangling open.
                if (mode == MeasurementMode.AREA && projectedPoints.size >= 3) {
                    val (pFirst, screenFirst) = projectedPoints.first()
                    val (pLast, screenLast) = projectedPoints.last()

                    if (screenFirst != null && screenLast != null) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.5f),
                            start = screenLast,
                            end = screenFirst,
                            strokeWidth = 6.dp.toPx()
                        )
                        drawLine(
                            color = themeColor,
                            start = screenLast,
                            end = screenFirst,
                            strokeWidth = 3.5.dp.toPx()
                        )

                        val closeDist = GeometryUtils.distance3D(pLast, pFirst) * scaleFactor
                        val closeLabelText = GeometryUtils.formatLength(closeDist, unitSystem)

                        val midX = (screenLast.x + screenFirst.x) / 2f
                        val midY = (screenLast.y + screenFirst.y) / 2f
                        val closeTextResult = textMeasurer.measure(
                            text = closeLabelText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        val paddingPx = 8.dp.toPx()
                        val badgeWidth = closeTextResult.size.width + paddingPx * 2
                        val badgeHeight = closeTextResult.size.height + paddingPx

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.8f),
                            topLeft = Offset(midX - badgeWidth / 2f, midY - badgeHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawRoundRect(
                            color = themeColor,
                            topLeft = Offset(midX - badgeWidth / 2f, midY - badgeHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawText(
                            textLayoutResult = closeTextResult,
                            topLeft = Offset(midX - closeTextResult.size.width / 2f, midY - closeTextResult.size.height / 2f)
                        )
                    }
                }
            }

            // 4. Draw Vertex/Point Markers
            if (mode == MeasurementMode.VOLUME) {
                // Point cloud: small dots only, no vertex chrome
                projectedPoints.forEach { (_, screenPos) ->
                    if (screenPos != null) {
                        drawCircle(
                            color = themeColor.copy(alpha = 0.85f),
                            radius = 2.5.dp.toPx(),
                            center = screenPos
                        )
                    }
                }
            } else {
                val isClosedSurface = mode == MeasurementMode.AREA && projectedPoints.size >= 3
                projectedPoints.forEachIndexed { index, (pt3d, screenPos) ->
                    if (screenPos != null) {
                        // Highlight the starting point once the surface has
                        // closed back onto it, as visible confirmation.
                        if (isClosedSurface && index == 0) {
                            drawCircle(
                                color = AccentEmerald.copy(alpha = 0.35f),
                                radius = 16.dp.toPx(),
                                center = screenPos
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 9.dp.toPx(),
                                center = screenPos
                            )
                            drawCircle(
                                color = AccentEmerald,
                                radius = 7.dp.toPx(),
                                center = screenPos
                            )
                        } else {
                            drawCircle(
                                color = themeColor.copy(alpha = 0.3f),
                                radius = 12.dp.toPx(),
                                center = screenPos
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 8.dp.toPx(),
                                center = screenPos
                            )
                            drawCircle(
                                color = themeColor,
                                radius = 6.dp.toPx(),
                                center = screenPos
                            )
                        }
                    }
                }
            }

            // 5. REAL-TIME DYNAMIC LINE OVERLAY AS USER MOVES THE PHONE
            // (not applicable in VOLUME mode: no single "last marked corner"
            // to draw a live line towards while the point cloud is scanning)
            if (mode != MeasurementMode.VOLUME && points.isNotEmpty()) {
                val lastPoint3D = points.last()
                val lastScreenPos = projectPoint3DToScreen(lastPoint3D, latestFrame, size.width, size.height)
                val activeTargetScreenPos = Offset(centerX, centerY)

                val lineStart = lastScreenPos ?: Offset(centerX, centerY + 120f)

                // Draw Dashed Active Real-Time Path Line to Reticle Center
                drawLine(
                    color = Color.Black.copy(alpha = 0.6f),
                    start = lineStart,
                    end = activeTargetScreenPos,
                    strokeWidth = 6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                )
                drawLine(
                    color = themeColor,
                    start = lineStart,
                    end = activeTargetScreenPos,
                    strokeWidth = 3.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                )

                // Calculate Live Distance to Current Camera Pose
                val currentCam = currentCamPose
                val liveDist = if (currentCam != null) {
                    val cam3D = Point3D(currentCam.tx(), currentCam.ty(), currentCam.tz())
                    GeometryUtils.distance3D(lastPoint3D, cam3D) * scaleFactor
                } else {
                    0.0
                }

                if (liveDist > 0.02) {
                    val liveLabel = "📍 " + GeometryUtils.formatLength(liveDist, unitSystem) + " (en vivo)"
                    val midX = (lineStart.x + activeTargetScreenPos.x) / 2f
                    val midY = (lineStart.y + activeTargetScreenPos.y) / 2f - 24.dp.toPx()

                    val liveTextResult = textMeasurer.measure(
                        text = liveLabel,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryAmber
                        )
                    )
                    val pad = 10.dp.toPx()
                    val bW = liveTextResult.size.width + pad * 2
                    val bH = liveTextResult.size.height + pad

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.85f),
                        topLeft = Offset(midX - bW / 2f, midY - bH / 2f),
                        size = androidx.compose.ui.geometry.Size(bW, bH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    drawRoundRect(
                        color = PrimaryAmber,
                        topLeft = Offset(midX - bW / 2f, midY - bH / 2f),
                        size = androidx.compose.ui.geometry.Size(bW, bH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = liveTextResult,
                        topLeft = Offset(midX - liveTextResult.size.width / 2f, midY - liveTextResult.size.height / 2f)
                    )
                }

                // 6. Real-Time Closing Line for Area Mode
                // (VOLUME is excluded from this whole block above, so this
                // only ever fires for AREA)
                if (mode == MeasurementMode.AREA && points.size >= 2) {
                    val firstPoint3D = points.first()
                    val firstScreenPos = projectPoint3DToScreen(firstPoint3D, latestFrame, size.width, size.height)

                    if (firstScreenPos != null) {
                        drawLine(
                            color = SecondaryCyan.copy(alpha = 0.7f),
                            start = activeTargetScreenPos,
                            end = firstScreenPos,
                            strokeWidth = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    }
                }
            }
            
            // Giant Green Arrow (como muestra la imagen de SR Measure)
            if (mode == MeasurementMode.DISTANCE && points.size == 1) {
                val arrowWidth = 180.dp.toPx()
                val arrowHeight = 120.dp.toPx()
                val tailHeight = 50.dp.toPx()
                val headWidth = 70.dp.toPx()
                
                val startX = centerX - arrowWidth / 2f
                val startY = centerY + 120.dp.toPx() // Positioned below reticle
                
                val arrowPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(startX, startY - tailHeight / 2f)
                    lineTo(startX + arrowWidth - headWidth, startY - tailHeight / 2f)
                    lineTo(startX + arrowWidth - headWidth, startY - arrowHeight / 2f)
                    lineTo(startX + arrowWidth, startY)
                    lineTo(startX + arrowWidth - headWidth, startY + arrowHeight / 2f)
                    lineTo(startX + arrowWidth - headWidth, startY + tailHeight / 2f)
                    lineTo(startX, startY + tailHeight / 2f)
                    close()
                }
                
                // Draw green fill
                drawPath(
                    path = arrowPath,
                    color = Color(0xFF00FF00) // Bright green like in the image
                )
                
                // Draw black outline
                drawPath(
                    path = arrowPath,
                    color = Color.Black,
                    style = Stroke(width = 6.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }

            // --- 3D LIDAR POINT CLOUD RENDERING FOR VOLUME / SCAN MODE ---
            if (mode == MeasurementMode.VOLUME && pointCloud.isNotEmpty()) {
                val step = (pointCloud.size / 600).coerceAtLeast(1)
                for (i in 0 until pointCloud.size step step) {
                    val pt = pointCloud[i]
                    val screenPos = projectPointCloudPointToScreen(pt, latestFrame, size.width, size.height)
                    if (screenPos != null) {
                        val ptColor = GeometryUtils.getColorForPoint(pt, pointCloudColorMap)
                        val radius = (2.5f * pt.confidence).coerceIn(1.5f, 5.0f).dp.toPx()
                        // Glowing laser halo
                        drawCircle(
                            color = ptColor.copy(alpha = 0.35f),
                            radius = radius * 2.2f,
                            center = screenPos
                        )
                        // Laser core
                        drawCircle(
                            color = ptColor,
                            radius = radius,
                            center = screenPos
                        )
                    }
                }

                // Render 3D Wireframe Bounding Box in AR
                boundingBox3D?.let { box ->
                    fun proj(x: Float, y: Float, z: Float) =
                        projectPointCloudPointToScreen(PointCloudPoint(x, y, z), latestFrame, size.width, size.height)

                    val c000 = proj(box.minX, box.minY, box.minZ)
                    val c100 = proj(box.maxX, box.minY, box.minZ)
                    val c110 = proj(box.maxX, box.maxY, box.minZ)
                    val c010 = proj(box.minX, box.maxY, box.minZ)

                    val c001 = proj(box.minX, box.minY, box.maxZ)
                    val c101 = proj(box.maxX, box.minY, box.maxZ)
                    val c111 = proj(box.maxX, box.maxY, box.maxZ)
                    val c011 = proj(box.minX, box.maxY, box.maxZ)

                    fun drawWireEdge(p1: Offset?, p2: Offset?, color: Color = AccentEmerald) {
                        if (p1 != null && p2 != null) {
                            drawLine(
                                color = color.copy(alpha = 0.85f),
                                start = p1,
                                end = p2,
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        }
                    }

                    // Bottom face
                    drawWireEdge(c000, c100)
                    drawWireEdge(c100, c101)
                    drawWireEdge(c101, c001)
                    drawWireEdge(c001, c000)

                    // Top face
                    drawWireEdge(c010, c110)
                    drawWireEdge(c110, c111)
                    drawWireEdge(c111, c011)
                    drawWireEdge(c011, c010)

                    // Vertical pillars
                    drawWireEdge(c000, c010)
                    drawWireEdge(c100, c110)
                    drawWireEdge(c101, c111)
                    drawWireEdge(c001, c011)

                    listOfNotNull(c000, c100, c110, c010, c001, c101, c111, c011).forEach { corner ->
                        drawCircle(PrimaryAmber, radius = 4.dp.toPx(), center = corner)
                    }
                }
            }

            // Real-time laser sweep scanner animation when actively scanning
            if (isScanning && mode == MeasurementMode.VOLUME) {
                val scanLineY = (System.currentTimeMillis() % 2200L) / 2200f * size.height
                drawLine(
                    color = AccentEmerald,
                    start = Offset(0f, scanLineY),
                    end = Offset(size.width, scanLineY),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            AccentEmerald.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = scanLineY,
                        endY = scanLineY + 80.dp.toPx()
                    ),
                    topLeft = Offset(0f, scanLineY),
                    size = Size(size.width, 80.dp.toPx())
                )
            }
        }

        // 3. Top Header Controls Overlay with TopAppBar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TopAppBar(
                modifier = Modifier.testTag("measure_top_app_bar"),
                title = {
                    // Mode Selector Pills inside TopAppBar Title
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MeasurementMode.values().forEach { m ->
                            val active = m == mode
                            val pillColor = when (m) {
                                MeasurementMode.DISTANCE -> PrimaryAmber
                                MeasurementMode.AREA -> SecondaryCyan
                                MeasurementMode.VOLUME -> AccentEmerald
                            }
                            val modeText = when (m) {
                                MeasurementMode.DISTANCE -> strings.modeDistanceTitle.split(" ")[0]
                                MeasurementMode.AREA -> strings.modeAreaTitle.split(" ")[0]
                                MeasurementMode.VOLUME -> strings.modeVolumeTitle.split(" ")[0]
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (active) pillColor else Color.Transparent)
                                    .clickable { viewModel.setMode(m) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("mode_${m.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (m) {
                                            MeasurementMode.DISTANCE -> Icons.Default.Straighten
                                            MeasurementMode.AREA -> Icons.Default.CropSquare
                                            MeasurementMode.VOLUME -> Icons.Default.ViewInAr
                                        },
                                        contentDescription = null,
                                        tint = if (active) (if (m == MeasurementMode.DISTANCE) Color.White else Color.Black) else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (active) {
                                        Text(
                                            text = modeText,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (m == MeasurementMode.DISTANCE) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Unit Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { viewModel.toggleUnitSystem() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("btn_toggle_unit")
                    ) {
                        Text(
                            text = if (unitSystem == UnitSystem.METRIC) "m/cm" else "ft/in",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SecondaryCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Help & 3D Scan Precision Guide Button
                    IconButton(
                        onClick = { showScanGuide = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("btn_open_scan_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = strings.scanGuideHelpTooltip,
                            tint = PrimaryAmber
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Delete Sweep button to Clear History
                    IconButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("btn_clear_history_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = strings.clearAll,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Calibration & Plane Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { viewModel.setShowCalibrationDialog(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = PrimaryAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${strings.calibration}: ${calibrationPreset.displayName.split(" ")[0]}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color.White
                        )
                    }
                }

                if (mode == MeasurementMode.VOLUME) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, AccentEmerald),
                        modifier = Modifier
                            .clickable {
                                if (pointCloud.isEmpty()) {
                                    viewModel.generateSimulatedPointCloud()
                                }
                                viewModel.set3DViewerMode(true)
                            }
                            .testTag("btn_quick_open_3d_viewer")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "🧊 ${strings.pointCloudViewer} (${pointCloud.size} pts)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = AccentEmerald
                            )
                        }
                    }
                }
            }
        }

        // 4. Volume Height / Depth Parameter Controller Card
        if (mode == MeasurementMode.VOLUME && isArInstalled == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 640.dp)
                    .padding(top = 135.dp, start = 20.dp, end = 20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Height,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${strings.heightText}: " + GeometryUtils.formatLength(heightMeters, unitSystem),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Slider(
                            value = heightSliderValue,
                            onValueChange = {
                                heightSliderValue = it
                                viewModel.setHeightMeters(it.toDouble())
                            },
                            valueRange = 0.1f..10.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentEmerald,
                                activeTrackColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("slider_height")
                        )
                        
                        // Material Selection Menu
                        var expanded by remember { mutableStateOf(false) }
                        val selectedMaterial by viewModel.selectedMaterial.collectAsStateWithLifecycle()
                        val materials = viewModel.availableMaterials
                        
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                                modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = strings.material,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                        Text(
                                            text = "${selectedMaterial.name} (${selectedMaterial.densityKgPerM3.toInt()} kg/m³)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Slate400
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                materials.forEach { material ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${material.name} - ${material.densityKgPerM3.toInt()} kg/m³",
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            viewModel.setMaterial(material)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Editable density (kg/m³) for the selected material
                        var densityInput by remember(selectedMaterial.name) {
                            mutableStateOf(selectedMaterial.densityKgPerM3.toInt().toString())
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Densidad:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                            OutlinedTextField(
                                value = densityInput,
                                onValueChange = { input ->
                                    densityInput = input
                                    input.toFloatOrNull()?.let { viewModel.setCustomDensity(it) }
                                },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("input_material_density")
                            )
                            Text(
                                text = "kg/m³",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        // 5. Dynamic Guidance Hint Banner
        if (isArInstalled == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 640.dp)
                    .padding(top = if (mode == MeasurementMode.VOLUME) 340.dp else 140.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = when {
                            points.isEmpty() -> strings.pointInstruction
                            points.size < mode.minPoints -> "${strings.addPoint}: ${points.size}/${mode.minPoints}"
                            mode == MeasurementMode.AREA -> strings.surfaceClosed
                            else -> strings.ready
                        },
                        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Countdown Overlay
        if (countdownValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Preparando escaneo en...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = countdownValue.toString(),
                        style = TextStyle(
                            fontSize = 140.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentEmerald,
                        )
                    )
                }
            }
        }

        // 6. Bottom Controls Bar & Live Measurement Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calculated Result Live Card
            AnimatedVisibility(
                visible = calculatedValue > 0.0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_result"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = when (mode) {
                            MeasurementMode.DISTANCE -> PrimaryAmber
                            MeasurementMode.AREA -> SecondaryCyan
                            MeasurementMode.VOLUME -> AccentEmerald
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (mode) {
                                MeasurementMode.DISTANCE -> strings.totalDistance
                                MeasurementMode.AREA -> strings.calculatedArea
                                MeasurementMode.VOLUME -> strings.estimatedVolume
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (mode) {
                                MeasurementMode.DISTANCE -> GeometryUtils.formatLength(calculatedValue, unitSystem)
                                MeasurementMode.AREA -> GeometryUtils.formatArea(calculatedValue, unitSystem)
                                MeasurementMode.VOLUME -> GeometryUtils.formatVolume(calculatedValue, unitSystem)
                            },
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (mode) {
                                    MeasurementMode.DISTANCE -> PrimaryAmber
                                    MeasurementMode.AREA -> SecondaryCyan
                                    MeasurementMode.VOLUME -> AccentEmerald
                                }
                            )
                        )

                        if (mode == MeasurementMode.VOLUME) {
                            val selectedMaterial by viewModel.selectedMaterial.collectAsStateWithLifecycle()
                            // Convert volume from m³ to mass in tons
                            val massTons = (calculatedValue * selectedMaterial.densityKgPerM3) / 1000.0
                            
                            Text(
                                text = "Masa: ${String.format(java.util.Locale.US, "%.2f", massTons)} Ton (${selectedMaterial.name})",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            
                            Text(
                                text = "${strings.baseArea}: ${GeometryUtils.formatArea(currentArea, unitSystem)} | ${strings.heightText}: ${GeometryUtils.formatLength(heightMeters, unitSystem)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Status text for start/end points
            if (mode == MeasurementMode.DISTANCE) {
                Text(
                    text = when (points.size) {
                        0 -> strings.locateStartingPoint
                        1 -> strings.pointSetWalkToEnd
                        else -> strings.bothPointsSet
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action Buttons for VOLUME / 3D Mode
                if (mode == MeasurementMode.VOLUME) {
                    // 1. Scan 3D / Stop Scan Button
                    Button(
                        onClick = { 
                            if (isScanning || countdownValue != null) {
                                isScanning = false
                                countdownValue = null
                            } else {
                                countdownValue = 2
                                if (pointCloud.isEmpty()) {
                                    viewModel.generateSimulatedPointCloud()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("btn_scan"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning || countdownValue != null) AccentEmerald else PrimaryAmber
                        )
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isScanning) strings.stopScan else if (countdownValue != null) strings.preparingScan else strings.scan3D,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // 2. Open 3D Viewer Button
                    Button(
                        onClick = {
                            if (pointCloud.isEmpty()) {
                                viewModel.generateSimulatedPointCloud()
                            }
                            viewModel.set3DViewerMode(true)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("btn_open_3d_viewer"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.pointCloudViewer,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // 3. Clear Cloud Button
                    Button(
                        onClick = {
                            viewModel.clearPointCloud()
                            viewModel.resetPoints()
                        },
                        enabled = pointCloud.isNotEmpty() || points.isNotEmpty(),
                        modifier = Modifier
                            .weight(0.7f)
                            .height(52.dp)
                            .testTag("btn_clear_cloud"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            disabledContainerColor = Color.White.copy(alpha = 0.06f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = strings.reset,
                            tint = if (pointCloud.isNotEmpty() || points.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Save Button
                    Button(
                        onClick = { viewModel.setShowSaveDialog(true) },
                        enabled = calculatedValue > 0.0 || pointCloud.isNotEmpty(),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(52.dp)
                            .testTag("btn_save_measurement"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEmerald,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = strings.save,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.save,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            latestFrame?.let { frame ->
                                val view = arSceneView
                                val centerX = (view?.width ?: 1080) / 2f
                                val centerY = (view?.height ?: 1920) / 2f
                                val hitResults = frame.hitTest(centerX, centerY)
                                val hit = hitResults.firstOrNull { it.trackable is com.google.ar.core.Plane }
                                if (hit != null) {
                                    val pose = hit.hitPose
                                    viewModel.addPoint(Point3D(pose.tx(), pose.ty(), pose.tz()))
                                    if (view != null) {
                                        try {
                                            val anchorNode = io.github.sceneview.ar.node.AnchorNode(view.engine, hit.createAnchor())
                                            val sphereNode = io.github.sceneview.node.SphereNode(
                                                engine = view.engine,
                                                radius = 0.025f,
                                                center = io.github.sceneview.math.Position(0f, 0f, 0f)
                                            )
                                            anchorNode.addChildNode(sphereNode)
                                            view.addChildNode(anchorNode)
                                        } catch (e: Exception) { }
                                    }
                                } else {
                                    val cameraPose = frame.camera.pose
                                    val px = cameraPose.tx() - cameraPose.zAxis[0] * 1.2f
                                    val py = cameraPose.ty() - cameraPose.zAxis[1] * 1.2f
                                    val pz = cameraPose.tz() - cameraPose.zAxis[2] * 1.2f
                                    viewModel.addPoint(Point3D(px, py, pz))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("btn_add_point_reticle"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAmber
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addPoint, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    // Undo Button
                    Button(
                        onClick = { viewModel.undoLastPoint() },
                        enabled = points.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_undo"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            disabledContainerColor = Color.White.copy(alpha = 0.06f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = strings.undo,
                            tint = if (points.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.undo, color = if (points.isNotEmpty()) Color.White else Color.Gray)
                    }

                    // Reset Button
                    Button(
                        onClick = { viewModel.resetPoints() },
                        enabled = points.isNotEmpty(),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(52.dp)
                            .testTag("btn_reset"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            disabledContainerColor = Color.White.copy(alpha = 0.06f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = strings.reset,
                            tint = if (points.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.setShowSaveDialog(true) },
                        enabled = calculatedValue > 0.0,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(52.dp)
                            .testTag("btn_save_measurement"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (mode) {
                                MeasurementMode.DISTANCE -> PrimaryAmber
                                MeasurementMode.AREA -> SecondaryCyan
                                MeasurementMode.VOLUME -> AccentEmerald
                            },
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = strings.save,
                            tint = if (mode == MeasurementMode.DISTANCE) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.save,
                            color = if (mode == MeasurementMode.DISTANCE) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 7. Full-Screen Interactive 3D Point Cloud Inspector
        if (is3DViewerMode && mode == MeasurementMode.VOLUME) {
            PointCloudViewer3D(
                points = pointCloud,
                boundingBox = boundingBox3D,
                colorMap = pointCloudColorMap,
                onColorMapChange = { viewModel.setPointCloudColorMap(it) },
                unitSystem = unitSystem,
                scaleFactor = scaleFactor,
                onCloseViewer = { viewModel.set3DViewerMode(false) },
                onExportPly = {
                    val plyText = GeometryUtils.generatePlyPointCloud(pointCloud)
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, plyText)
                        type = "text/plain"
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(sendIntent, "Exportar Nube de Puntos PLY")
                    )
                }
            )
        }
    }
    
    // Interactive 3D Surface Scanning Guidance Overlay
    if (showScanGuide) {
        ScanGuidanceOverlay(
            onDismiss = { dontShowAgain ->
                showScanGuide = false
                if (dontShowAgain) {
                    sharedPrefs.edit().putBoolean("has_completed_scan_guide", true).apply()
                }
            }
        )
    }
}

@Composable
fun AROnboardingOverlay(onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) { } // Consume clicks to prevent interacting with AR underneath
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            // Animated or static icon representing phone scanning
            Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = null,
                tint = PrimaryAmber,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = strings.mappingEnvironment,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = strings.mappingEnvironmentDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                Text(
                    text = strings.understood,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


