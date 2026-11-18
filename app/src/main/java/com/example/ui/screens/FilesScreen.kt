package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PdfDocumentEntity
import com.example.domain.model.FileFilterTab
import com.example.domain.model.FileSortOption
import com.example.ui.components.DeletePdfConfirmDialog
import com.example.ui.components.PdfDocumentGridCard
import com.example.ui.components.PdfDocumentListCard
import com.example.ui.components.PdfInfoDialog
import com.example.ui.components.RenamePdfDialog
import com.example.ui.i18n.AppStrings

@Composable
fun FilesScreen(
    strings: AppStrings,
    documents: List<PdfDocumentEntity>,
    searchQuery: String,
    activeTab: FileFilterTab,
    sortOption: FileSortOption,
    isGridView: Boolean,
    onSearchChange: (String) -> Unit,
    onTabChange: (FileFilterTab) -> Unit,
    onSortChange: (FileSortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onImportPdfClick: () -> Unit,
    onOpenDocument: (PdfDocumentEntity) -> Unit,
    onEditDocument: (PdfDocumentEntity) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onShareDocument: (PdfDocumentEntity) -> Unit,
    onRenameDocument: (PdfDocumentEntity, String) -> Unit,
    onDuplicateDocument: (PdfDocumentEntity) -> Unit,
    onSaveToDevice: (PdfDocumentEntity) -> Unit,
    onDeleteDocument: (PdfDocumentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var docToRename by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var docToDelete by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var docForInfo by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.navFiles,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${documents.size} PDF documents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onImportPdfClick,
                    modifier = Modifier.testTag("import_pdf_button")
                ) {
                    Icon(Icons.Filled.FileOpen, null, modifier = Modifier.padding(end = 6.dp))
                    Text("Import")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onToggleGridView,
                    modifier = Modifier.testTag("toggle_grid_list_btn")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view"
                    )
                }
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("sort_files_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort PDFs")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        FileSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = {
                                    RadioButton(
                                        selected = sortOption == option,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(strings.searchDocuments) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("files_search_input")
        )

        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FileFilterTab.entries.forEach { tab ->
                val label = when (tab) {
                    FileFilterTab.ALL -> strings.allFiles
                    FileFilterTab.RECENT -> "Recent"
                    FileFilterTab.FAVORITES -> strings.favorites
                }
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { onTabChange(tab) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_docs),
                        contentDescription = "Empty documents list",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(170.dp)
                            .height(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No PDF files found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Create a PDF from images or import one from your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(documents, key = { it.id }) { doc ->
                    PdfDocumentGridCard(
                        document = doc,
                        onOpen = { onOpenDocument(doc) },
                        onEdit = { onEditDocument(doc) },
                        onToggleFavorite = { onToggleFavorite(doc) },
                        onShare = { onShareDocument(doc) },
                        onRename = { docToRename = doc },
                        onDuplicate = { onDuplicateDocument(doc) },
                        onSaveToDevice = { onSaveToDevice(doc) },
                        onShowInfo = { docForInfo = doc },
                        onDelete = { docToDelete = doc }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(documents, key = { it.id }) { doc ->
                    PdfDocumentListCard(
                        document = doc,
                        onOpen = { onOpenDocument(doc) },
                        onEdit = { onEditDocument(doc) },
                        onToggleFavorite = { onToggleFavorite(doc) },
                        onShare = { onShareDocument(doc) },
                        onRename = { docToRename = doc },
                        onDuplicate = { onDuplicateDocument(doc) },
                        onSaveToDevice = { onSaveToDevice(doc) },
                        onShowInfo = { docForInfo = doc },
                        onDelete = { docToDelete = doc }
                    )
                }
            }
        }
    }

    docToRename?.let { target ->
        RenamePdfDialog(
            document = target,
            onDismiss = { docToRename = null },
            onConfirm = { newName ->
                onRenameDocument(target, newName)
                docToRename = null
            }
        )
    }

    docToDelete?.let { target ->
        DeletePdfConfirmDialog(
            document = target,
            onDismiss = { docToDelete = null },
            onConfirm = {
                onDeleteDocument(target)
                docToDelete = null
            }
        )
    }

    docForInfo?.let { target ->
        PdfInfoDialog(
            document = target,
            onDismiss = { docForInfo = null }
        )
    }
}
