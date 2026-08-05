package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.CropSquare
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
import com.example.ui.components.CameraManager
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val selectedPlane by viewModel.selectedPlane.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val heightMeters by viewModel.heightMeters.collectAsStateWithLifecycle()
    val scaleFactor by viewModel.scaleFactor.collectAsStateWithLifecycle()
    val calibrationPreset by viewModel.calibrationPreset.collectAsStateWithLifecycle()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsStateWithLifecycle()
    val showSaveDialog by viewModel.showSaveDialog.collectAsStateWithLifecycle()

    var saveTitleInput by remember { mutableStateOf("") }
    var heightSliderValue by remember(heightMeters) { mutableFloatStateOf(heightMeters.toFloat()) }
    var customRefInput by remember { mutableStateOf("1.00") }

    val calculatedValue = viewModel.calculateCurrentValue()
    val currentArea = viewModel.calculateCurrentArea()

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
                                    if (preset == calibrationPreset) PrimaryOrange.copy(alpha = 0.2f)
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
                                color = if (preset == calibrationPreset) PrimaryOrange else Color.White
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
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
                    text = "Guardar medición",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when (mode) {
                            MeasurementMode.DISTANCE -> "Distancia: " + GeometryUtils.formatLength(calculatedValue, unitSystem)
                            MeasurementMode.AREA -> "Área: " + GeometryUtils.formatArea(calculatedValue, unitSystem)
                            MeasurementMode.VOLUME -> "Volumen: " + GeometryUtils.formatVolume(calculatedValue, unitSystem) +
                                    "\n(Área base: ${GeometryUtils.formatArea(currentArea, unitSystem)}, Altura: ${GeometryUtils.formatLength(heightMeters, unitSystem)})"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = when (mode) {
                                MeasurementMode.DISTANCE -> PrimaryOrange
                                MeasurementMode.AREA -> SecondaryCyan
                                MeasurementMode.VOLUME -> AccentEmerald
                            }
                        )
                    )
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Etiqueta / Nota (ej. Tanque de agua)") },
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
                        Toast.makeText(context, "Medición guardada en el historial", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (mode) {
                            MeasurementMode.DISTANCE -> PrimaryOrange
                            MeasurementMode.AREA -> SecondaryCyan
                            MeasurementMode.VOLUME -> AccentEmerald
                        }
                    ),
                    modifier = Modifier.testTag("btn_confirm_save")
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSaveDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 1. Live Camera Feed Layer with Overlay Content
        CameraManager(
            modifier = Modifier.fillMaxSize(),
            overlayContent = {
                // 2. Interactive Measuring Canvas Overlay
                InteractiveMeasuringCanvas(
                    points = points,
                    mode = mode,
                    unitSystem = unitSystem,
                    planeType = selectedPlane,
                    heightMeters = heightMeters,
                    scaleFactor = scaleFactor,
                    onTapCanvas = { offset, canvasWidth, canvasHeight ->
                        val metersX = (offset.x - canvasWidth / 2f) / 180f
                        val metersY = (offset.y - canvasHeight / 2f) / 180f
                        val metersZ = when (selectedPlane) {
                            PlaneType.FLOOR -> 0f
                            PlaneType.TABLE -> 0.75f
                            PlaneType.WALL -> 1.5f
                            PlaneType.AIR -> (points.size * 0.2f)
                        }
                        viewModel.addPoint(Point3D(metersX, metersY, metersZ))
                    }
                )
            }
        )

        // 3. Top Header Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                // Mode Selector Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MeasurementMode.values().forEach { m ->
                        val active = m == mode
                        val pillColor = when (m) {
                            MeasurementMode.DISTANCE -> PrimaryOrange
                            MeasurementMode.AREA -> SecondaryCyan
                            MeasurementMode.VOLUME -> AccentEmerald
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) pillColor else Color.Transparent)
                                .clickable { viewModel.setMode(m) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("mode_${m.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (active) (if (m == MeasurementMode.DISTANCE) Color.White else Color.Black) else Color.LightGray
                            )
                        }
                    }
                }

                // Unit Toggle Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { viewModel.toggleUnitSystem() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("btn_toggle_unit")
                ) {
                    Text(
                        text = if (unitSystem == UnitSystem.METRIC) "m/cm" else "ft/in",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryCyan
                    )
                }
            }

            // Calibration & Plane Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.5f)),
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
                            tint = PrimaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Calibración: ${calibrationPreset.displayName.split(" ")[0]}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. Volume Height / Depth Parameter Controller Card
        if (mode == MeasurementMode.VOLUME) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
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
                                text = "Altura / Profundidad: " + GeometryUtils.formatLength(heightMeters, unitSystem),
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
                    }
                }
            }
        }

        // 5. Dynamic Guidance Hint Banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (mode == MeasurementMode.VOLUME) 210.dp else 140.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = when {
                        points.isEmpty() -> if (mode == MeasurementMode.DISTANCE)
                            "Tocá sobre la cámara en vivo para fijar puntos de medición."
                        else if (mode == MeasurementMode.AREA)
                            "Tocá los vértices de la superficie a delimitar (mínimo 3 puntos)."
                        else
                            "Tocá los vértices de la base del objeto y ajustá la altura arriba para calcular el volumen."
                        points.size < mode.minPoints -> "Agregá más puntos para completar el cálculo de ${mode.label.lowercase()}."
                        else -> "¡Cálculo en vivo! Podés seguir agregando puntos o guardar el resultado."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // 6. Bottom Controls Bar & Live Measurement Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
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
                            MeasurementMode.DISTANCE -> PrimaryOrange
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
                                MeasurementMode.DISTANCE -> "Distancia Total"
                                MeasurementMode.AREA -> "Área Calculada"
                                MeasurementMode.VOLUME -> "Volumen Estimado"
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
                                    MeasurementMode.DISTANCE -> PrimaryOrange
                                    MeasurementMode.AREA -> SecondaryCyan
                                    MeasurementMode.VOLUME -> AccentEmerald
                                }
                            )
                        )

                        if (mode == MeasurementMode.VOLUME) {
                            Text(
                                text = "Área base: ${GeometryUtils.formatArea(currentArea, unitSystem)} | Altura: ${GeometryUtils.formatLength(heightMeters, unitSystem)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Undo Button
                Button(
                    onClick = { viewModel.undoLastPoint() },
                    enabled = points.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_undo"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        disabledContainerColor = Color.White.copy(alpha = 0.06f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Deshacer",
                        tint = if (points.isNotEmpty()) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deshacer", color = if (points.isNotEmpty()) Color.White else Color.Gray)
                }

                // Reset Button
                Button(
                    onClick = { viewModel.resetPoints() },
                    enabled = points.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_reset"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        disabledContainerColor = Color.White.copy(alpha = 0.06f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reiniciar",
                        tint = if (points.isNotEmpty()) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reiniciar", color = if (points.isNotEmpty()) Color.White else Color.Gray)
                }

                // Save Button
                Button(
                    onClick = { viewModel.setShowSaveDialog(true) },
                    enabled = calculatedValue > 0.0,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("btn_save_measurement"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (mode) {
                            MeasurementMode.DISTANCE -> PrimaryOrange
                            MeasurementMode.AREA -> SecondaryCyan
                            MeasurementMode.VOLUME -> AccentEmerald
                        },
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Guardar",
                        tint = if (mode == MeasurementMode.DISTANCE) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guardar",
                        color = if (mode == MeasurementMode.DISTANCE) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive 2D/3D measuring canvas component overlay on live camera.
 */
@Composable
private fun InteractiveMeasuringCanvas(
    points: List<Point3D>,
    mode: MeasurementMode,
    unitSystem: UnitSystem,
    planeType: PlaneType,
    heightMeters: Double,
    scaleFactor: Double,
    onTapCanvas: (Offset, Float, Float) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTapCanvas(offset, size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Grid Overlay Guidelines
        val gridStep = 60f
        val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        var y = 0f
        while (y < height) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = gridPathEffect
            )
            y += gridStep
        }

        var x = 0f
        while (x < width) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f,
                pathEffect = gridPathEffect
            )
            x += gridStep
        }

        // 2. Center Crosshair Reticle
        val reticleColor = when (mode) {
            MeasurementMode.DISTANCE -> PrimaryOrange
            MeasurementMode.AREA -> SecondaryCyan
            MeasurementMode.VOLUME -> AccentEmerald
        }
        drawCircle(
            color = reticleColor.copy(alpha = 0.4f),
            radius = 20f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = reticleColor,
            radius = 3.5f,
            center = Offset(centerX, centerY)
        )

        // 3. Map 3D points to screen offsets
        val screenPoints = points.map { pt ->
            Offset(
                x = centerX + pt.x * 180f,
                y = centerY + pt.y * 180f
            )
        }

        // 4. Draw AREA or VOLUME Polygon Fill
        if ((mode == MeasurementMode.AREA || mode == MeasurementMode.VOLUME) && screenPoints.size >= 3) {
            val polyPath = Path().apply {
                moveTo(screenPoints.first().x, screenPoints.first().y)
                for (i in 1 until screenPoints.size) {
                    lineTo(screenPoints[i].x, screenPoints[i].y)
                }
                close()
            }
            val fillColor = if (mode == MeasurementMode.AREA) SecondaryCyan else AccentEmerald
            drawPath(
                path = polyPath,
                color = fillColor.copy(alpha = 0.25f)
            )

            // If Volume mode, draw 3D extruded prism guidelines to visualize depth/height
            if (mode == MeasurementMode.VOLUME) {
                val heightOffsetPx = (heightMeters * 35.0).toFloat().coerceIn(15f, 150f)
                val topPoints = screenPoints.map { Offset(it.x, it.y - heightOffsetPx) }

                // Top extruded polygon
                val topPolyPath = Path().apply {
                    moveTo(topPoints.first().x, topPoints.first().y)
                    for (i in 1 until topPoints.size) {
                        lineTo(topPoints[i].x, topPoints[i].y)
                    }
                    close()
                }
                drawPath(path = topPolyPath, color = AccentEmerald.copy(alpha = 0.15f))
                drawPath(path = topPolyPath, color = AccentEmerald, style = Stroke(width = 2f))

                // Vertical edges connecting bottom base to top extruded base
                for (i in screenPoints.indices) {
                    drawLine(
                        color = AccentEmerald.copy(alpha = 0.8f),
                        start = screenPoints[i],
                        end = topPoints[i],
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }
        }

        // 5. Draw Connecting Measuring Lines & Length Labels
        if (screenPoints.size >= 2) {
            val isClosedPolygon = (mode == MeasurementMode.AREA || mode == MeasurementMode.VOLUME) && screenPoints.size >= 3
            val lineCount = if (isClosedPolygon) screenPoints.size else screenPoints.size - 1

            for (i in 0 until lineCount) {
                val p1Screen = screenPoints[i]
                val p2Screen = screenPoints[(i + 1) % screenPoints.size]

                val p13d = points[i]
                val p23d = points[(i + 1) % points.size]
                val distMeters = GeometryUtils.distance3D(p13d, p23d) * scaleFactor

                val lineColor = when (mode) {
                    MeasurementMode.DISTANCE -> PrimaryOrange
                    MeasurementMode.AREA -> SecondaryCyan
                    MeasurementMode.VOLUME -> AccentEmerald
                }

                drawLine(
                    color = lineColor,
                    start = p1Screen,
                    end = p2Screen,
                    strokeWidth = 4.5f
                )

                // Segment length label
                val midX = (p1Screen.x + p2Screen.x) / 2f
                val midY = (p1Screen.y + p2Screen.y) / 2f

                val distText = GeometryUtils.formatLength(distMeters, unitSystem)
                val measuredText = textMeasurer.measure(
                    text = distText,
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.8f),
                    topLeft = Offset(midX - measuredText.size.width / 2f - 8f, midY - measuredText.size.height / 2f - 5f),
                    size = androidx.compose.ui.geometry.Size(
                        measuredText.size.width + 16f,
                        measuredText.size.height + 10f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(midX - measuredText.size.width / 2f, midY - measuredText.size.height / 2f)
                )
            }
        }

        // 6. Draw Node Point Markers
        screenPoints.forEachIndexed { index, pt ->
            val nodeColor = when (mode) {
                MeasurementMode.DISTANCE -> PrimaryOrange
                MeasurementMode.AREA -> SecondaryCyan
                MeasurementMode.VOLUME -> AccentEmerald
            }

            drawCircle(
                color = nodeColor.copy(alpha = 0.35f),
                radius = 22f,
                center = pt
            )
            drawCircle(
                color = nodeColor,
                radius = 11f,
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = pt
            )

            val indexText = textMeasurer.measure(
                text = "P${index + 1}",
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            drawText(
                textLayoutResult = indexText,
                topLeft = Offset(pt.x - indexText.size.width / 2f, pt.y - 34f)
            )
        }
    }
}
