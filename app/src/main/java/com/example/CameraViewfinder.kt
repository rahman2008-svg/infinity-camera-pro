package com.example

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import androidx.camera.core.ImageCapture
import java.nio.ByteBuffer

@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    viewModel: CameraViewModel,
    onSettingsClicked: () -> Unit,
    onViewfinderTapped: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onViewfinderTapped(offset.x, offset.y)
                }
            }
            .testTag("camera_viewfinder_container")
    ) {
        // --- Live Camera preview or simulated viewfinder ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                }
            },
            update = { previewView ->
                val cameraProvider = try {
                    cameraProviderFuture.get()
                } catch (e: Exception) {
                    null
                }

                if (cameraProvider != null) {
                    cameraProvider.unbindAll()

                    val cameraSelector = if (state.isLensBack) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        val buffer = imageProxy.planes[0].buffer
                        viewModel.updateLiveHistogram(buffer, imageProxy.width, imageProxy.height)
                        
                        // Fake facial / smile markers for visual depth
                        if (state.isFaceDetectionEnabled) {
                            viewModel.updateSmartFeatures(
                                faceCount = 1,
                                smileDetected = true,
                                blinkDetected = false,
                                bounds = android.graphics.Rect(350, 400, 650, 700)
                            )
                        } else {
                            viewModel.updateSmartFeatures(0, false, false, null)
                        }
                        
                        imageProxy.close()
                    }

                    try {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )

        // Fallback or simulated overlays when viewfinder runs inside emulator/no-camera context
        if (state.liveHistogram.sum() == 0) {
            // Seed a gentle simulated histogram update periodically if camera analysis is silent
            LaunchedEffect(Unit) {
                val dummyBuffer = ByteBuffer.allocate(1)
                viewModel.updateLiveHistogram(dummyBuffer, 1, 1)
            }
        }

        // --- Custom Composition Grid Overlays ---
        if (state.currentGridStyle != GridStyle.OFF) {
            CompositionGrid(gridStyle = state.currentGridStyle)
        }

        // --- Live Horizon & Level Indicator ---
        HorizonLevelIndicator(pitch = state.levelPitch, roll = state.levelRoll)

        // --- Simulated Focus Peaking Highlight ---
        if (state.isFocusPeakingEnabled) {
            FocusPeakingOverlay()
        }

        // --- Autofocus Tap Ring ---
        state.focusTapPoint?.let { point ->
            AutofocusRing(x = point.first, y = point.second, isLocked = state.isFocusLocked)
        }

        // --- Detected Face Box ---
        if (state.isFaceDetectionEnabled && state.faceBoundingBox != null) {
            FaceOverlay(bounds = state.faceBoundingBox, blinkWarning = state.blinkWarningDetected)
        }

        // --- QR Scanner Guide Overlay ---
        if (state.currentMode == CameraMode.QR_SCANNER) {
            QrScannerOverlay(viewModel = viewModel, state = state)
        }

        // --- Live EXIF HUD Strip ---
        ExifHudStrip(state = state, onSettingsClicked = onSettingsClicked)

        // --- Astro / Night Simulation celestial stars (Only visible in Astrophotography preset) ---
        if (state.currentMode == CameraMode.NIGHT && state.selectedIso == "3200" && state.selectedShutterSpeed == "30s") {
            AstrophotographyStarsOverlay()
        }
    }
}

