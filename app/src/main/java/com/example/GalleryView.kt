package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryView(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onBackClicked: () -> Unit
) {
    var isFullScreenMode by remember { mutableStateOf(false) }
    var isCompareMode by remember { mutableStateOf(false) }
    var showExifSheet by remember { mutableStateOf(false) }
    
    // Split compare drag offset ratio (0.0 to 1.0)
    var compareDividerRatio by remember { mutableStateOf(0.5f) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CarbonDark,
        topBar = {
            if (!isFullScreenMode) {
                TopAppBar(
                    title = {
                        Text(
                            "MEDIA GALLERY",
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
                    actions = {
                        if (state.galleryItems.isNotEmpty() && state.currentSelectedMediaIndex != -1) {
                            val media = state.galleryItems[state.currentSelectedMediaIndex]
                            if (!media.isVideo) {
                                IconButton(
                                    onClick = { isCompareMode = !isCompareMode },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = if (isCompareMode) AccentTeal else Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Compare, contentDescription = "Compare Before/After")
                                }
                            }
                            IconButton(onClick = { showExifSheet = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Metadata EXIF", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonGray)
                )
            }
        },
        bottomBar = {
            if (!isFullScreenMode && state.galleryItems.isNotEmpty() && state.currentSelectedMediaIndex != -1) {
                val media = state.galleryItems[state.currentSelectedMediaIndex]
                val isFav = state.favoriteItems.contains(media.file.absolutePath)
                
                BottomAppBar(
                    containerColor = CarbonGray,
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(media.file.absolutePath) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) AccentRed else Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.deleteMediaItem(media) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (state.galleryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No captured photos or videos yet", color = LightGray, fontSize = 13.sp)
                }
            }
        } else {
            if (isFullScreenMode) {
                // Pager view for detailed inspect
                val pagerState = rememberPagerState(initialPage = state.currentSelectedMediaIndex.coerceIn(0, state.galleryItems.lastIndex)) {
                    state.galleryItems.size
                }

                // Keep index in viewmodel in sync with swipe pager
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.setSelectedMediaIndex(pagerState.currentPage)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        val media = state.galleryItems[index]
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isFullScreenMode = false },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompareMode && !media.isVideo) {
                                // Before/After split comparison layout
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val newRatio = (compareDividerRatio + dragAmount.x / size.width).coerceIn(0.05f, 0.95f)
                                                compareDividerRatio = newRatio
                                            }
                                        }
                                ) {
                                    // 1. "After" image (Vivid / Filter applied view) on left background
                                    AsyncImage(
                                        model = media.file,
                                        contentDescription = "After profile effect",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )

                                    // 2. "Before" image on right (Simulate flat monochrome or natural original RAW version overlay)
                                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                        val maxWidth = maxWidth
                                        val dividerPx = constraints.maxWidth * compareDividerRatio

                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(maxWidth * (1f - compareDividerRatio))
                                                .align(Alignment.CenterEnd)
                                                .clip(RoundedCornerShape(0.dp))
                                                .background(Color.Black)
                                        ) {
                                            // Render flat gray-scale or default base photo for compare
                                            AsyncImage(
                                                model = media.file,
                                                contentDescription = "Original Raw Profile",
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(maxWidth)
                                                    .align(Alignment.CenterStart),
                                                contentScale = ContentScale.Fit
                                            )
                                        }

                                        // Drag divider bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(2.dp)
                                                .offset(x = maxWidth * compareDividerRatio)
                                                .background(AccentTeal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(AccentTeal, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.DragHandle, contentDescription = "Drag divider", tint = Color.Black, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    // Left/Right side labels
                                    Text(
                                        "FILTER PROFILING",
                                        color = AccentTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                    Text(
                                        "ORIGINAL RAW",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                }
                            } else {
                                // Standard single image or video card
                                AsyncImage(
                                    model = media.file,
                                    contentDescription = "Media detailed view",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                if (media.isVideo) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Video Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating close button
                    IconButton(
                        onClick = { isFullScreenMode = false; isCompareMode = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close detailed view", tint = Color.White)
                    }
                }
            } else {
                // Grid layout for browsing media
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(state.galleryItems) { index, media ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (state.currentSelectedMediaIndex == index) 2.dp else 0.dp,
                                        color = if (state.currentSelectedMediaIndex == index) AccentTeal else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.setSelectedMediaIndex(index)
                                        isFullScreenMode = true
                                    }
                            ) {
                                AsyncImage(
                                    model = media.file,
                                    contentDescription = "Media thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                if (media.isVideo) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video marker",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                    )
                                }

                                if (state.favoriteItems.contains(media.file.absolutePath)) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Favorite",
                                        tint = AccentRed,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Metadata EXIF Bottom Sheet dialog ---
        if (showExifSheet && state.galleryItems.isNotEmpty() && state.currentSelectedMediaIndex != -1) {
            val media = state.galleryItems[state.currentSelectedMediaIndex]
            
            AlertDialog(
                onDismissRequest = { showExifSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsApplications, contentDescription = "EXIF", tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "EXIF METADATA",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExifItemRow(label = "Filename", value = media.file.name)
                        ExifItemRow(label = "File Path", value = media.file.absolutePath)
                        ExifItemRow(label = "Format type", value = if (media.isVideo) "Video (MP4)" else if (media.file.extension == "dng") "RAW (DNG)" else "Photo (JPEG)")
                        ExifItemRow(label = "Date Modified", value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(media.dateTaken))
                        ExifItemRow(label = "Dimensions", value = media.resolution)
                        ExifItemRow(label = "ISO Speed rating", value = "ISO ${media.iso}")
                        ExifItemRow(label = "Aperture value", value = media.aperture)
                        ExifItemRow(label = "Shutter speed", value = media.shutterSpeed)
                        ExifItemRow(label = "Color Profile", value = media.whiteBalance)
                        ExifItemRow(label = "Focal Distance", value = "26 mm equivalent")
                        ExifItemRow(label = "Watermarked footer", value = if (media.hasWatermark) "YES (Classic Template)" else "NO")
                        ExifItemRow(label = "Copyright stamp", value = "© Infinity Camera Pro 2026")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showExifSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Close", color = Color.Black)
                    }
                },
                containerColor = CarbonGray,
                titleContentColor = Color.White
            )
        }
    }
}

@Composable
fun ExifItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
