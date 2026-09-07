package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MeasurementMode
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.MeasurementViewModel
import com.example.util.GeometryUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detail view of one [com.example.data.db.PileEntity]: its measurement history over time and quick re-scan actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PileDetailScreen(
    pileId: Long,
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    onMeasureManual: () -> Unit,
    onMeasureAr: () -> Unit
) {
    val pile by viewModel.pileById(pileId).collectAsStateWithLifecycle(initialValue = null)
    val measurements by viewModel.measurementsForPile(pileId).collectAsStateWithLifecycle(initialValue = emptyList())
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar acopio", fontWeight = FontWeight.Bold) },
            text = { Text("Se borra el acopio; sus mediciones quedan en el historial general.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePile(pileId)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 36.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).testTag("back_button_pile_detail")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = pile?.name ?: "Acopio",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.testTag("btn_delete_pile")) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar acopio", tint = MaterialTheme.colorScheme.error)
            }
        }

        pile?.materialType?.let {
            Text("Material por defecto: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            text = "Volvé a medir este acopio para ver su evolución (consumo o reposición) a lo largo del tiempo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    viewModel.setMode(MeasurementMode.VOLUME)
                    viewModel.setActivePile(pileId)
                    onMeasureManual()
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("btn_pile_measure_manual"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Text(" Manual", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    viewModel.setActivePile(pileId)
                    onMeasureAr()
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("btn_pile_measure_ar"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan)
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Text(" AR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Historial de este acopio (${measurements.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (measurements.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no mediste este acopio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = measurements, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_pile_measurement_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    GeometryUtils.formatVolume(item.value, unitSystem),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentEmerald
                                )
                                Text(
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item.tonnage?.let {
                                Text(
                                    "≈ ${GeometryUtils.formatTonnage(it, unitSystem)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryOrange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
