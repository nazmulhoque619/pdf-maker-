package com.example.domain.model

import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    HINDI("hi", "हिन्दी"),
    ARABIC("ar", "العربية")
}

enum class ImageFilterType(val displayName: String) {
    ORIGINAL("Original"),
    BLACK_AND_WHITE("B & W"),
    GRAYSCALE("Grayscale"),
    DOCUMENT("Document"),
    SCAN_EFFECT("Scan Effect"),
    VINTAGE("Vintage"),
    BRIGHT("Bright"),
    CONTRAST("Contrast"),
    SHARPEN("Sharpen"),
    AUTO_ENHANCE("Auto Enhance")
}

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFull: Boolean
        get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class ImageEditConfig(
    val filter: ImageFilterType = ImageFilterType.ORIGINAL,
    val brightness: Float = 0f,       // -1f .. 1f
    val contrast: Float = 1f,         // 0.5f .. 2.0f
    val saturation: Float = 1f,       // 0f .. 2.0f
    val sharpness: Float = 0f,        // 0f .. 1.0f
    val rotationDegrees: Int = 0,     // 0, 90, 180, 270
    val cropRect: NormalizedCropRect = NormalizedCropRect()
)

data class SelectedImageItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val editConfig: ImageEditConfig = ImageEditConfig()
)

enum class PdfPageSize(val label: String, val widthPt: Float, val heightPt: Float) {
    A4("A4 (210 × 297 mm)", 595.28f, 841.89f),
    A3("A3 (297 × 420 mm)", 841.89f, 1190.55f),
    A5("A5 (148 × 210 mm)", 419.53f, 595.28f),
    LETTER("Letter (8.5 × 11 in)", 612f, 792f),
    LEGAL("Legal (8.5 × 14 in)", 612f, 1008f),
    CUSTOM("Custom Size", 595.28f, 841.89f)
}

enum class PageOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class ImageScaleMode(val label: String) {
    FIT_TO_PAGE("Fit to Page"),
    FIT_TO_WIDTH("Fit to Width"),
    FIT_TO_HEIGHT("Fit to Height"),
    ORIGINAL_RATIO("Original Ratio")
}

enum class PageBackgroundPreset(val label: String, val argb: Int) {
    WHITE("White", android.graphics.Color.WHITE),
    BLACK("Black", android.graphics.Color.BLACK),
    CREAM("Warm Paper", 0xFFFDFBF7.toInt()),
    CUSTOM("Custom", 0xFFEFF6FF.toInt())
}

enum class ImageQualityPreset(val label: String, val jpegQuality: Int) {
    LOSSLESS_HIGH("High (95%)", 95),
    BALANCED("Medium (80%)", 80),
    COMPACT("Low (60%)", 60)
}

enum class PdfCompressionLevel(val label: String, val scaleFactor: Float, val jpegQuality: Int) {
    NONE("No Compression", 1.0f, 98),
    LOW("Light Compression", 0.90f, 85),
    MEDIUM("Balanced Compression", 0.75f, 72),
    HIGH("Maximum Compression", 0.55f, 55)
}

data class PdfCreationSettings(
    val fileName: String = "SmartScan_${System.currentTimeMillis() / 1000}",
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val customWidthMm: Float = 210f,
    val customHeightMm: Float = 297f,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val marginDp: Float = 16f,
    val pageSpacingDp: Float = 10f,
    val scaleMode: ImageScaleMode = ImageScaleMode.FIT_TO_PAGE,
    val imagesPerPage: Int = 1, // 1, 2, 4, 6
    val backgroundPreset: PageBackgroundPreset = PageBackgroundPreset.WHITE,
    val customBackgroundColorArgb: Int = 0xFFEFF6FF.toInt(),
    val imageQuality: ImageQualityPreset = ImageQualityPreset.LOSSLESS_HIGH,
    val compressionLevel: PdfCompressionLevel = PdfCompressionLevel.LOW,
    val usePassword: Boolean = false,
    val password: String = ""
)

enum class FileSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First")
}

enum class FileFilterTab(val label: String) {
    ALL("All Files"),
    RECENT("Recent"),
    FAVORITES("Favorites")
}

enum class PdfToolType(val title: String, val description: String) {
    MERGE("Merge PDFs", "Combine multiple PDF documents into one"),
    SPLIT("Split PDF", "Divide a PDF into separate page-range files"),
    COMPRESS("Compress PDF", "Reduce PDF file size cleanly"),
    EXTRACT_PAGES("Extract Pages", "Save selected pages as a new PDF"),
    ROTATE_PAGES("Rotate Pages", "Rotate individual or all PDF pages"),
    REORDER_PAGES("Reorder Pages", "Drag or shift pages into a new sequence"),
    DELETE_PAGES("Delete Pages", "Remove unwanted pages from a PDF"),
    IMAGE_TO_PDF("Image to PDF", "Convert photos & scans to PDF"),
    PDF_TO_IMAGES("PDF to Images", "Export PDF pages as high-res PNG/JPG"),
    SHARE_PDF("Share PDF", "Send or export PDFs to other apps")
}

enum class AnnotationToolType(val label: String) {
    SELECT("Pan / View"),
    TEXT("Text"),
    HIGHLIGHT("Highlight"),
    UNDERLINE("Underline"),
    STRIKETHROUGH("Strike"),
    FREEHAND("Draw"),
    ERASER("Eraser"),
    SHAPE_RECT("Rectangle"),
    SHAPE_CIRCLE("Circle"),
    ARROW("Arrow"),
    NOTE("Note"),
    DATE("Date"),
    SIGNATURE("Signature")
}

sealed class PdfAnnotationItem {
    abstract val id: String
    abstract val pageIndex: Int
    abstract val colorArgb: Int

    data class FreehandPath(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val colorArgb: Int,
        val strokeWidthNorm: Float, // relative to page width (e.g. 0.005f)
        val alpha: Float = 1f,
        val points: List<Pair<Float, Float>> // normalized 0f..1f
    ) : PdfAnnotationItem()

    data class LineOverlay(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val colorArgb: Int,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val strokeWidthNorm: Float,
        val kind: LineKind
    ) : PdfAnnotationItem()

    enum class LineKind { HIGHLIGHT, UNDERLINE, STRIKETHROUGH, ARROW }

    data class ShapeOverlay(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val colorArgb: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val strokeWidthNorm: Float,
        val isCircle: Boolean
    ) : PdfAnnotationItem()

    data class TextOverlay(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val colorArgb: Int,
        val xNorm: Float,
        val yNorm: Float,
        val text: String,
        val fontSizeSp: Float,
        val isNoteBadge: Boolean = false
    ) : PdfAnnotationItem()

    data class SignatureOverlay(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val colorArgb: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val strokes: List<List<Pair<Float, Float>>> // normalized inside signature box 0f..1f
    ) : PdfAnnotationItem()
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultImageQuality: ImageQualityPreset = ImageQualityPreset.LOSSLESS_HIGH,
    val defaultSaveLocation: String = "App Vault (Private Offline Storage)",
    val language: AppLanguage = AppLanguage.ENGLISH
)

val AnnotationPaletteColors: List<Color> = listOf(
    Color(0xFFDC2626), // Crimson Red
    Color(0xFF1D4ED8), // Royal Blue
    Color(0xFF0F172A), // Deep Ink
    Color(0xFFFACC15), // Highlighter Yellow
    Color(0xFF16A34A), // Emerald Green
    Color(0xFFEA580C), // Vivid Orange
    Color(0xFF7C3AED), // Violet
    Color(0xFFFFFFFF)  // White
)
