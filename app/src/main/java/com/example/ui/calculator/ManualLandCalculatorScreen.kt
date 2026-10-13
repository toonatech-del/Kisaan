package com.example.ui.calculator

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import java.util.Locale

enum class ManualCalcUnit(val labelHindi: String, val labelEnglish: String) {
    FEET("फ़ीट (Feet)", "Feet"),
    KADI("कड़ी / जरीब (Kadi)", "Kadi")
}

data class BiharManualLandResult(
    val northFeet: Double,
    val southFeet: Double,
    val eastFeet: Double,
    val westFeet: Double,
    val avgLengthFeet: Double,
    val avgWidthFeet: Double,
    val totalAreaSqFt: Double,
    val dismil: Double,
    val bigha: Int,
    val kattha: Int,
    val dhur: Double,
    val laggiHaath: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLandCalculatorScreen(
    currentLanguage: AppLanguage = AppLanguage.HINDI,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedUnit by remember { mutableStateOf(ManualCalcUnit.FEET) }
    var selectedLaggi by remember { mutableDoubleStateOf(6.0) }
    var laggiDropdownExpanded by remember { mutableStateOf(false) }

    var northInput by remember { mutableStateOf("") }
    var southInput by remember { mutableStateOf("") }
    var eastInput by remember { mutableStateOf("") }
    var westInput by remember { mutableStateOf("") }

    val laggiOptions = listOf(5.5, 6.0, 6.5, 7.0)

    val northRaw = northInput.toDoubleOrNull() ?: 0.0
    val southRaw = southInput.toDoubleOrNull() ?: 0.0
    val eastRaw = eastInput.toDoubleOrNull() ?: 0.0
    val westRaw = westInput.toDoubleOrNull() ?: 0.0

    // 1 Kadi = 0.66 Feet (7.92 inches)
    val factorToFeet = if (selectedUnit == ManualCalcUnit.KADI) 0.66 else 1.0

    val northFeet = northRaw * factorToFeet
    val southFeet = southRaw * factorToFeet
    val eastFeet = eastRaw * factorToFeet
    val westFeet = westRaw * factorToFeet

    val hasInputs = (northRaw > 0 || southRaw > 0) && (eastRaw > 0 || westRaw > 0)

    val calculationResult: BiharManualLandResult? = remember(
        northFeet, southFeet, eastFeet, westFeet, selectedLaggi, hasInputs
    ) {
        if (!hasInputs) null
        else {
            val avgLength = (northFeet + southFeet) / 2.0
            val avgWidth = (eastFeet + westFeet) / 2.0
            val totalAreaSqFt = avgLength * avgWidth
            val dismil = totalAreaSqFt / 435.6

            // 1 Dhur (Sq.Ft) = (Laggi * 1.5)^2
            val dhurInSqFt = (selectedLaggi * 1.5) * (selectedLaggi * 1.5)
            val totalDhur = if (dhurInSqFt > 0) totalAreaSqFt / dhurInSqFt else 0.0

            val totalKattha = (totalDhur / 20.0).toInt()
            val remainingDhur = totalDhur % 20.0
            val bigha = totalKattha / 20
            val kattha = totalKattha % 20

            BiharManualLandResult(
                northFeet = northFeet,
                southFeet = southFeet,
                eastFeet = eastFeet,
                westFeet = westFeet,
                avgLengthFeet = avgLength,
                avgWidthFeet = avgWidth,
                totalAreaSqFt = totalAreaSqFt,
                dismil = dismil,
                bigha = bigha,
                kattha = kattha,
                dhur = remainingDhur,
                laggiHaath = selectedLaggi
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "फीता / कड़ी नाप कैलकुलेटर",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "बिहार जमीन पैमाइश (100% सटीक)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1E7DD)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("calculator_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E5631)
                )
            )
        },
        containerColor = Color(0xFFF4F7F4)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unit & Laggi Selection Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "१. नाप की इकाई एवं लग्गी चुनें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E5631)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Unit Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedUnit == ManualCalcUnit.FEET,
                                onClick = { selectedUnit = ManualCalcUnit.FEET },
                                label = { Text(ManualCalcUnit.FEET.labelHindi, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = if (selectedUnit == ManualCalcUnit.FEET) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unit_feet_chip")
                            )

                            FilterChip(
                                selected = selectedUnit == ManualCalcUnit.KADI,
                                onClick = { selectedUnit = ManualCalcUnit.KADI },
                                label = { Text(ManualCalcUnit.KADI.labelHindi, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = if (selectedUnit == ManualCalcUnit.KADI) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unit_kadi_chip")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Laggi Dropdown Selector
                        Text(
                            text = "लग्गी (हाथ में):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { laggiDropdownExpanded = true }
                                    .testTag("laggi_dropdown_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedLaggi} हाथ (Laggi) ${if (selectedLaggi == 6.0) "- डिफ़ॉल्ट बिहार" else ""}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E5631)
                                    )
                                    Text(
                                        text = "बदलें ▼",
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = laggiDropdownExpanded,
                                onDismissRequest = { laggiDropdownExpanded = false }
                            ) {
                                laggiOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$opt हाथ ${if (opt == 6.0) "(अधिकांश बिहार)" else ""}",
                                                fontWeight = if (opt == selectedLaggi) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedLaggi = opt
                                            laggiDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4 Boundary Inputs Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "२. खेत की चारों मेड़ों की नाप दर्ज करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E5631)
                        )
                        val unitLabel = if (selectedUnit == ManualCalcUnit.FEET) "फ़ीट" else "कड़ी"
                        Text(
                            text = "इकाई: $unitLabel में माप लिखें",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Lengths: North & South
                        Text(
                            text = "लंबाई (Length):",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = northInput,
                                onValueChange = { northInput = it },
                                label = { Text("उत्तर (North)") },
                                placeholder = { Text("उदा. 120") },
                                trailingIcon = { Text(unitLabel, fontSize = 11.sp, color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E5631),
                                    unfocusedBorderColor = Color(0xFFB0BEC5)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_north")
                            )

                            OutlinedTextField(
                                value = southInput,
                                onValueChange = { southInput = it },
                                label = { Text("दक्षिण (South)") },
                                placeholder = { Text("उदा. 118") },
                                trailingIcon = { Text(unitLabel, fontSize = 11.sp, color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E5631),
                                    unfocusedBorderColor = Color(0xFFB0BEC5)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_south")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Widths: East & West
                        Text(
                            text = "चौड़ाई (Width):",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = eastInput,
                                onValueChange = { eastInput = it },
                                label = { Text("पूरब (East)") },
                                placeholder = { Text("उदा. 80") },
                                trailingIcon = { Text(unitLabel, fontSize = 11.sp, color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E5631),
                                    unfocusedBorderColor = Color(0xFFB0BEC5)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_east")
                            )

                            OutlinedTextField(
                                value = westInput,
                                onValueChange = { westInput = it },
                                label = { Text("पश्चिम (West)") },
                                placeholder = { Text("उदा. 82") },
                                trailingIcon = { Text(unitLabel, fontSize = 11.sp, color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E5631),
                                    unfocusedBorderColor = Color(0xFFB0BEC5)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_west")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Reset & Quick Fill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    northInput = ""
                                    southInput = ""
                                    eastInput = ""
                                    westInput = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("reset_inputs_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("रीसेट (Clear)")
                            }

                            if (selectedUnit == ManualCalcUnit.KADI) {
                                Text(
                                    text = "१ कड़ी = ०.६६ फ़ीट (७.९२ इंच)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF555555)
                                )
                            }
                        }
                    }
                }
            }

            // Big Bold Result Card
            item {
                if (calculationResult != null && calculationResult.totalAreaSqFt > 0) {
                    val res = calculationResult
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculation_result_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "जमीन का कुल रकबा (परिणाम)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E5631)
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text(
                                        text = "${res.laggiHaath} हाथ लग्गी",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Big Highlight: Bigha, Kattha, Dhur
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val formattedResultText = buildString {
                                        if (res.bigha > 0) append("${res.bigha} बीघा ")
                                        append("${res.kattha} कट्ठा ")
                                        append(String.format(Locale.US, "%.2f", res.dhur))
                                        append(" धूर")
                                    }

                                    Text(
                                        text = formattedResultText,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E5631),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.3f", res.dismil)} डिसमिल (Dismil)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Detailed Breakdown
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F8F2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ResultRow(
                                        label = "औसत लंबाई:",
                                        value = "${String.format(Locale.US, "%.2f", res.avgLengthFeet)} फ़ीट"
                                    )
                                    ResultRow(
                                        label = "औसत चौड़ाई:",
                                        value = "${String.format(Locale.US, "%.2f", res.avgWidthFeet)} फ़ीट"
                                    )
                                    ResultRow(
                                        label = "कुल वर्गफ़ीट (Sq.Ft):",
                                        value = "${String.format(Locale.US, "%.2f", res.totalAreaSqFt)} sq.ft"
                                    )
                                    ResultRow(
                                        label = "कुल डिसमिल (Dismil):",
                                        value = "${String.format(Locale.US, "%.3f", res.dismil)} डिसमिल"
                                    )
                                    ResultRow(
                                        label = "धूर मान (${res.laggiHaath} हाथ):",
                                        value = "${String.format(Locale.US, "%.2f", (res.laggiHaath * 1.5) * (res.laggiHaath * 1.5))} sq.ft / धूर"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Share Button
                            Button(
                                onClick = {
                                    val shareText = """
                                        🌾 बिहार जमीन नाप (फीता/कड़ी कैलकुलेटर) 🌾
                                        ---------------------------------------
                                        उत्तर: ${northInput} ${selectedUnit.labelHindi}
                                        दक्षिण: ${southInput} ${selectedUnit.labelHindi}
                                        पूरब: ${eastInput} ${selectedUnit.labelHindi}
                                        पश्चिम: ${westInput} ${selectedUnit.labelHindi}
                                        लग्गी: ${res.laggiHaath} हाथ
                                        ---------------------------------------
                                        कुल क्षेत्रफल: ${String.format(Locale.US, "%.2f", res.totalAreaSqFt)} वर्ग फ़ीट
                                        डिसमिल: ${String.format(Locale.US, "%.3f", res.dismil)} Dismil
                                        बीघा-कट्ठा-धूर: ${if (res.bigha > 0) "${res.bigha} बीघा " else ""}${res.kattha} कट्ठा ${String.format(Locale.US, "%.2f", res.dhur)} धूर
                                        ---------------------------------------
                                        नाप: किसान ज़मीन ऐप (बिहार मानक)
                                    """.trimIndent()

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "जमीन नाप रिपोर्ट शेयर करें"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5631)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("share_measurement_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("परिणाम साझा करें (Share)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "खेत की चारों भुजाओं का मान दर्ज करने पर कट्ठा, धूर और डिसमिल का परिणाम तुरंत यहाँ दिखेगा।",
                                fontSize = 13.5.sp,
                                color = Color(0xFF444444)
                            )
                        }
                    }
                }
            }

            // Info note at bottom
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF9E6),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📌 बिहार अमीन नियम (औसत विधि):",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A5300)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• औसत लंबाई = (उत्तर + दक्षिण) ÷ २\n• औसत चौड़ाई = (पूरब + पश्चिम) ÷ २\n• १ डिसमिल = ४३५.६ वर्ग फ़ीट\n• १ धूर = (लग्गी × १.५)² वर्ग फ़ीट\n• २० धूर = १ कट्ठा, २० कट्ठा = १ बीघा",
                            fontSize = 11.5.sp,
                            color = Color(0xFF5E3900),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
        Text(text = label, fontSize = 12.5.sp, color = Color(0xFF444444))
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E5631))
    }
}
