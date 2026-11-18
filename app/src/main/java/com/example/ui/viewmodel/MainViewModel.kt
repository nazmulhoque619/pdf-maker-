package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartPdfApp
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.FileFilterTab
import com.example.domain.model.FileSortOption
import com.example.domain.model.ImageEditConfig
import com.example.domain.model.ImageQualityPreset
import com.example.domain.model.PdfCreationSettings
import com.example.domain.model.PdfPageSize
import com.example.domain.model.SelectedImageItem
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import com.example.domain.pdf.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmartPdfApp
    val repository = app.pdfRepository
    private val settingsStore = app.settingsDataStore

    val preferences: StateFlow<UserPreferences> = settingsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<PdfDocumentEntity>> = repository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<PdfDocumentEntity>> = repository.favoriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentCount: StateFlow<Int> = repository.documentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Files Screen Filter/Sort/View state
    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    private val _fileFilterTab = MutableStateFlow(FileFilterTab.ALL)
    val fileFilterTab: StateFlow<FileFilterTab> = _fileFilterTab.asStateFlow()

    private val _fileSortOption = MutableStateFlow(FileSortOption.DATE_DESC)
    val fileSortOption: StateFlow<FileSortOption> = _fileSortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val filteredFiles: StateFlow<List<PdfDocumentEntity>> = combine(
        allDocuments,
        recentDocuments,
        favoriteDocuments,
        _fileFilterTab,
        _fileSearchQuery,
        _fileSortOption
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val all = arr[0] as List<PdfDocumentEntity>
        @Suppress("UNCHECKED_CAST")
        val recent = arr[1] as List<PdfDocumentEntity>
        @Suppress("UNCHECKED_CAST")
        val favs = arr[2] as List<PdfDocumentEntity>
        val tab = arr[3] as FileFilterTab
        val query = (arr[4] as String).trim()
        val sort = arr[5] as FileSortOption

        val base = when (tab) {
            FileFilterTab.ALL -> all
            FileFilterTab.RECENT -> recent
            FileFilterTab.FAVORITES -> favs
        }
        val searched = if (query.isEmpty()) base else {
            base.filter { it.name.contains(query, ignoreCase = true) }
        }
        when (sort) {
            FileSortOption.DATE_DESC -> searched.sortedByDescending { it.createdAt }
            FileSortOption.DATE_ASC -> searched.sortedBy { it.createdAt }
            FileSortOption.NAME_ASC -> searched.sortedBy { it.name.lowercase() }
            FileSortOption.NAME_DESC -> searched.sortedByDescending { it.name.lowercase() }
            FileSortOption.SIZE_DESC -> searched.sortedByDescending { it.sizeBytes }
            FileSortOption.SIZE_ASC -> searched.sortedBy { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Image to PDF workspace state
    private val _selectedImages = MutableStateFlow<List<SelectedImageItem>>(emptyList())
    val selectedImages: StateFlow<List<SelectedImageItem>> = _selectedImages.asStateFlow()

    private val _creationSettings = MutableStateFlow(PdfCreationSettings())
    val creationSettings: StateFlow<PdfCreationSettings> = _creationSettings.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = settingsStore.preferencesFlow.first()
            _creationSettings.update {
                it.copy(
                    pageSize = prefs.defaultPageSize,
                    imageQuality = prefs.defaultImageQuality
                )
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    fun setFileFilterTab(tab: FileFilterTab) {
        _fileFilterTab.value = tab
    }

    fun setFileSortOption(sort: FileSortOption) {
        _fileSortOption.value = sort
    }

    fun toggleGridView() {
        _isGridView.update { !it }
    }

    fun addImages(uris: List<Uri>, replaceExisting: Boolean = false) {
        if (uris.isEmpty()) return
        val newItems = uris.map { SelectedImageItem(uri = it) }
        _selectedImages.update { current ->
            if (replaceExisting) newItems else current + newItems
        }
    }

    fun removeImage(id: String) {
        _selectedImages.update { list -> list.filterNot { it.id == id } }
    }

    fun clearSelectedImages() {
        _selectedImages.value = emptyList()
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        _selectedImages.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update list
            }
            list.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
        }
    }

    fun rotateImageQuick(id: String) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    val nextRot = (item.editConfig.rotationDegrees + 90) % 360
                    item.copy(editConfig = item.editConfig.copy(rotationDegrees = nextRot))
                } else item
            }
        }
    }

    fun updateImageConfig(id: String, config: ImageEditConfig) {
        _selectedImages.update { list ->
            list.map { item -> if (item.id == id) item.copy(editConfig = config) else item }
        }
    }

    fun prepareNewPdfSettings() {
        val prefs = preferences.value
        _creationSettings.update {
            it.copy(
                fileName = "SmartPDF_${System.currentTimeMillis() / 1000}",
                pageSize = prefs.defaultPageSize,
                imageQuality = prefs.defaultImageQuality
            )
        }
    }

    fun updateCreationSettings(transform: (PdfCreationSettings) -> PdfCreationSettings) {
        _creationSettings.update(transform)
    }

    fun createPdfFromSelectedImages(onSuccess: (PdfDocumentEntity) -> Unit) {
        val images = _selectedImages.value
        if (images.isEmpty()) {
            showMessage("Please select at least one image")
            return
        }
        val settings = _creationSettings.value
        if (settings.usePassword && settings.password.isBlank()) {
            showMessage("Enter a password or turn off password protection")
            return
        }

        viewModelScope.launch {
            _isGeneratingPdf.value = true
            _generationProgress.value = 0.05f
            val targetFile = repository.createTargetPdfFile(settings.fileName)
            val result = PdfEngine.createPdfFromImages(
                context = getApplication(),
                images = images,
                settings = settings,
                outputFile = targetFile,
                onProgress = { current, total ->
                    _generationProgress.value = (current.toFloat() / total.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                }
            )
            result.onSuccess { pages ->
                val doc = repository.registerPdfFile(
                    file = targetFile,
                    displayName = targetFile.nameWithoutExtension,
                    pageCount = pages,
                    isPasswordProtected = settings.usePassword && settings.password.isNotBlank()
                )
                _generationProgress.value = 1f
                _isGeneratingPdf.value = false
                _selectedImages.value = emptyList()
                showMessage("PDF created: ${doc.name}.pdf")
                onSuccess(doc)
            }.onFailure { err ->
                _isGeneratingPdf.value = false
                _generationProgress.value = 0f
                showMessage("Failed to create PDF: ${err.message ?: "Unknown error"}")
            }
        }
    }

    fun importExternalPdf(uri: Uri, onImported: ((PdfDocumentEntity) -> Unit)? = null) {
        viewModelScope.launch {
            repository.importExternalPdf(uri)
                .onSuccess { doc ->
                    showMessage("Imported ${doc.name}.pdf")
                    onImported?.invoke(doc)
                }
                .onFailure { err ->
                    showMessage("Import failed: ${err.message ?: "Invalid PDF"}")
                }
        }
    }

    fun toggleFavorite(doc: PdfDocumentEntity) {
        viewModelScope.launch { repository.toggleFavorite(doc) }
    }

    fun renameDocument(doc: PdfDocumentEntity, newName: String) {
        viewModelScope.launch {
            repository.renameDocument(doc, newName)
                .onSuccess { showMessage("Renamed to $newName") }
                .onFailure { showMessage(it.message ?: "Rename failed") }
        }
    }

    fun duplicateDocument(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.duplicateDocument(doc)
                .onSuccess { copy -> showMessage("Duplicated as ${copy.name}.pdf") }
                .onFailure { showMessage(it.message ?: "Duplicate failed") }
        }
    }

    fun deleteDocument(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            showMessage("Deleted ${doc.name}.pdf")
        }
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDefaultPageSize(size: PdfPageSize) {
        viewModelScope.launch { settingsStore.setDefaultPageSize(size) }
    }

    fun setDefaultImageQuality(quality: ImageQualityPreset) {
        viewModelScope.launch { settingsStore.setDefaultImageQuality(quality) }
    }

    fun setDefaultSaveLocation(location: String) {
        viewModelScope.launch { settingsStore.setDefaultSaveLocation(location) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsStore.setLanguage(language) }
    }
}
