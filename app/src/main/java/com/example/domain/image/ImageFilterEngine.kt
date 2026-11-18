package com.example.domain.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import com.example.domain.model.ImageEditConfig
import com.example.domain.model.ImageFilterType
import com.example.domain.model.NormalizedCropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageFilterEngine {

    suspend fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val srcW = boundsOptions.outWidth
            val srcH = boundsOptions.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            var inSampleSize = 1
            while (srcW / inSampleSize > maxDimension * 2 || srcH / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching null

            val exifRotation = readExifRotation(context, uri)
            val rotated = if (exifRotation != 0) {
                rotateBitmap(decoded, exifRotation).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            // Downscale precisely if still above maxDimension
            val largest = max(rotated.width, rotated.height)
            if (largest > maxDimension) {
                val scale = maxDimension.toFloat() / largest
                val targetW = max(1, (rotated.width * scale).roundToInt())
                val targetH = max(1, (rotated.height * scale).roundToInt())
                Bitmap.createScaledBitmap(rotated, targetW, targetH, true).also {
                    if (it !== rotated) rotated.recycle()
                }
            } else {
                rotated
            }
        }.getOrNull()
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropBitmap(source: Bitmap, cropRect: NormalizedCropRect): Bitmap {
        if (cropRect.isFull) return source
        val left = (cropRect.left.coerceIn(0f, 0.95f) * source.width).roundToInt()
        val top = (cropRect.top.coerceIn(0f, 0.95f) * source.height).roundToInt()
        val right = (cropRect.right.coerceIn(cropRect.left + 0.05f, 1f) * source.width).roundToInt()
        val bottom = (cropRect.bottom.coerceIn(cropRect.top + 0.05f, 1f) * source.height).roundToInt()
        val width = max(1, right - left)
        val height = max(1, bottom - top)
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    suspend fun applyEdits(
        source: Bitmap,
        config: ImageEditConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Crop first (normalized to original orientation), then rotate
        val cropped = cropBitmap(source, config.cropRect)
        val rotated = rotateBitmap(cropped, config.rotationDegrees)
        if (cropped !== source && cropped !== rotated) cropped.recycle()

        // 2. Build ColorMatrix from preset filter + manual brightness/contrast/saturation
        val combinedMatrix = ColorMatrix()

        when (config.filter) {
            ImageFilterType.ORIGINAL -> Unit
            ImageFilterType.GRAYSCALE -> {
                combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(0f) })
            }
            ImageFilterType.BLACK_AND_WHITE -> {
                // High-contrast monochrome before thresholding
                combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(0f) })
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.9f, brightness = 0.08f))
            }
            ImageFilterType.DOCUMENT -> {
                // Crisp document scan: desaturate slightly, brighten paper whites, deepen ink
                combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(0.25f) })
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.48f, brightness = 0.14f))
            }
            ImageFilterType.SCAN_EFFECT -> {
                combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(0.1f) })
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.72f, brightness = 0.18f))
            }
            ImageFilterType.VINTAGE -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 12f,
                        0.349f, 0.686f, 0.168f, 0f, 6f,
                        0.272f, 0.534f, 0.131f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(sepia)
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 0.95f, brightness = 0.04f))
            }
            ImageFilterType.BRIGHT -> {
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.05f, brightness = 0.22f))
            }
            ImageFilterType.CONTRAST -> {
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.55f, brightness = -0.02f))
            }
            ImageFilterType.SHARPEN -> {
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.15f, brightness = 0.02f))
            }
            ImageFilterType.AUTO_ENHANCE -> {
                combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(1.22f) })
                combinedMatrix.postConcat(contrastBrightnessMatrix(contrast = 1.24f, brightness = 0.06f))
            }
        }

        // Manual adjustments
        if (config.saturation != 1f) {
            combinedMatrix.postConcat(ColorMatrix().apply { setSaturation(config.saturation) })
        }
        if (config.contrast != 1f || config.brightness != 0f) {
            combinedMatrix.postConcat(contrastBrightnessMatrix(config.contrast, config.brightness))
        }

        val outBitmap = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        if (rotated !== source) rotated.recycle()

        // Binary adaptive threshold pass for BLACK_AND_WHITE filter
        val afterThreshold = if (config.filter == ImageFilterType.BLACK_AND_WHITE) {
            applyBinaryThreshold(outBitmap, threshold = 138).also {
                if (it !== outBitmap) outBitmap.recycle()
            }
        } else {
            outBitmap
        }

        // Sharpen pass for SHARPEN preset or manual sharpness slider > 0
        val effectiveSharpness = when {
            config.sharpness > 0.01f -> config.sharpness
            config.filter == ImageFilterType.SHARPEN -> 0.65f
            config.filter == ImageFilterType.SCAN_EFFECT -> 0.45f
            config.filter == ImageFilterType.DOCUMENT -> 0.35f
            else -> 0f
        }

        if (effectiveSharpness > 0.01f) {
            applySharpen(afterThreshold, effectiveSharpness).also {
                if (it !== afterThreshold) afterThreshold.recycle()
            }
        } else {
            afterThreshold
        }
    }

    private fun contrastBrightnessMatrix(contrast: Float, brightness: Float): ColorMatrix {
        val translate = (-0.5f * contrast + 0.5f + brightness * 0.5f) * 255f
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun applyBinaryThreshold(bitmap: Bitmap, threshold: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 30 + g * 59 + b * 11) / 100
            pixels[i] = if (lum >= threshold) 0xFFFFFFFF.toInt() else 0xFF111827.toInt()
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private fun applySharpen(bitmap: Bitmap, amount: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3) return bitmap
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        src.copyInto(dst)

        val strength = amount.coerceIn(0f, 1f) * 0.85f
        for (y in 1 until h - 1) {
            val rowOffset = y * w
            for (x in 1 until w - 1) {
                val idx = rowOffset + x
                val c = src[idx]
                val top = src[idx - w]
                val bottom = src[idx + w]
                val left = src[idx - 1]
                val right = src[idx + 1]

                val rC = (c shr 16) and 0xFF
                val gC = (c shr 8) and 0xFF
                val bC = c and 0xFF

                val rAvg = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                    ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) / 4
                val gAvg = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                    ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) / 4
                val bAvg = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) / 4

                val rOut = (rC + ((rC - rAvg) * strength)).roundToInt().coerceIn(0, 255)
                val gOut = (gC + ((gC - gAvg) * strength)).roundToInt().coerceIn(0, 255)
                val bOut = (bC + ((bC - bAvg) * strength)).roundToInt().coerceIn(0, 255)

                dst[idx] = (0xFF shl 24) or (rOut shl 16) or (gOut shl 8) or bOut
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(dst, 0, w, 0, 0, w, h)
        }
    }
}
