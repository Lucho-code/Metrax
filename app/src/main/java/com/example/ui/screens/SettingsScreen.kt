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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var hapticsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(false) }
    var highQualityMode by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
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
                    .clickable { }
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
                            "LR",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lucio Rostagno",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "luciorostagno@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Slate400
                    )
                }
            }
            
            SettingsCategoryTitle("MEASUREMENT")
            
            SettingsItem(
                title = "Unit System",
                subtitle = "Metric (m, cm)",
                icon = Icons.Default.Straighten,
                onClick = { }
            )
            
            SettingsCategoryTitle("AR EXPERIENCE")

            SettingsSwitchItem(
                title = "Show Tracking Grid",
                subtitle = "Display dot grid on detected surfaces",
                icon = Icons.Default.GridOn,
                checked = showGrid,
                onCheckedChange = { showGrid = it }
            )
            
            SettingsSwitchItem(
                title = "High Quality Mode",
                subtitle = "Uses more battery but improves tracking accuracy",
                icon = Icons.Default.HighQuality,
                checked = highQualityMode,
                onCheckedChange = { highQualityMode = it }
            )

            SettingsCategoryTitle("PREFERENCES")

            SettingsSwitchItem(
                title = "Haptic Feedback",
                subtitle = "Vibrate when placing points",
                icon = Icons.Default.Vibration,
                checked = hapticsEnabled,
                onCheckedChange = { hapticsEnabled = it }
            )

            SettingsSwitchItem(
                title = "Measurement Sounds",
                subtitle = "Play sound when point is placed",
                icon = Icons.Default.VolumeUp,
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )
            
            SettingsCategoryTitle("ACCOUNT")

            SettingsItem(
                title = "Manage Subscription",
                subtitle = "Business Plan Active",
                icon = Icons.Default.CreditCard,
                onClick = { }
            )

            SettingsItem(
                title = "Sign Out",
                subtitle = null,
                icon = Icons.Default.Logout,
                iconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = { }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
