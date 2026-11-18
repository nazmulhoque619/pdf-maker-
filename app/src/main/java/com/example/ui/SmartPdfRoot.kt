package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.PdfToolType
import com.example.ui.i18n.stringsFor
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageEditorScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.PdfCreationSettingsScreen
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolWorkspaceScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.SmartPdfTheme
import com.example.ui.util.createTempCameraImageUri
import com.example.ui.util.formatFileSize
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReaderEditorViewModel
import com.example.ui.viewmodel.ToolsViewModel
import kotlinx.coroutines.launch
import java.io.File

private data class BottomNavTab(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun SmartPdfRoot(
    incomingPdfUri: Uri?,
    onConsumedIncomingUri: () -> Unit,
    mainViewModel: MainViewModel = viewModel(),
    readerEditorViewModel: ReaderEditorViewModel = viewModel(),
    toolsViewModel: ToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
    val allDocuments by mainViewModel.allDocuments.collectAsStateWithLifecycle()
    val recentDocuments by mainViewModel.recentDocuments.collectAsStateWithLifecycle()
    val favoriteDocuments by mainViewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val documentCount by mainViewModel.documentCount.collectAsStateWithLifecycle()
    val filteredFiles by mainViewModel.filteredFiles.collectAsStateWithLifecycle()
    val fileSearchQuery by mainViewModel.fileSearchQuery.collectAsStateWithLifecycle()
    val fileFilterTab by mainViewModel.fileFilterTab.collectAsStateWithLifecycle()
    val fileSortOption by mainViewModel.fileSortOption.collectAsStateWithLifecycle()
    val isGridView by mainViewModel.isGridView.collectAsStateWithLifecycle()
    val selectedImages by mainViewModel.selectedImages.collectAsStateWithLifecycle()
    val creationSettings by mainViewModel.creationSettings.collectAsStateWithLifecycle()
    val isGeneratingPdf by mainViewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val generationProgress by mainViewModel.generationProgress.collectAsStateWithLifecycle()
    val snackbarMessage by mainViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val strings = remember(preferences.language) { stringsFor(preferences.language) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Pending actions state
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var navigateToImagesAfterPick by remember { mutableStateOf(false) }
    var pendingPdfImportCallback by remember { mutableStateOf<((PdfDocumentEntity) -> Unit)?>(null) }
    var pendingSafExportFile by remember { mutableStateOf<File?>(null) }
    var quickPickModeForEditor by remember { mutableStateOf<Boolean?>(null) } // false=Reader, true=Editor

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearSnackbar()
        }
    }

    // Handle external PDF opened via ACTION_VIEW
    LaunchedEffect(incomingPdfUri) {
        incomingPdfUri?.let { uri ->
            mainViewModel.importExternalPdf(uri) { doc ->
                navController.navigate(AppRoute.PdfReader(doc.id))
            }
            onConsumedIncomingUri()
        }
    }

    // Zero-permission Photo Picker (supports JPG, JPEG, PNG, WEBP)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 40)
    ) { uris ->
        if (uris.isNotEmpty()) {
            mainViewModel.addImages(uris)
            if (navigateToImagesAfterPick) {
                navigateToImagesAfterPick = false
                navController.navigate(AppRoute.ImageToPdf) { launchSingleTop = true }
            }
        } else {
            navigateToImagesAfterPick = false
        }
    }

    // System Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            mainViewModel.addImages(listOf(uri))
            navController.navigate(AppRoute.ImageToPdf) { launchSingleTop = true }
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = createTempCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            mainViewModel.showMessage("Camera permission is required to scan documents")
        }
    }

    fun launchCameraScan() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val (uri, _) = createTempCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGalleryPicker(navigateAfter: Boolean = false) {
        navigateToImagesAfterPick = navigateAfter
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // SAF OpenDocument for importing PDFs
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val cb = pendingPdfImportCallback
            pendingPdfImportCallback = null
            mainViewModel.importExternalPdf(it) { doc ->
                cb?.invoke(doc)
            }
        } ?: run { pendingPdfImportCallback = null }
    }

    // SAF CreateDocument for exporting PDFs to Downloads/device storage
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        val srcFile = pendingSafExportFile
        pendingSafExportFile = null
        if (destUri != null && srcFile != null) {
            scope.launch {
                mainViewModel.repository.exportPdfToUri(srcFile, destUri)
                    .onSuccess { mainViewModel.showMessage("Saved ${srcFile.name} to device storage") }
                    .onFailure { mainViewModel.showMessage("Export failed: ${it.message}") }
            }
        }
    }

    fun shareDocument(doc: PdfDocumentEntity) {
        val intent = mainViewModel.repository.buildSharePdfIntent(File(doc.filePath))
        context.startActivity(intent)
    }

    fun saveDocumentToDevice(doc: PdfDocumentEntity) {
        pendingSafExportFile = File(doc.filePath)
        exportPdfLauncher.launch("${doc.name}.pdf")
    }

    SmartPdfTheme(themeMode = preferences.themeMode) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val topLevelTabs = listOf(
            BottomNavTab(AppRoute.Home, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomNavTab(AppRoute.Tools, strings.navTools, Icons.Filled.Build, Icons.Outlined.Build, "nav_tools"),
            BottomNavTab(AppRoute.Files, strings.navFiles, Icons.Filled.Folder, Icons.Outlined.Folder, "nav_files"),
            BottomNavTab(AppRoute.Settings, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
        )

        val showBottomNav = currentDestination?.let { dest ->
            topLevelTabs.any { dest.hasRoute(it.route::class) }
        } ?: true

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (showBottomNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(16.dp))
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomNav && !useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                topLevelTabs.forEach { tab ->
                                    val selected = currentDestination?.hasRoute(tab.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Home
                        ) {
                            composable<AppRoute.Home> {
                                HomeScreen(
                                    strings = strings,
                                    totalPdfCount = documentCount,
                                    recentDocs = recentDocuments,
                                    favoriteDocs = favoriteDocuments,
                                    allDocs = allDocuments,
                                    onCreatePdfClick = {
                                        navController.navigate(AppRoute.ImageToPdf) { launchSingleTop = true }
                                    },
                                    onImageToPdfClick = {
                                        launchGalleryPicker(navigateAfter = true)
                                    },
                                    onCameraScanClick = { launchCameraScan() },
                                    onGalleryImportClick = { launchGalleryPicker(navigateAfter = true) },
                                    onOpenReaderClick = { quickPickModeForEditor = false },
                                    onOpenEditorClick = { quickPickModeForEditor = true },
                                    onOpenDocument = { doc -> navController.navigate(AppRoute.PdfReader(doc.id)) },
                                    onEditDocument = { doc -> navController.navigate(AppRoute.PdfEditor(doc.id)) },
                                    onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                    onShareDocument = { shareDocument(it) },
                                    onRenameDocument = { doc, name -> mainViewModel.renameDocument(doc, name) },
                                    onDuplicateDocument = { mainViewModel.duplicateDocument(it) },
                                    onSaveToDevice = { saveDocumentToDevice(it) },
                                    onDeleteDocument = { mainViewModel.deleteDocument(it) },
                                    onViewAllFiles = {
                                        navController.navigate(AppRoute.Files) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                )
                            }

                            composable<AppRoute.Tools> {
                                ToolsScreen(
                                    strings = strings,
                                    onToolSelected = { tool ->
                                        if (tool == PdfToolType.IMAGE_TO_PDF) {
                                            navController.navigate(AppRoute.ImageToPdf)
                                        } else {
                                            toolsViewModel.clearResult()
                                            navController.navigate(AppRoute.ToolWorkspace(tool.name))
                                        }
                                    },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                )
                            }

                            composable<AppRoute.Files> {
                                FilesScreen(
                                    strings = strings,
                                    documents = filteredFiles,
                                    searchQuery = fileSearchQuery,
                                    activeTab = fileFilterTab,
                                    sortOption = fileSortOption,
                                    isGridView = isGridView,
                                    onSearchChange = { mainViewModel.setFileSearchQuery(it) },
                                    onTabChange = { mainViewModel.setFileFilterTab(it) },
                                    onSortChange = { mainViewModel.setFileSortOption(it) },
                                    onToggleGridView = { mainViewModel.toggleGridView() },
                                    onImportPdfClick = {
                                        pendingPdfImportCallback = null
                                        importPdfLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onOpenDocument = { doc -> navController.navigate(AppRoute.PdfReader(doc.id)) },
                                    onEditDocument = { doc -> navController.navigate(AppRoute.PdfEditor(doc.id)) },
                                    onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                    onShareDocument = { shareDocument(it) },
                                    onRenameDocument = { doc, name -> mainViewModel.renameDocument(doc, name) },
                                    onDuplicateDocument = { mainViewModel.duplicateDocument(it) },
                                    onSaveToDevice = { saveDocumentToDevice(it) },
                                    onDeleteDocument = { mainViewModel.deleteDocument(it) },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                )
                            }

                            composable<AppRoute.Settings> {
                                SettingsScreen(
                                    strings = strings,
                                    preferences = preferences,
                                    onThemeModeChange = { mainViewModel.setThemeMode(it) },
                                    onDefaultPageSizeChange = { mainViewModel.setDefaultPageSize(it) },
                                    onDefaultImageQualityChange = { mainViewModel.setDefaultImageQuality(it) },
                                    onDefaultSaveLocationChange = { mainViewModel.setDefaultSaveLocation(it) },
                                    onLanguageChange = { mainViewModel.setLanguage(it) },
                                    onShareApp = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Check out Smart PDF Maker – Offline PDF Editor, Scanner & Converter!"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Smart PDF Maker"))
                                    },
                                    onShowMessage = { mainViewModel.showMessage(it) },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                )
                            }

                            composable<AppRoute.ImageToPdf> {
                                ImageToPdfScreen(
                                    selectedImages = selectedImages,
                                    onAddFromGallery = { launchGalleryPicker(navigateAfter = false) },
                                    onCaptureFromCamera = { launchCameraScan() },
                                    onRemoveImage = { mainViewModel.removeImage(it) },
                                    onRotateImageQuick = { mainViewModel.rotateImageQuick(it) },
                                    onMoveImage = { from, to -> mainViewModel.moveImage(from, to) },
                                    onEditImage = { imgId ->
                                        navController.navigate(AppRoute.ImageEditor(imgId))
                                    },
                                    onContinueToSettings = {
                                        mainViewModel.prepareNewPdfSettings()
                                        navController.navigate(AppRoute.PdfCreationSettings)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<AppRoute.ImageEditor> { entry ->
                                val args = entry.toRoute<AppRoute.ImageEditor>()
                                val item = selectedImages.firstOrNull { it.id == args.imageId }
                                if (item != null) {
                                    ImageEditorScreen(
                                        imageItem = item,
                                        onSaveConfig = { config -> mainViewModel.updateImageConfig(item.id, config) },
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }

                            composable<AppRoute.PdfCreationSettings> {
                                PdfCreationSettingsScreen(
                                    selectedImages = selectedImages,
                                    settings = creationSettings,
                                    isGenerating = isGeneratingPdf,
                                    generationProgress = generationProgress,
                                    onUpdateSettings = { mainViewModel.updateCreationSettings(it) },
                                    onCreatePdf = {
                                        mainViewModel.createPdfFromSelectedImages { createdDoc ->
                                            navController.navigate(AppRoute.PdfReader(createdDoc.id)) {
                                                popUpTo(AppRoute.Home)
                                            }
                                        }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<AppRoute.PdfReader> { entry ->
                                val args = entry.toRoute<AppRoute.PdfReader>()
                                PdfReaderScreen(
                                    documentId = args.documentId,
                                    viewModel = readerEditorViewModel,
                                    onOpenEditor = { id -> navController.navigate(AppRoute.PdfEditor(id)) },
                                    onSharePdf = { shareDocument(it) },
                                    onPrintPdf = { doc ->
                                        mainViewModel.repository.printPdf(context, File(doc.filePath), doc.name)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<AppRoute.PdfEditor> { entry ->
                                val args = entry.toRoute<AppRoute.PdfEditor>()
                                PdfEditorScreen(
                                    documentId = args.documentId,
                                    viewModel = readerEditorViewModel,
                                    onSaved = { savedDoc ->
                                        navController.navigate(AppRoute.PdfReader(savedDoc.id)) {
                                            popUpTo(AppRoute.PdfEditor(args.documentId)) { inclusive = true }
                                        }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<AppRoute.ToolWorkspace> { entry ->
                                val args = entry.toRoute<AppRoute.ToolWorkspace>()
                                val toolType = PdfToolType.entries.firstOrNull { it.name == args.toolName }
                                    ?: PdfToolType.MERGE
                                ToolWorkspaceScreen(
                                    toolType = toolType,
                                    allDocuments = allDocuments,
                                    toolsViewModel = toolsViewModel,
                                    onImportExternalPdf = { onImported ->
                                        pendingPdfImportCallback = onImported
                                        importPdfLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onOpenPdf = { doc ->
                                        navController.navigate(AppRoute.PdfReader(doc.id))
                                    },
                                    onSharePdf = { shareDocument(it) },
                                    onShareExportedImages = { files ->
                                        context.startActivity(mainViewModel.repository.buildShareImagesIntent(files))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Document Picker Dialog for Home "PDF Reader" / "PDF Editor" cards
        quickPickModeForEditor?.let { forEditor ->
            AlertDialog(
                onDismissRequest = { quickPickModeForEditor = null },
                title = { Text(if (forEditor) "Choose PDF to Edit" else "Choose PDF to Read") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                quickPickModeForEditor = null
                                pendingPdfImportCallback = { imported ->
                                    if (forEditor) navController.navigate(AppRoute.PdfEditor(imported.id))
                                    else navController.navigate(AppRoute.PdfReader(imported.id))
                                }
                                importPdfLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Browse Device Storage (.pdf)")
                        }
                        if (allDocuments.isEmpty()) {
                            Text(
                                text = "No saved PDFs yet. Browse storage or create a PDF first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(modifier = Modifier.height(220.dp)) {
                                items(allDocuments, key = { it.id }) { doc ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                quickPickModeForEditor = null
                                                if (forEditor) navController.navigate(AppRoute.PdfEditor(doc.id))
                                                else navController.navigate(AppRoute.PdfReader(doc.id))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${doc.name}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${doc.pageCount} pages • ${formatFileSize(doc.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { quickPickModeForEditor = null }) { Text("Cancel") }
                }
            )
        }
    }
}
