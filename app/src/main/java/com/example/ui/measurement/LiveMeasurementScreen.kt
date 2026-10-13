package com.example.ui.measurement

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
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
    onNavigateToReview: () -> Unit,
    onNavigateToManualCalculator: () -> Unit = {}
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

    LaunchedEffect(Unit) {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.setPermissionGranted(true)
            if (!uiState.isTracking) {
                viewModel.startTracking()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
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
                    FilledTonalButton(
                        onClick = onNavigateToManualCalculator,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("action_manual_calc"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF1E5631)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "फ़ीता नाप",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E5631)
                        )
                    }
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

            if (uiState.recordedPoints.size in 1..2) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("live_progress_metrics_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📏 वर्तमान दूरी (Last Corner)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", uiState.liveWalkingDistanceM)} m (${String.format(Locale.US, "%.1f", uiState.liveWalkingDistanceFt)} ft)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "🔄 लाइव कुल परिधि",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", uiState.estimatedPerimeterM)} m (${String.format(Locale.US, "%.1f", uiState.estimatedPerimeterM * 3.28084)} ft)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (uiState.recordedPoints.size >= 3) {
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

            // Dedicated Bounded Card Container for Map
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("map_bounded_card_container"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    com.example.ui.components.OsmLandMapView(
                        points = uiState.recordedPoints,
                        currentLocation = uiState.currentLocation,
                        isClosed = uiState.isClosed,
                        onPointMoved = { index, lat, lng ->
                            viewModel.updatePointCoordinates(index, lat, lng)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dedicated Responsive Bottom Control Dock
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_action_dock"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status & Undo Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (uiState.recordedPoints.size >= 3) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${uiState.recordedPoints.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    uiState.recordedPoints.isEmpty() -> "पहला कोना (START दर्ज करें)"
                                    uiState.recordedPoints.size == 1 -> "१ कोना दर्ज ✓ (अगले कोने पर जाएं)"
                                    uiState.recordedPoints.size == 4 -> "४ कोने (चौकोर / Square ✓)"
                                    else -> "${uiState.recordedPoints.size} कोने दर्ज ✓"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (uiState.recordedPoints.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.undoLastPoint() },
                                enabled = uiState.recordedPoints.size > 1,
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("undo_point_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.undoLast, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Live Walking Distance & Real-Time Perimeter Tracker Row
                    if (uiState.recordedPoints.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth().testTag("live_dock_distance_tracker")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "📏 वर्तमान दूरी: ${String.format(Locale.US, "%.1f", uiState.liveWalkingDistanceM)} m (${String.format(Locale.US, "%.1f", uiState.liveWalkingDistanceFt)} फ़ीट)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "कुल: ${String.format(Locale.US, "%.1f", uiState.estimatedPerimeterM)}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E5631)
                                )
                            }
                        }
                    }

                    // Primary Action Rows
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
                                .heightIn(min = 48.dp)
                                .testTag("start_measurement_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.startMeasurementBtn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Row 1: Add Point / Start + Pause/Resume
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isGpsAccurate = uiState.currentAccuracy in 0.001f..7.0f
                            Button(
                                onClick = { viewModel.addCornerRequested() },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .heightIn(min = 48.dp)
                                    .testTag("add_point_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E5631),
                                    disabledContainerColor = Color(0xFFE0E0E0),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF757575)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                enabled = isGpsAccurate
                            ) {
                                Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                val buttonText = if (uiState.recordedPoints.isEmpty()) {
                                    "पहला कोना (START)"
                                } else {
                                    "अगला कोना (#${uiState.recordedPoints.size + 1})"
                                }
                                Text(
                                    text = buttonText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (uiState.isPaused) {
                                OutlinedButton(
                                    onClick = { viewModel.resumeTracking() },
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .heightIn(min = 48.dp)
                                        .testTag("resume_tracking_button"),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.resume, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.pauseTracking() },
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .heightIn(min = 48.dp)
                                        .testTag("pause_tracking_button"),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.pause, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                                }
                            }
                        }

                        // Row 2: Secondary / Completion Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!uiState.isClosed && uiState.recordedPoints.size >= 3) {
                                OutlinedButton(
                                    onClick = { viewModel.closeBoundary() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("close_boundary_button"),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = strings.closeBoundary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.finishMeasurement()
                                    onNavigateToReview()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("complete_measurement_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF15803D),
                                    disabledContainerColor = Color(0xFFE0E0E0),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF757575)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                enabled = uiState.recordedPoints.size >= 3
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = strings.completeMeasurement,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .heightIn(min = 48.dp)
                                    .testTag("cancel_measurement_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = strings.cancel,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
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
        modifier = Modifier.fillMaxWidth().testTag("gps_status_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${strings.gpsSignalStatus}: ${uiState.gpsQuality.hindiLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uiState.currentAccuracy > 0f) {
                            "${strings.accuracy}: ±${String.format(Locale.US, "%.1f", uiState.currentAccuracy)} m"
                        } else {
                            "${strings.accuracy}: ${strings.waiting}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.detectedAreaName.isNotBlank()) {
                        Text(
                            text = "📍 स्थान: ${uiState.detectedAreaName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E5631),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!uiState.isGpsEnabled) {
                Button(
                    onClick = onEnableGpsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 38.dp).testTag("enable_gps_button")
                ) {
                    Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("GPS चालू करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (uiState.gpsQuality) {
                        GpsQualityLevel.EXCELLENT -> Color(0xFFDCFCE7)
                        GpsQualityLevel.GOOD -> Color(0xFFECFCCB)
                        GpsQualityLevel.MEDIUM -> Color(0xFFFEF9C3)
                        GpsQualityLevel.WEAK -> Color(0xFFFEE2E2)
                        GpsQualityLevel.UNAVAILABLE -> Color(0xFFE2E8F0)
                    }
                ) {
                    Text(
                        text = if (uiState.isTracking) (if (uiState.isPaused) "विराम (Paused)" else "सक्रिय (Active)") else "तैयार (Ready)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = when (uiState.gpsQuality) {
                            GpsQualityLevel.EXCELLENT -> Color(0xFF166534)
                            GpsQualityLevel.GOOD -> Color(0xFF3F6212)
                            GpsQualityLevel.MEDIUM -> Color(0xFF854D0E)
                            GpsQualityLevel.WEAK -> Color(0xFF991B1B)
                            GpsQualityLevel.UNAVAILABLE -> Color(0xFF475569)
                        }
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
        modifier = Modifier.fillMaxWidth().testTag("area_metrics_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.estimatedArea,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bihar.toHindiString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.estimatedPerimeter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", perimeterM)} m (${String.format(java.util.Locale.US, "%.1f", perimeterM * 3.28084)} ft)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${String.format(Locale.US, "%.2f", bihar.acres)} Acres (${String.format(Locale.US, "%.1f", bihar.dismil)} Dismil) • ${String.format(Locale.US, "%.1f", areaSqM)} sq.m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5.5, 6.0, 6.5, 7.0).forEach { size ->
                        FilterChip(
                            selected = laggiHaath == size,
                            onClick = { onLaggiChange(size) },
                            label = { Text("$size", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.heightIn(min = 32.dp)
                        )
                    }
                }
            }
        }
    }
}
