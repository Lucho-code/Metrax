package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UnitSystem
import com.example.data.model.User
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MeasurementViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MeasurementViewModel? = null,
    authViewModel: AuthViewModel? = null,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var hapticsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(false) }
    var highQualityMode by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }

    val currentUser by (authViewModel?.currentUser ?: MutableStateFlow<User?>(null)).collectAsStateWithLifecycle()
    val unitSystem by (viewModel?.unitSystem ?: MutableStateFlow(UnitSystem.METRIC)).collectAsStateWithLifecycle()

    val initials = remember(currentUser) {
        val name = currentUser?.name?.trim().orEmpty()
        if (name.isBlank()) "IN" else name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ajustes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // Profile Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(PrimaryAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name ?: "Invitado",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = currentUser?.email ?: "Sin cuenta iniciada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    }
                }
            }

            SettingsCategoryTitle("MEDICIÓN")

            SettingsItem(
                title = "Sistema de Unidades",
                subtitle = if (unitSystem == UnitSystem.METRIC) "Métrico (m, cm)" else "Imperial (ft, in)",
                icon = Icons.Default.Straighten,
                onClick = { viewModel?.toggleUnitSystem() },
                modifier = Modifier.testTag("settings_toggle_unit_system")
            )

            SettingsCategoryTitle("EXPERIENCIA AR")

            SettingsSwitchItem(
                title = "Mostrar Grilla de Rastreo",
                subtitle = "Muestra la grilla de puntos sobre superficies detectadas",
                icon = Icons.Default.GridOn,
                checked = showGrid,
                onCheckedChange = { showGrid = it }
            )

            SettingsSwitchItem(
                title = "Modo Alta Calidad",
                subtitle = "Usa más batería pero mejora la precisión de rastreo",
                icon = Icons.Default.HighQuality,
                checked = highQualityMode,
                onCheckedChange = { highQualityMode = it }
            )

            SettingsCategoryTitle("PREFERENCIAS")

            SettingsSwitchItem(
                title = "Vibración",
                subtitle = "Vibrar al colocar puntos",
                icon = Icons.Default.Vibration,
                checked = hapticsEnabled,
                onCheckedChange = { hapticsEnabled = it }
            )

            SettingsSwitchItem(
                title = "Sonidos de Medición",
                subtitle = "Reproducir sonido al colocar un punto",
                icon = Icons.Default.VolumeUp,
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )

            SettingsCategoryTitle("CUENTA")

            SettingsItem(
                title = "Cerrar Sesión",
                subtitle = if (currentUser != null) "Salir de tu cuenta actual" else null,
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    authViewModel?.logout()
                    onLogout()
                },
                modifier = Modifier.testTag("settings_sign_out")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Metrax App v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = PrimaryAmber,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    iconColor: Color = Slate400,
    textColor: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = PrimaryAmber,
                uncheckedThumbColor = Slate400,
                uncheckedTrackColor = DarkSurface
            )
        )
    }
}
