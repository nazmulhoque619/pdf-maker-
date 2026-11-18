package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartPdfApp
import com.example.data.local.BookmarkEntity
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.AnnotationToolType
import com.example.domain.model.PdfAnnotationItem
import com.example.domain.pdf.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SearchMatch(
    val pageIndex: Int,
    val snippet: String
)

class ReaderEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmartPdfApp
    val repository = app.pdfRepository

    private val _document = MutableStateFlow<PdfDocumentEntity?>(null)
    val document: StateFlow<PdfDocumentEntity?> = _document.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

    private val _pageRotations = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val pageRotations: StateFlow<Map<Int, Int>> = _pageRotations.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private var activePassword: String? = null

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    // Search state
    private val _pageTexts = MutableStateFlow<List<String>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val searchMatches: StateFlow<List<SearchMatch>> = _searchMatches.asStateFlow()

    // Annotation Editor State
    private val _activeTool = MutableStateFlow(AnnotationToolType.SELECT)
    val activeTool: StateFlow<AnnotationToolType> = _activeTool.asStateFlow()

    private val _activeColor = MutableStateFlow(Color(0xFFDC2626))
    val activeColor: StateFlow<Color> = _activeColor.asStateFlow()

    private val _textSizeSp = MutableStateFlow(16f)
    val textSizeSp: StateFlow<Float> = _textSizeSp.asStateFlow()

    private val _strokeWidthNorm = MutableStateFlow(0.006f)
    val strokeWidthNorm: StateFlow<Float> = _strokeWidthNorm.asStateFlow()

    private val _annotations = MutableStateFlow<List<PdfAnnotationItem>>(emptyList())
    val annotations: StateFlow<List<PdfAnnotationItem>> = _annotations.asStateFlow()

    private val _undoStack = MutableStateFlow<List<List<PdfAnnotationItem>>>(emptyList())
    private val _redoStack = MutableStateFlow<List<List<PdfAnnotationItem>>>(emptyList())

    val canUndo: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            _undoStack.collect { flow.value = it.isNotEmpty() }
        }
    }
    val canRedo: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            _redoStack.collect { flow.value = it.isNotEmpty() }
        }
    }

    private val _savedSignature = MutableStateFlow<List<List<Pair<Float, Float>>>>(emptyList())
    val savedSignature: StateFlow<List<List<Pair<Float, Float>>>> = _savedSignature.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(documentId) ?: return@launch
            _document.value = doc
            repository.markOpened(doc.id)
            _annotations.value = emptyList()
            _undoStack.value = emptyList()
            _redoStack.value = emptyList()
            _pageBitmaps.value = emptyMap()
            _pageRotations.value = emptyMap()
            _currentPage.value = 0
            activePassword = null
            _passwordError.value = null

            val file = File(doc.filePath)
            val (count, encrypted) = PdfEngine.inspectPdfInfo(file, null)
            if (encrypted && count == 0) {
                _isLocked.value = true
                _pageCount.value = doc.pageCount
            } else {
                _isLocked.value = false
                _pageCount.value = count.coerceAtLeast(1)
                preloadInitialPages(file, count.coerceAtLeast(1))
                extractTextIndex(file, null)
            }

            launch {
                repository.observeBookmarks(doc.id).collect {
                    _bookmarks.value = it
                }
            }
        }
    }

    fun unlockWithPassword(password: String) {
        val doc = _document.value ?: return
        viewModelScope.launch {
            val file = File(doc.filePath)
            val (count, _) = PdfEngine.inspectPdfInfo(file, password)
            if (count > 0) {
                activePassword = password
                _isLocked.value = false
                _passwordError.value = null
                _pageCount.value = count
                preloadInitialPages(file, count)
                extractTextIndex(file, password)
            } else {
                _passwordError.value = "Incorrect password. Try again."
            }
        }
    }

    private fun preloadInitialPages(file: File, total: Int) {
        viewModelScope.launch {
            for (i in 0 until minOf(total, 3)) {
                renderPageIfNeeded(i, file)
            }
        }
    }

    fun ensurePageRendered(pageIndex: Int) {
        val doc = _document.value ?: return
        if (_isLocked.value) return
        if (_pageBitmaps.value.containsKey(pageIndex)) return
        viewModelScope.launch {
            renderPageIfNeeded(pageIndex, File(doc.filePath))
        }
    }

    private suspend fun renderPageIfNeeded(pageIndex: Int, file: File) {
        if (_pageBitmaps.value.containsKey(pageIndex)) return
        val bmp = PdfEngine.renderPageBitmap(
            context = getApplication(),
            pdfFile = file,
            pageIndex = pageIndex,
            targetWidthPx = 1280,
            password = activePassword
        ) ?: return
        _pageBitmaps.update { current ->
            val next = current.toMutableMap()
            // Evict distant pages if cache grows beyond 8 bitmaps to keep memory safe
            if (next.size >= 8) {
                val farthestKey = next.keys.maxByOrNull { kotlin.math.abs(it - pageIndex) }
                if (farthestKey != null && farthestKey != pageIndex) {
                    next.remove(farthestKey)
                }
            }
            next[pageIndex] = bmp
            next
        }
    }

    private fun extractTextIndex(file: File, password: String?) {
        viewModelScope.launch {
            _pageTexts.value = PdfEngine.extractTextPerPage(file, password)
        }
    }

    fun setCurrentPage(index: Int) {
        val maxPage = (_pageCount.value - 1).coerceAtLeast(0)
        val clamped = index.coerceIn(0, maxPage)
        _currentPage.value = clamped
        ensurePageRendered(clamped)
        if (clamped + 1 <= maxPage) ensurePageRendered(clamped + 1)
    }

    fun rotateCurrentPage() {
        val page = _currentPage.value
        _pageRotations.update { map ->
            val current = map[page] ?: 0
            map + (page to ((current + 90) % 360))
        }
    }

    fun toggleCurrentPageBookmark() {
        val doc = _document.value ?: return
        val page = _currentPage.value
        val isBookmarked = _bookmarks.value.any { it.pageIndex == page }
        viewModelScope.launch {
            repository.toggleBookmark(doc.id, page, isBookmarked)
            _statusMessage.value = if (isBookmarked) "Removed bookmark on page ${page + 1}" else "Bookmarked page ${page + 1}"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val q = query.trim()
        if (q.isEmpty()) {
            _searchMatches.value = emptyList()
            return
        }
        val texts = _pageTexts.value
        val found = mutableListOf<SearchMatch>()
        texts.forEachIndexed { idx, content ->
            val pos = content.indexOf(q, ignoreCase = true)
            if (pos >= 0) {
                val start = (pos - 28).coerceAtLeast(0)
                val end = (pos + q.length + 40).coerceAtMost(content.length)
                val snippet = content.substring(start, end).replace('\n', ' ').trim()
                found.add(SearchMatch(idx, "...$snippet..."))
            }
        }
        _searchMatches.value = found
        if (found.isNotEmpty()) {
            setCurrentPage(found.first().pageIndex)
        }
    }

    // Annotation Actions
    fun setActiveTool(tool: AnnotationToolType) {
        _activeTool.value = tool
    }

    fun setActiveColor(color: Color) {
        _activeColor.value = color
    }

    fun setTextSizeSp(size: Float) {
        _textSizeSp.value = size.coerceIn(10f, 36f)
    }

    fun setStrokeWidthNorm(width: Float) {
        _strokeWidthNorm.value = width.coerceIn(0.002f, 0.02f)
    }

    fun saveSignaturePattern(strokes: List<List<Pair<Float, Float>>>) {
        _savedSignature.value = strokes
    }

    fun addAnnotation(item: PdfAnnotationItem) {
        _undoStack.update { it + listOf(_annotations.value) }
        _redoStack.value = emptyList()
        _annotations.update { it + item }
    }

    fun eraseAnnotationAt(pageIndex: Int, normX: Float, normY: Float) {
        val current = _annotations.value
        val hit = current.lastOrNull { item ->
            if (item.pageIndex != pageIndex) return@lastOrNull false
            when (item) {
                is PdfAnnotationItem.FreehandPath -> item.points.any { (px, py) ->
                    kotlin.math.hypot((px - normX).toDouble(), (py - normY).toDouble()) < 0.055
                }
                is PdfAnnotationItem.LineOverlay -> {
                    val minX = minOf(item.startX, item.endX) - 0.04f
                    val maxX = maxOf(item.startX, item.endX) + 0.04f
                    val minY = minOf(item.startY, item.endY) - 0.04f
                    val maxY = maxOf(item.startY, item.endY) + 0.04f
                    normX in minX..maxX && normY in minY..maxY
                }
                is PdfAnnotationItem.ShapeOverlay -> {
                    val minX = minOf(item.left, item.right) - 0.03f
                    val maxX = maxOf(item.left, item.right) + 0.03f
                    val minY = minOf(item.top, item.bottom) - 0.03f
                    val maxY = maxOf(item.top, item.bottom) + 0.03f
                    normX in minX..maxX && normY in minY..maxY
                }
                is PdfAnnotationItem.TextOverlay -> {
                    kotlin.math.abs(normX - item.xNorm) < 0.14f && kotlin.math.abs(normY - item.yNorm) < 0.06f
                }
                is PdfAnnotationItem.SignatureOverlay -> {
                    normX in (item.left - 0.02f)..(item.right + 0.02f) &&
                        normY in (item.top - 0.02f)..(item.bottom + 0.02f)
                }
            }
        } ?: return

        _undoStack.update { it + listOf(current) }
        _redoStack.value = emptyList()
        _annotations.update { list -> list.filterNot { it.id == hit.id } }
    }

    fun undo() {
        val undoList = _undoStack.value
        if (undoList.isEmpty()) return
        val previous = undoList.last()
        _undoStack.value = undoList.dropLast(1)
        _redoStack.update { it + listOf(_annotations.value) }
        _annotations.value = previous
    }

    fun redo() {
        val redoList = _redoStack.value
        if (redoList.isEmpty()) return
        val next = redoList.last()
        _redoStack.value = redoList.dropLast(1)
        _undoStack.update { it + listOf(_annotations.value) }
        _annotations.value = next
    }

    fun saveAnnotatedPdf(
        overwriteExisting: Boolean,
        newFileName: String?,
        onSaved: (PdfDocumentEntity) -> Unit
    ) {
        val doc = _document.value ?: return
        val srcFile = File(doc.filePath)
        val currentAnnots = _annotations.value
        viewModelScope.launch {
            _isSaving.value = true
            val destFile = if (overwriteExisting) {
                srcFile
            } else {
                repository.createTargetPdfFile(newFileName?.ifBlank { "${doc.name}_edited" } ?: "${doc.name}_edited")
            }

            val result = PdfEngine.bakeAnnotationsIntoPdf(
                sourceFile = srcFile,
                annotations = currentAnnots,
                outputFile = destFile,
                password = activePassword
            )

            result.onSuccess { pages ->
                val savedEntity = if (overwriteExisting) {
                    repository.refreshExistingDocumentMetadata(doc, activePassword)
                } else {
                    repository.registerPdfFile(
                        file = destFile,
                        displayName = destFile.nameWithoutExtension,
                        pageCount = pages,
                        isPasswordProtected = doc.isPasswordProtected
                    )
                }
                _annotations.value = emptyList()
                _undoStack.value = emptyList()
                _redoStack.value = emptyList()
                _pageBitmaps.value = emptyMap()
                _document.value = savedEntity
                _isSaving.value = false
                _statusMessage.value = "Saved ${savedEntity.name}.pdf"
                preloadInitialPages(File(savedEntity.filePath), pages)
                onSaved(savedEntity)
            }.onFailure { err ->
                _isSaving.value = false
                _statusMessage.value = "Failed to save PDF: ${err.message ?: "Error"}"
            }
        }
    }

    fun activeColorArgb(): Int = _activeColor.value.toArgb()
}
