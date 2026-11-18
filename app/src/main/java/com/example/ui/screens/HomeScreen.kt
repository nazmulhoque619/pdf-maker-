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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.DeletePdfConfirmDialog
import com.example.ui.components.PdfDocumentListCard
import com.example.ui.components.PdfInfoDialog
import com.example.ui.components.PdfThumbnailBox
import com.example.ui.components.RenamePdfDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.util.formatFileSize

@Composable
fun HomeScreen(
    strings: AppStrings,
    totalPdfCount: Int,
    recentDocs: List<PdfDocumentEntity>,
    favoriteDocs: List<PdfDocumentEntity>,
    allDocs: List<PdfDocumentEntity>,
    onCreatePdfClick: () -> Unit,
    onImageToPdfClick: () -> Unit,
    onCameraScanClick: () -> Unit,
    onGalleryImportClick: () -> Unit,
    onOpenReaderClick: () -> Unit,
    onOpenEditorClick: () -> Unit,
    onOpenDocument: (PdfDocumentEntity) -> Unit,
    onEditDocument: (PdfDocumentEntity) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onShareDocument: (PdfDocumentEntity) -> Unit,
    onRenameDocument: (PdfDocumentEntity, String) -> Unit,
    onDuplicateDocument: (PdfDocumentEntity) -> Unit,
    onSaveToDevice: (PdfDocumentEntity) -> Unit,
    onDeleteDocument: (PdfDocumentEntity) -> Unit,
    onViewAllFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var docToRename by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var docToDelete by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var docForInfo by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    val displayedDocs = remember(searchQuery, recentDocs, allDocs) {
        if (searchQuery.isBlank()) recentDocs
        else allDocs.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header with App Logo, Title & Total PDFs Counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "Smart PDF Maker logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.appSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("total_pdf_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$totalPdfCount ${strings.totalPdfs}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search Documents Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchDocuments) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input")
            )
        }

        // Stylized Hero Banner Card
        if (searchQuery.isBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "PDF workspace visual banner",
                            contentScale = ContentScale.Crop,
                            alpha = 0.28f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "100% OFFLINE STUDIO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Scan, Edit & Convert in Seconds",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = onCreatePdfClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hero_create_pdf_btn")
                                    ) {
                                        Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.createPdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(
                                        onClick = onCameraScanClick,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.14f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hero_camera_scan_btn")
                                    ) {
                                        Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.cameraScanner, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6 Main Quick Feature Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickFeatureCard(
                            title = strings.createPdf,
                            subtitle = "Build new PDF",
                            icon = Icons.Filled.AddCircle,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onCreatePdfClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_create_pdf")
                        )
                        QuickFeatureCard(
                            title = strings.imageToPdf,
                            subtitle = "JPG, PNG, WEBP",
                            icon = Icons.Filled.Image,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onImageToPdfClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_image_to_pdf")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickFeatureCard(
                            title = strings.cameraScanner,
                            subtitle = "Capture & filter",
                            icon = Icons.Filled.CameraAlt,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onCameraScanClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_camera_scanner")
                        )
                        QuickFeatureCard(
                            title = strings.galleryImport,
                            subtitle = "Multi-select",
                            icon = Icons.Filled.PhotoLibrary,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onGalleryImportClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_gallery_import")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickFeatureCard(
                            title = strings.pdfReader,
                            subtitle = "Zoom, search, print",
                            icon = Icons.Filled.MenuBook,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = onOpenReaderClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_pdf_reader")
                        )
                        QuickFeatureCard(
                            title = strings.pdfEditor,
                            subtitle = "Sign & annotate",
                            icon = Icons.Filled.EditNote,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenEditorClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_pdf_editor")
                        )
                    }
                }
            }
        }

        // Favorites Carousel
        if (favoriteDocs.isNotEmpty() && searchQuery.isBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.favorites,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = onViewAllFiles) {
                            Text("See All")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteDocs, key = { "fav_${it.id}" }) { doc ->
                            FavoriteMiniCard(
                                doc = doc,
                                onClick = { onOpenDocument(doc) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Documents Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) strings.recentDocuments else "Search Results (${displayedDocs.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onViewAllFiles) {
                    Text("View All")
                }
            }
        }

        if (displayedDocs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_docs),
                            contentDescription = "No documents yet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(150.dp)
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No PDFs created yet" else "No matching documents",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap Create PDF or Camera Scan to build your first offline document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayedDocs, key = { "recent_${it.id}" }) { doc ->
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

@Composable
private fun QuickFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FavoriteMiniCard(
    doc: PdfDocumentEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            PdfThumbnailBox(
                thumbnailPath = doc.thumbnailPath,
                isPasswordProtected = doc.isPasswordProtected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = doc.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${doc.pageCount} pg • ${formatFileSize(doc.sizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
