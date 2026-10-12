package com.example.ui.measurement

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.GpsQualityLevel
import com.example.ui.components.SchematicPlotView
import com.example.ui.theme.QualityExcellent
import com.example.ui.theme.QualityGood
import com.example.ui.theme.QualityMedium
import com.example.ui.theme.QualityWeak
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import com.example.util.PolygonValidation
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveMeasurementScreen(
    viewModel: LiveMeasurementViewModel,
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = AppStrings.get(currentLanguage)

    var showCancelDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(!uiState.isTracking && uiState.recordedPoints.isEmpty()) }
    var selectedPointForEdit by remember { mutableStateOf<Int?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fineGranted || coarseGranted
        viewModel.setPermissionGranted(granted)
        if (granted) {
            viewModel.startTracking()
        }
    }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
        }
    }

    LaunchedEffect(uiState.isReadyForReview) {
        if (uiState.isReadyForReview) {
            onNavigateToReview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.liveMeasurementTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.linkedLandCase?.let { c ->
                            Text(
                                text = "Case: ${c.farmerName} (Khesra: ${c.khesraNumber})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.recordedPoints.isNotEmpty()) {
                                showCancelDialog = true
                            } else {
                                viewModel.cancelMeasurement()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExplanationDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Guidance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            GpsStatusCard(
                uiState = uiState,
                strings = strings,
                onEnableGpsClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.recordedPoints.size >= 3) {
                AreaMetricsCard(
                    areaSqM = uiState.estimatedAreaSqM,
                    perimeterM = uiState.estimatedPerimeterM,
                    laggiHaath = uiState.selectedLaggiHaath,
                    strings = strings,
                    onLaggiChange = { viewModel.setLaggiHaath(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.warningMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.warningMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (uiState.polygonValidation) {
                is PolygonValidation.SelfIntersecting -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                strings.warningSelfIntersect,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> Unit
            }

            SchematicPlotView(
                points = uiState.recordedPoints,
                currentLocation = uiState.currentLocation,
                isClosed = uiState.isClosed,
                selectedPointIndex = selectedPointForEdit,
                onPointSelected = { selectedPointForEdit = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.recordedPoints}: ${uiState.recordedPoints.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.recordedPoints.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastPoint() },
                        modifier = Modifier.testTag("undo_point_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.undoLast, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!uiState.isTracking && uiState.recordedPoints.isEmpty()) {
                Button(
                    onClick = {
                        if (!uiState.isPermissionGranted) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_measurement_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.startMeasurementBtn, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addManualPoint() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("add_point_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addPoint, fontWeight = FontWeight.Bold)
                        }

                        if (uiState.isPaused) {
                            OutlinedButton(
                                onClick = { viewModel.resumeTracking() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("resume_tracking_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.resume, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.pauseTracking() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_tracking_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.pause, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.isClosed && uiState.recordedPoints.size >= 3) {
                            OutlinedButton(
                                onClick = { viewModel.closeBoundary() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("close_boundary_button")
                            ) {
                                Text(strings.closeBoundary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = { viewModel.finishMeasurement() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("complete_measurement_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            enabled = uiState.recordedPoints.size >= 3
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.completeMeasurement, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.height(48.dp).testTag("cancel_measurement_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.cancel)
                        }
                    }
                }
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.liveMeasurementTitle, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.instructionStart, style = MaterialTheme.typography.bodyMedium)
                    Text(strings.instructionWalk, style = MaterialTheme.typography.bodyMedium)
                    Text(strings.instructionClose, style = MaterialTheme.typography.bodyMedium)

                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            text = strings.gpsWarningDisclaimer,
                            color = Color(0xFF991B1B),
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExplanationDialog = false }) {
                    Text("OK / Understand")
                }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Measurement?") },
            text = { Text("All recorded points will be lost. Do you really want to exit?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelMeasurement()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Continue")
                }
            }
        )
    }
}

@Composable
private fun GpsStatusCard(
    uiState: LiveMeasurementUiState,
    strings: com.example.util.Strings,
    onEnableGpsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (uiState.gpsQuality) {
                GpsQualityLevel.EXCELLENT -> Color(0xFFF0FDF4)
                GpsQualityLevel.GOOD -> Color(0xFFF7FEE7)
                GpsQualityLevel.MEDIUM -> Color(0xFFFEFCE8)
                GpsQualityLevel.WEAK -> Color(0xFFFEF2F2)
                GpsQualityLevel.UNAVAILABLE -> Color(0xFFF1F5F9)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val qualityColor = when (uiState.gpsQuality) {
                    GpsQualityLevel.EXCELLENT -> QualityExcellent
                    GpsQualityLevel.GOOD -> QualityGood
                    GpsQualityLevel.MEDIUM -> QualityMedium
                    GpsQualityLevel.WEAK -> QualityWeak
                    GpsQualityLevel.UNAVAILABLE -> Color.Gray
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(qualityColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${strings.gpsSignalStatus}: ${uiState.gpsQuality.hindiLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.currentAccuracy > 0f) {
                            "${strings.accuracy}: ±${String.format(Locale.US, "%.1f", uiState.currentAccuracy)} m"
                        } else {
                            "${strings.accuracy}: ${strings.waiting}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.isGpsEnabled) {
                OutlinedButton(
                    onClick = onEnableGpsClick,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.enableGps, fontSize = 11.sp)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (uiState.isTracking) (if (uiState.isPaused) "Paused" else "Active") else "Ready",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaMetricsCard(
    areaSqM: Double,
    perimeterM: Double,
    laggiHaath: Double,
    strings: com.example.util.Strings,
    onLaggiChange: (Double) -> Unit
) {
    val bihar = GeoLandCalculator.convertToBiharUnits(areaSqM, laggiHaath)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.estimatedArea,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bihar.toHindiString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.estimatedPerimeter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", perimeterM)} m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${String.format(Locale.US, "%.2f", bihar.acres)} Acres (${String.format(Locale.US, "%.1f", bihar.dismil)} Dismil) • ${String.format(Locale.US, "%.1f", areaSqM)} sq.m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${strings.laggiMeasurement}: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5.5, 6.0, 6.5, 7.0).forEach { size ->
                        FilterChip(
                            selected = laggiHaath == size,
                            onClick = { onLaggiChange(size) },
                            label = { Text("$size", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}
