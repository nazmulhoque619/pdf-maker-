package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.ImageQualityPreset
import com.example.domain.model.PdfPageSize
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import com.example.ui.i18n.AppStrings

@Composable
fun SettingsScreen(
    strings: AppStrings,
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultPageSizeChange: (PdfPageSize) -> Unit,
    onDefaultImageQualityChange: (ImageQualityPreset) -> Unit,
    onDefaultSaveLocationChange: (String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onShareApp: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPageSizeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSaveLocDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.settings,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Customize appearance, PDF defaults, and offline storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Theme Mode
        item {
            SettingsGroupCard(title = strings.appearance) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Brightness6, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "Auto"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // PDF Defaults
        item {
            SettingsGroupCard(title = strings.pdfDefaults) {
                SettingsRowItem(
                    icon = Icons.Filled.Description,
                    title = "Default PDF Page Size",
                    subtitle = preferences.defaultPageSize.label,
                    onClick = { showPageSizeDialog = true },
                    testTag = "setting_default_page_size"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Filled.HighQuality,
                    title = "Default Image Quality",
                    subtitle = preferences.defaultImageQuality.label,
                    onClick = { showQualityDialog = true },
                    testTag = "setting_default_quality"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Filled.Folder,
                    title = "Default Save Location",
                    subtitle = preferences.defaultSaveLocation,
                    onClick = { showSaveLocDialog = true },
                    testTag = "setting_save_location"
                )
            }
        }

        // Language
        item {
            SettingsGroupCard(title = strings.language) {
                SettingsRowItem(
                    icon = Icons.Filled.Language,
                    title = "App Language",
                    subtitle = preferences.language.displayName,
                    onClick = { showLanguageDialog = true },
                    testTag = "setting_language"
                )
            }
        }

        // About, Privacy, Rate, Share
        item {
            SettingsGroupCard(title = strings.aboutAndLegal) {
                SettingsRowItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "100% offline local document processing",
                    onClick = { showPrivacyDialog = true },
                    testTag = "setting_privacy"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Filled.Info,
                    title = "About App",
                    subtitle = "Smart PDF Maker – PDF Editor & Scanner v1.0",
                    onClick = { showAboutDialog = true },
                    testTag = "setting_about"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Filled.Star,
                    title = "Rate Us",
                    subtitle = "Share your feedback",
                    onClick = { showRateDialog = true },
                    testTag = "setting_rate"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Filled.Share,
                    title = "Share App",
                    subtitle = "Recommend Smart PDF Maker to friends",
                    onClick = onShareApp,
                    testTag = "setting_share_app"
                )
            }
        }
    }

    if (showPageSizeDialog) {
        AlertDialog(
            onDismissRequest = { showPageSizeDialog = false },
            title = { Text("Default Page Size") },
            text = {
                Column {
                    PdfPageSize.entries.filter { it != PdfPageSize.CUSTOM }.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDefaultPageSizeChange(size)
                                    showPageSizeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = preferences.defaultPageSize == size, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(size.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageSizeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default Image Quality") },
            text = {
                Column {
                    ImageQualityPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDefaultImageQualityChange(preset)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = preferences.defaultImageQuality == preset, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(preset.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSaveLocDialog) {
        val options = listOf(
            "App Vault (Private Offline Storage)",
            "App Vault + Quick Export Prompt"
        )
        AlertDialog(
            onDismissRequest = { showSaveLocDialog = false },
            title = { Text("Default Save Location") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDefaultSaveLocationChange(option)
                                    showSaveLocDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = preferences.defaultSaveLocation == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSaveLocDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang.displayName)
                            if (preferences.language == lang) {
                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Smart PDF Maker operates 100% offline on your Android device. " +
                        "All scanned photos, converted PDFs, annotations, signatures, and bookmarks are stored privately on your device. " +
                        "No documents, images, or personal data are ever uploaded to external servers."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) { Text("Understood") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Smart PDF Maker") },
            text = {
                Text(
                    "Smart PDF Maker – PDF Editor & Scanner\nVersion 1.0.0\n\n" +
                        "• Image to PDF & Camera Document Scanner\n" +
                        "• 10 Pro Photo Filters & Crop/Rotate Studio\n" +
                        "• Custom Page Sizes, Multi-image Layouts & Password Encryption\n" +
                        "• Full PDF Reader with Search, Bookmarks, Night Mode & Print\n" +
                        "• Complete PDF Annotator, Signature Pad & 10-Tool PDF Toolkit"
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showRateDialog) {
        var stars by remember { mutableIntStateOf(5) }
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text("Rate Smart PDF Maker") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("How has your experience been?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            IconButton(onClick = { stars = i }) {
                                Icon(
                                    imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "$i stars",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRateDialog = false
                    onShowMessage("Thank you for rating us $stars stars!")
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) { Text("Later") }
            }
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
