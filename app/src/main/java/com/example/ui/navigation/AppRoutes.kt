package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Tools : AppRoute

    @Serializable
    data object Files : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object ImageToPdf : AppRoute

    @Serializable
    data class ImageEditor(val imageId: String) : AppRoute

    @Serializable
    data object PdfCreationSettings : AppRoute

    @Serializable
    data class PdfReader(val documentId: Long) : AppRoute

    @Serializable
    data class PdfEditor(val documentId: Long) : AppRoute

    @Serializable
    data class ToolWorkspace(val toolName: String) : AppRoute
}
