package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.image.ImageFilterEngine
import com.example.domain.model.ImageEditConfig
import com.example.domain.model.ImageFilterType
import com.example.domain.model.NormalizedCropRect
import com.example.domain.model.SelectedImageItem
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageItem: SelectedImageItem,
    onSaveConfig: (ImageEditConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var draftConfig by remember(imageItem.id) { mutableStateOf(imageItem.editConfig) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Filters, 1=Adjust, 2=Crop & Rotate

    val baseBitmap by produceState<Bitmap?>(initialValue = null, imageItem.uri) {
        value = ImageFilterEngine.decodeSampledBitmap(context, imageItem.uri, 1080)
    }

    val livePreview by produceState<Bitmap?>(
        initialValue = null,
        baseBitmap,
        draftConfig.filter,
        draftConfig.brightness,
        draftConfig.contrast,
        draftConfig.saturation,
        draftConfig.sharpness,
        draftConfig.rotationDegrees,
        if (selectedTab == 2) NormalizedCropRect() else draftConfig.cropRect
    ) {
        val src = baseBitmap ?: return@produceState
        val configForPreview = if (selectedTab == 2) {
            // Show uncropped image in Crop tab so user can drag crop box freely
            draftConfig.copy(cropRect = NormalizedCropRect())
        } else {
            draftConfig
        }
        value = ImageFilterEngine.applyEdits(src, configForPreview)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { draftConfig = ImageEditConfig() }) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            onSaveConfig(draftConfig)
                            onBack()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("apply_image_edits_btn")
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                livePreview?.let { bmp ->
                    val aspect = (bmp.width.toFloat() / bmp.height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)
                    Box(
                        modifier = Modifier
                            .aspectRatio(aspect)
                            .fillMaxSize()
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Live edited image preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (selectedTab == 2) {
                            CropOverlayBox(
                                cropRect = draftConfig.cropRect,
                                onCropChange = { newRect ->
                                    draftConfig = draftConfig.copy(cropRect = newRect)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Tool Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Filters") },
                    icon = { Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Adjust") },
                    icon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Crop & Rotate") },
                    icon = { Icon(Icons.Filled.Crop, null, modifier = Modifier.size(18.dp)) }
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        LazyRow(
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(ImageFilterType.entries) { filter ->
                                val isSelected = draftConfig.filter == filter
                                val thumbBmp by produceState<Bitmap?>(initialValue = null, baseBitmap, filter) {
                                    val src = baseBitmap ?: return@produceState
                                    val small = Bitmap.createScaledBitmap(src, 140, (140f * src.height / src.width).toInt().coerceAtLeast(80), true)
                                    value = ImageFilterEngine.applyEdits(small, ImageEditConfig(filter = filter))
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { draftConfig = draftConfig.copy(filter = filter) }
                                        .testTag("filter_${filter.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(82.dp, 102.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        thumbBmp?.let { tb ->
                                            Image(
                                                bitmap = tb.asImageBitmap(),
                                                contentDescription = filter.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = filter.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AdjustmentSliderRow(
                                label = "Brightness",
                                value = draftConfig.brightness,
                                valueRange = -0.6f..0.6f,
                                displayValue = "${(draftConfig.brightness * 100).toInt()}",
                                onValueChange = { draftConfig = draftConfig.copy(brightness = it) }
                            )
                            AdjustmentSliderRow(
                                label = "Contrast",
                                value = draftConfig.contrast,
                                valueRange = 0.5f..2.0f,
                                displayValue = String.format("%.2fx", draftConfig.contrast),
                                onValueChange = { draftConfig = draftConfig.copy(contrast = it) }
                            )
                            AdjustmentSliderRow(
                                label = "Saturation",
                                value = draftConfig.saturation,
                                valueRange = 0f..2.0f,
                                displayValue = "${(draftConfig.saturation * 100).toInt()}%",
                                onValueChange = { draftConfig = draftConfig.copy(saturation = it) }
                            )
                            AdjustmentSliderRow(
                                label = "Sharpness",
                                value = draftConfig.sharpness,
                                valueRange = 0f..1.0f,
                                displayValue = "${(draftConfig.sharpness * 100).toInt()}%",
                                onValueChange = { draftConfig = draftConfig.copy(sharpness = it) }
                            )
                        }
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Drag corners on image to crop. Choose quick aspect or rotate:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = draftConfig.cropRect.isFull,
                                    onClick = { draftConfig = draftConfig.copy(cropRect = NormalizedCropRect()) },
                                    label = { Text("Full") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { draftConfig = draftConfig.copy(cropRect = NormalizedCropRect(0.1f, 0.1f, 0.9f, 0.9f)) },
                                    label = { Text("Trim 10%") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { draftConfig = draftConfig.copy(cropRect = NormalizedCropRect(0.15f, 0.2f, 0.85f, 0.8f)) },
                                    label = { Text("Center") }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val next = (draftConfig.rotationDegrees + 270) % 360
                                        draftConfig = draftConfig.copy(rotationDegrees = next)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateLeft, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rotate Left")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val next = (draftConfig.rotationDegrees + 90) % 360
                                        draftConfig = draftConfig.copy(rotationDegrees = next)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rotate Right")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(displayValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun CropOverlayBox(
    cropRect: NormalizedCropRect,
    onCropChange: (NormalizedCropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableIntStateOf(-1) } // 0=TL, 1=TR, 2=BL, 3=BR
    Box(
        modifier = modifier.pointerInput(cropRect) {
            detectDragGestures(
                onDragStart = { offset ->
                    val nx = offset.x / size.width
                    val ny = offset.y / size.height
                    val corners = listOf(
                        cropRect.left to cropRect.top,
                        cropRect.right to cropRect.top,
                        cropRect.left to cropRect.bottom,
                        cropRect.right to cropRect.bottom
                    )
                    activeHandle = corners.indices.minByOrNull { i ->
                        abs(corners[i].first - nx) + abs(corners[i].second - ny)
                    } ?: -1
                },
                onDragEnd = { activeHandle = -1 },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / size.width
                    val dy = dragAmount.y / size.height
                    val minGap = 0.15f
                    val next = when (activeHandle) {
                        0 -> cropRect.copy(
                            left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minGap),
                            top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minGap)
                        )
                        1 -> cropRect.copy(
                            right = (cropRect.right + dx).coerceIn(cropRect.left + minGap, 1f),
                            top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minGap)
                        )
                        2 -> cropRect.copy(
                            left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minGap),
                            bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minGap, 1f)
                        )
                        3 -> cropRect.copy(
                            right = (cropRect.right + dx).coerceIn(cropRect.left + minGap, 1f),
                            bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minGap, 1f)
                        )
                        else -> cropRect
                    }
                    onCropChange(next)
                }
            )
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val l = cropRect.left * size.width
            val t = cropRect.top * size.height
            val r = cropRect.right * size.width
            val b = cropRect.bottom * size.height

            // Dim outside region
            drawRect(Color.Black.copy(alpha = 0.5f), Offset.Zero, Size(size.width, t))
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, b), Size(size.width, size.height - b))
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, t), Size(l, b - t))
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(r, t), Size(size.width - r, b - t))

            // Crop frame
            drawRect(
                color = Color.White,
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(width = 4f)
            )
            // Corner handles
            val handleR = 14f
            listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { pt ->
                drawCircle(color = Color(0xFFDC2626), radius = handleR, center = pt)
                drawCircle(color = Color.White, radius = handleR, center = pt, style = Stroke(3f))
            }
        }
    }
}
