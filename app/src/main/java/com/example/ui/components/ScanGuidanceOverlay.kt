package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.util.LocalAppStrings
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive onboarding overlay guiding the user through surface scanning,
 * optimal environmental conditions, 3D point cloud generation and calibration.
 */
@Composable
fun ScanGuidanceOverlay(
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var currentStep by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }
    val totalSteps = 4

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks */ }
            .padding(16.dp)
            .testTag("scan_guidance_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.45f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Top title & Step Indicators & Close Button
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = PrimaryAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.scanGuideTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = strings.scanGuideSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDismiss(dontShowAgain) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .testTag("btn_guide_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Progress Indicator Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until totalSteps) {
                            val isActive = i == currentStep
                            val isCompleted = i < currentStep
                            val barColor by animateColorAsState(
                                targetValue = when {
                                    isActive -> PrimaryAmber
                                    isCompleted -> AccentEmerald
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                label = "step_color_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                                    .clickable { currentStep = i }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step Content with Smooth Slide/Fade Transition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "step_content_anim"
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (step) {
                                0 -> GuideStep1SurfaceMapping()
                                1 -> GuideStep2LightingAndTexture()
                                2 -> GuideStep3PointCloudScan()
                                3 -> GuideStep4CalibrationAndPrecision()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer: Bottom actions & Don't show again checkbox
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Checkbox "No volver a mostrar automáticamente"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("chk_dont_show_again")
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryAmber,
                                checkmarkColor = Color.Black,
                                uncheckedColor = Slate400
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.scanGuideDoNotShowAgain,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }

                    // Navigation Buttons (Back / Next / Start)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_guide_prev"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.scanGuidePrev, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            TextButton(
                                onClick = { onDismiss(dontShowAgain) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_guide_skip")
                            ) {
                                Text(strings.scanGuideSkip, color = Slate400)
                            }
                        }

                        if (currentStep < totalSteps - 1) {
                            Button(
                                onClick = { currentStep++ },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("btn_guide_next"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryAmber
                                )
                            ) {
                                Text(
                                    text = strings.scanGuideNext,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onDismiss(dontShowAgain) },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("btn_guide_start_measuring"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentEmerald
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.scanGuideStartMeasuring,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: Surface Mapping & Interactive Scan Simulator
// -------------------------------------------------------------
@Composable
private fun GuideStep1SurfaceMapping() {
    val strings = LocalAppStrings.current
    var touchX by remember { mutableFloatStateOf(0.5f) }
    var scannedPointsCount by remember { mutableIntStateOf(18) }

    val infiniteTransition = rememberInfiniteTransition(label = "phone_sweep_anim")
    val sweepOffset by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep_anim"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = strings.scanGuideStep1Title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryAmber
        )
        Text(
            text = strings.scanGuideStep1Desc,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )

        // Interactive Scan Simulator Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF030712),
            border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            touchX = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                            scannedPointsCount = (scannedPointsCount + 1).coerceAtMost(48)
                        }
                    }
            ) {
                // Interactive Grid & Radar Wave Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val effectiveSweepX = (touchX + sweepOffset * 0.4f).coerceIn(0.1f, 0.9f) * w

                    // Draw floor perspective grid
                    val gridCols = 10
                    val gridRows = 6
                    for (c in 0..gridCols) {
                        val gx = c * (w / gridCols)
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(gx, h * 0.35f),
                            end = Offset(gx, h),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (r in 0..gridRows) {
                        val gy = h * 0.35f + r * (h * 0.65f / gridRows)
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw animated laser scanning beam from top (phone position)
                    val phonePos = Offset(effectiveSweepX, h * 0.18f)
                    val beamWidth = 70.dp.toPx()

                    val beamPath = Path().apply {
                        moveTo(phonePos.x, phonePos.y)
                        lineTo(effectiveSweepX - beamWidth, h)
                        lineTo(effectiveSweepX + beamWidth, h)
                        close()
                    }

                    drawPath(
                        path = beamPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimaryAmber.copy(alpha = 0.45f),
                                PrimaryAmber.copy(alpha = 0.05f)
                            ),
                            startY = phonePos.y,
                            endY = h
                        )
                    )

                    // Draw scanning line on surface
                    drawLine(
                        color = PrimaryAmber,
                        start = Offset(effectiveSweepX - beamWidth, h * 0.85f),
                        end = Offset(effectiveSweepX + beamWidth, h * 0.85f),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Draw detected feature points on the grid
                    val ptCols = 8
                    val ptRows = 4
                    for (i in 0 until ptCols) {
                        for (j in 0 until ptRows) {
                            val px = (i + 0.5f) * (w / ptCols)
                            val py = h * 0.45f + (j + 0.5f) * (h * 0.5f / ptRows)
                            val distToBeam = kotlin.math.abs(px - effectiveSweepX)
                            val isDetected = distToBeam < beamWidth * 1.5f || (i * ptRows + j) < scannedPointsCount

                            if (isDetected) {
                                drawCircle(
                                    color = if (distToBeam < beamWidth) PrimaryAmber else SecondaryCyan,
                                    radius = if (distToBeam < beamWidth) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // Draw phone symbol at phonePos
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(phonePos.x - 16.dp.toPx(), phonePos.y - 12.dp.toPx()),
                        size = Size(32.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = PrimaryAmber,
                        radius = 3.dp.toPx(),
                        center = phonePos
                    )
                }

                // Interactive badge at bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (scannedPointsCount > 24) strings.scanGuideSurfaceDetected else strings.scanGuideSurfaceScanning,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (scannedPointsCount > 24) AccentEmerald else PrimaryAmber
                        )
                    }
                }
            }
        }

        Text(
            text = "👆 ${strings.scanGuideInteractiveHint}",
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -------------------------------------------------------------
// STEP 2: Optimal Lighting and Surface Texture
// -------------------------------------------------------------
@Composable
private fun GuideStep2LightingAndTexture() {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.scanGuideStep2Title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SecondaryCyan
        )
        Text(
            text = strings.scanGuideStep2Desc,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )

        // Recommended conditions card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rule 1: Good Lighting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "☀️ Buena Iluminación",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Espacios bien iluminados permiten al sensor óptico fijar puntos de anclaje estables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                // Rule 2: Textured Surfaces
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🧱 Texturas Naturales y Contrastes",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Pisos de madera, baldosas, paredes con textura o terreno natural ofrecen el mejor seguimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                // Rule 3: Avoid Pure Glass / Mirrors
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = PrimaryAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "⚠️ Evitar Espejos y Vidrios Claros",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Los reflejos confunden el cálculo de profundidad. Enfoca los marcos o bordes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: 3D Point Cloud Scanning (LiDAR & Volume)
// -------------------------------------------------------------
@Composable
private fun GuideStep3PointCloudScan() {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "lidar_cloud_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "orbit_angle"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = strings.scanGuideStep3Title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = AccentEmerald
        )
        Text(
            text = strings.scanGuideStep3Desc,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )

        // 3D Object Reconstruction Demonstration Canvas
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF030712),
            border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rad = Math.toRadians(angle.toDouble())

                // Draw rotating 3D wireframe box
                val boxSize = 55.dp.toPx()
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()

                fun project3D(x: Float, y: Float, z: Float): Offset {
                    val rotX = x * cosA - z * sinA
                    val rotZ = x * sinA + z * cosA + 160f
                    val scale = 200f / rotZ
                    return Offset(cx + rotX * scale, cy + y * scale)
                }

                val p000 = project3D(-boxSize, -boxSize, -boxSize)
                val p100 = project3D(boxSize, -boxSize, -boxSize)
                val p110 = project3D(boxSize, boxSize, -boxSize)
                val p010 = project3D(-boxSize, boxSize, -boxSize)

                val p001 = project3D(-boxSize, -boxSize, boxSize)
                val p101 = project3D(boxSize, -boxSize, boxSize)
                val p111 = project3D(boxSize, boxSize, boxSize)
                val p011 = project3D(-boxSize, boxSize, boxSize)

                fun drawEdge(a: Offset, b: Offset, col: Color) {
                    drawLine(
                        color = col,
                        start = a,
                        end = b,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }

                // Draw box wireframe edges
                drawEdge(p000, p100, AccentEmerald)
                drawEdge(p100, p110, AccentEmerald)
                drawEdge(p110, p010, AccentEmerald)
                drawEdge(p010, p000, AccentEmerald)

                drawEdge(p001, p101, AccentEmerald)
                drawEdge(p101, p111, AccentEmerald)
                drawEdge(p111, p011, AccentEmerald)
                drawEdge(p011, p001, AccentEmerald)

                drawEdge(p000, p001, AccentEmerald)
                drawEdge(p100, p101, AccentEmerald)
                drawEdge(p110, p111, AccentEmerald)
                drawEdge(p010, p011, AccentEmerald)

                // Draw laser point cloud dots inside and on faces
                for (i in -2..2) {
                    for (j in -2..2) {
                        for (k in -2..2) {
                            if (kotlin.math.abs(i) == 2 || kotlin.math.abs(j) == 2 || kotlin.math.abs(k) == 2) {
                                val dotPos = project3D(i * (boxSize / 2f), j * (boxSize / 2f), k * (boxSize / 2f))
                                drawCircle(
                                    color = PrimaryAmber.copy(alpha = 0.85f),
                                    radius = 2.5.dp.toPx(),
                                    center = dotPos
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔄 Orbitar 360°",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentEmerald
            )
            Text(
                text = "Distancia recomendada: 1.0 m – 3.0 m",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Calibration & Millimeter Precision
// -------------------------------------------------------------
@Composable
private fun GuideStep4CalibrationAndPrecision() {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.scanGuideStep4Title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryAmber
        )
        Text(
            text = strings.scanGuideStep4Desc,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calibrate Tool Highlight
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = PrimaryAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Calibración Óptica Instantánea",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Coloca una tarjeta de crédito (85.6 mm) o cinta métrica para autoajustar la escala.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                // Crosshair Alignment Tip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SecondaryCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Retícula Central y Detección de Borde",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Alinea el círculo central con la arista o esquina antes de presionar 'Añadir Punto'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}