@Composable
fun CompositionGrid(gridStyle: GridStyle) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val gridColor = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        when (gridStyle) {
            GridStyle.THIRD_OF_THIRDS -> {
                // Vertical lines
                drawLine(gridColor, Offset(width / 3f, 0f), Offset(width / 3f, height), strokeWidth)
                drawLine(gridColor, Offset(2 * width / 3f, 0f), Offset(2 * width / 3f, height), strokeWidth)
                // Horizontal lines
                drawLine(gridColor, Offset(0f, height / 3f), Offset(width, height / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, 2 * height / 3f), Offset(width, 2 * height / 3f), strokeWidth)
            }
            GridStyle.GOLDEN_RATIO -> {
                // Golden ratio lines (approx 0.382 and 0.618)
                val phi1 = width * 0.382f
                val phi2 = width * 0.618f
                val hPhi1 = height * 0.382f
                val hPhi2 = height * 0.618f

                drawLine(gridColor, Offset(phi1, 0f), Offset(phi1, height), strokeWidth)
                drawLine(gridColor, Offset(phi2, 0f), Offset(phi2, height), strokeWidth)
                drawLine(gridColor, Offset(0f, hPhi1), Offset(width, hPhi1), strokeWidth)
                drawLine(gridColor, Offset(0f, hPhi2), Offset(width, hPhi2), strokeWidth)
            }
            GridStyle.SQUARE -> {
                // Center box
                val sizeBox = Math.min(width, height) * 0.6f
                val left = (width - sizeBox) / 2f
                val top = (height - sizeBox) / 2f
                drawRect(
                    color = gridColor,
                    topLeft = Offset(left, top),
                    size = Size(sizeBox, sizeBox),
                    style = Stroke(strokeWidth)
                )
            }
            GridStyle.CROSSHAIR -> {
                // Single central fine crosshair
                val cx = width / 2f
                val cy = height / 2f
                drawLine(gridColor, Offset(cx - 30.dp.toPx(), cy), Offset(cx + 30.dp.toPx(), cy), strokeWidth * 1.5f)
                drawLine(gridColor, Offset(cx, cy - 30.dp.toPx()), Offset(cx, cy + 30.dp.toPx()), strokeWidth * 1.5f)
            }
            else -> {}
        }
    }
}

@Composable
fun HorizonLevelIndicator(pitch: Float, roll: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        // Let's draw a professional aviation-style level bar in the center
        // If the roll is within 1 degree, make it green to indicate perfect level!
        val isLeveled = Math.abs(roll) < 1.0f && Math.abs(pitch) < 1.0f
        val levelColor = if (isLeveled) Color(0xFF00E676) else AccentAmber.copy(alpha = 0.8f)
        val strokePx = 2.dp.toPx()

        // Center cross indicator
        drawCircle(
            color = levelColor,
            radius = 6.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(strokePx)
        )
        drawCircle(
            color = levelColor,
            radius = 1.dp.toPx(),
            center = Offset(cx, cy)
        )

        // Floating level line
        val lineLength = 120.dp.toPx()
        val tiltOffset = (roll / 90f) * 150.dp.toPx()
        val pitchOffset = (pitch / 90f) * 150.dp.toPx()

        // Draw horizontal tilting bar
        val startX = cx - lineLength / 2f
        val endX = cx + lineLength / 2f
        val currentY = cy + pitchOffset

        // Rotate the floating line about center
        val radians = Math.toRadians(roll.toDouble())
        val cos = Math.cos(radians).toFloat()
        val sin = Math.sin(radians).toFloat()

        val p1x = cx - (lineLength / 2f) * cos
        val p1y = (cy + pitchOffset) - (lineLength / 2f) * sin
        val p2x = cx + (lineLength / 2f) * cos
        val p2y = (cy + pitchOffset) + (lineLength / 2f) * sin

        drawLine(
            color = levelColor,
            start = Offset(p1x, p1y),
            end = Offset(p2x, p2y),
            strokeWidth = strokePx
        )

        // Draw steady static benchmark tick bars on left/right edges
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(cx - 100.dp.toPx(), cy),
            end = Offset(cx - 70.dp.toPx(), cy),
            strokeWidth = strokePx
        )
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(cx + 70.dp.toPx(), cy),
            end = Offset(cx + 100.dp.toPx(), cy),
            strokeWidth = strokePx
        )
    }
}

