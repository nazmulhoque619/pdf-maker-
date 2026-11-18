package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.ImageQualityPreset
import com.example.domain.model.PdfPageSize
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_pdf_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PAGE_SIZE = stringPreferencesKey("default_page_size")
        val DEFAULT_IMAGE_QUALITY = stringPreferencesKey("default_image_quality")
        val DEFAULT_SAVE_LOCATION = stringPreferencesKey("default_save_location")
        val LANGUAGE = stringPreferencesKey("language_code")
    }

    val preferencesFlow: Flow<UserPreferences> = context.settingsDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.SYSTEM,
            defaultPageSize = prefs[Keys.DEFAULT_PAGE_SIZE]?.let { name ->
                PdfPageSize.entries.firstOrNull { it.name == name }
            } ?: PdfPageSize.A4,
            defaultImageQuality = prefs[Keys.DEFAULT_IMAGE_QUALITY]?.let { name ->
                ImageQualityPreset.entries.firstOrNull { it.name == name }
            } ?: ImageQualityPreset.LOSSLESS_HIGH,
            defaultSaveLocation = prefs[Keys.DEFAULT_SAVE_LOCATION]
                ?: "App Vault (Private Offline Storage)",
            language = prefs[Keys.LANGUAGE]?.let { code ->
                AppLanguage.entries.firstOrNull { it.code == code }
            } ?: AppLanguage.ENGLISH
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultPageSize(size: PdfPageSize) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_PAGE_SIZE] = size.name }
    }

    suspend fun setDefaultImageQuality(quality: ImageQualityPreset) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_IMAGE_QUALITY] = quality.name }
    }

    suspend fun setDefaultSaveLocation(location: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_SAVE_LOCATION] = location }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }
}
