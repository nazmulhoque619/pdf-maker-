package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.AnnotationPaletteColors
import com.example.domain.model.AnnotationToolType
import com.example.domain.model.PdfAnnotationItem
import com.example.ui.viewmodel.ReaderEditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    documentId: Long,
    viewModel: ReaderEditorViewModel,
    onSaved: (PdfDocumentEntity) -> Unit,
    onBack: () -> Unit
) {
    val document by viewModel.document.collectAsStateWithLifecycle()
    val pageCount by viewModel.pageCount.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val pageBitmaps by viewModel.pageBitmaps.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val activeColor by viewModel.activeColor.collectAsStateWithLifecycle()
    val textSizeSp by viewModel.textSizeSp.collectAsStateWithLifecycle()
    val annotations by viewModel.annotations.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val savedSignature by viewModel.savedSignature.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showSaveMenu by remember { mutableStateOf(false) }
    var showSaveAsNewDialog by remember { mutableStateOf(false) }
    var showOverwriteConfirm by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var pendingTextTap by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var pendingNoteTap by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // Live drag preview state
    val activeStrokePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    var dragStartNorm by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var dragEndNorm by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val currentBitmap = pageBitmaps[currentPage]
    val pageAnnotations = remember(annotations, currentPage) {
        annotations.filter { it.pageIndex == currentPage }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document?.let { "Edit: ${it.name}" } ?: "PDF Editor",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Page ${currentPage + 1}/${pageCount.coerceAtLeast(1)} • ${pageAnnotations.size} edits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("editor_undo_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("editor_redo_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Box {
                        Button(
                            onClick = { showSaveMenu = true },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("editor_save_btn")
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                        DropdownMenu(expanded = showSaveMenu, onDismissRequest = { showSaveMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Save as New PDF") },
                                onClick = {
                                    showSaveMenu = false
                                    showSaveAsNewDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Overwrite Original") },
                                onClick = {
                                    showSaveMenu = false
                                    showOverwriteConfirm = true
                                }
                            )
                        }
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
            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Canvas Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    val aspect = (currentBitmap.width.toFloat() / currentBitmap.height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)
                    Box(
                        modifier = Modifier
                            .aspectRatio(aspect)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            .background(Color.White)
                    ) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Editable PDF page",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Annotation Overlay Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeTool, currentPage, activeColor, textSizeSp, savedSignature) {
                                    detectTapGestures { offset ->
                                        val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                        val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                        when (activeTool) {
                                            AnnotationToolType.TEXT -> pendingTextTap = nx to ny
                                            AnnotationToolType.NOTE -> pendingNoteTap = nx to ny
                                            AnnotationToolType.DATE -> {
                                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                viewModel.addAnnotation(
                                                    PdfAnnotationItem.TextOverlay(
                                                        pageIndex = currentPage,
                                                        colorArgb = viewModel.activeColorArgb(),
                                                        xNorm = nx,
                                                        yNorm = ny,
                                                        text = today,
                                                        fontSizeSp = textSizeSp
                                                    )
                                                )
                                            }
                                            AnnotationToolType.SIGNATURE -> {
                                                if (savedSignature.isEmpty()) {
                                                    showSignaturePad = true
                                                } else {
                                                    val w = 0.28f
                                                    val h = 0.10f
                                                    viewModel.addAnnotation(
                                                        PdfAnnotationItem.SignatureOverlay(
                                                            pageIndex = currentPage,
                                                            colorArgb = viewModel.activeColorArgb(),
                                                            left = (nx - w / 2f).coerceIn(0.02f, 0.98f - w),
                                                            top = (ny - h / 2f).coerceIn(0.02f, 0.98f - h),
                                                            right = (nx + w / 2f).coerceIn(w + 0.02f, 0.98f),
                                                            bottom = (ny + h / 2f).coerceIn(h + 0.02f, 0.98f),
                                                            strokes = savedSignature
                                                        )
                                                    )
                                                }
                                            }
                                            AnnotationToolType.ERASER -> viewModel.eraseAnnotationAt(currentPage, nx, ny)
                                            else -> Unit
                                        }
                                    }
                                }
                                .pointerInput(activeTool, currentPage, activeColor) {
                                    if (activeTool in setOf(
                                            AnnotationToolType.FREEHAND,
                                            AnnotationToolType.HIGHLIGHT,
                                            AnnotationToolType.UNDERLINE,
                                            AnnotationToolType.STRIKETHROUGH,
                                            AnnotationToolType.SHAPE_RECT,
                                            AnnotationToolType.SHAPE_CIRCLE,
                                            AnnotationToolType.ARROW,
                                            AnnotationToolType.ERASER
                                        )
                                    ) {
                                        detectDragGestures(
                                            onDragStart = { start ->
                                                val nx = (start.x / size.width).coerceIn(0f, 1f)
                                                val ny = (start.y / size.height).coerceIn(0f, 1f)
                                                if (activeTool == AnnotationToolType.FREEHAND) {
                                                    activeStrokePoints.clear()
                                                    activeStrokePoints.add(nx to ny)
                                                } else if (activeTool == AnnotationToolType.ERASER) {
                                                    viewModel.eraseAnnotationAt(currentPage, nx, ny)
                                                } else {
                                                    dragStartNorm = nx to ny
                                                    dragEndNorm = nx to ny
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                                if (activeTool == AnnotationToolType.FREEHAND) {
                                                    activeStrokePoints.add(nx to ny)
                                                } else if (activeTool == AnnotationToolType.ERASER) {
                                                    viewModel.eraseAnnotationAt(currentPage, nx, ny)
                                                } else {
                                                    dragEndNorm = nx to ny
                                                }
                                            },
                                            onDragEnd = {
                                                val col = viewModel.activeColorArgb()
                                                val sw = viewModel.strokeWidthNorm.value
                                                when (activeTool) {
                                                    AnnotationToolType.FREEHAND -> {
                                                        if (activeStrokePoints.size >= 2) {
                                                            viewModel.addAnnotation(
                                                                PdfAnnotationItem.FreehandPath(
                                                                    pageIndex = currentPage,
                                                                    colorArgb = col,
                                                                    strokeWidthNorm = sw,
                                                                    points = activeStrokePoints.toList()
                                                                )
                                                            )
                                                        }
                                                        activeStrokePoints.clear()
                                                    }
                                                    AnnotationToolType.HIGHLIGHT,
                                                    AnnotationToolType.UNDERLINE,
                                                    AnnotationToolType.STRIKETHROUGH,
                                                    AnnotationToolType.ARROW -> {
                                                        val s = dragStartNorm
                                                        val e = dragEndNorm
                                                        if (s != null && e != null) {
                                                            val kind = when (activeTool) {
                                                                AnnotationToolType.HIGHLIGHT -> PdfAnnotationItem.LineKind.HIGHLIGHT
                                                                AnnotationToolType.UNDERLINE -> PdfAnnotationItem.LineKind.UNDERLINE
                                                                AnnotationToolType.STRIKETHROUGH -> PdfAnnotationItem.LineKind.STRIKETHROUGH
                                                                else -> PdfAnnotationItem.LineKind.ARROW
                                                            }
                                                            val endY = if (kind == PdfAnnotationItem.LineKind.ARROW) e.second else s.second
                                                            viewModel.addAnnotation(
                                                                PdfAnnotationItem.LineOverlay(
                                                                    pageIndex = currentPage,
                                                                    colorArgb = col,
                                                                    startX = s.first,
                                                                    startY = s.second,
                                                                    endX = e.first,
                                                                    endY = endY,
                                                                    strokeWidthNorm = sw,
                                                                    kind = kind
                                                                )
                                                            )
                                                        }
                                                    }
                                                    AnnotationToolType.SHAPE_RECT,
                                                    AnnotationToolType.SHAPE_CIRCLE -> {
                                                        val s = dragStartNorm
                                                        val e = dragEndNorm
                                                        if (s != null && e != null) {
                                                            viewModel.addAnnotation(
                                                                PdfAnnotationItem.ShapeOverlay(
                                                                    pageIndex = currentPage,
                                                                    colorArgb = col,
                                                                    left = min(s.first, e.first),
                                                                    top = min(s.second, e.second),
                                                                    right = max(s.first, e.first),
                                                                    bottom = max(s.second, e.second),
                                                                    strokeWidthNorm = sw,
                                                                    isCircle = activeTool == AnnotationToolType.SHAPE_CIRCLE
                                                                )
                                                            )
                                                        }
                                                    }
                                                    else -> Unit
                                                }
                                                dragStartNorm = null
                                                dragEndNorm = null
                                            }
                                        )
                                    }
                                }
                        ) {
                            pageAnnotations.forEach { item ->
                                renderAnnotationOverlay(item, textMeasurer)
                            }
                            // Live drag preview
                            if (activeStrokePoints.size >= 2) {
                                val path = Path().apply {
                                    moveTo(activeStrokePoints.first().first * size.width, activeStrokePoints.first().second * size.height)
                                    for (i in 1 until activeStrokePoints.size) {
                                        lineTo(activeStrokePoints[i].first * size.width, activeStrokePoints[i].second * size.height)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = activeColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                            val s = dragStartNorm
                            val e = dragEndNorm
                            if (s != null && e != null) {
                                val x1 = s.first * size.width
                                val y1 = s.second * size.height
                                val x2 = e.first * size.width
                                val y2 = e.second * size.height
                                when (activeTool) {
                                    AnnotationToolType.HIGHLIGHT -> drawLine(
                                        color = activeColor.copy(alpha = 0.38f),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y1),
                                        strokeWidth = 28f,
                                        cap = StrokeCap.Round
                                    )
                                    AnnotationToolType.UNDERLINE, AnnotationToolType.STRIKETHROUGH -> drawLine(
                                        color = activeColor,
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y1),
                                        strokeWidth = 5f
                                    )
                                    AnnotationToolType.ARROW -> drawArrow(activeColor, Offset(x1, y1), Offset(x2, y2), 5f)
                                    AnnotationToolType.SHAPE_RECT -> drawRect(
                                        color = activeColor,
                                        topLeft = Offset(min(x1, x2), min(y1, y2)),
                                        size = Size(abs(x2 - x1), abs(y2 - y1)),
                                        style = Stroke(width = 5f)
                                    )
                                    AnnotationToolType.SHAPE_CIRCLE -> drawOval(
                                        color = activeColor,
                                        topLeft = Offset(min(x1, x2), min(y1, y2)),
                                        size = Size(abs(x2 - x1), abs(y2 - y1)),
                                        style = Stroke(width = 5f)
                                    )
                                    else -> Unit
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }

            // Editor Controls Panel
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Page Switcher + Color Swatches + Text Size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (currentPage > 0) viewModel.setCurrentPage(currentPage - 1) },
                                enabled = currentPage > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous page")
                            }
                            Text(
                                text = "${currentPage + 1}/$pageCount",
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = { if (currentPage < pageCount - 1) viewModel.setCurrentPage(currentPage + 1) },
                                enabled = currentPage < pageCount - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next page")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AnnotationPaletteColors) { swatch ->
                                val selected = swatch.toArgb() == activeColor.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setActiveColor(swatch) }
                                )
                            }
                        }
                    }

                    if (activeTool in setOf(AnnotationToolType.TEXT, AnnotationToolType.NOTE, AnnotationToolType.DATE)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Text ${textSizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                            Slider(
                                value = textSizeSp,
                                onValueChange = { viewModel.setTextSizeSp(it) },
                                valueRange = 10f..34f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (activeTool == AnnotationToolType.SIGNATURE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (savedSignature.isEmpty()) "Draw signature first, then tap page"
                                else "Tap anywhere on PDF page to stamp signature",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(onClick = { showSignaturePad = true }) {
                                Text(if (savedSignature.isEmpty()) "Draw Sign" else "Redraw")
                            }
                        }
                    }

                    // 13 Tool Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AnnotationToolType.entries) { tool ->
                            FilterChip(
                                selected = activeTool == tool,
                                onClick = {
                                    viewModel.setActiveTool(tool)
                                    if (tool == AnnotationToolType.SIGNATURE && savedSignature.isEmpty()) {
                                        showSignaturePad = true
                                    }
                                },
                                label = { Text(tool.label) },
                                leadingIcon = {
                                    Icon(iconForAnnotationTool(tool), null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("annot_tool_${tool.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }

    pendingTextTap?.let { (nx, ny) ->
        var textInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingTextTap = null },
            title = { Text("Add Text to PDF") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.addAnnotation(
                                PdfAnnotationItem.TextOverlay(
                                    pageIndex = currentPage,
                                    colorArgb = viewModel.activeColorArgb(),
                                    xNorm = nx,
                                    yNorm = ny,
                                    text = textInput,
                                    fontSizeSp = textSizeSp
                                )
                            )
                        }
                        pendingTextTap = null
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTextTap = null }) { Text("Cancel") }
            }
        )
    }

    pendingNoteTap?.let { (nx, ny) ->
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingNoteTap = null },
            title = { Text("Add Sticky Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note content") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            viewModel.addAnnotation(
                                PdfAnnotationItem.TextOverlay(
                                    pageIndex = currentPage,
                                    colorArgb = viewModel.activeColorArgb(),
                                    xNorm = nx,
                                    yNorm = ny,
                                    text = noteText,
                                    fontSizeSp = textSizeSp,
                                    isNoteBadge = true
                                )
                            )
                        }
                        pendingNoteTap = null
                    }
                ) { Text("Pin Note") }
            },
            dismissButton = {
                TextButton(onClick = { pendingNoteTap = null }) { Text("Cancel") }
            }
        )
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            penColor = activeColor,
            onDismiss = { showSignaturePad = false },
            onConfirm = { strokes ->
                viewModel.saveSignaturePattern(strokes)
                showSignaturePad = false
            }
        )
    }

    if (showSaveAsNewDialog) {
        var newName by remember(document) { mutableStateOf("${document?.name ?: "Edited"}_annotated") }
        AlertDialog(
            onDismissRequest = { showSaveAsNewDialog = false },
            title = { Text("Save as New PDF") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New PDF Name") },
                    suffix = { Text(".pdf") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveAsNewDialog = false
                        viewModel.saveAnnotatedPdf(overwriteExisting = false, newFileName = newName, onSaved = onSaved)
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Save PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsNewDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showOverwriteConfirm) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirm = false },
            title = { Text("Overwrite Original PDF?") },
            text = { Text("This will permanently apply your annotations to \"${document?.name}.pdf\".") },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirm = false
                        viewModel.saveAnnotatedPdf(overwriteExisting = true, newFileName = null, onSaved = onSaved)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SignaturePadDialog(
    penColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (List<List<Pair<Float, Float>>>) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    val activeStroke = remember { mutableStateListOf<Pair<Float, Float>>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Signature") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sign inside the box below:", style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        activeStroke.clear()
                                        activeStroke.add((start.x / size.width).coerceIn(0f, 1f) to (start.y / size.height).coerceIn(0f, 1f))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activeStroke.add((change.position.x / size.width).coerceIn(0f, 1f) to (change.position.y / size.height).coerceIn(0f, 1f))
                                    },
                                    onDragEnd = {
                                        if (activeStroke.size >= 2) {
                                            strokes.add(activeStroke.toList())
                                        }
                                        activeStroke.clear()
                                    }
                                )
                            }
                    ) {
                        val allStrokes = strokes + if (activeStroke.size >= 2) listOf(activeStroke.toList()) else emptyList()
                        allStrokes.forEach { pts ->
                            val path = Path().apply {
                                moveTo(pts.first().first * size.width, pts.first().second * size.height)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].first * size.width, pts[i].second * size.height)
                                }
                            }
                            drawPath(path, color = penColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
                TextButton(onClick = {
                    strokes.clear()
                    activeStroke.clear()
                }) {
                    Text("Clear Pad")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(strokes.toList()) },
                enabled = strokes.isNotEmpty()
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Use Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun DrawScope.renderAnnotationOverlay(
    item: PdfAnnotationItem,
    textMeasurer: TextMeasurer
) {
    val color = Color(item.colorArgb)
    when (item) {
        is PdfAnnotationItem.FreehandPath -> {
            if (item.points.size < 2) return
            val path = Path().apply {
                moveTo(item.points.first().first * size.width, item.points.first().second * size.height)
                for (i in 1 until item.points.size) {
                    lineTo(item.points[i].first * size.width, item.points[i].second * size.height)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = item.alpha),
                style = Stroke(
                    width = (item.strokeWidthNorm * size.width).coerceAtLeast(3f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        is PdfAnnotationItem.LineOverlay -> {
            val start = Offset(item.startX * size.width, item.startY * size.height)
            val end = Offset(item.endX * size.width, item.endY * size.height)
            when (item.kind) {
                PdfAnnotationItem.LineKind.HIGHLIGHT -> drawLine(
                    color = color.copy(alpha = 0.38f),
                    start = start,
                    end = end,
                    strokeWidth = (item.strokeWidthNorm * size.width * 3.5f).coerceAtLeast(24f),
                    cap = StrokeCap.Round
                )
                PdfAnnotationItem.LineKind.UNDERLINE,
                PdfAnnotationItem.LineKind.STRIKETHROUGH -> drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = (item.strokeWidthNorm * size.width).coerceAtLeast(4f)
                )
                PdfAnnotationItem.LineKind.ARROW -> drawArrow(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = (item.strokeWidthNorm * size.width).coerceAtLeast(4f)
                )
            }
        }
        is PdfAnnotationItem.ShapeOverlay -> {
            val l = item.left * size.width
            val t = item.top * size.height
            val r = item.right * size.width
            val b = item.bottom * size.height
            val sw = (item.strokeWidthNorm * size.width).coerceAtLeast(4f)
            if (item.isCircle) {
                drawOval(color, topLeft = Offset(l, t), size = Size(r - l, b - t), style = Stroke(sw))
            } else {
                drawRect(color, topLeft = Offset(l, t), size = Size(r - l, b - t), style = Stroke(sw))
            }
        }
        is PdfAnnotationItem.TextOverlay -> {
            val x = item.xNorm * size.width
            val y = item.yNorm * size.height
            val layoutResult = textMeasurer.measure(
                text = item.text,
                style = TextStyle(color = color, fontSize = item.fontSizeSp.sp, fontWeight = FontWeight.Bold)
            )
            if (item.isNoteBadge) {
                drawRect(
                    color = Color(0xFFFEF3C7),
                    topLeft = Offset((x - 6f).coerceAtLeast(0f), (y - 6f).coerceAtLeast(0f)),
                    size = Size(layoutResult.size.width + 12f, layoutResult.size.height + 12f)
                )
                drawRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset((x - 6f).coerceAtLeast(0f), (y - 6f).coerceAtLeast(0f)),
                    size = Size(layoutResult.size.width + 12f, layoutResult.size.height + 12f),
                    style = Stroke(2f)
                )
            }
            drawText(textLayoutResult = layoutResult, topLeft = Offset(x, y))
        }
        is PdfAnnotationItem.SignatureOverlay -> {
            val l = item.left * size.width
            val t = item.top * size.height
            val w = (item.right - item.left) * size.width
            val h = (item.bottom - item.top) * size.height
            item.strokes.forEach { pts ->
                if (pts.size >= 2) {
                    val path = Path().apply {
                        moveTo(l + pts.first().first * w, t + pts.first().second * h)
                        for (i in 1 until pts.size) {
                            lineTo(l + pts[i].first * w, t + pts[i].second * h)
                        }
                    }
                    drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

private fun DrawScope.drawArrow(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float
) {
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLen = (size.width * 0.035f).coerceIn(14f, 36f)
    val a1 = angle - Math.PI / 6.0
    val a2 = angle + Math.PI / 6.0
    drawLine(
        color = color,
        start = end,
        end = Offset((end.x - headLen * cos(a1)).toFloat(), (end.y - headLen * sin(a1)).toFloat()),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = end,
        end = Offset((end.x - headLen * cos(a2)).toFloat(), (end.y - headLen * sin(a2)).toFloat()),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun iconForAnnotationTool(tool: AnnotationToolType): ImageVector = when (tool) {
    AnnotationToolType.SELECT -> Icons.Filled.PanTool
    AnnotationToolType.TEXT -> Icons.Filled.TextFields
    AnnotationToolType.HIGHLIGHT -> Icons.Filled.Highlight
    AnnotationToolType.UNDERLINE -> Icons.Filled.FormatUnderlined
    AnnotationToolType.STRIKETHROUGH -> Icons.Filled.StrikethroughS
    AnnotationToolType.FREEHAND -> Icons.Filled.Draw
    AnnotationToolType.ERASER -> Icons.Filled.AutoFixNormal
    AnnotationToolType.SHAPE_RECT -> Icons.Filled.CropSquare
    AnnotationToolType.SHAPE_CIRCLE -> Icons.Filled.Circle
    AnnotationToolType.ARROW -> Icons.Filled.ArrowOutward
    AnnotationToolType.NOTE -> Icons.Filled.NoteAdd
    AnnotationToolType.DATE -> Icons.Filled.CalendarToday
    AnnotationToolType.SIGNATURE -> Icons.Filled.BorderColor
}