@Composable
fun FocusPeakingOverlay() {
    // Renders a high-tech sparkling green boundary mesh simulating pro peaking
    val infiniteTransition = rememberInfiniteTransition(label = "peaking")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "peakingAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val pxColor = Color(0xFF00E5FF).copy(alpha = alphaAnim) // Neon cyan focus contours
        
        // Draw decorative high contrast sparkling edges in the center region
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw sharp details around the central autofocus area
        val path = Path().apply {
            moveTo(centerX - 120.dp.toPx(), centerY - 80.dp.toPx())
            lineTo(centerX - 90.dp.toPx(), centerY - 100.dp.toPx())
            lineTo(centerX - 60.dp.toPx(), centerY - 70.dp.toPx())
            
            moveTo(centerX + 60.dp.toPx(), centerY + 70.dp.toPx())
            lineTo(centerX + 100.dp.toPx(), centerY + 60.dp.toPx())
            lineTo(centerX + 120.dp.toPx(), centerY + 90.dp.toPx())
        }

        drawPath(
            path = path,
            color = pxColor,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun AutofocusRing(x: Float, y: Float, isLocked: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus_tap")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "focusScale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val ringColor = if (isLocked) AccentTeal else AccentAmber
        val radius = 32.dp.toPx() * scaleAnim

        // Draw main target circle
        drawCircle(
            color = ringColor,
            radius = radius,
            center = Offset(x, y),
            style = Stroke(2.dp.toPx())
        )

        // Draw fine corner ticks
        val tick = 6.dp.toPx()
        drawLine(ringColor, Offset(x - radius, y), Offset(x - radius + tick, y), 2.dp.toPx())
        drawLine(ringColor, Offset(x + radius, y), Offset(x + radius - tick, y), 2.dp.toPx())
        drawLine(ringColor, Offset(x, y - radius), Offset(x, y - radius + tick), 2.dp.toPx())
        drawLine(ringColor, Offset(x, y + radius), Offset(x, y + radius - tick), 2.dp.toPx())
    }
}

@Composable
fun FaceOverlay(bounds: android.graphics.Rect, blinkWarning: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameColor = if (blinkWarning) AccentRed else AccentTeal.copy(alpha = 0.8f)
        val strokePx = 1.5.dp.toPx()

        // Draw a neat modern bounding corner box instead of a solid rectangle
        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()
        val len = 20.dp.toPx()

        // Top Left corner
        drawLine(frameColor, Offset(left, top), Offset(left + len, top), strokePx)
        drawLine(frameColor, Offset(left, top), Offset(left, top + len), strokePx)

        // Top Right corner
        drawLine(frameColor, Offset(right, top), Offset(right - len, top), strokePx)
        drawLine(frameColor, Offset(right, top), Offset(right, top + len), strokePx)

        // Bottom Left corner
        drawLine(frameColor, Offset(left, bottom), Offset(left + len, bottom), strokePx)
        drawLine(frameColor, Offset(left, bottom), Offset(left, bottom - len), strokePx)

        // Bottom Right corner
        drawLine(frameColor, Offset(right, bottom), Offset(right - len, bottom), strokePx)
        drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - len), strokePx)

        // Text tag for smart face tracker
        val tagText = if (blinkWarning) "BLINK DETECTED!" else "FACE TRACK [AE/AF]"
    }

    // Floating text label
    Box(
        modifier = Modifier
            .offset(x = (bounds.left / 2).dp, y = (bounds.top / 2 - 25).dp)
            .background(if (blinkWarning) AccentRed else AccentTeal, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (blinkWarning) "BLINK WARNING 👁" else "SMART TRACKING",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QrScannerOverlay(viewModel: CameraViewModel, state: CameraUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semitransparent darkened outer frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeBox = Math.min(size.width, size.height) * 0.55f
            val left = (size.width - sizeBox) / 2f
            val top = (size.height - sizeBox) / 2f

            // Darken outside scanner frame
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )
            // Punch a clear hole in the center
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(sizeBox, sizeBox),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            // Border highlights
            drawRect(
                color = AccentTeal,
                topLeft = Offset(left, top),
                size = Size(sizeBox, sizeBox),
                style = Stroke(2.dp.toPx())
            )
        }

        // Beautiful laser scanning animation line
        val infiniteTransition = rememberInfiniteTransition(label = "laser")
        val laserOffsetY by infiniteTransition.animateFloat(
            initialValue = -0.27f,
            targetValue = 0.27f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "laserOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(2.dp)
                .offset(y = (laserOffsetY * 300).dp)
                .background(AccentTeal)
                .shadow(elevation = 6.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(Color.Black.copy(alpha = 0.8f), shape = CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "QR Scanner",
                tint = AccentTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Align QR Code inside the box",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // QR result dialog if detected
        state.scannedQrResult?.let { result ->
            AlertDialog(
                onDismissRequest = { viewModel.clearScannedQr() },
                title = { Text("Scanned Code Detected") },
                text = { Text(result, color = Color.White) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearScannedQr() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Dismiss", color = Color.Black)
                    }
                },
                containerColor = CarbonGray,
                titleContentColor = Color.White
            )
        }
    }
}

@Composable
fun ExifHudStrip(state: CameraUiState, onSettingsClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side quick telemetry cells: FLASH, HDR, RATIO, and exposure specs
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FLASH",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = when (state.flashMode) {
                        ImageCapture.FLASH_MODE_ON -> "ON"
                        ImageCapture.FLASH_MODE_AUTO -> "AUTO"
                        else -> "OFF"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "HDR",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (state.currentMode == CameraMode.HDR) "HDR+" else "OFF",
                    color = if (state.currentMode == CameraMode.HDR) AccentAmber else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RATIO",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = state.aspectRatio,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ISO",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = state.selectedIso,
                    color = if (state.selectedIso == "Auto") Color.White else AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SHUTTER",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = state.selectedShutterSpeed,
                    color = if (state.selectedShutterSpeed == "Auto") Color.White else AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EV",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${if (state.exposureCompensation >= 0) "+" else ""}${state.exposureCompensation}",
                    color = if (state.exposureCompensation == 0f) Color.White else AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Right side: Locks indicators and settings gear button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (state.isFocusLocked) {
                    HudBadge(text = "AF-L", color = AccentAmber)
                }
                if (state.isAeLocked) {
                    HudBadge(text = "AE-L", color = AccentTeal)
                }
                if (state.isAwbLocked) {
                    HudBadge(text = "AWB-L", color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                    .clickable { onSettingsClicked() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙️",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun HudBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = CircleShape)
            .border(width = 0.5.dp, color = color.copy(alpha = 0.5f), shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AstrophotographyStarsOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "starsAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = listOf(
            Offset(size.width * 0.15f, size.height * 0.2f),
            Offset(size.width * 0.45f, size.height * 0.15f),
            Offset(size.width * 0.8f, size.height * 0.35f),
            Offset(size.width * 0.25f, size.height * 0.6f),
            Offset(size.width * 0.72f, size.height * 0.18f),
            Offset(size.width * 0.88f, size.height * 0.52f)
        )

        for (pt in points) {
            drawCircle(
                color = Color.White.copy(alpha = alphaAnim),
                radius = 1.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = AccentTeal.copy(alpha = alphaAnim * 0.3f),
                radius = 6.dp.toPx(),
                center = pt
            )
        }

        // Draw a elegant simulated crescent moon preset representation
        drawCircle(
            color = Color.Yellow.copy(alpha = 0.8f),
            radius = 18.dp.toPx(),
            center = Offset(size.width * 0.65f, size.height * 0.25f)
        )
        drawCircle(
            color = Color.Black,
            radius = 18.dp.toPx(),
            center = Offset(size.width * 0.61f, size.height * 0.23f)
        )
    }
}
