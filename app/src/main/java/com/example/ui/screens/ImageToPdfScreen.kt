package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.image.ImageFilterEngine
import com.example.domain.model.SelectedImageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    selectedImages: List<SelectedImageItem>,
    onAddFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onRotateImageQuick: (String) -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onEditImage: (String) -> Unit,
    onContinueToSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image to PDF (${selectedImages.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("img_pdf_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCaptureFromCamera, modifier = Modifier.testTag("img_pdf_camera_btn")) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Camera Capture")
                    }
                    IconButton(onClick = onAddFromGallery, modifier = Modifier.testTag("img_pdf_gallery_btn")) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add from Gallery")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddFromGallery,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add More")
                    }
                    Button(
                        onClick = onContinueToSettings,
                        enabled = selectedImages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("next_to_pdf_settings_btn")
                    ) {
                        Text("PDF Settings (${selectedImages.size})")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        if (selectedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Select Images or Scan Documents",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Supports JPG, JPEG, PNG, and WEBP. Apply filters, crop, and reorder before building your PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onAddFromGallery,
                            modifier = Modifier.testTag("empty_pick_gallery_btn")
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery")
                        }
                        FilledTonalButton(
                            onClick = onCaptureFromCamera,
                            modifier = Modifier.testTag("empty_open_camera_btn")
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Tap any photo to apply filters, crop, or adjust brightness & contrast. Use arrows to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 158.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(selectedImages, key = { _, item -> item.id }) { idx, item ->
                        SelectedImageCard(
                            index = idx,
                            total = selectedImages.size,
                            item = item,
                            onEdit = { onEditImage(item.id) },
                            onRotate = { onRotateImageQuick(item.id) },
                            onRemove = { onRemoveImage(item.id) },
                            onMovePrev = { if (idx > 0) onMoveImage(idx, idx - 1) },
                            onMoveNext = { if (idx < selectedImages.lastIndex) onMoveImage(idx, idx + 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImageCard(
    index: Int,
    total: Int,
    item: SelectedImageItem,
    onEdit: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.editConfig) {
        val raw = ImageFilterEngine.decodeSampledBitmap(context, item.uri, 480)
        value = if (raw != null) {
            ImageFilterEngine.applyEdits(raw, item.editConfig).also {
                if (it !== raw) raw.recycle()
            }
        } else null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                previewBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Selected page ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.88f))
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Tune, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.editConfig.filter.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMovePrev, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Move earlier", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90°", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = "Edit filters", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveNext, enabled = index < total - 1, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move later", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
