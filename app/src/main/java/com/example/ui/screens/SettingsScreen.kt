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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(false) }
    var highQualityMode by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = strings.selectLanguage,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguageManager.setLanguage(context, lang)
                                    showLanguageDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryAmber.copy(alpha = 0.15f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryAmber else DarkBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = lang.flagEmoji,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Column {
                                        Text(
                                            text = lang.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.SPANISH) "Español (Predeterminado)"
                                            else if (lang == AppLanguage.ENGLISH) "English (US / UK)"
                                            else "Português (Brasil)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seleccionado",
                                        tint = PrimaryAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.close, color = PrimaryAmber, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
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
                
                SettingsCategoryTitle(strings.languageSetting.uppercase())
                
                SettingsItem(
                    title = strings.languageSetting,
                    subtitle = "${currentLanguage.flagEmoji} ${currentLanguage.displayName}",
                    icon = Icons.Default.Language,
                    iconColor = SecondaryCyan,
                    onClick = { showLanguageDialog = true }
                )

                SettingsCategoryTitle("MEASUREMENT")
                
                SettingsItem(
                    title = strings.unitSystem,
                    subtitle = strings.unitSystemMetric,
                    icon = Icons.Default.Straighten,
                    onClick = { }
                )
                
                SettingsCategoryTitle("AR EXPERIENCE")

                SettingsSwitchItem(
                    title = strings.showTrackingGrid,
                    subtitle = strings.showTrackingGridDesc,
                    icon = Icons.Default.GridOn,
                    checked = showGrid,
                    onCheckedChange = { showGrid = it }
                )
                
                SettingsSwitchItem(
                    title = strings.highQualityMode,
                    subtitle = strings.highQualityModeDesc,
                    icon = Icons.Default.HighQuality,
                    checked = highQualityMode,
                    onCheckedChange = { highQualityMode = it }
                )

                SettingsCategoryTitle("PREFERENCES")

                SettingsSwitchItem(
                    title = strings.hapticFeedback,
                    subtitle = strings.hapticFeedbackDesc,
                    icon = Icons.Default.Vibration,
                    checked = hapticsEnabled,
                    onCheckedChange = { hapticsEnabled = it }
                )

                SettingsSwitchItem(
                    title = strings.measurementSounds,
                    subtitle = strings.measurementSoundsDesc,
                    icon = Icons.Default.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
                
                SettingsCategoryTitle("ACCOUNT")

                SettingsItem(
                    title = strings.manageSubscription,
                    subtitle = strings.manageSubscriptionSubtitle,
                    icon = Icons.Default.CreditCard,
                    onClick = { }
                )

                SettingsItem(
                    title = strings.signOut,
                    subtitle = null,
                    icon = Icons.Default.Logout,
                    iconColor = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onNavigateBack
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = strings.appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
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
