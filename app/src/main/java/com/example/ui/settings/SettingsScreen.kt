package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.AreaDisplayUnit
import com.example.util.GeoLandCalculator
import com.example.util.PreferencesManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = AppStrings.get(currentLanguage)

    val db = remember { AppDatabase.getInstance(context) }
    val measurementDao = remember { db.landMeasurementDao() }
    val caseDao = remember { db.landCaseDao() }

    val measurementCount by measurementDao.getMeasurementCount().collectAsState(initial = 0)
    val caseCount by caseDao.getCaseCount().collectAsState(initial = 0)

    var currentLaggi by remember { mutableDoubleStateOf(PreferencesManager.getDefaultLaggi(context)) }
    var currentDisplayUnit by remember { mutableStateOf(PreferencesManager.getDefaultDisplayUnit(context)) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.settingsSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएं / Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("settings_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. LANGUAGE SELECTION SECTION
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Language,
                    title = strings.languageSectionTitle
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageSelectionCard(
                            languageName = "हिन्दी",
                            subLabel = "Hindi (बिहार एवं क्षेत्रीय)",
                            isSelected = currentLanguage == AppLanguage.HINDI,
                            onClick = {
                                onLanguageChanged(AppLanguage.HINDI)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lang_hindi")
                        )
                        LanguageSelectionCard(
                            languageName = "English",
                            subLabel = "English",
                            isSelected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = {
                                onLanguageChanged(AppLanguage.ENGLISH)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lang_english")
                        )
                    }
                }
            }

            // B. MEASUREMENT UNITS SECTION
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.SquareFoot,
                    title = strings.measurementUnitsSection
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.defaultLaggiTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.defaultLaggiDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Laggi selector chips
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PreferencesManager.STANDARD_LAGGI_OPTIONS.forEach { laggiOption ->
                                val isSelected = currentLaggi == laggiOption
                                val isStandard = laggiOption == 6.0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        currentLaggi = laggiOption
                                        PreferencesManager.saveDefaultLaggi(context, laggiOption)
                                    },
                                    label = {
                                        Text(
                                            text = if (isStandard) "${laggiOption} हाथ ★" else "${laggiOption} हाथ",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("settings_laggi_chip_${laggiOption.toString().replace('.', '_')}")
                                )
                            }
                        }

                        // Laggi formula breakdown box
                        val laggiFeet = currentLaggi * 1.5
                        val oneDhurSqFt = laggiFeet * laggiFeet
                        val oneDhurSqM = oneDhurSqFt / GeoLandCalculator.SQ_METERS_TO_SQ_FEET
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.HINDI) {
                                        "चुनी गई लग्गी: $currentLaggi हाथ (${String.format(Locale.US, "%.1f", laggiFeet)} फीट)\n" +
                                                "• 1 धूर = ${String.format(Locale.US, "%.2f", oneDhurSqFt)} वर्ग फीट (${String.format(Locale.US, "%.2f", oneDhurSqM)} वर्ग मीटर)\n" +
                                                "• 1 कट्ठा = 20 धूर = ${String.format(Locale.US, "%.1f", oneDhurSqFt * 20)} वर्ग फीट\n" +
                                                "• 1 बीघा = 20 कट्ठा = ${String.format(Locale.US, "%.1f", oneDhurSqFt * 400)} वर्ग फीट"
                                    } else {
                                        "Selected Laggi: $currentLaggi Haath (${String.format(Locale.US, "%.1f", laggiFeet)} ft)\n" +
                                                "• 1 Dhur = ${String.format(Locale.US, "%.2f", oneDhurSqFt)} sq.ft (${String.format(Locale.US, "%.2f", oneDhurSqM)} sq.m)\n" +
                                                "• 1 Katha = 20 Dhur = ${String.format(Locale.US, "%.1f", oneDhurSqFt * 20)} sq.ft\n" +
                                                "• 1 Bigha = 20 Katha = ${String.format(Locale.US, "%.1f", oneDhurSqFt * 400)} sq.ft"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary area display unit
                        Text(
                            text = strings.primaryDisplayUnitTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.primaryDisplayUnitDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AreaDisplayUnit.values().forEach { unit ->
                                val isSelected = currentDisplayUnit == unit
                                Surface(
                                    onClick = {
                                        currentDisplayUnit = unit
                                        PreferencesManager.saveDefaultDisplayUnit(context, unit)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_unit_${unit.key.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.HINDI) unit.hindiLabel else unit.englishLabel,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = PreferencesManager.formatAreaWithUnit(
                                                    areaSqM = 2529.285, // Example 1 Bigha / 0.625 Acre
                                                    unit = unit,
                                                    laggiHaath = currentLaggi,
                                                    language = currentLanguage
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.unitNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // C. PRIVACY & LOCAL DATA MANAGEMENT SECTION
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Storage,
                    title = strings.privacyAndDataSection
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Storage status & record count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.localStorageStatusTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.storageDesc,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(strings.savedRecordsCountTemplate, measurementCount, caseCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PDF Export & Backup Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.pdfExportBackupTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.pdfExportBackupDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Permissions Explanation Card
                        Text(
                            text = strings.permissionsExplanationTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionItemRow(
                            icon = Icons.Default.MyLocation,
                            title = strings.locationPermissionTitle,
                            description = strings.locationPermissionDesc
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItemRow(
                            icon = Icons.Default.Public,
                            title = strings.internetPermissionTitle,
                            description = strings.internetPermissionDesc
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItemRow(
                            icon = Icons.Default.Security,
                            title = strings.noAdsNoTrackingTitle,
                            description = strings.noAdsNoTrackingDesc
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Clear All Data Danger Button
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.clearDataBtn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // D. LEGAL DISCLAIMER & FIELD SAFETY SECTION
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Gavel,
                    title = strings.legalAndSafetySection
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Legal Advisory Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.legalAdvisoryTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.legalAdvisoryDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.legalDisputeAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field Safety Instructions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFCE8)),
                    border = BorderStroke(1.dp, Color(0xFFFEF08A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFCA8A04),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.fieldSafetyTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF854D0E)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SafetyItemText(text = strings.safetyWeather)
                        Spacer(modifier = Modifier.height(6.dp))
                        SafetyItemText(text = strings.safetyTerrain)
                        Spacer(modifier = Modifier.height(6.dp))
                        SafetyItemText(text = strings.safetyMachinery)
                        Spacer(modifier = Modifier.height(6.dp))
                        SafetyItemText(text = strings.safetyWires)
                    }
                }
            }

            // E. ABOUT & VERSION SECTION
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Info,
                    title = strings.aboutAndVersionSection
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Agriculture,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.appNameDisplay,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.appVersionNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.appPurposeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // OpenStreetMap attribution
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.mapAttributionTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.mapAttributionDesc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation Dialog for Clearing Data
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.clearDataConfirmTitle,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = strings.clearDataConfirmMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            measurementDao.clearAllMeasurements()
                            caseDao.clearAllCases()
                            Toast.makeText(context, strings.clearDataSuccess, Toast.LENGTH_LONG).show()
                            showClearConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_data_button")
                ) {
                    Text(text = strings.clearDataBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_clear_data_button")
                ) {
                    Text(text = strings.cancel)
                }
            },
            modifier = Modifier.testTag("clear_data_dialog")
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LanguageSelectionCard(
    languageName: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = languageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionItemRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SafetyItemText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF713F12),
        lineHeight = 18.sp
    )
}
