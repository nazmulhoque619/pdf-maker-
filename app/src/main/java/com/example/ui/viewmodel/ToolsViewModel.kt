package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartPdfApp
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.PdfCompressionLevel
import com.example.domain.pdf.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ToolsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmartPdfApp
    val repository = app.pdfRepository

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    private val _pageThumbnails = MutableStateFlow<List<Bitmap?>>(emptyList())
    val pageThumbnails: StateFlow<List<Bitmap?>> = _pageThumbnails.asStateFlow()

    private val _exportedImages = MutableStateFlow<List<File>>(emptyList())
    val exportedImages: StateFlow<List<File>> = _exportedImages.asStateFlow()

    fun clearResult() {
        _resultMessage.value = null
    }

    fun clearExportedImages() {
        _exportedImages.value = emptyList()
    }

    fun loadPageThumbnails(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            _pageThumbnails.value = emptyList()
            val file = File(doc.filePath)
            val total = PdfEngine.getPageCount(getApplication(), file)
            val list = MutableList<Bitmap?>(total) { null }
            _pageThumbnails.value = list
            for (i in 0 until total) {
                val bmp = PdfEngine.renderPageBitmap(getApplication(), file, i, targetWidthPx = 320)
                _pageThumbnails.update { current ->
                    current.toMutableList().also { if (i in it.indices) it[i] = bmp }
                }
            }
        }
    }

    fun mergePdfs(docs: List<PdfDocumentEntity>, outputName: String, onDone: (PdfDocumentEntity) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.25f
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.mergePdfs(docs.map { File(it.filePath) }, outFile)
                .onSuccess { pages ->
                    val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                    _isProcessing.value = false
                    _progress.value = 1f
                    _resultMessage.value = "Merged ${docs.size} PDFs into ${saved.name}.pdf ($pages pages)"
                    onDone(saved)
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Merge failed: ${it.message}"
                }
        }
    }

    fun splitPdf(
        doc: PdfDocumentEntity,
        rangesSpec: String,
        splitEveryPage: Boolean,
        onDone: (List<PdfDocumentEntity>) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.3f
            PdfEngine.splitPdf(
                sourceFile = File(doc.filePath),
                rangesSpec = rangesSpec,
                splitEveryPage = splitEveryPage,
                outputDir = repository.pdfDir,
                baseName = doc.name
            ).onSuccess { files ->
                val created = files.map { repository.registerPdfFile(it, it.nameWithoutExtension) }
                _isProcessing.value = false
                _progress.value = 1f
                _resultMessage.value = "Created ${created.size} split PDF files"
                onDone(created)
            }.onFailure {
                _isProcessing.value = false
                _resultMessage.value = "Split failed: ${it.message}"
            }
        }
    }

    fun compressPdf(
        doc: PdfDocumentEntity,
        level: PdfCompressionLevel,
        outputName: String,
        onDone: (PdfDocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.05f
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.compressPdf(
                context = getApplication(),
                sourceFile = File(doc.filePath),
                level = level,
                outputFile = outFile,
                onProgress = { cur, tot -> _progress.value = cur.toFloat() / tot.coerceAtLeast(1) }
            ).onSuccess { pages ->
                val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                _isProcessing.value = false
                val savedPct = if (doc.sizeBytes > 0) {
                    (((doc.sizeBytes - saved.sizeBytes).toFloat() / doc.sizeBytes) * 100f).toInt().coerceAtLeast(0)
                } else 0
                _resultMessage.value = "Compressed ${saved.name}.pdf ($savedPct% smaller)"
                onDone(saved)
            }.onFailure {
                _isProcessing.value = false
                _resultMessage.value = "Compression failed: ${it.message}"
            }
        }
    }

    fun extractPages(
        doc: PdfDocumentEntity,
        selectedIndices: List<Int>,
        outputName: String,
        onDone: (PdfDocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.extractPages(File(doc.filePath), selectedIndices, outFile)
                .onSuccess { pages ->
                    val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                    _isProcessing.value = false
                    _resultMessage.value = "Extracted $pages pages to ${saved.name}.pdf"
                    onDone(saved)
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Extract failed: ${it.message}"
                }
        }
    }

    fun rotatePages(
        doc: PdfDocumentEntity,
        rotations: Map<Int, Int>,
        outputName: String,
        onDone: (PdfDocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.rotatePages(File(doc.filePath), rotations, outFile)
                .onSuccess { pages ->
                    val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                    _isProcessing.value = false
                    _resultMessage.value = "Saved rotated PDF: ${saved.name}.pdf"
                    onDone(saved)
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Rotate failed: ${it.message}"
                }
        }
    }

    fun reorderPages(
        doc: PdfDocumentEntity,
        newOrder: List<Int>,
        outputName: String,
        onDone: (PdfDocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.reorderPages(File(doc.filePath), newOrder, outFile)
                .onSuccess { pages ->
                    val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                    _isProcessing.value = false
                    _resultMessage.value = "Saved reordered PDF: ${saved.name}.pdf"
                    onDone(saved)
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Reorder failed: ${it.message}"
                }
        }
    }

    fun deletePages(
        doc: PdfDocumentEntity,
        indicesToDelete: Set<Int>,
        outputName: String,
        onDone: (PdfDocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outFile = repository.createTargetPdfFile(outputName)
            PdfEngine.deletePages(File(doc.filePath), indicesToDelete, outFile)
                .onSuccess { pages ->
                    val saved = repository.registerPdfFile(outFile, outFile.nameWithoutExtension, pages)
                    _isProcessing.value = false
                    _resultMessage.value = "Saved ${saved.name}.pdf ($pages pages remaining)"
                    onDone(saved)
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Delete pages failed: ${it.message}"
                }
        }
    }

    fun exportToImages(doc: PdfDocumentEntity, asPng: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outDir = File(repository.exportDir, "${doc.name}_${System.currentTimeMillis() / 1000}")
            PdfEngine.exportPdfToImages(getApplication(), File(doc.filePath), outDir, doc.name, asPng)
                .onSuccess { files ->
                    _isProcessing.value = false
                    _exportedImages.value = files
                    _resultMessage.value = "Exported ${files.size} page images!"
                }
                .onFailure {
                    _isProcessing.value = false
                    _resultMessage.value = "Export failed: ${it.message}"
                }
        }
    }
}
