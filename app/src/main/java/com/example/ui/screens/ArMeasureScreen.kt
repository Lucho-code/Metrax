package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ar.ArAvailability
import com.example.ar.ArTrackingStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.UnitSystem
import com.example.ui.components.ArCameraView
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.ArMeasurementViewModel
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils

private val PointCloudPalette = listOf(
    Color(0xFF4ADE80), // green
    Color(0xFF38BDF8), // cyan/blue
    Color(0xFFE879F9), // magenta
    Color(0xFFFB7185)  // red
)

private enum class GridQuality(val label: String, val resolution: Int) {
    LOW("Rápida", 12),
    MEDIUM("Media", 20),
    HIGH("Alta", 32)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArMeasureScreen(
    sharedViewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    arViewModel: ArMeasurementViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by arViewModel.uiState.collectAsStateWithLifecycle()
    val availability by arViewModel.availability.collectAsStateWithLifecycle()
    val arResult by arViewModel.arResult.collectAsStateWithLifecycle()
    val isComputing by arViewModel.isComputing.collectAsStateWithLifecycle()
    val showSaveDialog by arViewModel.showSaveDialog.collectAsStateWithLifecycle()
    val unitSystem by sharedViewModel.unitSystem.collectAsStateWithLifecycle()

    var saveTitleInput by remember { mutableStateOf("") }
    var gridQuality by remember { mutableStateOf(GridQuality.MEDIUM) }
    var calibrationDistanceInput by remember { mutableStateOf("") }

    val toeCount = uiState.toePointsScreen.size
    val calibrationActive = uiState.calibrationModeActive

    if (showSaveDialog && arResult != null) {
        val result = arResult!!
        AlertDialog(
            onDismissRequest = { arViewModel.setShowSaveDialog(false) },
            title = { Text("Guardar medición AR", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Volumen: " + GeometryUtils.formatVolume(result.volumeCubicMeters, unitSystem),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AccentEmerald
                        )
                    )
                    Text(
                        text = "Área base: ${GeometryUtils.formatArea(result.baseAreaSquareMeters, unitSystem)} · " +
                            "Altura máx: ${GeometryUtils.formatLength(result.maxHeightMeters, unitSystem)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Etiqueta (ej. Pila de arena, Acopio N°3)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_save_ar_title")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        arViewModel.saveMeasurement(saveTitleInput)
                        saveTitleInput = ""
                        Toast.makeText(context, "Medición AR guardada en el historial", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier.testTag("btn_confirm_save_ar")
                ) {
                    Text("Guardar", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { arViewModel.setShowSaveDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ArCameraView(
            modifier = Modifier.fillMaxSize(),
            onUiState = arViewModel::onUiStateUpdate,
            onToePointsChanged = arViewModel::onToePointsChanged,
            onVolumeResult = arViewModel::onVolumeResult,
            onAvailabilityChanged = arViewModel::onAvailabilityChanged,
            onRendererReady = arViewModel::onRendererReady
        )

        // Point cloud + toe polygon overlay. No pointer input here on purpose:
        // taps must reach the GLSurfaceView underneath (see ArCameraView).
        Canvas(modifier = Modifier.fillMaxSize()) {
            uiState.pointCloudScreenPoints.forEachIndexed { index, point ->
                drawCircle(
                    color = PointCloudPalette[index % PointCloudPalette.size],
                    radius = 4.5f,
                    center = point
                )
            }

            val toePoints = uiState.toePointsScreen
            if (toePoints.size >= 2) {
                val path = Path().apply {
                    moveTo(toePoints.first().x, toePoints.first().y)
                    for (i in 1 until toePoints.size) lineTo(toePoints[i].x, toePoints[i].y)
                    if (toePoints.size >= 3) close()
                }
                drawPath(path, color = AccentEmerald, style = Stroke(width = 5f))
                if (toePoints.size >= 3) {
                    drawPath(path, color = AccentEmerald.copy(alpha = 0.18f))
                }
            }
            toePoints.forEachIndexed { index, point ->
                drawCircle(color = AccentEmerald.copy(alpha = 0.35f), radius = 20f, center = point)
                drawCircle(color = AccentEmerald, radius = 10f, center = point)
                drawCircle(color = Color.White, radius = 3f, center = point)
            }

            val calibrationPoints = uiState.calibrationPointsScreen
            if (calibrationPoints.size == 2) {
                drawLine(
                    color = PrimaryOrange,
                    start = calibrationPoints[0],
                    end = calibrationPoints[1],
                    strokeWidth = 5f
                )
            }
            calibrationPoints.forEach { point ->
                drawCircle(color = PrimaryOrange.copy(alpha = 0.35f), radius = 20f, center = point)
                drawCircle(color = PrimaryOrange, radius = 10f, center = point)
                drawCircle(color = Color.White, radius = 3f, center = point)
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("back_button_ar")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "Volumen AR · Nube de puntos",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentEmerald,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { arViewModel.setCalibrationMode(!calibrationActive) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (calibrationActive) PrimaryOrange else Color.Black.copy(alpha = 0.65f))
                        .testTag("btn_toggle_calibration_ar")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Calibrar AR", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .testTag("btn_toggle_unit_ar")
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (unitSystem == UnitSystem.METRIC) "m³" else "yd³",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryCyan
                    )
                }
            }
        }

        // Status / tracking pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val trackingColor = when (uiState.trackingStatus) {
                        ArTrackingStatus.TRACKING -> AccentEmerald
                        ArTrackingStatus.INITIALIZING -> PrimaryOrange
                        ArTrackingStatus.LOST -> AccentRose
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(trackingColor)
                    )
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        color = Color.White
                    )
                }
            }
        }

        // Active calibration correction indicator (only when not mid-calibration, to avoid
        // duplicating the live readout already shown in the calibration panel below).
        if (!calibrationActive && uiState.lengthCorrectionFactor != 1.0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = PrimaryOrange.copy(alpha = 0.85f)) {
                    Text(
                        text = "Calibración activa: ×${"%.3f".format(uiState.lengthCorrectionFactor)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Availability overlays (permission / install / unsupported / checking)
        when (val currentAvailability = availability) {
            is ArAvailability.Checking -> AvailabilityOverlay(
                message = "Verificando compatibilidad con AR…",
                showSpinner = true
            )
            is ArAvailability.NeedsCameraPermission -> AvailabilityOverlay(
                message = "Se necesita permiso de cámara para medir con nube de puntos AR.",
                showSpinner = false
            )
            is ArAvailability.NeedsInstall -> AvailabilityOverlay(
                message = "Instalando/actualizando Google Play Services para AR…",
                showSpinner = true
            )
            is ArAvailability.Unsupported -> AvailabilityOverlay(
                message = currentAvailability.reason,
                showSpinner = false,
                actionLabel = "Volver a medición manual",
                onAction = onNavigateBack
            )
            is ArAvailability.Ready -> { /* normal AR UI shown below */ }
        }

        // Bottom control panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (calibrationActive) {
                CalibrationPanel(
                    measuredDistance = uiState.calibrationMeasuredDistance,
                    unitSystem = unitSystem,
                    distanceInput = calibrationDistanceInput,
                    onDistanceInputChange = { calibrationDistanceInput = it },
                    onReset = { arViewModel.resetCalibrationPoints() },
                    onApply = {
                        val trueValue = calibrationDistanceInput.replace(",", ".").toDoubleOrNull()
                        if (trueValue == null || trueValue <= 0.0) {
                            Toast.makeText(context, "Ingresá una medida real válida", Toast.LENGTH_SHORT).show()
                        } else {
                            arViewModel.applyCalibration(trueValue)
                            calibrationDistanceInput = ""
                        }
                    },
                    onExit = { arViewModel.setCalibrationMode(false) }
                )
                return@Column
            }

            arResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_ar_result"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.88f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentEmerald)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Volumen Estimado", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                        Text(
                            text = GeometryUtils.formatVolume(result.volumeCubicMeters, unitSystem),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                color = AccentEmerald
                            )
                        )
                        Text(
                            text = "Área base: ${GeometryUtils.formatArea(result.baseAreaSquareMeters, unitSystem)} · " +
                                "Altura máx: ${GeometryUtils.formatLength(result.maxHeightMeters, unitSystem)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            ConfidenceBadge("Cobertura Superficie", result.surfaceCoverageLevel)
                            ConfidenceBadge("Cobertura Contorno", result.toeCoverageLevel)
                        }
                    }
                }
            }

            // Grid quality selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GridQuality.values().forEach { quality ->
                    val active = quality == gridQuality
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) SecondaryCyan else Color.Transparent)
                            .clickable { gridQuality = quality }
                            .testTag("grid_quality_${quality.name}")
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Precisión: ${quality.label}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (active) Color.Black else Color.LightGray
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { arViewModel.undo() },
                    enabled = toeCount > 0,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("btn_undo_ar"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deshacer", color = Color.White)
                }

                Button(
                    onClick = { arViewModel.reset() },
                    enabled = toeCount > 0,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("btn_reset_ar"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reiniciar", color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { arViewModel.computeVolume(gridQuality.resolution) },
                    enabled = toeCount >= 3 && !isComputing,
                    modifier = Modifier.weight(1.3f).height(52.dp).testTag("btn_compute_volume"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    if (isComputing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Calcular Volumen", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { arViewModel.setShowSaveDialog(true) },
                    enabled = arResult != null,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("btn_save_ar"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = {
                        val result = arResult ?: return@Button
                        val text = "📏 Metraje Instante - Volumen AR\n" +
                            "📊 Volumen: ${GeometryUtils.formatVolume(result.volumeCubicMeters, unitSystem)}\n" +
                            "📐 Área base: ${GeometryUtils.formatArea(result.baseAreaSquareMeters, unitSystem)}\n" +
                            "📈 Altura máxima: ${GeometryUtils.formatLength(result.maxHeightMeters, unitSystem)}\n" +
                            "✅ Cobertura superficie: ${result.surfaceCoverageLevel.displayName}\n" +
                            "✅ Cobertura contorno: ${result.toeCoverageLevel.displayName}\n" +
                            "🔬 Método: ARCore Depth + Nube de puntos"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Medición AR de volumen")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir medición"))
                    },
                    enabled = arResult != null,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("btn_share_ar"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCyan,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CalibrationPanel(
    measuredDistance: Double?,
    unitSystem: UnitSystem,
    distanceInput: String,
    onDistanceInputChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_ar_calibration"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                Text(
                    text = "Calibración AR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Surface(shape = RoundedCornerShape(10.dp), color = PrimaryOrange.copy(alpha = 0.12f)) {
                Text(
                    text = "1) Elegí un objeto de longitud conocida (ej. una cinta métrica extendida en el piso).\n" +
                        "2) Tocá el punto INICIAL y después el FINAL sobre ese objeto, en la cámara.\n" +
                        "3) Escribí cuánto mide en realidad y presioná \"Aplicar\".\n" +
                        "Si un toque no marca nada, acercate o apuntá a una zona con más textura/luz.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (measuredDistance != null) {
                Surface(shape = RoundedCornerShape(10.dp), color = SecondaryCyan.copy(alpha = 0.15f)) {
                    Text(
                        text = "Medición actual de la app: " + GeometryUtils.formatLength(measuredDistance, unitSystem),
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = onDistanceInputChange,
                    label = { Text("Medida REAL en metros (ej. 1.00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_ar_calibration_distance")
                )
            } else {
                Text(
                    text = "💡 Tocá el primer punto y luego el segundo sobre el objeto de referencia.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).testTag("btn_ar_calibration_reset"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f))
                ) {
                    Text("Reiniciar puntos", color = Color.White)
                }
                Button(
                    onClick = onApply,
                    enabled = measuredDistance != null,
                    modifier = Modifier.weight(1f).testTag("btn_ar_calibration_apply"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Aplicar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth().testTag("btn_ar_calibration_exit")) {
                Text("Salir de calibración", color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(label: String, level: ConfidenceLevel) {
    val color = when (level) {
        ConfidenceLevel.HIGH -> AccentEmerald
        ConfidenceLevel.MEDIUM -> PrimaryOrange
        ConfidenceLevel.LOW -> AccentRose
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Text(level.displayName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun AvailabilityOverlay(
    message: String,
    showSpinner: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(color = AccentEmerald)
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                color = Color.White
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                    Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
