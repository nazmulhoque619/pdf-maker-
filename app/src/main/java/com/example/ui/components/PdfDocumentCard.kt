package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PdfDocumentEntity
import com.example.ui.util.formatDate
import com.example.ui.util.formatFileSize
import com.example.ui.util.formatShortDate
import java.io.File

@Composable
fun PdfDocumentListCard(
    document: PdfDocumentEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_list_card_${document.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfThumbnailBox(
                thumbnailPath = document.thumbnailPath,
                isPasswordProtected = document.isPasswordProtected,
                modifier = Modifier
                    .width(58.dp)
                    .height(74.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${document.name}.pdf",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${document.pageCount} pg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatFileSize(document.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${formatShortDate(document.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_btn_${document.id}")
            ) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (document.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (document.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("doc_menu_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Document options"
                    )
                }
                DocumentActionMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onOpen = { menuExpanded = false; onOpen() },
                    onEdit = { menuExpanded = false; onEdit() },
                    onRename = { menuExpanded = false; onRename() },
                    onShare = { menuExpanded = false; onShare() },
                    onSaveToDevice = { menuExpanded = false; onSaveToDevice() },
                    onDuplicate = { menuExpanded = false; onDuplicate() },
                    onShowInfo = { menuExpanded = false; onShowInfo() },
                    onDelete = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
fun PdfDocumentGridCard(
    document: PdfDocumentEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_grid_card_${document.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
            ) {
                PdfThumbnailBox(
                    thumbnailPath = document.thumbnailPath,
                    isPasswordProtected = document.isPasswordProtected,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                ) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (document.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${document.pageCount}p",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${document.name}.pdf",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(document.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Document options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DocumentActionMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onOpen = { menuExpanded = false; onOpen() },
                        onEdit = { menuExpanded = false; onEdit() },
                        onRename = { menuExpanded = false; onRename() },
                        onShare = { menuExpanded = false; onShare() },
                        onSaveToDevice = { menuExpanded = false; onSaveToDevice() },
                        onDuplicate = { menuExpanded = false; onDuplicate() },
                        onShowInfo = { menuExpanded = false; onShowInfo() },
                        onDelete = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfThumbnailBox(
    thumbnailPath: String?,
    isPasswordProtected: Boolean,
    modifier: Modifier = Modifier
) {
    val thumbExists = remember(thumbnailPath) {
        thumbnailPath != null && File(thumbnailPath).exists()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbExists && !isPasswordProtected) {
            AsyncImage(
                model = File(thumbnailPath!!),
                contentDescription = "PDF preview thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPasswordProtected) Icons.Filled.Lock else Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isPasswordProtected) "LOCKED" else "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DocumentActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDuplicate: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Open PDF") },
            leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
            onClick = onOpen
        )
        DropdownMenuItem(
            text = { Text("Edit & Annotate") },
            leadingIcon = { Icon(Icons.Filled.Edit, null) },
            onClick = onEdit
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
            onClick = onRename
        )
        DropdownMenuItem(
            text = { Text("Share PDF") },
            leadingIcon = { Icon(Icons.Filled.Share, null) },
            onClick = onShare
        )
        DropdownMenuItem(
            text = { Text("Save to Device") },
            leadingIcon = { Icon(Icons.Outlined.SaveAlt, null) },
            onClick = onSaveToDevice
        )
        DropdownMenuItem(
            text = { Text("Duplicate") },
            leadingIcon = { Icon(Icons.Filled.FileCopy, null) },
            onClick = onDuplicate
        )
        DropdownMenuItem(
            text = { Text("File Info") },
            leadingIcon = { Icon(Icons.Filled.Info, null) },
            onClick = onShowInfo
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = onDelete
        )
    }
}

@Composable
fun RenamePdfDialog(
    document: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(document.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename PDF") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Document Name") },
                singleLine = true,
                suffix = { Text(".pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_pdf_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeletePdfConfirmDialog(
    document: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete PDF?") },
        text = {
            Text("Are you sure you want to permanently delete \"${document.name}.pdf\" from device storage?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PdfInfoDialog(
    document: PdfDocumentEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Name", "${document.name}.pdf")
                InfoRow("Pages", "${document.pageCount}")
                InfoRow("File Size", formatFileSize(document.sizeBytes))
                InfoRow("Created", formatDate(document.createdAt))
                InfoRow("Last Opened", formatDate(document.lastOpenedAt))
                InfoRow("Protection", if (document.isPasswordProtected) "Password Protected" else "Standard PDF")
                InfoRow("Location", document.filePath)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
