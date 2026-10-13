package com.example.ui.measurement

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.core.content.FileProvider
import com.example.data.model.LandMeasurementEntity
import com.example.data.model.PointsConverter
import com.example.ui.components.SchematicPlotView
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import com.example.util.PdfReportGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementReviewScreen(
    viewModel: LiveMeasurementViewModel,
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit,
    onSavedDone: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val strings = AppStrings.get(currentLanguage)

    BackHandler(onBack = onNavigateBack)

    var title by remember {
        mutableStateOf(
            if (uiState.linkedLandCase != null) {
                "${uiState.linkedLandCase!!.farmerName} - Khesra ${uiState.linkedLandCase!!.khesraNumber}"
            } else {
                "Field Measurement #${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
            }
        )
    }
    var notes by remember { mutableStateOf("") }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val bihar = GeoLandCalculator.convertToBiharUnits(uiState.estimatedAreaSqM, uiState.selectedLaggiHaath)
    val avgAccuracy = if (uiState.recordedPoints.isNotEmpty()) {
        uiState.recordedPoints.map { it.accuracy }.average().toFloat()
    } else 0f

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text(strings.measurementReviewTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("review_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("review_area_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    strings.finalEstimatedArea,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    bihar.toHindiString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    softWrap = true
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${uiState.selectedLaggiHaath} Laggi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ReviewMetricRow(
                            label = "Acres / Dismil:",
                            value = "${String.format(Locale.US, "%.3f", bihar.acres)} Acres (${String.format(Locale.US, "%.2f", bihar.dismil)} Dismil)"
                        )

                        ReviewMetricRow(
                            label = "Sq.M / Sq.Ft:",
                            value = "${String.format(Locale.US, "%.1f", uiState.estimatedAreaSqM)} sq.m (${String.format(Locale.US, "%.0f", bihar.sqFeet)} sq.ft)"
                        )

                        ReviewMetricRow(
                            label = "Perimeter:",
                            value = "${String.format(Locale.US, "%.1f", uiState.estimatedPerimeterM)} meters"
                        )

                        ReviewMetricRow(
                            label = "GPS Accuracy:",
                            value = "±${String.format(Locale.US, "%.1f", avgAccuracy)} m (${uiState.gpsQuality.hindiLabel})"
                        )

                        if (uiState.wasManuallyEdited) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("✓ Manually edited boundary.", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Text(strings.fieldPlotDiagram, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                SchematicPlotView(
                    points = uiState.recordedPoints,
                    currentLocation = null,
                    isClosed = true,
                    selectedPointIndex = selectedPointIndex,
                    onPointSelected = { selectedPointIndex = it },
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            }

            item {
                Text("${strings.boundaryPointsList} (${uiState.recordedPoints.size} points)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            itemsIndexed(uiState.recordedPoints) { index, pt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (selectedPointIndex == index) Color(0xFFEFF6FF) else Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Point #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Lat: ${String.format(Locale.US, "%.5f", pt.lat)}, Lng: ${String.format(Locale.US, "%.5f", pt.lng)} (±${String.format(Locale.US, "%.1f", pt.accuracy)} m)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (uiState.recordedPoints.size > 3) {
                            IconButton(onClick = { viewModel.removePointAt(index) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = strings.deletePoint, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                uiState.linkedLandCase?.let { c ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(strings.linkedLandCase, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("${c.farmerName} • Mouza: ${c.villageMouza} • Khata: ${c.khataNumber} • Khesra: ${c.khesraNumber}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.measurementTitleLabel) },
                    modifier = Modifier.fillMaxWidth().testTag("measurement_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesOptional) },
                    modifier = Modifier.fillMaxWidth().testTag("measurement_notes_input"),
                    minLines = 2
                )
            }

            item {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Legal Disclaimer:", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(strings.legalDisclaimer, color = Color(0xFF7F1D1D), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (uiState.linkedLandCase != null) {
                                showOverwriteDialog = true
                            } else {
                                viewModel.saveMeasurement(title, notes) {
                                    Toast.makeText(context, strings.savedSuccessfully, Toast.LENGTH_SHORT).show()
                                    onSavedDone()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("confirm_and_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.confirmAndSave, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val dummyMeasurement = LandMeasurementEntity(
                                    title = title,
                                    pointsJson = PointsConverter.toJson(uiState.recordedPoints),
                                    pointCount = uiState.recordedPoints.size,
                                    areaSqM = uiState.estimatedAreaSqM,
                                    perimeterM = uiState.estimatedPerimeterM,
                                    laggiHaath = uiState.selectedLaggiHaath,
                                    averageAccuracyM = avgAccuracy,
                                    gpsQualitySummary = "Avg Accuracy: ±${String.format(Locale.US, "%.1f", avgAccuracy)} m",
                                    notes = notes,
                                    wasManuallyEdited = uiState.wasManuallyEdited
                                )
                                val file = PdfReportGenerator.generateMeasurementPdf(
                                    context = context,
                                    measurement = dummyMeasurement,
                                    points = uiState.recordedPoints,
                                    landCase = uiState.linkedLandCase,
                                    language = currentLanguage
                                )
                                Toast.makeText(context, "PDF generated: ${file.name}", Toast.LENGTH_LONG).show()

                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "View PDF Report"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "PDF error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportPdf, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }
                }
            }
        }
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("Save Measurement") },
            text = { Text("Do you want to attach this measurement to case '${uiState.linkedLandCase?.farmerName}'?") },
            confirmButton = {
                Button(onClick = {
                    showOverwriteDialog = false
                    viewModel.saveMeasurement(title, notes) {
                        Toast.makeText(context, strings.savedSuccessfully, Toast.LENGTH_SHORT).show()
                        onSavedDone()
                    }
                }) {
                    Text("Yes, Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReviewMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.3f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
