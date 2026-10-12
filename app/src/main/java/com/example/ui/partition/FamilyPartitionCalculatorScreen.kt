package com.example.ui.partition

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.LandMeasurementEntity
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import com.example.util.GeoPoint
import com.example.util.PdfReportGenerator
import java.io.File
import java.util.Locale

data class HeirShare(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    var name: String = "",
    var relation: String = "",
    var shareWeight: Double = 1.0
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyPartitionCalculatorScreen(
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit,
    onViewDiagram: (Double, Double, List<HeirShare>) -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.get(currentLanguage)

    var laggiHaath by remember { mutableDoubleStateOf(6.0) }
    var inputMode by remember { mutableStateOf("BIHAR") }

    var bighaText by remember { mutableStateOf("1") }
    var kathaText by remember { mutableStateOf("4") }
    var dhurText by remember { mutableStateOf("10") }

    var acreText by remember { mutableStateOf("1") }
    var dismilText by remember { mutableStateOf("25") }

    val heirs = remember {
        mutableStateListOf(
            HeirShare(name = "Rameshwar Yadav", relation = "Elder Son", shareWeight = 1.0),
            HeirShare(name = "Suresh Yadav", relation = "Younger Son", shareWeight = 1.0),
            HeirShare(name = "Sunita Devi", relation = "Daughter", shareWeight = 1.0)
        )
    }

    val totalSqM = remember(inputMode, bighaText, kathaText, dhurText, acreText, dismilText, laggiHaath) {
        if (inputMode == "BIHAR") {
            val b = bighaText.toDoubleOrNull() ?: 0.0
            val k = kathaText.toDoubleOrNull() ?: 0.0
            val d = dhurText.toDoubleOrNull() ?: 0.0
            GeoLandCalculator.biharUnitsToSqM(b, k, d, laggiHaath)
        } else {
            val a = acreText.toDoubleOrNull() ?: 0.0
            val dis = dismilText.toDoubleOrNull() ?: 0.0
            (a * GeoLandCalculator.ACRE_TO_SQ_METERS) + (dis * GeoLandCalculator.DISMIL_TO_SQ_METERS)
        }
    }

    val totalBiharBreakdown = GeoLandCalculator.convertToBiharUnits(totalSqM, laggiHaath)
    val totalWeight = heirs.sumOf { it.shareWeight }.coerceAtLeast(0.0001)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.familyPartitionTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("partition_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onViewDiagram(totalSqM, laggiHaath, heirs) },
                        modifier = Modifier.testTag("view_diagram_button")
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = "View Diagram", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            strings.enterTotalLand,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = inputMode == "BIHAR",
                                onClick = { inputMode = "BIHAR" },
                                label = { Text("Bigha - Katha - Dhur") }
                            )
                            FilterChip(
                                selected = inputMode == "ACRE",
                                onClick = { inputMode = "ACRE" },
                                label = { Text("Acres - Dismil") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (inputMode == "BIHAR") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bighaText,
                                    onValueChange = { bighaText = it },
                                    label = { Text("Bigha") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_bigha")
                                )
                                OutlinedTextField(
                                    value = kathaText,
                                    onValueChange = { kathaText = it },
                                    label = { Text("Katha") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_katha")
                                )
                                OutlinedTextField(
                                    value = dhurText,
                                    onValueChange = { dhurText = it },
                                    label = { Text("Dhur") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_dhur")
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = acreText,
                                    onValueChange = { acreText = it },
                                    label = { Text("Acres") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_acre")
                                )
                                OutlinedTextField(
                                    value = dismilText,
                                    onValueChange = { dismilText = it },
                                    label = { Text("Dismil") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_dismil")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${strings.laggiMeasurement}: ", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5.5, 6.0, 6.5, 7.0).forEach { s ->
                                    FilterChip(
                                        selected = laggiHaath == s,
                                        onClick = { laggiHaath = s },
                                        label = { Text("$s", fontSize = 11.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Area:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    totalBiharBreakdown.toHindiString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "${String.format(Locale.US, "%.3f", totalBiharBreakdown.acres)} Acres (${String.format(Locale.US, "%.1f", totalBiharBreakdown.dismil)} Dismil) • ${String.format(Locale.US, "%.1f", totalSqM)} sq.m",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onViewDiagram(totalSqM, laggiHaath, heirs) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.viewDivisionDiagram, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${strings.heirsCount} (${heirs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            heirs.add(HeirShare(name = "Heir #${heirs.size + 1}", relation = "Co-sharer", shareWeight = 1.0))
                        },
                        modifier = Modifier.testTag("add_heir_button")
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addHeir, fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(heirs) { index, heir ->
                val fraction = heir.shareWeight / totalWeight
                val heirSqM = totalSqM * fraction
                val heirBreakdown = GeoLandCalculator.convertToBiharUnits(heirSqM, laggiHaath)
                val percentage = fraction * 100.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = heir.name,
                                    onValueChange = {
                                        heirs[index] = heir.copy(name = it)
                                    },
                                    label = { Text(strings.heirName) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("heir_name_$index")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = heir.relation,
                                        onValueChange = {
                                            heirs[index] = heir.copy(relation = it)
                                        },
                                        label = { Text(strings.relationOrRole) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = heir.shareWeight.toString(),
                                        onValueChange = {
                                            val w = it.toDoubleOrNull() ?: 1.0
                                            heirs[index] = heir.copy(shareWeight = w)
                                        },
                                        label = { Text(strings.shareRatio) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (heirs.size > 1) {
                                IconButton(
                                    onClick = { heirs.removeAt(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = Color(0xFFDC2626))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${strings.allocatedShare} (${String.format(Locale.US, "%.1f", percentage)}%):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        heirBreakdown.toHindiString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        "${String.format(Locale.US, "%.2f", heirBreakdown.dismil)} Dismil • ${String.format(Locale.US, "%.0f", heirBreakdown.sqFeet)} sq.ft",
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFFFEFCE8),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Partition Advice:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF854D0E),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            strings.partitionAdvice,
                            color = Color(0xFF713F12),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
