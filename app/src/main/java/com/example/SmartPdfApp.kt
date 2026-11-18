package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.PdfRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class SmartPdfApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val pdfRepository: PdfRepository by lazy {
        PdfRepository(
            context = this,
            pdfDao = database.pdfDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
