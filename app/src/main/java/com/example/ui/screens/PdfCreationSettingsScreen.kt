package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.ImageQualityPreset
import com.example.domain.model.ImageScaleMode
import com.example.domain.model.PageBackgroundPreset
import com.example.domain.model.PageOrientation
import com.example.domain.model.PdfCompressionLevel
import com.example.domain.model.PdfCreationSettings
import com.example.domain.model.PdfPageSize
import com.example.domain.model.SelectedImageItem
import com.example.domain.pdf.PdfEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PdfCreationSettingsScreen(
    selectedImages: List<SelectedImageItem>,
    settings: PdfCreationSettings,
    isGenerating: Boolean,
    generationProgress: Float,
    onUpdateSettings: ((PdfCreationSettings) -> PdfCreationSettings) -> Unit,
    onCreatePdf: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Creation Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isGenerating) {
                        LinearProgressIndicator(
                            progress = { generationProgress.coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                        Text(
                            text = "Building PDF... ${(generationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    previewBitmap = PdfEngine.renderLayoutPreview(context, selectedImages, settings)
                                    showPreviewDialog = true
                                }
                            },
                            enabled = !isGenerating && selectedImages.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_pdf_layout_btn")
                        ) {
                            Icon(Icons.Filled.Preview, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Preview PDF")
                        }
                        Button(
                            onClick = onCreatePdf,
                            enabled = !isGenerating && selectedImages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("create_pdf_submit_btn")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create PDF")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // File Name
            OutlinedTextField(
                value = settings.fileName,
                onValueChange = { name -> onUpdateSettings { it.copy(fileName = name) } },
                label = { Text("PDF File Name") },
                suffix = { Text(".pdf") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_filename_input")
            )

            // Page Size
            SettingsSectionCard(title = "Page Size") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageSize.entries.forEach { size ->
                        FilterChip(
                            selected = settings.pageSize == size,
                            onClick = { onUpdateSettings { it.copy(pageSize = size) } },
                            label = { Text(size.name) }
                        )
                    }
                }
                if (settings.pageSize == PdfPageSize.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = settings.customWidthMm.toInt().toString(),
                            onValueChange = { txt ->
                                txt.toFloatOrNull()?.let { w -> onUpdateSettings { it.copy(customWidthMm = w) } }
                            },
                            label = { Text("Width (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = settings.customHeightMm.toInt().toString(),
                            onValueChange = { txt ->
                                txt.toFloatOrNull()?.let { h -> onUpdateSettings { it.copy(customHeightMm = h) } }
                            },
                            label = { Text("Height (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Orientation & Images per page
            SettingsSectionCard(title = "Orientation & Layout") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageOrientation.entries.forEach { orient ->
                        FilterChip(
                            selected = settings.orientation == orient,
                            onClick = { onUpdateSettings { it.copy(orientation = orient) } },
                            label = { Text(orient.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Images per Page", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 6).forEach { count ->
                        FilterChip(
                            selected = settings.imagesPerPage == count,
                            onClick = { onUpdateSettings { it.copy(imagesPerPage = count) } },
                            label = { Text("$count / page") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Image Fit Mode", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageScaleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.scaleMode == mode,
                            onClick = { onUpdateSettings { it.copy(scaleMode = mode) } },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            // Margins, Spacing, Background
            SettingsSectionCard(title = "Margins, Spacing & Page Color") {
                Text("Page Margins: ${settings.marginDp.toInt()} pt", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.marginDp,
                    onValueChange = { m -> onUpdateSettings { it.copy(marginDp = m) } },
                    valueRange = 0f..48f
                )
                Text("Page Grid Spacing: ${settings.pageSpacingDp.toInt()} pt", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.pageSpacingDp,
                    onValueChange = { s -> onUpdateSettings { it.copy(pageSpacingDp = s) } },
                    valueRange = 0f..32f
                )
                Spacer(Modifier.height(6.dp))
                Text("Background Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageBackgroundPreset.entries.forEach { bg ->
                        FilterChip(
                            selected = settings.backgroundPreset == bg,
                            onClick = { onUpdateSettings { it.copy(backgroundPreset = bg) } },
                            label = { Text(bg.label) }
                        )
                    }
                }
                if (settings.backgroundPreset == PageBackgroundPreset.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    val customSwatches = listOf(0xFFEFF6FF, 0xFFFEE2E2, 0xFFECFDF5, 0xFFFEF3C7, 0xFFF3E8FF)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        customSwatches.forEach { hex ->
                            val argb = hex.toInt()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (settings.customBackgroundColorArgb == argb) 3.dp else 1.dp,
                                        color = if (settings.customBackgroundColorArgb == argb) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateSettings { it.copy(customBackgroundColorArgb = argb) }
                                    }
                            )
                        }
                    }
                }
            }

            // Quality & Compression
            SettingsSectionCard(title = "Image Quality & Compression") {
                Text("Image Quality", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageQualityPreset.entries.forEach { q ->
                        FilterChip(
                            selected = settings.imageQuality == q,
                            onClick = { onUpdateSettings { it.copy(imageQuality = q) } },
                            label = { Text(q.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("PDF Compression", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfCompressionLevel.entries.forEach { comp ->
                        FilterChip(
                            selected = settings.compressionLevel == comp,
                            onClick = { onUpdateSettings { it.copy(compressionLevel = comp) } },
                            label = { Text(comp.label) }
                        )
                    }
                }
            }

            // Password Protection
            SettingsSectionCard(title = "Security") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Password Protection", style = MaterialTheme.typography.titleSmall)
                            Text("Encrypt PDF with 128-bit password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = settings.usePassword,
                        onCheckedChange = { checked -> onUpdateSettings { it.copy(usePassword = checked) } }
                    )
                }
                if (settings.usePassword) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = settings.password,
                        onValueChange = { pwd -> onUpdateSettings { it.copy(password = pwd) } },
                        label = { Text("Enter PDF Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Page 1 Layout Preview") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF layout preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Text("Unable to render preview")
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
