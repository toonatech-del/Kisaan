package com.example.ui.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnitConverterScreen(
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var laggiHaath by remember { mutableDoubleStateOf(com.example.util.PreferencesManager.getDefaultLaggi(context)) }
    var selectedFromUnit by remember { mutableStateOf("DISMIL") }
    var inputValueText by remember { mutableStateOf("10") }
    val strings = AppStrings.get(currentLanguage)

    val inputVal = inputValueText.toDoubleOrNull() ?: 0.0

    val baseSqM = remember(inputVal, selectedFromUnit, laggiHaath) {
        when (selectedFromUnit) {
            "DISMIL" -> inputVal * GeoLandCalculator.DISMIL_TO_SQ_METERS
            "ACRE" -> inputVal * GeoLandCalculator.ACRE_TO_SQ_METERS
            "HECTARE" -> inputVal * GeoLandCalculator.HECTARE_TO_SQ_METERS
            "SQ_M" -> inputVal
            "SQ_FT" -> inputVal / GeoLandCalculator.SQ_METERS_TO_SQ_FEET
            "KATHA" -> GeoLandCalculator.biharUnitsToSqM(0.0, inputVal, 0.0, laggiHaath)
            "BIGHA" -> GeoLandCalculator.biharUnitsToSqM(inputVal, 0.0, 0.0, laggiHaath)
            else -> inputVal
        }
    }

    val breakdown = GeoLandCalculator.convertToBiharUnits(baseSqM, laggiHaath)

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text(strings.unitConverterTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("converter_back_button")) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            strings.selectInitialUnit,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "DISMIL" to "Dismil",
                                "ACRE" to "Acre",
                                "KATHA" to "Katha",
                                "BIGHA" to "Bigha",
                                "SQ_M" to "Sq.Meters",
                                "SQ_FT" to "Sq.Feet"
                            ).forEach { (unitKey, unitLabel) ->
                                FilterChip(
                                    selected = selectedFromUnit == unitKey,
                                    onClick = { selectedFromUnit = unitKey },
                                    label = { Text(unitLabel) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputValueText,
                            onValueChange = { inputValueText = it },
                            label = { Text(strings.enterValue) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("converter_input_value")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${strings.laggiMeasurement} (haath): ", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0).forEach { size ->
                                    FilterChip(
                                        selected = laggiHaath == size,
                                        onClick = { laggiHaath = size },
                                        label = { Text("$size", fontSize = 11.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    strings.convertedMeasurements,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "${strings.biharLocalMeasurement} (Laggi: $laggiHaath):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            breakdown.toHindiString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${strings.totalDhur}: ${String.format(Locale.US, "%.2f", breakdown.totalDhur)} Dhur",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultRow("Acre:", "${String.format(Locale.US, "%.4f", breakdown.acres)} Acres")
                        ResultRow("Dismil:", "${String.format(Locale.US, "%.2f", breakdown.dismil)} Dismil")
                        ResultRow("Hectare:", "${String.format(Locale.US, "%.4f", breakdown.hectares)} Hectares")
                        ResultRow("Sq. Meters:", "${String.format(Locale.US, "%.2f", breakdown.sqMetres)} sq.m")
                        ResultRow("Sq. Feet:", "${String.format(Locale.US, "%.1f", breakdown.sqFeet)} sq.ft")
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            strings.biharFormulaInfo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "• 1 haath = 1.5 feet (18 inches)\n" +
                                    "• 1 Dhur = (Laggi length)² sq.ft\n" +
                                    "  Example ($laggiHaath haath): 1 Dhur = ${laggiHaath * 1.5} × ${laggiHaath * 1.5} = ${String.format(Locale.US, "%.2f", (laggiHaath * 1.5) * (laggiHaath * 1.5))} sq.ft\n" +
                                    "• 20 Dhur = 1 Katha\n" +
                                    "• 20 Katha = 1 Bigha (400 Dhur)\n" +
                                    "• 1 Acre = 100 Dismil = 43,560 sq.ft",
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
