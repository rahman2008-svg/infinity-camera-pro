package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun DslrControls(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onShutterClicked: () -> Unit,
    onGalleryClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    var expandedControl by remember { mutableStateOf<String?>(null) } // "zoom", "ev", "wb", "iso", "shutter", "profile", "presets"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonDark.copy(alpha = 0.95f))
            .padding(bottom = 16.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Quick Info HUD or Expanded Adjustment Slider ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (expandedControl == null) {
                // Quick info / preset row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickButton(
                        label = "ZOOM ${state.zoomLevel}x",
                        icon = Icons.Default.ZoomIn,
                        active = state.zoomLevel > 1.0f,
                        onClick = { expandedControl = "zoom" }
                    )
                    QuickButton(
                        label = "EV ${state.exposureCompensation}",
                        icon = Icons.Default.Exposure,
                        active = state.exposureCompensation != 0f,
                        onClick = { expandedControl = "ev" }
                    )
                    QuickButton(
                        label = state.selectedColorProfile.name,
                        icon = Icons.Default.Palette,
                        active = state.selectedColorProfile != ColorProfile.STANDARD,
                        onClick = { expandedControl = "profile" }
                    )
                    QuickButton(
                        label = "PRESETS",
                        icon = Icons.Default.Stars,
                        active = false,
                        onClick = { expandedControl = "presets" }
                    )
                    QuickButton(
                        label = "GRID",
                        icon = Icons.Default.GridOn,
                        active = state.currentGridStyle != GridStyle.OFF,
                        onClick = { viewModel.toggleGridStyle() }
                    )
                }
            } else {
                // Expanded adjuster drawer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { expandedControl = null },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        when (expandedControl) {
                            "zoom" -> ZoomSlider(state = state, viewModel = viewModel)
                            "ev" -> EvSlider(state = state, viewModel = viewModel)
                            "profile" -> ColorProfileSelector(state = state, viewModel = viewModel)
                            "presets" -> CustomPresetsRow(viewModel = viewModel, onClose = { expandedControl = null })
                            "iso" -> WheelSelector(
                                title = "ISO SPEED",
                                currentValue = state.selectedIso,
                                values = listOf("Auto", "50", "100", "200", "400", "800", "1600", "3200", "6400"),
                                onValueChanged = { viewModel.selectIso(it) }
                            )
                            "shutter" -> WheelSelector(
                                title = "SHUTTER SPEED",
                                currentValue = state.selectedShutterSpeed,
                                values = listOf("Auto", "1/4000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1s", "5s", "10s", "30s"),
                                onValueChanged = { viewModel.selectShutterSpeed(it) }
                            )
                            "wb" -> WhiteBalanceSelector(state = state, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. Pro Mode Manual Dial Anchors ---
        if (state.currentMode == CameraMode.PRO_MODE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(CarbonGray, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProDialAnchor(
                    label = "ISO",
                    value = state.selectedIso,
                    active = expandedControl == "iso",
                    onClick = { expandedControl = if (expandedControl == "iso") null else "iso" }
                )
                ProDialAnchor(
                    label = "SHUTTER",
                    value = state.selectedShutterSpeed,
                    active = expandedControl == "shutter",
                    onClick = { expandedControl = if (expandedControl == "shutter") null else "shutter" }
                )
                ProDialAnchor(
                    label = "WB",
                    value = state.selectedWbPreset.displayName,
                    active = expandedControl == "wb",
                    onClick = { expandedControl = if (expandedControl == "wb") null else "wb" }
                )
                ProDialAnchor(
                    label = "FOCUS",
                    value = if (state.isManualFocus) "${(state.manualFocusDistance * 100).toInt()}cm" else "Auto",
                    active = state.isManualFocus,
                    onClick = {
                        val nextDist = if (state.isManualFocus) 0.0f else 0.5f
                        viewModel.setManualFocusDistance(nextDist)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. Camera Mode Sliding Selector Strip ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            state = rememberLazyListState(initialFirstVisibleItemIndex = CameraMode.PHOTO.ordinal),
            contentPadding = PaddingValues(horizontal = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(CameraMode.values()) { mode ->
                val isSelected = state.currentMode == mode
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 0.9f, label = "modeScale")
                
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setCameraMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mode.icon,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) AccentTeal else LightGray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Main Trigger Panel (Gallery, Shutter Button, Switch Camera, Settings) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Thumbnail View
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BorderGray)
                    .clickable { onGalleryClicked() }
                    .testTag("gallery_thumbnail"),
                contentAlignment = Alignment.Center
            ) {
                if (state.galleryItems.isNotEmpty()) {
                    AsyncImage(
                        model = state.galleryItems.first().file,
                        contentDescription = "Gallery",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Shutter click trigger
            ShutterTriggerButton(
                mode = state.currentMode,
                isRecording = state.isRecordingVideo,
                countdown = state.activeTimerCountdown,
                onClick = onShutterClicked
            )

            // Camera Lens Toggle
            IconButton(
                onClick = { viewModel.toggleLens() },
                modifier = Modifier
                    .size(54.dp)
                    .background(BorderGray, shape = CircleShape)
                    .testTag("lens_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Lens Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Settings Screen trigger
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier
                    .size(54.dp)
                    .background(BorderGray, shape = CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ShutterTriggerButton(
    mode: CameraMode,
    isRecording: Boolean,
    countdown: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(width = 4.dp, color = Color.White, shape = CircleShape)
            .clickable { onClick() }
            .padding(6.dp)
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        if (countdown > 0) {
            // Display active timer countdown
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AccentAmber, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdown.toString(),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            val color = if (mode == CameraMode.VIDEO || isRecording) AccentRed else Color.White
            val shape = if (isRecording) RoundedCornerShape(12.dp) else CircleShape

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = shape)
            )
        }
    }
}

@Composable
fun ProDialAnchor(
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(if (active) AccentTeal.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (active) AccentTeal else Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = if (active) AccentTeal else AccentAmber,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun QuickButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) AccentTeal else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (active) AccentTeal else LightGray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ZoomSlider(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("1x", color = Color.White, fontSize = 11.sp)
        Slider(
            value = state.zoomLevel,
            onValueChange = { viewModel.setZoomLevel(it) },
            valueRange = 1.0f..state.maxZoom,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AccentTeal,
                activeTrackColor = AccentTeal,
                inactiveTrackColor = BorderGray
            )
        )
        Text("${state.maxZoom.toInt()}x", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun EvSlider(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("-3 EV", color = Color.White, fontSize = 11.sp)
        Slider(
            value = state.exposureCompensation,
            onValueChange = { viewModel.setExposureCompensation(it) },
            valueRange = -3.0f..3.0f,
            steps = 20,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AccentAmber,
                activeTrackColor = AccentAmber,
                inactiveTrackColor = BorderGray
            )
        )
        Text("+3 EV", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun ColorProfileSelector(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorProfile.values().forEach { profile ->
            val isSelected = state.selectedColorProfile == profile
            Button(
                onClick = { viewModel.setColorProfile(profile) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) AccentTeal else BorderGray,
                    contentColor = if (isSelected) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(profile.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomPresetsRow(viewModel: CameraViewModel, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                viewModel.applyMoonPreset()
                onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = BorderGray, contentColor = Color.White),
            modifier = Modifier.height(32.dp)
        ) {
            Text("🌙 MOON PRESET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                viewModel.applyAstroPreset()
                onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = BorderGray, contentColor = Color.White),
            modifier = Modifier.height(32.dp)
        ) {
            Text("🌌 ASTROPHOTOGRAPHY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WhiteBalanceSelector(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WhiteBalancePreset.values().forEach { preset ->
            val isSelected = state.selectedWbPreset == preset
            Button(
                onClick = { viewModel.selectWbPreset(preset) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) AccentAmber else BorderGray,
                    contentColor = if (isSelected) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(preset.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WheelSelector(
    title: String,
    currentValue: String,
    values: List<String>,
    onValueChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = AccentAmber,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            values.forEach { valStr ->
                val isSelected = currentValue == valStr
                Text(
                    text = valStr,
                    color = if (isSelected) AccentAmber else Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onValueChanged(valStr) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- Live RGB and Luminance Histogram Overlay (Floating Composable) ---
@Composable
fun RealtimeHistogramBox(
    modifier: Modifier = Modifier,
    state: CameraUiState
) {
    Box(
        modifier = modifier
            .size(110.dp, 64.dp)
            .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render Luminance Histogram
            val maxVal = (state.liveHistogram.maxOrNull() ?: 1).coerceAtLeast(1)
            val path = Path()
            path.moveTo(0f, height)
            
            val stepX = width / 256f
            for (i in 0..255) {
                val ratio = state.liveHistogram[i].toFloat() / maxVal
                val x = i * stepX
                val y = height - (ratio * height * 0.85f)
                path.lineTo(x, y)
            }
            path.lineTo(width, height)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f))
                )
            )

            // Render RGB Profile lines
            val rMax = (state.liveRgbHistogram.first.maxOrNull() ?: 1).coerceAtLeast(1)
            val gMax = (state.liveRgbHistogram.second.maxOrNull() ?: 1).coerceAtLeast(1)
            val bMax = (state.liveRgbHistogram.third.maxOrNull() ?: 1).coerceAtLeast(1)

            val rPath = Path().apply { moveTo(0f, height) }
            val gPath = Path().apply { moveTo(0f, height) }
            val bPath = Path().apply { moveTo(0f, height) }

            for (i in 0..255) {
                rPath.lineTo(i * stepX, height - (state.liveRgbHistogram.first[i].toFloat() / rMax * height * 0.7f))
                gPath.lineTo(i * stepX, height - (state.liveRgbHistogram.second[i].toFloat() / gMax * height * 0.7f))
                bPath.lineTo(i * stepX, height - (state.liveRgbHistogram.third[i].toFloat() / bMax * height * 0.7f))
            }

            drawPath(rPath, Color.Red.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            drawPath(gPath, Color.Green.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            drawPath(bPath, Color.Blue.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
        }
        
        Text(
            text = "LIVE HISTOGRAM",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
