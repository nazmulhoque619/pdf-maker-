package com.example.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.domain.image.ImageFilterEngine
import com.example.domain.model.ImageScaleMode
import com.example.domain.model.PageBackgroundPreset
import com.example.domain.model.PageOrientation
import com.example.domain.model.PdfAnnotationItem
import com.example.domain.model.PdfCompressionLevel
import com.example.domain.model.PdfCreationSettings
import com.example.domain.model.PdfPageSize
import com.example.domain.model.SelectedImageItem
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object PdfEngine {

    private fun resolvePageSize(settings: PdfCreationSettings): PDRectangle {
        val baseW: Float
        val baseH: Float
        if (settings.pageSize == PdfPageSize.CUSTOM) {
            baseW = (settings.customWidthMm.coerceIn(50f, 1200f)) * 2.83465f
            baseH = (settings.customHeightMm.coerceIn(50f, 1200f)) * 2.83465f
        } else {
            baseW = settings.pageSize.widthPt
            baseH = settings.pageSize.heightPt
        }
        return if (settings.orientation == PageOrientation.LANDSCAPE) {
            PDRectangle(max(baseW, baseH), min(baseW, baseH))
        } else {
            PDRectangle(min(baseW, baseH), max(baseW, baseH))
        }
    }

    private fun gridDimensions(imagesPerPage: Int, isLandscape: Boolean): Pair<Int, Int> {
        return when (imagesPerPage) {
            2 -> if (isLandscape) 2 to 1 else 1 to 2
            4 -> 2 to 2
            6 -> if (isLandscape) 3 to 2 else 2 to 3
            else -> 1 to 1
        }
    }

    suspend fun createPdfFromImages(
        context: Context,
        images: List<SelectedImageItem>,
        settings: PdfCreationSettings,
        outputFile: File,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(images.isNotEmpty()) { "Select at least one image" }
            outputFile.parentFile?.mkdirs()

            val pageRect = resolvePageSize(settings)
            val isLandscape = pageRect.width > pageRect.height
            val (cols, rows) = gridDimensions(settings.imagesPerPage, isLandscape)
            val perPage = cols * rows
            val marginPt = settings.marginDp.coerceAtLeast(0f) * 0.75f
            val spacingPt = settings.pageSpacingDp.coerceAtLeast(0f) * 0.75f

            val bgColor = when (settings.backgroundPreset) {
                PageBackgroundPreset.CUSTOM -> settings.customBackgroundColorArgb
                else -> settings.backgroundPreset.argb
            }
            val bgR = android.graphics.Color.red(bgColor) / 255f
            val bgG = android.graphics.Color.green(bgColor) / 255f
            val bgB = android.graphics.Color.blue(bgColor) / 255f

            val maxDecodeDim = when (settings.compressionLevel) {
                PdfCompressionLevel.NONE -> 2200
                PdfCompressionLevel.LOW -> 1800
                PdfCompressionLevel.MEDIUM -> 1400
                PdfCompressionLevel.HIGH -> 1000
            }
            val effectiveQuality = (min(
                settings.imageQuality.jpegQuality,
                settings.compressionLevel.jpegQuality
            ) / 100f).coerceIn(0.35f, 1.0f)

            val document = PDDocument()
            try {
                val chunks = images.chunked(perPage)
                var processed = 0
                for (chunk in chunks) {
                    val page = PDPage(pageRect)
                    document.addPage(page)

                    PDPageContentStream(document, page).use { cs ->
                        // Page background
                        cs.setNonStrokingColor(bgR, bgG, bgB)
                        cs.addRect(0f, 0f, pageRect.width, pageRect.height)
                        cs.fill()

                        val usableW = (pageRect.width - 2f * marginPt).coerceAtLeast(20f)
                        val usableH = (pageRect.height - 2f * marginPt).coerceAtLeast(20f)
                        val cellW = (usableW - spacingPt * (cols - 1)).coerceAtLeast(10f) / cols
                        val cellH = (usableH - spacingPt * (rows - 1)).coerceAtLeast(10f) / rows

                        chunk.forEachIndexed { idx, item ->
                            val rawBitmap = ImageFilterEngine.decodeSampledBitmap(
                                context = context,
                                uri = item.uri,
                                maxDimension = maxDecodeDim
                            ) ?: return@forEachIndexed

                            val editedBitmap = ImageFilterEngine.applyEdits(rawBitmap, item.editConfig)
                            if (editedBitmap !== rawBitmap) rawBitmap.recycle()

                            val pdImage = if (settings.compressionLevel == PdfCompressionLevel.NONE &&
                                settings.imageQuality.jpegQuality >= 95
                            ) {
                                LosslessFactory.createFromImage(document, editedBitmap)
                            } else {
                                JPEGFactory.createFromImage(document, editedBitmap, effectiveQuality)
                            }

                            val col = idx % cols
                            val row = idx / cols
                            val cellX = marginPt + col * (cellW + spacingPt)
                            // PDF origin is bottom-left
                            val cellY = pageRect.height - marginPt - (row + 1) * cellH - row * spacingPt

                            val imgW = editedBitmap.width.toFloat().coerceAtLeast(1f)
                            val imgH = editedBitmap.height.toFloat().coerceAtLeast(1f)
                            val (drawW, drawH) = computeDrawDimensions(
                                imgW = imgW,
                                imgH = imgH,
                                cellW = cellW,
                                cellH = cellH,
                                scaleMode = settings.scaleMode
                            )
                            val drawX = cellX + (cellW - drawW) / 2f
                            val drawY = cellY + (cellH - drawH) / 2f

                            cs.saveGraphicsState()
                            cs.addRect(cellX, cellY, cellW, cellH)
                            cs.clip()
                            cs.drawImage(pdImage, drawX, drawY, drawW, drawH)
                            cs.restoreGraphicsState()

                            editedBitmap.recycle()
                            processed++
                            onProgress(processed, images.size)
                        }
                    }
                }

                if (settings.usePassword && settings.password.isNotBlank()) {
                    val ap = AccessPermission()
                    val policy = StandardProtectionPolicy(settings.password, settings.password, ap).apply {
                        encryptionKeyLength = 128
                        permissions = ap
                    }
                    document.protect(policy)
                }

                FileOutputStream(outputFile).use { fos ->
                    document.save(fos)
                }
                document.numberOfPages
            } finally {
                document.close()
            }
        }
    }

    suspend fun renderLayoutPreview(
        context: Context,
        images: List<SelectedImageItem>,
        settings: PdfCreationSettings
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val pageRect = resolvePageSize(settings)
            val aspect = pageRect.width / pageRect.height
            val previewH = 960
            val previewW = (previewH * aspect).roundToInt().coerceIn(360, 1200)
            val scale = previewW / pageRect.width

            val bitmap = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgColor = when (settings.backgroundPreset) {
                PageBackgroundPreset.CUSTOM -> settings.customBackgroundColorArgb
                else -> settings.backgroundPreset.argb
            }
            canvas.drawColor(bgColor)

            val isLandscape = pageRect.width > pageRect.height
            val (cols, rows) = gridDimensions(settings.imagesPerPage, isLandscape)
            val perPage = cols * rows
            val marginPx = settings.marginDp.coerceAtLeast(0f) * 0.75f * scale
            val spacingPx = settings.pageSpacingDp.coerceAtLeast(0f) * 0.75f * scale

            val usableW = (previewW - 2f * marginPx).coerceAtLeast(20f)
            val usableH = (previewH - 2f * marginPx).coerceAtLeast(20f)
            val cellW = (usableW - spacingPx * (cols - 1)).coerceAtLeast(10f) / cols
            val cellH = (usableH - spacingPx * (rows - 1)).coerceAtLeast(10f) / rows

            images.take(perPage).forEachIndexed { idx, item ->
                val raw = ImageFilterEngine.decodeSampledBitmap(context, item.uri, 640)
                    ?: return@forEachIndexed
                val edited = ImageFilterEngine.applyEdits(raw, item.editConfig)
                if (edited !== raw) raw.recycle()

                val col = idx % cols
                val row = idx / cols
                val cellX = marginPx + col * (cellW + spacingPx)
                val cellY = marginPx + row * (cellH + spacingPx)

                val (drawW, drawH) = computeDrawDimensions(
                    imgW = edited.width.toFloat(),
                    imgH = edited.height.toFloat(),
                    cellW = cellW,
                    cellH = cellH,
                    scaleMode = settings.scaleMode
                )
                val drawX = cellX + (cellW - drawW) / 2f
                val drawY = cellY + (cellH - drawH) / 2f

                canvas.save()
                canvas.clipRect(cellX, cellY, cellX + cellW, cellY + cellH)
                val destRect = android.graphics.RectF(drawX, drawY, drawX + drawW, drawY + drawH)
                canvas.drawBitmap(edited, null, destRect, null)
                canvas.restore()
                edited.recycle()
            }
            bitmap
        }.getOrNull()
    }

    private fun computeDrawDimensions(
        imgW: Float,
        imgH: Float,
        cellW: Float,
        cellH: Float,
        scaleMode: ImageScaleMode
    ): Pair<Float, Float> {
        val ratio = imgW / imgH
        return when (scaleMode) {
            ImageScaleMode.FIT_TO_PAGE -> {
                val scale = min(cellW / imgW, cellH / imgH)
                (imgW * scale) to (imgH * scale)
            }
            ImageScaleMode.FIT_TO_WIDTH -> {
                cellW to (cellW / ratio)
            }
            ImageScaleMode.FIT_TO_HEIGHT -> {
                (cellH * ratio) to cellH
            }
            ImageScaleMode.ORIGINAL_RATIO -> {
                val scale = min(1f, min(cellW / imgW, cellH / imgH))
                (imgW * scale) to (imgH * scale)
            }
        }
    }

    suspend fun inspectPdfInfo(file: File, password: String? = null): Pair<Int, Boolean> =
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(file, password ?: "").use { doc ->
                    doc.numberOfPages to doc.isEncrypted
                }
            } catch (e: Exception) {
                // Check if encrypted
                val encrypted = e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("decrypt", ignoreCase = true) == true
                0 to encrypted
            }
        }

    suspend fun generateThumbnail(
        context: Context,
        pdfFile: File,
        thumbOutFile: File,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            thumbOutFile.parentFile?.mkdirs()
            val bmp = renderPageBitmap(context, pdfFile, 0, targetWidthPx = 420, password = password)
                ?: return@runCatching false
            FileOutputStream(thumbOutFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            bmp.recycle()
            true
        }.getOrDefault(false)
    }

    suspend fun getDecryptedWorkingFile(
        context: Context,
        pdfFile: File,
        password: String?
    ): File? = withContext(Dispatchers.IO) {
        if (password.isNullOrEmpty()) return@withContext pdfFile
        runCatching {
            val cacheFile = File(context.cacheDir, "unlocked_${pdfFile.name.hashCode()}.pdf")
            PDDocument.load(pdfFile, password).use { doc ->
                doc.isAllSecurityToBeRemoved = true
                doc.save(cacheFile)
            }
            cacheFile
        }.getOrNull()
    }

    suspend fun renderPageBitmap(
        context: Context,
        pdfFile: File,
        pageIndex: Int,
        targetWidthPx: Int = 1080,
        password: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val workingFile = if (!password.isNullOrEmpty()) {
                getDecryptedWorkingFile(context, pdfFile, password) ?: pdfFile
            } else {
                pdfFile
            }
            ParcelFileDescriptor.open(workingFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex !in 0 until renderer.pageCount) return@runCatching null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / max(1, page.width)
                        val w = targetWidthPx.coerceIn(120, 2200)
                        val h = (w * ratio).roundToInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }

    suspend fun getPageCount(
        context: Context,
        pdfFile: File,
        password: String? = null
    ): Int = withContext(Dispatchers.IO) {
        runCatching {
            val working = if (!password.isNullOrEmpty()) {
                getDecryptedWorkingFile(context, pdfFile, password) ?: pdfFile
            } else pdfFile
            ParcelFileDescriptor.open(working, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            }
        }.getOrElse {
            runCatching {
                PDDocument.load(pdfFile, password ?: "").use { it.numberOfPages }
            }.getOrDefault(0)
        }
    }

    suspend fun extractTextPerPage(
        pdfFile: File,
        password: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            PDDocument.load(pdfFile, password ?: "").use { doc ->
                val count = doc.numberOfPages
                val stripper = PDFTextStripper()
                (1..count).map { pageNum ->
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    stripper.getText(doc).orEmpty()
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun mergePdfs(
        sourceFiles: List<File>,
        outputFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(sourceFiles.size >= 2) { "Select at least 2 PDFs to merge" }
            outputFile.parentFile?.mkdirs()
            val merger = PDFMergerUtility()
            sourceFiles.forEach { merger.addSource(it) }
            merger.destinationFileName = outputFile.absolutePath
            merger.mergeDocuments(null)
            PDDocument.load(outputFile).use { it.numberOfPages }
        }
    }

    suspend fun splitPdf(
        sourceFile: File,
        rangesSpec: String,
        splitEveryPage: Boolean,
        outputDir: File,
        baseName: String
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        runCatching {
            outputDir.mkdirs()
            val outFiles = mutableListOf<File>()
            PDDocument.load(sourceFile).use { srcDoc ->
                val total = srcDoc.numberOfPages
                val groups: List<List<Int>> = if (splitEveryPage) {
                    (0 until total).map { listOf(it) }
                } else {
                    parsePageRangeGroups(rangesSpec, total)
                }
                require(groups.isNotEmpty()) { "Enter valid page ranges (e.g. 1-2, 3-4)" }

                groups.forEachIndexed { idx, pageIndices ->
                    val destFile = File(outputDir, "${baseName}_part${idx + 1}.pdf")
                    PDDocument().use { outDoc ->
                        pageIndices.forEach { pIdx ->
                            outDoc.importPage(srcDoc.getPage(pIdx))
                        }
                        outDoc.save(destFile)
                    }
                    outFiles.add(destFile)
                }
            }
            outFiles
        }
    }

    private fun parsePageRangeGroups(spec: String, totalPages: Int): List<List<Int>> {
        val groups = mutableListOf<List<Int>>()
        spec.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            if (token.contains("-")) {
                val parts = token.split("-").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 2) {
                    val start = parts[0].coerceIn(1, totalPages)
                    val end = parts[1].coerceIn(start, totalPages)
                    groups.add((start - 1 until end).toList())
                }
            } else {
                val p = token.toIntOrNull()
                if (p != null && p in 1..totalPages) {
                    groups.add(listOf(p - 1))
                }
            }
        }
        return groups
    }

    suspend fun compressPdf(
        context: Context,
        sourceFile: File,
        level: PdfCompressionLevel,
        outputFile: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            outputFile.parentFile?.mkdirs()
            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    val targetWidth = (1654 * level.scaleFactor).roundToInt().coerceIn(600, 1800)
                    val jpegQuality = (level.jpegQuality / 100f).coerceIn(0.35f, 0.95f)

                    PDDocument().use { outDoc ->
                        for (i in 0 until count) {
                            renderer.openPage(i).use { page ->
                                val ratio = page.height.toFloat() / max(1, page.width)
                                val w = targetWidth
                                val h = (w * ratio).roundToInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageRect = PDRectangle(page.width.toFloat(), page.height.toFloat())
                                val pdPage = PDPage(pageRect)
                                outDoc.addPage(pdPage)

                                val pdImage = JPEGFactory.createFromImage(outDoc, bmp, jpegQuality)
                                PDPageContentStream(outDoc, pdPage).use { cs ->
                                    cs.drawImage(pdImage, 0f, 0f, pageRect.width, pageRect.height)
                                }
                                bmp.recycle()
                            }
                            onProgress(i + 1, count)
                        }
                        outDoc.save(outputFile)
                    }
                    count
                }
            }
        }
    }

    suspend fun extractPages(
        sourceFile: File,
        selectedIndices: List<Int>,
        outputFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(selectedIndices.isNotEmpty()) { "Select at least one page to extract" }
            outputFile.parentFile?.mkdirs()
            PDDocument.load(sourceFile).use { src ->
                PDDocument().use { dst ->
                    selectedIndices.sorted().distinct().forEach { idx ->
                        if (idx in 0 until src.numberOfPages) {
                            dst.importPage(src.getPage(idx))
                        }
                    }
                    dst.save(outputFile)
                    dst.numberOfPages
                }
            }
        }
    }

    suspend fun rotatePages(
        sourceFile: File,
        pageRotations: Map<Int, Int>,
        outputFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(sourceFile).use { doc ->
                for (i in 0 until doc.numberOfPages) {
                    val delta = pageRotations[i] ?: 0
                    if (delta != 0) {
                        val page = doc.getPage(i)
                        page.rotation = (page.rotation + delta) % 360
                    }
                }
                doc.save(outputFile)
                doc.numberOfPages
            }
        }
    }

    suspend fun reorderPages(
        sourceFile: File,
        newOrderIndices: List<Int>,
        outputFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(newOrderIndices.isNotEmpty()) { "No pages to save" }
            outputFile.parentFile?.mkdirs()
            PDDocument.load(sourceFile).use { src ->
                PDDocument().use { dst ->
                    newOrderIndices.forEach { idx ->
                        if (idx in 0 until src.numberOfPages) {
                            dst.importPage(src.getPage(idx))
                        }
                    }
                    dst.save(outputFile)
                    dst.numberOfPages
                }
            }
        }
    }

    suspend fun deletePages(
        sourceFile: File,
        indicesToDelete: Set<Int>,
        outputFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            PDDocument.load(sourceFile).use { src ->
                val remaining = (0 until src.numberOfPages).filterNot { it in indicesToDelete }
                require(remaining.isNotEmpty()) { "Cannot delete all pages from a PDF" }
                outputFile.parentFile?.mkdirs()
                PDDocument().use { dst ->
                    remaining.forEach { idx -> dst.importPage(src.getPage(idx)) }
                    dst.save(outputFile)
                    dst.numberOfPages
                }
            }
        }
    }

    suspend fun exportPdfToImages(
        context: Context,
        sourceFile: File,
        outputDir: File,
        baseName: String,
        asPng: Boolean = true
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        runCatching {
            outputDir.mkdirs()
            val files = mutableListOf<File>()
            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        val bmp = renderPageBitmap(context, sourceFile, i, targetWidthPx = 1600)
                            ?: continue
                        val ext = if (asPng) "png" else "jpg"
                        val outFile = File(outputDir, "${baseName}_page_${i + 1}.$ext")
                        FileOutputStream(outFile).use { fos ->
                            bmp.compress(
                                if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                92,
                                fos
                            )
                        }
                        bmp.recycle()
                        files.add(outFile)
                    }
                }
            }
            files
        }
    }

    suspend fun bakeAnnotationsIntoPdf(
        sourceFile: File,
        annotations: List<PdfAnnotationItem>,
        outputFile: File,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            outputFile.parentFile?.mkdirs()
            val tempOut = if (sourceFile.absolutePath == outputFile.absolutePath) {
                File(outputFile.parentFile, ".tmp_${outputFile.name}")
            } else {
                outputFile
            }

            PDDocument.load(sourceFile, password ?: "").use { doc ->
                val byPage = annotations.groupBy { it.pageIndex }
                byPage.forEach { (pageIdx, pageAnnots) ->
                    if (pageIdx !in 0 until doc.numberOfPages) return@forEach
                    val page = doc.getPage(pageIdx)
                    val box = page.cropBox ?: page.mediaBox
                    val pw = box.width
                    val ph = box.height

                    PDPageContentStream(
                        doc,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { cs ->
                        pageAnnots.forEach { item ->
                            drawAnnotationOnPdfPage(cs, item, box.lowerLeftX, box.lowerLeftY, pw, ph)
                        }
                    }
                }
                doc.save(tempOut)
            }

            if (tempOut.absolutePath != outputFile.absolutePath) {
                tempOut.copyTo(outputFile, overwrite = true)
                tempOut.delete()
            }
            PDDocument.load(outputFile, password ?: "").use { it.numberOfPages }
        }
    }

    private fun drawAnnotationOnPdfPage(
        cs: PDPageContentStream,
        item: PdfAnnotationItem,
        originX: Float,
        originY: Float,
        pw: Float,
        ph: Float
    ) {
        fun toPdfX(normX: Float) = originX + normX * pw
        fun toPdfY(normY: Float) = originY + (1f - normY) * ph

        val r = android.graphics.Color.red(item.colorArgb) / 255f
        val g = android.graphics.Color.green(item.colorArgb) / 255f
        val b = android.graphics.Color.blue(item.colorArgb) / 255f

        when (item) {
            is PdfAnnotationItem.FreehandPath -> {
                if (item.points.size < 2) return
                cs.saveGraphicsState()
                if (item.alpha < 0.99f) {
                    val gs = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = item.alpha; strokingAlphaConstant = item.alpha }
                    cs.setGraphicsStateParameters(gs)
                }
                cs.setStrokingColor(r, g, b)
                cs.setLineWidth((item.strokeWidthNorm * pw).coerceAtLeast(1f))
                cs.setLineCapStyle(1)
                cs.setLineJoinStyle(1)
                val first = item.points.first()
                cs.moveTo(toPdfX(first.first), toPdfY(first.second))
                for (i in 1 until item.points.size) {
                    val pt = item.points[i]
                    cs.lineTo(toPdfX(pt.first), toPdfY(pt.second))
                }
                cs.stroke()
                cs.restoreGraphicsState()
            }
            is PdfAnnotationItem.LineOverlay -> {
                cs.saveGraphicsState()
                val x1 = toPdfX(item.startX)
                val y1 = toPdfY(item.startY)
                val x2 = toPdfX(item.endX)
                val y2 = toPdfY(item.endY)

                if (item.kind == PdfAnnotationItem.LineKind.HIGHLIGHT) {
                    val gs = PDExtendedGraphicsState().apply { strokingAlphaConstant = 0.38f; nonStrokingAlphaConstant = 0.38f }
                    cs.setGraphicsStateParameters(gs)
                    cs.setStrokingColor(r, g, b)
                    cs.setLineWidth((item.strokeWidthNorm * pw * 3.5f).coerceAtLeast(10f))
                    cs.setLineCapStyle(1)
                    cs.moveTo(x1, y1)
                    cs.lineTo(x2, y2)
                    cs.stroke()
                } else {
                    cs.setStrokingColor(r, g, b)
                    cs.setLineWidth((item.strokeWidthNorm * pw).coerceAtLeast(1.5f))
                    cs.setLineCapStyle(1)
                    cs.moveTo(x1, y1)
                    cs.lineTo(x2, y2)
                    cs.stroke()

                    if (item.kind == PdfAnnotationItem.LineKind.ARROW) {
                        val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
                        val headLen = (pw * 0.025f).coerceIn(8f, 22f)
                        val a1 = angle - Math.PI / 6.0
                        val a2 = angle + Math.PI / 6.0
                        cs.moveTo(x2, y2)
                        cs.lineTo((x2 - headLen * cos(a1)).toFloat(), (y2 - headLen * sin(a1)).toFloat())
                        cs.moveTo(x2, y2)
                        cs.lineTo((x2 - headLen * cos(a2)).toFloat(), (y2 - headLen * sin(a2)).toFloat())
                        cs.stroke()
                    }
                }
                cs.restoreGraphicsState()
            }
            is PdfAnnotationItem.ShapeOverlay -> {
                val x1 = toPdfX(min(item.left, item.right))
                val x2 = toPdfX(max(item.left, item.right))
                val yBottom = toPdfY(max(item.top, item.bottom))
                val yTop = toPdfY(min(item.top, item.bottom))
                val w = (x2 - x1).coerceAtLeast(2f)
                val h = (yTop - yBottom).coerceAtLeast(2f)

                cs.saveGraphicsState()
                cs.setStrokingColor(r, g, b)
                cs.setLineWidth((item.strokeWidthNorm * pw).coerceAtLeast(1.5f))
                if (item.isCircle) {
                    // Approximate ellipse with 4 cubic Bezier curves
                    val cx = x1 + w / 2f
                    val cy = yBottom + h / 2f
                    val rx = w / 2f
                    val ry = h / 2f
                    val k = 0.5522848f
                    cs.moveTo(cx + rx, cy)
                    cs.curveTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
                    cs.curveTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy)
                    cs.curveTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
                    cs.curveTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy)
                    cs.closePath()
                    cs.stroke()
                } else {
                    cs.addRect(x1, yBottom, w, h)
                    cs.stroke()
                }
                cs.restoreGraphicsState()
            }
            is PdfAnnotationItem.TextOverlay -> {
                val x = toPdfX(item.xNorm)
                val y = toPdfY(item.yNorm)
                val safeText = item.text.replace(Regex("[^\\x20-\\x7E]"), " ")
                cs.saveGraphicsState()
                if (item.isNoteBadge) {
                    val pad = 6f
                    val boxW = (safeText.length * item.fontSizeSp * 0.55f + pad * 2f).coerceIn(60f, pw * 0.6f)
                    val boxH = item.fontSizeSp + pad * 2f
                    cs.setNonStrokingColor(1f, 0.96f, 0.72f)
                    cs.addRect(x, y - boxH + item.fontSizeSp, boxW, boxH)
                    cs.fill()
                    cs.setStrokingColor(0.85f, 0.65f, 0.05f)
                    cs.setLineWidth(1f)
                    cs.addRect(x, y - boxH + item.fontSizeSp, boxW, boxH)
                    cs.stroke()
                }
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, item.fontSizeSp.coerceIn(8f, 48f))
                cs.setNonStrokingColor(r, g, b)
                cs.newLineAtOffset(x + if (item.isNoteBadge) 5f else 0f, y)
                cs.showText(safeText)
                cs.endText()
                cs.restoreGraphicsState()
            }
            is PdfAnnotationItem.SignatureOverlay -> {
                val boxLeft = min(item.left, item.right)
                val boxTop = min(item.top, item.bottom)
                val boxW = max(0.05f, kotlin.math.abs(item.right - item.left))
                val boxH = max(0.03f, kotlin.math.abs(item.bottom - item.top))

                cs.saveGraphicsState()
                cs.setStrokingColor(r, g, b)
                cs.setLineWidth((pw * 0.004f).coerceAtLeast(1.5f))
                cs.setLineCapStyle(1)
                cs.setLineJoinStyle(1)
                item.strokes.forEach { stroke ->
                    if (stroke.size >= 2) {
                        val p0 = stroke.first()
                        cs.moveTo(
                            toPdfX(boxLeft + p0.first * boxW),
                            toPdfY(boxTop + p0.second * boxH)
                        )
                        for (i in 1 until stroke.size) {
                            val pt = stroke[i]
                            cs.lineTo(
                                toPdfX(boxLeft + pt.first * boxW),
                                toPdfY(boxTop + pt.second * boxH)
                            )
                        }
                        cs.stroke()
                    }
                }
                cs.restoreGraphicsState()
            }
        }
    }
}
