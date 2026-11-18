package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.PdfCompressionLevel
import com.example.domain.model.PdfToolType
import com.example.ui.components.PdfThumbnailBox
import com.example.ui.util.formatFileSize
import com.example.ui.viewmodel.ToolsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolWorkspaceScreen(
    toolType: PdfToolType,
    allDocuments: List<PdfDocumentEntity>,
    toolsViewModel: ToolsViewModel,
    onImportExternalPdf: ((PdfDocumentEntity) -> Unit) -> Unit,
    onOpenPdf: (PdfDocumentEntity) -> Unit,
    onSharePdf: (PdfDocumentEntity) -> Unit,
    onShareExportedImages: (List<File>) -> Unit,
    onBack: () -> Unit
) {
    val isProcessing by toolsViewModel.isProcessing.collectAsStateWithLifecycle()
    val progress by toolsViewModel.progress.collectAsStateWithLifecycle()
    val resultMessage by toolsViewModel.resultMessage.collectAsStateWithLifecycle()
    val pageThumbnails by toolsViewModel.pageThumbnails.collectAsStateWithLifecycle()
    val exportedImages by toolsViewModel.exportedImages.collectAsStateWithLifecycle()

    // Merge state
    val mergeSelection = remember { mutableStateListOf<PdfDocumentEntity>() }

    // Single-doc tool state
    var selectedDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var outputName by remember(selectedDoc, toolType) {
        mutableStateOf("${selectedDoc?.name ?: "SmartPDF"}_${toolType.name.lowercase()}")
    }

    // Split state
    var splitEveryPage by remember { mutableStateOf(false) }
    var splitRanges by remember { mutableStateOf("1-1, 2-3") }

    // Compress state
    var compressionLevel by remember { mutableStateOf(PdfCompressionLevel.MEDIUM) }

    // Extract / Delete state
    val selectedPageIndices = remember { mutableStateListOf<Int>() }

    // Rotate state
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }

    // Reorder state
    val pageOrder = remember { mutableStateListOf<Int>() }

    // PDF to Images state
    var exportAsPng by remember { mutableStateOf(true) }

    LaunchedEffect(selectedDoc) {
        toolsViewModel.clearExportedImages()
        selectedPageIndices.clear()
        pageRotations.clear()
        pageOrder.clear()
        selectedDoc?.let { doc ->
            toolsViewModel.loadPageThumbnails(doc)
        }
    }

    LaunchedEffect(pageThumbnails.size) {
        if (pageThumbnails.isNotEmpty() && pageOrder.isEmpty()) {
            pageOrder.addAll(0 until pageThumbnails.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(toolType.title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tool_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            onImportExternalPdf { imported ->
                                if (toolType == PdfToolType.MERGE) {
                                    mergeSelection.add(imported)
                                } else {
                                    selectedDoc = imported
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.FileOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick PDF")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (isProcessing) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            resultMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (exportedImages.isNotEmpty()) {
                            Button(onClick = { onShareExportedImages(exportedImages) }) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share Images")
                            }
                        }
                    }
                }
            }

            if (toolType == PdfToolType.SHARE_PDF) {
                Text(
                    text = "Select a PDF to share immediately",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DocumentPickerList(
                    documents = allDocuments,
                    selectedDoc = null,
                    onSelect = { onSharePdf(it) },
                    modifier = Modifier.weight(1f)
                )
                return@Scaffold
            }

            if (toolType == PdfToolType.MERGE) {
                Text(
                    text = "Select 2 or more PDFs in merge order (${mergeSelection.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Merged Output Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(allDocuments, key = { _, d -> d.id }) { _, doc ->
                        val selectedIdx = mergeSelection.indexOfFirst { it.id == doc.id }
                        val isChecked = selectedIdx >= 0
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isChecked) mergeSelection.removeAll { it.id == doc.id }
                                    else mergeSelection.add(doc)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) mergeSelection.add(doc)
                                        else mergeSelection.removeAll { it.id == doc.id }
                                    }
                                )
                                PdfThumbnailBox(
                                    thumbnailPath = doc.thumbnailPath,
                                    isPasswordProtected = doc.isPasswordProtected,
                                    modifier = Modifier.size(44.dp, 56.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${doc.name}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${doc.pageCount} pages • ${formatFileSize(doc.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (isChecked) {
                                    Text(
                                        text = "#${selectedIdx + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        toolsViewModel.mergePdfs(mergeSelection.toList(), outputName) { created ->
                            onOpenPdf(created)
                        }
                    },
                    enabled = mergeSelection.size >= 2 && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("execute_merge_btn")
                ) {
                    Text("Merge ${mergeSelection.size} PDFs")
                }
                return@Scaffold
            }

            // Single-PDF tools: step 1 select document if none chosen yet
            if (selectedDoc == null) {
                Text(
                    text = "Choose a PDF document",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DocumentPickerList(
                    documents = allDocuments,
                    selectedDoc = null,
                    onSelect = { selectedDoc = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val doc = selectedDoc!!
                // Selected Document Banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${doc.name}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.pageCount} pages • ${formatFileSize(doc.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { selectedDoc = null }) {
                            Text("Change")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                when (toolType) {
                    PdfToolType.SPLIT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Split every page into separate PDFs", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = splitEveryPage, onCheckedChange = { splitEveryPage = it })
                        }
                        if (!splitEveryPage) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = splitRanges,
                                onValueChange = { splitRanges = it },
                                label = { Text("Page ranges (e.g. 1-2, 3-5)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                toolsViewModel.splitPdf(doc, splitRanges, splitEveryPage) { created ->
                                    created.firstOrNull()?.let(onOpenPdf)
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Split PDF")
                        }
                    }

                    PdfToolType.COMPRESS -> {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Compression Level", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            PdfCompressionLevel.LOW,
                            PdfCompressionLevel.MEDIUM,
                            PdfCompressionLevel.HIGH
                        ).forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { compressionLevel = level }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = compressionLevel == level,
                                    onClick = { compressionLevel = level },
                                    label = { Text(level.label) }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Quality ${level.jpegQuality}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                toolsViewModel.compressPdf(doc, compressionLevel, outputName) { created ->
                                    onOpenPdf(created)
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Compress PDF")
                        }
                    }

                    PdfToolType.EXTRACT_PAGES, PdfToolType.DELETE_PAGES -> {
                        val isDelete = toolType == PdfToolType.DELETE_PAGES
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isDelete) "Tap pages to mark for deletion (${selectedPageIndices.size} selected)"
                            else "Tap pages to extract (${selectedPageIndices.size} selected)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(104.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(pageThumbnails) { idx, bmp ->
                                val selected = idx in selectedPageIndices
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.74f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) {
                                                if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            } else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (selected) selectedPageIndices.remove(idx)
                                            else selectedPageIndices.add(idx)
                                        }
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Page ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.78f))
                                            .padding(vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Page ${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = if (isDelete) Icons.Filled.Delete else Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (isDelete) {
                                    toolsViewModel.deletePages(doc, selectedPageIndices.toSet(), outputName) { created ->
                                        onOpenPdf(created)
                                    }
                                } else {
                                    toolsViewModel.extractPages(doc, selectedPageIndices.toList(), outputName) { created ->
                                        onOpenPdf(created)
                                    }
                                }
                            },
                            enabled = selectedPageIndices.isNotEmpty() && !isProcessing,
                            colors = if (isDelete) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            else ButtonDefaults.buttonColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(if (isDelete) "Delete Selected Pages" else "Extract Selected Pages")
                        }
                    }

                    PdfToolType.ROTATE_PAGES -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = outputName,
                                onValueChange = { outputName = it },
                                label = { Text("Output Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    for (i in 0 until pageThumbnails.size) {
                                        pageRotations[i] = ((pageRotations[i] ?: 0) + 90) % 360
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("All +90°")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(110.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(pageThumbnails) { idx, bmp ->
                                val rot = pageRotations[idx] ?: 0
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .aspectRatio(0.75f)
                                        .clickable { pageRotations[idx] = (rot + 90) % 360 }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Page ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { rotationZ = rot.toFloat() }
                                            )
                                        }
                                        Text(
                                            text = "P${idx + 1} • ${rot}°",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                toolsViewModel.rotatePages(doc, pageRotations.toMap(), outputName) { created ->
                                    onOpenPdf(created)
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Save Rotated PDF")
                        }
                    }

                    PdfToolType.REORDER_PAGES -> {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(pageOrder, key = { pos, origIdx -> "${pos}_$origIdx" }) { pos, origIdx ->
                                val bmp = pageThumbnails.getOrNull(origIdx)
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp, 60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Original Page ${origIdx + 1}", style = MaterialTheme.typography.titleSmall)
                                            Text("New Position #${pos + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(
                                            onClick = {
                                                if (pos > 0) {
                                                    val moved = pageOrder.removeAt(pos)
                                                    pageOrder.add(pos - 1, moved)
                                                }
                                            },
                                            enabled = pos > 0
                                        ) {
                                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (pos < pageOrder.lastIndex) {
                                                    val moved = pageOrder.removeAt(pos)
                                                    pageOrder.add(pos + 1, moved)
                                                }
                                            },
                                            enabled = pos < pageOrder.lastIndex
                                        ) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                toolsViewModel.reorderPages(doc, pageOrder.toList(), outputName) { created ->
                                    onOpenPdf(created)
                                }
                            },
                            enabled = pageOrder.isNotEmpty() && !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Save Reordered PDF")
                        }
                    }

                    PdfToolType.PDF_TO_IMAGES -> {
                        Text("Export Format", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = exportAsPng,
                                onClick = { exportAsPng = true },
                                label = { Text("PNG (Lossless)") }
                            )
                            FilterChip(
                                selected = !exportAsPng,
                                onClick = { exportAsPng = false },
                                label = { Text("JPEG (Compact)") }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { toolsViewModel.exportToImages(doc, exportAsPng) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Convert ${doc.pageCount} Pages to Images")
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun DocumentPickerList(
    documents: List<PdfDocumentEntity>,
    selectedDoc: PdfDocumentEntity?,
    onSelect: (PdfDocumentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (documents.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text("No PDFs in library yet. Tap 'Pick PDF' above to import one.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(documents, key = { _, d -> d.id }) { _, doc ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedDoc?.id == doc.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable { onSelect(doc) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PdfThumbnailBox(
                            thumbnailPath = doc.thumbnailPath,
                            isPasswordProtected = doc.isPasswordProtected,
                            modifier = Modifier.size(48.dp, 62.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${doc.name}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.pageCount} pages • ${formatFileSize(doc.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
