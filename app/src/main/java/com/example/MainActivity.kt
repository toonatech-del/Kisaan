package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.cases.LandCasesScreen
import com.example.ui.converter.UnitConverterScreen
import com.example.ui.home.HomeScreen
import com.example.ui.measurement.LiveMeasurementScreen
import com.example.ui.measurement.LiveMeasurementViewModel
import com.example.ui.measurement.MeasurementReviewScreen
import com.example.ui.partition.DivisionDiagramScreen
import com.example.ui.partition.FamilyPartitionCalculatorScreen
import com.example.ui.partition.HeirShare
import com.example.ui.services.BiharServicesScreen
import com.example.ui.settings.HelpSettingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.KisanZameenTheme
import com.example.util.AppLanguage
import com.example.util.LanguageManager

enum class Screen {
    HOME,
    LIVE_MEASUREMENT,
    MEASUREMENT_REVIEW,
    LAND_CASES,
    FAMILY_PARTITION,
    DIVISION_DIAGRAM,
    UNIT_CONVERTER,
    BIHAR_SERVICES,
    HELP_SETTINGS,
    SETTINGS,
    MANUAL_CALCULATOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val basePath = applicationContext.filesDir
        org.osmdroid.config.Configuration.getInstance().osmdroidBasePath = basePath
        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = java.io.File(basePath, "osmdroid/tiles")
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContent {
            KisanZameenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KisanZameenApp()
                }
            }
        }
    }
}

@Composable
fun KisanZameenApp(
    measurementViewModel: LiveMeasurementViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentLanguage by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // State passed to division diagram
    var diagramTotalSqM by remember { mutableDoubleStateOf(0.0) }
    var diagramLaggi by remember { mutableDoubleStateOf(6.0) }
    val diagramHeirs = remember { androidx.compose.runtime.mutableStateListOf<HeirShare>() }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                currentLanguage = currentLanguage,
                onLanguageChanged = { newLang ->
                    currentLanguage = newLang
                    LanguageManager.saveLanguage(context, newLang)
                },
                onNavigateToLiveMeasurement = {
                    measurementViewModel.cancelMeasurement()
                    currentScreen = Screen.LIVE_MEASUREMENT
                },
                onNavigateToCases = { currentScreen = Screen.LAND_CASES },
                onNavigateToPartition = { currentScreen = Screen.FAMILY_PARTITION },
                onNavigateToConverter = { currentScreen = Screen.UNIT_CONVERTER },
                onNavigateToServices = { currentScreen = Screen.BIHAR_SERVICES },
                onNavigateToHelp = { currentScreen = Screen.HELP_SETTINGS },
                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                onNavigateToManualCalculator = { currentScreen = Screen.MANUAL_CALCULATOR }
            )
        }

        Screen.LIVE_MEASUREMENT -> {
            BackHandler {
                measurementViewModel.cancelMeasurement()
                currentScreen = Screen.HOME
            }
            LiveMeasurementScreen(
                viewModel = measurementViewModel,
                currentLanguage = currentLanguage,
                onNavigateBack = {
                    measurementViewModel.cancelMeasurement()
                    currentScreen = Screen.HOME
                },
                onNavigateToReview = {
                    currentScreen = Screen.MEASUREMENT_REVIEW
                },
                onNavigateToManualCalculator = {
                    currentScreen = Screen.MANUAL_CALCULATOR
                }
            )
        }

        Screen.MEASUREMENT_REVIEW -> {
            BackHandler {
                currentScreen = Screen.LIVE_MEASUREMENT
            }
            MeasurementReviewScreen(
                viewModel = measurementViewModel,
                currentLanguage = currentLanguage,
                onNavigateBack = {
                    currentScreen = Screen.LIVE_MEASUREMENT
                },
                onSavedDone = {
                    measurementViewModel.cancelMeasurement()
                    currentScreen = Screen.HOME
                }
            )
        }

        Screen.LAND_CASES -> {
            BackHandler { currentScreen = Screen.HOME }
            LandCasesScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME },
                onStartMeasurementForCase = { caseId ->
                    measurementViewModel.cancelMeasurement()
                    measurementViewModel.setLinkedCaseId(caseId)
                    currentScreen = Screen.LIVE_MEASUREMENT
                }
            )
        }

        Screen.FAMILY_PARTITION -> {
            BackHandler { currentScreen = Screen.HOME }
            FamilyPartitionCalculatorScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME },
                onViewDiagram = { totalSqM, laggi, heirsList ->
                    diagramTotalSqM = totalSqM
                    diagramLaggi = laggi
                    diagramHeirs.clear()
                    diagramHeirs.addAll(heirsList)
                    currentScreen = Screen.DIVISION_DIAGRAM
                }
            )
        }

        Screen.DIVISION_DIAGRAM -> {
            BackHandler { currentScreen = Screen.FAMILY_PARTITION }
            DivisionDiagramScreen(
                totalAreaSqM = diagramTotalSqM,
                laggiHaath = diagramLaggi,
                heirs = diagramHeirs,
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.FAMILY_PARTITION }
            )
        }

        Screen.UNIT_CONVERTER -> {
            BackHandler { currentScreen = Screen.HOME }
            UnitConverterScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.BIHAR_SERVICES -> {
            BackHandler { currentScreen = Screen.HOME }
            BiharServicesScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.HELP_SETTINGS -> {
            BackHandler { currentScreen = Screen.HOME }
            HelpSettingsScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.SETTINGS -> {
            BackHandler { currentScreen = Screen.HOME }
            SettingsScreen(
                currentLanguage = currentLanguage,
                onLanguageChanged = { newLang ->
                    currentLanguage = newLang
                    LanguageManager.saveLanguage(context, newLang)
                },
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.MANUAL_CALCULATOR -> {
            BackHandler { currentScreen = Screen.HOME }
            com.example.ui.calculator.ManualLandCalculatorScreen(
                currentLanguage = currentLanguage,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
