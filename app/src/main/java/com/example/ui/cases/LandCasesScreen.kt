package com.example.ui.cases

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.AppDatabase
import com.example.data.model.LandCaseEntity
import com.example.data.model.PointsConverter
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandCasesScreen(
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit,
    onStartMeasurementForCase: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val caseDao = remember { db.landCaseDao() }
    val measurementDao = remember { db.landMeasurementDao() }

    val cases by caseDao.getAllCases().collectAsState(initial = emptyList())
    val strings = AppStrings.get(currentLanguage)

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCaseForDetail by remember { mutableStateOf<LandCaseEntity?>(null) }
    var caseToDelete by remember { mutableStateOf<LandCaseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.landCasesTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("cases_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_case_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addNewCase)
            }
        }
    ) { paddingValues ->
        if (cases.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    strings.noCasesYet,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    strings.noCasesDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_case_empty_button")
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addNewCase)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cases, key = { it.id }) { landCase ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCaseForDetail = landCase }
                            .testTag("case_item_${landCase.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        landCase.farmerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "${strings.villageMouza}: ${landCase.villageMouza} ${if (landCase.thanaNumber.isNotBlank()) "(Thana ${landCase.thanaNumber})" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { caseToDelete = landCase }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "${strings.khataNo}: ${landCase.khataNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${strings.khesraNo}: ${landCase.khesraNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (landCase.jamabandiNumber.isNotBlank()) {
                                    Text(
                                        "Jamabandi: ${landCase.jamabandiNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (landCase.recordedRakba.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${strings.recordedRakba}: ${landCase.recordedRakba}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onStartMeasurementForCase(landCase.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("measure_case_button_${landCase.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    Icons.Default.AddLocation,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    strings.measureForCase,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var farmerName by remember { mutableStateOf("") }
        var mouza by remember { mutableStateOf("") }
        var thana by remember { mutableStateOf("") }
        var khata by remember { mutableStateOf("") }
        var khesra by remember { mutableStateOf("") }
        var jamabandi by remember { mutableStateOf("") }
        var rakba by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addNewCase, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = farmerName,
                            onValueChange = { farmerName = it },
                            label = { Text("${strings.farmerName} *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_farmer_name"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = mouza,
                            onValueChange = { mouza = it },
                            label = { Text("${strings.villageMouza} *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_mouza"),
                            singleLine = true
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = thana,
                                onValueChange = { thana = it },
                                label = { Text(strings.thanaNo) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = jamabandi,
                                onValueChange = { jamabandi = it },
                                label = { Text(strings.jamabandiNo) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = khata,
                                onValueChange = { khata = it },
                                label = { Text("${strings.khataNo} *") },
                                modifier = Modifier.weight(1f).testTag("input_khata"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = khesra,
                                onValueChange = { khesra = it },
                                label = { Text("${strings.khesraNo} *") },
                                modifier = Modifier.weight(1f).testTag("input_khesra"),
                                singleLine = true
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = rakba,
                            onValueChange = { rakba = it },
                            label = { Text(strings.recordedRakba) },
                            modifier = Modifier.fillMaxWidth().testTag("input_rakba"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(strings.notesOptional) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (farmerName.isNotBlank() && mouza.isNotBlank() && khata.isNotBlank() && khesra.isNotBlank()) {
                            scope.launch {
                                caseDao.insertCase(
                                    LandCaseEntity(
                                        farmerName = farmerName.trim(),
                                        villageMouza = mouza.trim(),
                                        thanaNumber = thana.trim(),
                                        khataNumber = khata.trim(),
                                        khesraNumber = khesra.trim(),
                                        jamabandiNumber = jamabandi.trim(),
                                        recordedRakba = rakba.trim(),
                                        notes = notes.trim()
                                    )
                                )
                                showAddDialog = false
                                Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_case_submit_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    caseToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { caseToDelete = null },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text("${strings.deleteConfirmMsg} ('${c.farmerName}')?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            caseDao.deleteCase(c)
                            caseToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { caseToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    selectedCaseForDetail?.let { currentCase ->
        val caseMeasurements by measurementDao.getMeasurementsForCase(currentCase.id).collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { selectedCaseForDetail = null },
            title = {
                Column {
                    Text(currentCase.farmerName, fontWeight = FontWeight.Bold)
                    Text("Mouza: ${currentCase.villageMouza} • Khata: ${currentCase.khataNumber} • Khesra: ${currentCase.khesraNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (currentCase.recordedRakba.isNotBlank()) {
                                    Text("Recorded Area: ${currentCase.recordedRakba}", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                }
                                if (currentCase.notes.isNotBlank()) {
                                    Text("Notes: ${currentCase.notes}", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${strings.attachedMeasurements} (${caseMeasurements.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (caseMeasurements.isEmpty()) {
                        item {
                            Text("No GPS measurements attached yet.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        items(caseMeasurements) { m ->
                            val bihar = GeoLandCalculator.convertToBiharUnits(m.areaSqM, m.laggiHaath)
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(m.timestamp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(bihar.toHindiString(), color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Perimeter: ${String.format(Locale.US, "%.1f", m.perimeterM)} m • Date: $dateStr", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                val points = PointsConverter.fromJson(m.pointsJson)
                                                val pdfFile = PdfReportGenerator.generateMeasurementPdf(
                                                    context = context,
                                                    measurement = m,
                                                    points = points,
                                                    landCase = currentCase,
                                                    language = currentLanguage
                                                )
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "View PDF Report"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Report", tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = currentCase.id
                        selectedCaseForDetail = null
                        onStartMeasurementForCase(cid)
                    }
                ) {
                    Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.measureForCase)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCaseForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}
