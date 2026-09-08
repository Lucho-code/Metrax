package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoundingBox3D
import com.example.data.model.PointCloudColorMap
import com.example.data.model.PointCloudPoint
import com.example.data.model.UnitSystem
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SecondaryCyan
import com.example.util.GeometryUtils
import com.example.util.LocalAppStrings
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class ProjectedPoint(
    val point: PointCloudPoint,
    val screenX: Float,
    val screenY: Float,
    val depthZ: Float
)

@Composable
fun PointCloudViewer3D(
    points: List<PointCloudPoint>,
    boundingBox: BoundingBox3D?,
    colorMap: PointCloudColorMap,
    onColorMapChange: (PointCloudColorMap) -> Unit,
    unitSystem: UnitSystem,
    scaleFactor: Double,
    onCloseViewer: () -> Unit,
    onExportPly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val textMeasurer = rememberTextMeasurer()

    var rotX by remember { mutableFloatStateOf(18f) }
    var rotY by remember { mutableFloatStateOf(35f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var autoRotate by remember { mutableStateOf(true) }
    var showColorMapMenu by remember { mutableStateOf(false) }
    var showWireframe by remember { mutableStateOf(true) }

    var selectedPoint1 by remember { mutableStateOf<PointCloudPoint?>(null) }
    var selectedPoint2 by remember { mutableStateOf<PointCloudPoint?>(null) }

    // Auto-spin animation
    LaunchedEffect(autoRotate) {
        while (autoRotate) {
            kotlinx.coroutines.delay(20)
            rotY = (rotY + 0.35f) % 360f
        }
    }

    val centroid = remember(points) {
        if (points.isEmpty()) Offset(0f, 0f) to 0f
        else {
            var sumX = 0f
            var sumY = 0f
            var sumZ = 0f
            for (p in points) {
                sumX += p.x
                sumY += p.y
                sumZ += p.z
            }
            val count = points.size.toFloat()
            Offset(sumX / count, sumY / count) to (sumZ / count)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
    ) {
        // 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        autoRotate = false
                        rotY += pan.x * 0.4f
                        rotX = (rotX - pan.y * 0.4f).coerceIn(-85f, 85f)
                        zoom = (zoom * gestureZoom).coerceIn(0.4f, 4.0f)
                    }
                }
                .testTag("canvas_point_cloud_3d")
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val focal = size.width * 0.85f * zoom

            val radX = Math.toRadians(rotX.toDouble()).toFloat()
            val radY = Math.toRadians(rotY.toDouble()).toFloat()

            val cosX = cos(radX)
            val sinX = sin(radX)
            val cosY = cos(radY)
            val sinY = sin(radY)

            val centerX = centroid.first.x
            val centerY = centroid.first.y
            val centerZ = centroid.second

            // Helper to project a 3D coordinate to screen
            fun project3D(x: Float, y: Float, z: Float): Offset? {
                val dx = x - centerX
                val dy = y - centerY
                val dz = z - centerZ

                // Yaw (around Y)
                val rx = dx * cosY - dz * sinY
                val rz = dx * sinY + dz * cosY

                // Pitch (around X)
                val ry = dy * cosX - rz * sinX
                val rz2 = dy * sinX + rz * cosX

                val camDist = 2.2f
                val depthZ = rz2 + camDist
                if (depthZ <= 0.1f) return null

                val sx = cx + (rx / depthZ) * focal
                val sy = cy - (ry / depthZ) * focal
                return Offset(sx, sy)
            }

            // 1. Draw 3D Perspective Ground Grid Floor
            val gridRadius = 1.2f
            val gridSteps = 6
            val floorY = (boundingBox?.minY ?: -0.5f)
            val stepSize = (gridRadius * 2f) / gridSteps

            for (i in 0..gridSteps) {
                val lineCoord = -gridRadius + i * stepSize
                val p1 = project3D(lineCoord, floorY, -gridRadius)
                val p2 = project3D(lineCoord, floorY, gridRadius)
                if (p1 != null && p2 != null) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val q1 = project3D(-gridRadius, floorY, lineCoord)
                val q2 = project3D(gridRadius, floorY, lineCoord)
                if (q1 != null && q2 != null) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        start = q1,
                        end = q2,
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw Coordinate Axes Origin (X=Red, Y=Green, Z=Blue)
            val origin = project3D(centerX, floorY, centerZ)
            val axisX = project3D(centerX + 0.35f, floorY, centerZ)
            val axisY = project3D(centerX, floorY + 0.35f, centerZ)
            val axisZ = project3D(centerX, floorY, centerZ + 0.35f)

            if (origin != null) {
                if (axisX != null) drawLine(Color(0xFFEF4444), origin, axisX, strokeWidth = 2.5.dp.toPx())
                if (axisY != null) drawLine(Color(0xFF10B981), origin, axisY, strokeWidth = 2.5.dp.toPx())
                if (axisZ != null) drawLine(Color(0xFF3B82F6), origin, axisZ, strokeWidth = 2.5.dp.toPx())
            }

            // 2. Project and Depth-Sort Points
            val projectedPoints = ArrayList<ProjectedPoint>(points.size)
            for (p in points) {
                val dx = p.x - centerX
                val dy = p.y - centerY
                val dz = p.z - centerZ

                val rx = dx * cosY - dz * sinY
                val rz = dx * sinY + dz * cosY

                val ry = dy * cosX - rz * sinX
                val rz2 = dy * sinX + rz * cosX

                val depthZ = rz2 + 2.2f
                if (depthZ > 0.1f) {
                    val sx = cx + (rx / depthZ) * focal
                    val sy = cy - (ry / depthZ) * focal
                    projectedPoints.add(ProjectedPoint(p, sx, sy, depthZ))
                }
            }

            // Sort points by depth (farthest first)
            projectedPoints.sortByDescending { it.depthZ }

            // 3. Render 3D Point Cloud with Glowing Halo and LiDAR Depth Gradient
            for (pt in projectedPoints) {
                val baseColor = GeometryUtils.getColorForPoint(pt.point, colorMap)
                val pointRadius = (2.2f / pt.depthZ * zoom).coerceIn(1.5f, 6.5f).dp.toPx()

                // Subtle glowing halo
                drawCircle(
                    color = baseColor.copy(alpha = 0.25f),
                    radius = pointRadius * 2.2f,
                    center = Offset(pt.screenX, pt.screenY)
                )
                // Crisp laser core
                drawCircle(
                    color = baseColor,
                    radius = pointRadius,
                    center = Offset(pt.screenX, pt.screenY)
                )
            }

            // 4. Draw 3D Bounding Box Wireframe with Live Dimension Labels
            if (showWireframe && boundingBox != null) {
                val b = boundingBox
                // 8 corners
                val c000 = project3D(b.minX, b.minY, b.minZ)
                val c100 = project3D(b.maxX, b.minY, b.minZ)
                val c110 = project3D(b.maxX, b.maxY, b.minZ)
                val c010 = project3D(b.minX, b.maxY, b.minZ)

                val c001 = project3D(b.minX, b.minY, b.maxZ)
                val c101 = project3D(b.maxX, b.minY, b.maxZ)
                val c111 = project3D(b.maxX, b.maxY, b.maxZ)
                val c011 = project3D(b.minX, b.maxY, b.maxZ)

                fun drawBoxEdge(p1: Offset?, p2: Offset?, color: Color = SecondaryCyan) {
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = color.copy(alpha = 0.85f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }
                }

                // Bottom face
                drawBoxEdge(c000, c100)
                drawBoxEdge(c100, c101)
                drawBoxEdge(c101, c001)
                drawBoxEdge(c001, c000)

                // Top face
                drawBoxEdge(c010, c110)
                drawBoxEdge(c110, c111)
                drawBoxEdge(c111, c011)
                drawBoxEdge(c011, c010)

                // Vertical pillars
                drawBoxEdge(c000, c010)
                drawBoxEdge(c100, c110)
                drawBoxEdge(c101, c111)
                drawBoxEdge(c001, c011)

                // Corner highlight markers
                listOfNotNull(c000, c100, c110, c010, c001, c101, c111, c011).forEach { corner ->
                    drawCircle(PrimaryAmber, radius = 3.dp.toPx(), center = corner)
                }

                // Dimension Badges on Wireframe
                if (c000 != null && c100 != null) {
                    val widthMeters = b.width.toDouble() * scaleFactor
                    drawDimensionBadge(
                        textMeasurer = textMeasurer,
                        label = "X: ${GeometryUtils.formatLength(widthMeters, unitSystem)}",
                        p1 = c000,
                        p2 = c100,
                        accent = SecondaryCyan
                    )
                }
                if (c100 != null && c110 != null) {
                    val heightMeters = b.height.toDouble() * scaleFactor
                    drawDimensionBadge(
                        textMeasurer = textMeasurer,
                        label = "Y: ${GeometryUtils.formatLength(heightMeters, unitSystem)}",
                        p1 = c100,
                        p2 = c110,
                        accent = AccentEmerald
                    )
                }
                if (c100 != null && c101 != null) {
                    val depthMeters = b.depth.toDouble() * scaleFactor
                    drawDimensionBadge(
                        textMeasurer = textMeasurer,
                        label = "Z: ${GeometryUtils.formatLength(depthMeters, unitSystem)}",
                        p1 = c100,
                        p2 = c101,
                        accent = PrimaryAmber
                    )
                }
            }

            // 5. Point-to-Point 3D measurement in Viewer
            val p1 = selectedPoint1
            val p2 = selectedPoint2
            if (p1 != null && p2 != null) {
                val sp1 = project3D(p1.x, p1.y, p1.z)
                val sp2 = project3D(p2.x, p2.y, p2.z)
                if (sp1 != null && sp2 != null) {
                    drawLine(
                        color = Color.White,
                        start = sp1,
                        end = sp2,
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(PrimaryAmber, radius = 6.dp.toPx(), center = sp1)
                    drawCircle(PrimaryAmber, radius = 6.dp.toPx(), center = sp2)

                    val dist = GeometryUtils.distance3D(p1, p2) * scaleFactor
                    drawDimensionBadge(
                        textMeasurer = textMeasurer,
                        label = "📏 ${GeometryUtils.formatLength(dist, unitSystem)}",
                        p1 = sp1,
                        p2 = sp2,
                        accent = PrimaryAmber
                    )
                }
            }
        }

        // Top Navigation & Stats Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to AR Camera Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.clickable { onCloseViewer() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Cámara AR",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Vista AR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            // Point Cloud Badge Count
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Text(
                        text = "${points.size} pts LiDAR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentEmerald
                    )
                }
            }

            // Right Quick Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Auto-rotate toggle
                IconButton(
                    onClick = { autoRotate = !autoRotate },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (autoRotate) PrimaryAmber else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Girar",
                        tint = if (autoRotate) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Palette dropdown toggle
                IconButton(
                    onClick = { showColorMapMenu = !showColorMapMenu },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Paleta",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Color Palette Selector Overlay Card
        AnimatedVisibility(
            visible = showColorMapMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Shader LiDAR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    PointCloudColorMap.entries.forEach { map ->
                        val isSel = map == colorMap
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (isSel) BorderStroke(1.dp, PrimaryAmber) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorMapChange(map)
                                    showColorMapMenu = false
                                }
                        ) {
                            Text(
                                text = map.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) PrimaryAmber else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom 3D Dimensions & Volume Floating HUD
        boundingBox?.let { box ->
            val volM3 = box.volume.toDouble() * scaleFactor * scaleFactor * scaleFactor
            val widthM = box.width.toDouble() * scaleFactor
            val heightM = box.height.toDouble() * scaleFactor
            val depthM = box.depth.toDouble() * scaleFactor

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DIMENSIONES 3D ESCANEADAS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = PrimaryAmber
                            )
                            Text(
                                text = GeometryUtils.formatVolume(volM3, unitSystem),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                        }

                        // Export PLY Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AccentEmerald),
                            modifier = Modifier.clickable { onExportPly() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Exportar",
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "PLY / OBJ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dimension axes breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DimensionChip(
                            label = "Ancho (X)",
                            value = GeometryUtils.formatLength(widthM, unitSystem),
                            color = SecondaryCyan,
                            modifier = Modifier.weight(1f)
                        )
                        DimensionChip(
                            label = "Alto (Y)",
                            value = GeometryUtils.formatLength(heightM, unitSystem),
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        DimensionChip(
                            label = "Prof. (Z)",
                            value = GeometryUtils.formatLength(depthM, unitSystem),
                            color = PrimaryAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Arrastra con un dedo para rotar 360° • Pellizca con dos dedos para zoom",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.LightGray.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

private fun DrawScope.drawDimensionBadge(
    textMeasurer: TextMeasurer,
    label: String,
    p1: Offset,
    p2: Offset,
    accent: Color
) {
    val midX = (p1.x + p2.x) / 2f
    val midY = (p1.y + p2.y) / 2f - 14.dp.toPx()

    val textResult = textMeasurer.measure(
        text = label,
        style = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    )
    val pad = 6.dp.toPx()
    val bw = textResult.size.width + pad * 2
    val bh = textResult.size.height + pad

    drawRoundRect(
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        topLeft = Offset(midX - bw / 2f, midY - bh / 2f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = accent,
        topLeft = Offset(midX - bw / 2f, midY - bh / 2f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(midX - textResult.size.width / 2f, midY - textResult.size.height / 2f)
    )
}
