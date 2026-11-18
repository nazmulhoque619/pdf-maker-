package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.BookmarkEntity
import com.example.data.local.PdfDao
import com.example.data.local.PdfDocumentEntity
import com.example.domain.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class PdfRepository(
    private val context: Context,
    private val pdfDao: PdfDao
) {
    val pdfDir: File = File(context.filesDir, "pdfs").apply { mkdirs() }
    val thumbDir: File = File(context.filesDir, "thumbnails").apply { mkdirs() }
    val exportDir: File = File(context.filesDir, "exports").apply { mkdirs() }

    val allDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.observeAllDocuments()
    val recentDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.observeRecentDocuments()
    val favoriteDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.observeFavoriteDocuments()
    val documentCount: Flow<Int> = pdfDao.observeDocumentCount()

    fun observeBookmarks(documentId: Long): Flow<List<BookmarkEntity>> =
        pdfDao.observeBookmarks(documentId)

    suspend fun getDocumentById(id: Long): PdfDocumentEntity? = pdfDao.getDocumentById(id)

    fun createTargetPdfFile(rawName: String): File {
        val sanitized = rawName
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .ifBlank { "Document_${System.currentTimeMillis() / 1000}" }
            .removeSuffix(".pdf")
        var candidate = File(pdfDir, "$sanitized.pdf")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(pdfDir, "${sanitized}_$counter.pdf")
            counter++
        }
        return candidate
    }

    suspend fun registerPdfFile(
        file: File,
        displayName: String = file.nameWithoutExtension,
        pageCount: Int? = null,
        isPasswordProtected: Boolean = false
    ): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val (detectedPages, encrypted) = if (pageCount == null) {
            PdfEngine.inspectPdfInfo(file)
        } else {
            pageCount to isPasswordProtected
        }
        val thumbFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")
        val thumbOk = if (!encrypted) {
            PdfEngine.generateThumbnail(context, file, thumbFile)
        } else {
            false
        }
        val entity = PdfDocumentEntity(
            name = displayName.removeSuffix(".pdf"),
            filePath = file.absolutePath,
            thumbnailPath = if (thumbOk) thumbFile.absolutePath else null,
            sizeBytes = file.length(),
            pageCount = detectedPages.coerceAtLeast(1),
            isPasswordProtected = encrypted
        )
        val newId = pdfDao.insertDocument(entity)
        entity.copy(id = newId)
    }

    suspend fun refreshExistingDocumentMetadata(
        document: PdfDocumentEntity,
        password: String? = null
    ): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val file = File(document.filePath)
        val (pages, encrypted) = PdfEngine.inspectPdfInfo(file, password)
        val thumbFile = document.thumbnailPath?.let { File(it) }
            ?: File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")
        val thumbOk = PdfEngine.generateThumbnail(context, file, thumbFile, password)
        val updated = document.copy(
            sizeBytes = file.length(),
            pageCount = pages.coerceAtLeast(1),
            isPasswordProtected = encrypted || document.isPasswordProtected,
            thumbnailPath = if (thumbOk) thumbFile.absolutePath else document.thumbnailPath,
            lastOpenedAt = System.currentTimeMillis()
        )
        pdfDao.updateDocument(updated)
        updated
    }

    suspend fun importExternalPdf(uri: Uri): Result<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedName = resolveFileName(uri) ?: "Imported_${System.currentTimeMillis() / 1000}"
            val targetFile = createTargetPdfFile(resolvedName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Cannot read selected PDF file")
            registerPdfFile(targetFile, targetFile.nameWithoutExtension)
        }
    }

    suspend fun exportPdfToUri(sourceFile: File, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: error("Cannot write to destination")
            Unit
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()
    }

    suspend fun markOpened(id: Long) {
        pdfDao.updateLastOpened(id)
    }

    suspend fun toggleFavorite(doc: PdfDocumentEntity) {
        pdfDao.setFavorite(doc.id, !doc.isFavorite)
    }

    suspend fun renameDocument(doc: PdfDocumentEntity, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanName = newName.trim().removeSuffix(".pdf")
                require(cleanName.isNotBlank()) { "File name cannot be empty" }
                val oldFile = File(doc.filePath)
                val targetFile = createTargetPdfFile(cleanName)
                if (oldFile.exists()) {
                    oldFile.copyTo(targetFile, overwrite = true)
                    oldFile.delete()
                }
                pdfDao.renameDocument(doc.id, cleanName, targetFile.absolutePath)
            }
        }

    suspend fun duplicateDocument(doc: PdfDocumentEntity): Result<PdfDocumentEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = File(doc.filePath)
                require(src.exists()) { "Source file no longer exists" }
                val copyFile = createTargetPdfFile("${doc.name}_copy")
                src.copyTo(copyFile, overwrite = true)
                registerPdfFile(
                    file = copyFile,
                    displayName = copyFile.nameWithoutExtension,
                    pageCount = doc.pageCount,
                    isPasswordProtected = doc.isPasswordProtected
                )
            }
        }

    suspend fun deleteDocument(doc: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        runCatching { File(doc.filePath).delete() }
        doc.thumbnailPath?.let { path -> runCatching { File(path).delete() } }
        pdfDao.deleteDocument(doc)
    }

    suspend fun toggleBookmark(documentId: Long, pageIndex: Int, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) {
            pdfDao.removeBookmark(documentId, pageIndex)
        } else {
            pdfDao.insertBookmark(
                BookmarkEntity(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    label = "Page ${pageIndex + 1}"
                )
            )
        }
    }

    fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun buildSharePdfIntent(file: File): Intent {
        val uri = getShareUri(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share PDF") }
    }

    fun buildShareImagesIntent(files: List<File>): Intent {
        val uris = ArrayList(files.map { getShareUri(it) })
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share Exported Pages") }
    }

    fun printPdf(activityContext: Context, file: File, jobName: String) {
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true || destination == null) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }
}
