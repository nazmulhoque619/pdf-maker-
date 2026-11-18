package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PdfDocumentEntity
import com.example.ui.viewmodel.ReaderEditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    documentId: Long,
    viewModel: ReaderEditorViewModel,
    onOpenEditor: (Long) -> Unit,
    onSharePdf: (PdfDocumentEntity) -> Unit,
    onPrintPdf: (PdfDocumentEntity) -> Unit,
    onBack: () -> Unit
) {
    val document by viewModel.document.collectAsStateWithLifecycle()
    val pageCount by viewModel.pageCount.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val pageBitmaps by viewModel.pageBitmaps.collectAsStateWithLifecycle()
    val pageRotations by viewModel.pageRotations.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchMatches by viewModel.searchMatches.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var isFullScreen by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount.coerceAtLeast(1) }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
        zoomScale = 1f
        panX = 0f
        panY = 0f
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && currentPage in 0 until pageCount.coerceAtLeast(1)) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    val isCurrentBookmarked = bookmarks.any { it.pageIndex == currentPage }
    val nightFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -0.9f, 0f, 0f, 0f, 235f,
                    0f, -0.9f, 0f, 0f, 235f,
                    0f, 0f, -0.85f, 0f, 240f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(visible = !isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = document?.let { "${it.name}.pdf" } ?: "PDF Reader",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (pageCount > 0) {
                                Text(
                                    text = "Page ${currentPage + 1} of $pageCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }, modifier = Modifier.testTag("reader_search_btn")) {
                            Icon(Icons.Filled.Search, contentDescription = "Search inside PDF")
                        }
                        IconButton(onClick = { viewModel.toggleCurrentPageBookmark() }, modifier = Modifier.testTag("reader_bookmark_btn")) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark page",
                                tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onOpenEditor(documentId) }, modifier = Modifier.testTag("reader_edit_btn")) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit and annotate PDF")
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = !isFullScreen) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { if (currentPage > 0) viewModel.setCurrentPage(currentPage - 1) } },
                            enabled = currentPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous page")
                        }
                        IconButton(onClick = { zoomScale = (zoomScale - 0.5f).coerceAtLeast(1f) }) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                        }
                        IconButton(onClick = { zoomScale = (zoomScale + 0.5f).coerceAtMost(3.5f) }) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                        }
                        IconButton(onClick = { isNightMode = !isNightMode }, modifier = Modifier.testTag("reader_night_btn")) {
                            Icon(
                                Icons.Filled.DarkMode,
                                contentDescription = "Night reading mode",
                                tint = if (isNightMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.rotateCurrentPage() }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate page")
                        }
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Bookmarks")
                        }
                        IconButton(onClick = { document?.let(onPrintPdf) }) {
                            Icon(Icons.Filled.Print, contentDescription = "Print PDF")
                        }
                        IconButton(onClick = { document?.let(onSharePdf) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share PDF")
                        }
                        IconButton(
                            onClick = { scope.launch { if (currentPage < pageCount - 1) viewModel.setCurrentPage(currentPage + 1) } },
                            enabled = currentPage < pageCount - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next page")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) Color(0xFF0A0F1A) else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (isLocked) {
                PasswordUnlockPane(
                    error = passwordError,
                    onUnlock = { viewModel.unlockWithPassword(it) }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = showSearchBar && !isFullScreen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search text inside PDF...") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        viewModel.updateSearchQuery("")
                                        showSearchBar = false
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close search")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (searchMatches.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${searchMatches.size} page matches",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LazyColumn(modifier = Modifier.height(110.dp)) {
                                    items(searchMatches) { match ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setCurrentPage(match.pageIndex) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "P.${match.pageIndex + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.width(42.dp)
                                            )
                                            Text(
                                                text = match.snippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "No text matches found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = zoomScale <= 1.05f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pageIdx ->
                        LaunchedEffect(pageIdx) {
                            viewModel.ensurePageRendered(pageIdx)
                        }
                        val bmp: Bitmap? = pageBitmaps[pageIdx]
                        val rot = (pageRotations[pageIdx] ?: 0).toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pageIdx) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            zoomScale = if (zoomScale > 1.2f) 1f else 2.2f
                                            if (zoomScale == 1f) {
                                                panX = 0f
                                                panY = 0f
                                            }
                                        },
                                        onTap = {
                                            isFullScreen = !isFullScreen
                                        }
                                    )
                                }
                                .pointerInput(zoomScale > 1.05f) {
                                    if (zoomScale > 1.05f) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                            if (zoomScale > 1.05f) {
                                                panX += pan.x
                                                panY += pan.y
                                            } else {
                                                panX = 0f
                                                panY = 0f
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                val aspect = (bmp.width.toFloat() / bmp.height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF page ${pageIdx + 1}",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (isNightMode) nightFilter else null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .aspectRatio(aspect)
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = zoomScale
                                            scaleY = zoomScale
                                            translationX = panX
                                            translationY = panY
                                            rotationZ = rot
                                        }
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // Fullscreen floating toggle chip
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable { isFullScreen = !isFullScreen }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle full screen",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${currentPage + 1}/$pageCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Bookmarked Pages", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (bookmarks.isEmpty()) {
                    Text(
                        text = "No bookmarks yet. Tap the bookmark icon while reading to save pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    bookmarks.forEach { bm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.setCurrentPage(bm.pageIndex)
                                    showBookmarksSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(10.dp))
                                Text(bm.label, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PasswordUnlockPane(
    error: String?,
    onUnlock: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(44.dp))
                Text("Password Protected PDF", style = MaterialTheme.typography.titleLarge)
                Text("Enter the document password to unlock and view.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { onUnlock(pwd) },
                    enabled = pwd.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock PDF")
                }
            }
        }
    }
}
