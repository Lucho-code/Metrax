package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.ConfidenceLevel
import com.example.data.model.MeasurementMode
import com.example.data.model.UnitSystem
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils
import com.example.util.PhotoStorage
import com.example.util.ShareUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Roughly covers a 46dp thumbnail up to ~2.6x density; BitmapFactory rounds
// inSampleSize to the nearest power of two anyway, so this only needs to be
// in the right ballpark to avoid decoding full-resolution photos per row.
private const val THUMBNAIL_REQ_SIZE_PX = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val historyItems by viewModel.historyList.collectAsStateWithLifecycle()
    val activeFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    // Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Borrar historial", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que querés borrar todas las mediciones guardadas?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                        Toast.makeText(context, "Historial borrado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar todo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("back_button_history")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Historial",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (historyItems.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("btn_clear_all_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Borrar todo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Borrar todo",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar medición por nombre o superficie...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_history"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todos",
                    "DISTANCE" to "Distancias",
                    "AREA" to "Áreas",
                    "VOLUME" to "Volúmenes"
                ).forEach { (filterKey, label) ->
                    val selected = activeFilter == filterKey
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setHistoryFilter(filterKey) },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_$filterKey")
                    )
                }
            }

            // List or Empty State
            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Todavía no guardaste ninguna medición.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = historyItems,
                        key = { it.id }
                    ) { item ->
                        MeasurementCardItem(
                            item = item,
                            unitSystem = unitSystem,
                            onDelete = { viewModel.deleteMeasurement(item.id) },
                            onCopy = {
                                val textToCopy = buildSummaryText(item, unitSystem)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Medición", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val textToShare = buildSummaryText(item, unitSystem)
                                ShareUtils.shareMeasurement(context, "Medición: ${item.title}", textToShare, item.photoPath)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildSummaryText(item: MeasurementEntity, unitSystem: UnitSystem): String {
    val modeLabel = when (item.mode) {
        MeasurementMode.DISTANCE.name -> "Distancia"
        MeasurementMode.AREA.name -> "Área"
        MeasurementMode.VOLUME.name -> "Volumen"
        else -> item.mode
    }

    val formattedVal = when (item.mode) {
        MeasurementMode.DISTANCE.name -> GeometryUtils.formatLength(item.value, unitSystem)
        MeasurementMode.AREA.name -> GeometryUtils.formatArea(item.value, unitSystem)
        MeasurementMode.VOLUME.name -> GeometryUtils.formatVolume(item.value, unitSystem)
        else -> "${item.value}"
    }

    val extraInfo = if (item.mode == MeasurementMode.VOLUME.name && item.heightValue > 0) {
        "\n• Altura/Profundidad: ${GeometryUtils.formatLength(item.heightValue, unitSystem)}"
    } else ""

    val arInfo = if (item.method == "AR_POINT_CLOUD") {
        val coverage = item.surfaceCoverageConfidence?.let { "${(it * 100).toInt()}%" } ?: "N/D"
        "\n🔬 Método: ARCore Depth + Nube de puntos\n📡 Cobertura de superficie: $coverage"
    } else ""

    val tonnageInfo = item.tonnage?.let {
        "\n⚖️ Peso estimado: ${GeometryUtils.formatTonnage(it, unitSystem)} (${item.materialType ?: ""})"
    } ?: ""

    return "📏 Metraje Instante - $modeLabel\n" +
            "📌 Título: ${item.title}\n" +
            "📊 Resultado: $formattedVal$extraInfo$tonnageInfo\n" +
            "🌐 Superficie: ${item.planeType}$arInfo"
}

/** Combined confidence for a saved AR measurement: the weaker of its two coverage signals. */
private fun overallConfidenceLevel(item: MeasurementEntity): ConfidenceLevel? {
    val surface = item.surfaceCoverageConfidence ?: return null
    val toe = item.toeCoverageConfidence ?: surface
    return ConfidenceLevel.fromRatio(minOf(surface, toe))
}

@Composable
private fun MeasurementCardItem(
    item: MeasurementEntity,
    unitSystem: UnitSystem,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val isDistance = item.mode == MeasurementMode.DISTANCE.name
    val isArea = item.mode == MeasurementMode.AREA.name
    val isVolume = item.mode == MeasurementMode.VOLUME.name

    val accentColor = when {
        isDistance -> PrimaryOrange
        isArea -> SecondaryCyan
        else -> AccentEmerald
    }

    val formattedDate = remember(item.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_history_item_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                val thumbnailBitmap = remember(item.photoPath) {
                    item.photoPath?.let { path -> PhotoStorage.decodeThumbnail(path, THUMBNAIL_REQ_SIZE_PX)?.asImageBitmap() }
                }
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDistance -> Icons.Default.Straighten
                                isArea -> Icons.Default.CropSquare
                                else -> Icons.Default.ViewInAr
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when {
                                isDistance -> "Distancia"
                                isArea -> "Área"
                                else -> "Volumen"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.planeType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = when {
                            isDistance -> GeometryUtils.formatLength(item.value, unitSystem)
                            isArea -> GeometryUtils.formatArea(item.value, unitSystem)
                            else -> GeometryUtils.formatVolume(item.value, unitSystem)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isVolume && item.heightValue > 0) {
                        Text(
                            text = "Altura/Profundidad: ${GeometryUtils.formatLength(item.heightValue, unitSystem)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentEmerald
                        )
                    }

                    item.tonnage?.let { tonnage ->
                        Text(
                            text = "≈ ${GeometryUtils.formatTonnage(tonnage, unitSystem)}" +
                                (item.materialType?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryOrange
                        )
                    }

                    if (item.method == "AR_POINT_CLOUD") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentEmerald.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "AR · Nube de puntos",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            overallConfidenceLevel(item)?.let { level ->
                                val levelColor = when (level) {
                                    ConfidenceLevel.HIGH -> AccentEmerald
                                    ConfidenceLevel.MEDIUM -> PrimaryOrange
                                    ConfidenceLevel.LOW -> AccentRose
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = levelColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "Confianza: ${level.displayName}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = levelColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (item.title.isNotBlank()) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
