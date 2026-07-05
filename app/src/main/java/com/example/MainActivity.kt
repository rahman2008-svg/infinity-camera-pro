package com.example

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.LightGray
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppHost() {
    val context = LocalContext.current
    val viewModel: CameraViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    // Manage camera, microphone, and location permissions cleanly using Accompanist
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    if (permissionsState.allPermissionsGranted) {
        // Render Main DSLR App Navigation Controller
        val navController = rememberNavController()
        
        NavHost(
            navController = navController,
            startDestination = "viewfinder",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("viewfinder") {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // High-end Viewfinder preview
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                CameraViewfinder(
                                    state = state,
                                    viewModel = viewModel,
                                    onSettingsClicked = { navController.navigate("settings") },
                                    onViewfinderTapped = { x, y ->
                                        viewModel.tapToFocus(x, y)
                                    }
                                )

                                // Real-time Floating RGB and Luminance Histogram Overlay
                                RealtimeHistogramBox(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    state = state
                                )

                                // Recording Indicator
                                if (state.isRecordingVideo) {
                                    RecordingIndicator(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .statusBarsPadding()
                                            .padding(top = 70.dp, start = 16.dp),
                                        durationSeconds = state.recordedDurationSeconds
                                    )
                                }
                            }

                            // Tactile Physical DSLR Wheels & Control dials
                            DslrControls(
                                state = state,
                                viewModel = viewModel,
                                onShutterClicked = {
                                    if (state.currentMode == CameraMode.VIDEO) {
                                        if (state.isRecordingVideo) {
                                            viewModel.stopVideoRecording(context) { file ->
                                                Toast.makeText(context, "Video saved: ${file.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            viewModel.startVideoRecording(context) {
                                                Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        viewModel.capturePhoto(context) { file ->
                                            Toast.makeText(context, "Photo captured: ${file.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onGalleryClicked = {
                                    navController.navigate("gallery")
                                },
                                onSettingsClicked = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                }
            }

            composable("gallery") {
                GalleryView(
                    state = state,
                    viewModel = viewModel,
                    onBackClicked = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBackClicked = {
                        navController.popBackStack()
                    }
                )
            }
        }
    } else {
        // Onboarding Screen to cleanly prompt permissions with professional DSLR illustrations
        OnboardingPermissionScreen(
            onRequestPermissions = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    }
}

@Composable
fun OnboardingPermissionScreen(onRequestPermissions: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Camera Onboarding",
                tint = AccentTeal,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "INFINITY CAMERA PRO",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "To unlock high-end manual controls, raw photo capturing, video stabilization, and metadata tagging, please authorize camera and hardware permissions.",
                color = LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_permissions_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = "Grant")
                Spacer(modifier = Modifier.width(8.dp))
                Text("ACTIVATE DSLR MODE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RecordingIndicator(modifier: Modifier = Modifier, durationSeconds: Int) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.Red, shape = CircleShape)
        )
        Text(
            text = "REC $timeStr",
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
