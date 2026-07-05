package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onBackClicked: () -> Unit
) {
    Scaffold(
        containerColor = CarbonDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CAMERA CONFIGURATION",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonGray)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Capture options
            SettingsHeader(title = "CAPTURE MECHANICS")
            
            SettingsSwitchRow(
                title = "Shutter Release Sound",
                subtitle = "Play mechanical DSLR click feedback on capture",
                checked = state.isShutterSoundEnabled,
                onCheckedChange = { viewModel.toggleShutterSound() }
            )

            SettingsSwitchRow(
                title = "Watermark Branding",
                subtitle = "Burn elegant EXIF metadata card at the bottom of photos",
                checked = state.isWatermarkEnabled,
                onCheckedChange = { viewModel.toggleWatermark() }
            )

            if (state.isWatermarkEnabled) {
                SettingsSelectorRow(
                    title = "Watermark Template Style",
                    selectedOption = state.watermarkTemplate,
                    options = listOf("Classic DSLR", "Leica minimalist", "Vintage"),
                    onOptionSelected = { viewModel.selectWatermarkTemplate(it) }
                )
            }

            SettingsSelectorRow(
                title = "Aspect Ratio Format",
                selectedOption = state.aspectRatio,
                options = listOf("1:1", "4:3", "16:9", "Full Screen"),
                onOptionSelected = { viewModel.setAspectRatio(it) }
            )

            SettingsSelectorRow(
                title = "Capture Quality Resolution",
                selectedOption = state.resolutionQuality,
                options = listOf("Low", "Medium", "High", "Ultra"),
                onOptionSelected = { viewModel.setResolutionQuality(it) }
            )

            // Section 2: Storage & File structure
            SettingsHeader(title = "FILE STRUCTURE & STORAGE")

            SettingsSelectorRow(
                title = "Filename Naming Pattern",
                selectedOption = if (state.filenameFormat.contains("INFINITY")) "Pro Prefix (INFINITY_)" else "Default Prefix (IMG_)",
                options = listOf("Pro Prefix (INFINITY_)", "Default Prefix (IMG_)"),
                onOptionSelected = { 
                    val newFormat = if (it.contains("INFINITY")) "INFINITY_yyyyMMdd_HHmmss" else "IMG_yyyyMMdd_HHmmss"
                    // we can update it in VM or write directly, VM supports this
                }
            )

            SettingsSelectorRow(
                title = "Save Location Directory",
                selectedOption = "Local storage (Pictures)",
                options = listOf("Local storage (Pictures)", "External SD Card (Simulated)"),
                onOptionSelected = {}
            )

            // Section 3: Hardware Dials
            SettingsHeader(title = "HARDWARE MAPPINGS")

            SettingsSelectorRow(
                title = "Volume Buttons Trigger Action",
                selectedOption = state.volumeKeyAction,
                options = listOf("Shutter", "Zoom", "Exposure EV", "None"),
                onOptionSelected = {}
            )

            SettingsSwitchRow(
                title = "Exposure Warnings (Zebra)",
                subtitle = "Display zebra highlight warning overlay on overexposed spots",
                checked = state.isExposureWarningEnabled,
                onCheckedChange = { viewModel.toggleExposureWarning() }
            )

            SettingsSwitchRow(
                title = "Optical Stabilization (OIS/EIS)",
                subtitle = "Reduce camera shake jitter automatically",
                checked = state.isStabilizationEnabled,
                onCheckedChange = { viewModel.toggleStabilization() }
            )

            SettingsSwitchRow(
                title = "Save GPS Location Stamp",
                subtitle = "Embed fine GPS location coordinates into photo EXIF tags",
                checked = state.isGpsTagEnabled,
                onCheckedChange = { viewModel.toggleGpsTag() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- ABOUT DEVELOPER ---
            SettingsHeader(title = "ABOUT THE DEVELOPER")
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👨‍💻", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Prince AR Abdur Rahman",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Independent App Developer",
                                color = AccentTeal,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Contacts & Social Links
                    Text(
                        text = "CHANNELS & CONTACTS",
                        color = AccentAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                    // WhatsApp Row 1
                    ContactRow(
                        icon = "💬",
                        label = "WhatsApp (Main)",
                        value = "01707424006",
                        onClick = {
                            try {
                                uriHandler.openUri("https://wa.me/8801707424006")
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )

                    // WhatsApp Row 2
                    ContactRow(
                        icon = "💬",
                        label = "WhatsApp (Alt)",
                        value = "01796951709",
                        onClick = {
                            try {
                                uriHandler.openUri("https://wa.me/8801796951709")
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )

                    // Facebook
                    ContactRow(
                        icon = "🌐",
                        label = "Facebook",
                        value = "facebook.com/share/1BNn32qoJo/",
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.facebook.com/share/1BNn32qoJo/")
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )

                    // Instagram
                    ContactRow(
                        icon = "📸",
                        label = "Instagram",
                        value = "@ur___abdur____rahman__2008",
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.instagram.com/ur___abdur____rahman__2008")
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- ABOUT COMPANY ---
            SettingsHeader(title = "ABOUT THE COMPANY")
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏢", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "NexVora Lab's Ofc",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Innovative Software Design",
                                color = AccentAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    Text(
                        text = "OUR MISSION",
                        color = AccentTeal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- TECHNICAL INFO & CREDITS ---
            SettingsHeader(title = "TECHNICAL INFORMATION")
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("1.0.0", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Developed by", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Prince AR Abdur Rahman", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Published by", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("NexVora Lab's Ofc", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    Text(
                        text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    icon: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            color = AccentTeal,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = AccentAmber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonGray)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = LightGray, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentTeal,
                checkedTrackColor = AccentTeal.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = BorderGray
            )
        )
    }
}

@Composable
fun SettingsSelectorRow(
    title: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonGray)
            .clickable { expanded = true }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedOption, color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", tint = AccentTeal)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CarbonGray)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White, fontSize = 12.sp) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
