package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.MeasurementEntity
import com.example.data.model.MeasurementMode
import com.example.ui.components.ConfidenceGaugeDial
import com.example.ui.components.ContourMapView
import com.example.ui.components.OsmMapView
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils
import com.example.util.PhotoStorage
import com.example.util.ShareUtils
import com.example.util.buildMeasurementSummaryText
import com.example.util.parseHeightGrid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full detail view of a single saved measurement — photo, result, coverage
 * confidence gauges, "areas not seen by camera" contour map, the scaling
 * method used and where it was taken — the way SR Measure's measurement
 * detail screen lays things out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDetailScreen(
    measurementId: Long,
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val item by viewModel.measurementById(measurementId).collectAsStateWithLifecycle(initialValue = null)
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val measurement = item

    if (showDeleteDialog && measurement != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar medición", fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeasurement(measurement.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("back_button_detail")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "Detalle de medición",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(
                onClick = {
                    measurement ?: return@IconButton
                    val text = buildMeasurementSummaryText(measurement, unitSystem)
                    ShareUtils.shareMeasurement(context, "Medición: ${measurement.title}", text, measurement.photoPath)
                },
                enabled = measurement != null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange.copy(alpha = 0.15f))
                    .testTag("btn_share_detail")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = PrimaryOrange)
            }
        }

        if (measurement == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MeasurementPhoto(measurement)
            MeasurementResultCard(measurement, unitSystem)

            if (measurement.method == "AR_POINT_CLOUD") {
                val surface = measurement.surfaceCoverageConfidence
                val toe = measurement.toeCoverageConfidence
                if (surface != null && toe != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ConfidenceGaugeDial(label = "Toe Coverage Confidence", ratio = toe)
                                ConfidenceGaugeDial(label = "Surface Coverage Confidence", ratio = surface)
                            }

                            val heightGrid = remember(measurement.heightGridJson) { parseHeightGrid(measurement.heightGridJson) }
                            if (heightGrid.isNotEmpty()) {
                                ContourMapView(
                                    heightGrid = heightGrid,
                                    maxHeightMeters = measurement.heightValue,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Método de escala",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(measurement.calibrationLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (measurement.latitude != null && measurement.longitude != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ubicación de la medición",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    OsmMapView(latitude = measurement.latitude, longitude = measurement.longitude)
                    Text(
                        text = "%.5f, %.5f".format(measurement.latitude, measurement.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_delete_detail"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(" Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MeasurementPhoto(item: MeasurementEntity) {
    val bitmap = remember(item.photoPath) {
        item.photoPath?.let { path -> PhotoStorage.decodeThumbnail(path, 1000)?.asImageBitmap() }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    } else {
        val icon = when (item.mode) {
            MeasurementMode.DISTANCE.name -> Icons.Default.Straighten
            MeasurementMode.AREA.name -> Icons.Default.CropSquare
            else -> Icons.Default.ViewInAr
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun MeasurementResultCard(item: MeasurementEntity, unitSystem: com.example.data.model.UnitSystem) {
    val accentColor = when (item.mode) {
        MeasurementMode.DISTANCE.name -> PrimaryOrange
        MeasurementMode.AREA.name -> SecondaryCyan
        else -> AccentEmerald
    }
    val formattedValue = when (item.mode) {
        MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
        MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
        MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
        else -> "${item.value}"
    }
    val formattedDate = remember(item.createdAt) {
        SimpleDateFormat("EEE, dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
            color = accentColor
        )
        if (item.title.isNotBlank()) {
            Text(item.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (item.mode == MeasurementMode.VOLUME.name && item.heightValue > 0) {
            Text(
                "Altura/Profundidad: ${GeometryUtils.formatLength(item.heightValue, unitSystem)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item.tonnage?.let { tonnage ->
            Surface(shape = RoundedCornerShape(10.dp), color = PrimaryOrange.copy(alpha = 0.12f)) {
                Text(
                    text = "≈ ${GeometryUtils.formatTonnage(tonnage, unitSystem)}" + (item.materialType?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryOrange,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
