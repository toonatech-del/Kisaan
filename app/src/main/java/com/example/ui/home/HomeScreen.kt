package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.model.LandMeasurementEntity
import com.example.data.model.PointsConverter
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onNavigateToLiveMeasurement: () -> Unit,
    onNavigateToCases: () -> Unit,
    onNavigateToPartition: () -> Unit,
    onNavigateToConverter: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManualCalculator: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val measurementDao = remember { db.landMeasurementDao() }
    val caseDao = remember { db.landCaseDao() }

    val recentMeasurements by measurementDao.getAllMeasurements().collectAsState(initial = emptyList())
    var measurementToDelete by remember { mutableStateOf<LandMeasurementEntity?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val isConnected = remember { checkInternetConnection(context) }
    val strings = AppStrings.get(currentLanguage)

    // Background gradient: Soft mint-tinted cream
    val screenBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF3F8F2),
            Color(0xFFEDF5EC),
            Color(0xFFE8F2E7)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            CustomBottomNavigationBar(
                currentLanguage = currentLanguage,
                onLanguageChanged = onLanguageChanged,
                onConverterClick = onNavigateToConverter,
                onRecordsClick = onNavigateToCases,
                onFabClick = onNavigateToLiveMeasurement,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBgBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. TOP HEADER BAR (Language Switcher removed from here)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular Leaf/Plant Sprout Logo
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E4D36),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = strings.appTitle,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF113824),
                                    maxLines = 1
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF3F634E),
                                    maxLines = 1
                                )
                            }
                        }

                        // Right action buttons: Circular Help Button (Language switcher removed)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular Outline Help Icon
                            IconButton(
                                onClick = onNavigateToHelp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("help_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = strings.helpAndInfo,
                                    tint = Color(0xFF3F634E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 2. CONNECTIVITY STATUS PILL BADGE
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFE5F3E7),
                        border = BorderStroke(1.dp, Color(0xFFC7E6CB)),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) strings.onlineStatus else strings.offlineStatus,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isConnected) Color(0xFF1B4D2E) else Color(0xFFB71C1C)
                            )
                        }
                    }
                }

                // 3. HERO BANNER CARD (Live GPS Land Measurement)
                item {
                    HeroMeasurementCard(
                        strings = strings,
                        onClick = onNavigateToLiveMeasurement
                    )
                }

                // 3b. ULTRA-SIMPLE FITA / KADI BIHAR MANUAL CALCULATOR CARD
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFC8E6C9)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToManualCalculator() }
                            .testTag("nav_fita_kadi_calculator")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SquareFoot,
                                        contentDescription = null,
                                        tint = Color(0xFF1E5631),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "फीता / कड़ी कैलकुलेटर",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF113824)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "बिहार मानक",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "खेत की ४ मेड़ (उत्तर-दक्षिण-पूरब-पश्चिम) से कट्ठा, धूर और डिसमिल निकालें",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A6B53),
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF1E5631),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 4. MAIN OPTIONS HEADING
                item {
                    Text(
                        text = strings.mainOptions,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 5. 2x2 FEATURE CARDS GRID (Exact match to screenshot)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // New Partition (Warm Yellow / Cream)
                        OptionGridCard(
                            title = strings.newPartition,
                            subtitle = strings.newPartitionDesc,
                            icon = Icons.Default.Calculate,
                            iconBgColor = Color(0xFFB6CDB9),
                            iconTint = Color(0xFF1E4D36),
                            cardBg = Brush.verticalGradient(listOf(Color(0xFFFBF4DC), Color(0xFFF5E8C7))),
                            borderColor = Color(0xFFEADBBA),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_partition_card"),
                            onClick = onNavigateToPartition
                        )

                        // My Land Records (Soft Sage / Mint)
                        OptionGridCard(
                            title = strings.myLandRecords,
                            subtitle = strings.myLandRecordsDesc,
                            icon = Icons.Default.Folder,
                            iconBgColor = Color(0xFF90B9AB),
                            iconTint = Color(0xFF1A4637),
                            cardBg = Brush.verticalGradient(listOf(Color(0xFFCFE5DC), Color(0xFFBEDBD0))),
                            borderColor = Color(0xFFAFD0C5),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_cases_card"),
                            onClick = onNavigateToCases
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Official Land Services (Sky / Lavender Blue)
                        OptionGridCard(
                            title = strings.biharServices,
                            subtitle = strings.biharServicesDesc,
                            icon = Icons.Default.Public,
                            iconBgColor = Color(0xFFA1BBD4),
                            iconTint = Color(0xFF1E3A5F),
                            cardBg = Brush.verticalGradient(listOf(Color(0xFFDBE5EF), Color(0xFFC9D8E7))),
                            borderColor = Color(0xFFB9CDE2),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_services_card"),
                            onClick = onNavigateToServices
                        )

                        // Unit Converter (Olive / Khaki)
                        OptionGridCard(
                            title = strings.unitConverter,
                            subtitle = strings.unitConverterDesc,
                            icon = Icons.Default.SquareFoot,
                            iconBgColor = Color(0xFFB5C09D),
                            iconTint = Color(0xFF384323),
                            cardBg = Brush.verticalGradient(listOf(Color(0xFFE5E9D5), Color(0xFFD8DEC2))),
                            borderColor = Color(0xFFCBD2B0),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_converter_card"),
                            onClick = onNavigateToConverter
                        )
                    }
                }

                // 6. RECENT MEASUREMENTS HEADING
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${strings.recentMeasurements} (${recentMeasurements.size})",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // 7. RECENT MEASUREMENTS LIST OR EMPTY STATE CARD
                if (recentMeasurements.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEDF6ED),
                            border = BorderStroke(1.dp, Color(0xFFD3E7D5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 24.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.noMeasurementsYet,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.startFirstMeasurement,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                } else {
                    items(recentMeasurements, key = { it.id }) { measurement ->
                        val bihar = GeoLandCalculator.convertToBiharUnits(measurement.areaSqM, measurement.laggiHaath)
                        val dateFormatted = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(Date(measurement.timestamp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("measurement_item_${measurement.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2EBE2)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = measurement.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = dateFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val points = PointsConverter.fromJson(measurement.pointsJson)
                                                        val landCase = measurement.landCaseId?.let { caseDao.getCaseById(it) }
                                                        val pdfFile = PdfReportGenerator.generateMeasurementPdf(
                                                            context = context,
                                                            measurement = measurement,
                                                            points = points,
                                                            landCase = landCase,
                                                            language = currentLanguage
                                                        )
                                                        val uri = FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.provider",
                                                            pdfFile
                                                        )
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, "application/pdf")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(intent, "PDF report"))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("pdf_btn_${measurement.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = "PDF Report",
                                                tint = Color(0xFFDC2626)
                                            )
                                        }

                                        IconButton(
                                            onClick = { measurementToDelete = measurement }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
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
                                                text = bihar.toHindiString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.3f", bihar.acres)} Acres (${String.format(Locale.US, "%.1f", bihar.dismil)} Dismil)",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF15803D)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Perimeter: ${String.format(Locale.US, "%.1f", measurement.perimeterM)} m",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${measurement.pointCount} pts",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    measurementToDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { measurementToDelete = null },
            title = { Text("Delete measurement?") },
            text = { Text("Do you want to delete '${m.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            measurementDao.deleteMeasurement(m)
                            measurementToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { measurementToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Hero Card matching the screenshot:
 * Dark emerald gradient with topographic contours watermark,
 * gold "★ Core Feature" tag, "📍 Live GPS Land Measurement" title,
 * white "Start Measurement →" button, and 3D compass badge on the right.
 */
@Composable
private fun HeroMeasurementCard(
    strings: com.example.util.Strings,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("core_live_measurement_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F3624),
                            Color(0xFF174732),
                            Color(0xFF1E523A)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 800f)
                    )
                )
        ) {
            // Topographic contour line watermark in the background
            Canvas(modifier = Modifier.matchParentSize()) {
                val contourColor = Color(0xFF387B5B).copy(alpha = 0.22f)
                val stroke = Stroke(width = 1.6f)

                // Top right wavy concentric curves
                for (radius in listOf(60f, 110f, 160f, 210f, 270f, 330f)) {
                    val path = Path().apply {
                        moveTo(size.width - radius * 1.3f, 0f)
                        cubicTo(
                            size.width - radius * 1.1f, radius * 0.7f,
                            size.width - radius * 0.6f, radius * 1.1f,
                            size.width, radius * 0.9f
                        )
                    }
                    drawPath(path, contourColor, style = stroke)
                }

                // Bottom left contour curves
                for (radius in listOf(80f, 140f, 200f, 280f)) {
                    val path = Path().apply {
                        moveTo(0f, size.height - radius * 0.8f)
                        cubicTo(
                            radius * 0.7f, size.height - radius * 0.6f,
                            radius * 1.2f, size.height - radius * 0.3f,
                            radius * 1.4f, size.height
                        )
                    }
                    drawPath(path, contourColor, style = stroke)
                }
            }

            // Foreground Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // ★ Core Feature Gold Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD4A359),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = strings.coreFeatureTag,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3E2802),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title with Red Pin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.liveMeasurementTitle.replace("📍", "").trim(),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 26.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.liveMeasurementSubtitle,
                        fontSize = 13.sp,
                        color = Color(0xFFD1E7DD),
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Measurement Button (White pill with dark green text)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("start_measurement_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.startMeasurementBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF133824)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 3D Compass Medallion
                Compass3DMedallion(modifier = Modifier.size(80.dp))
            }
        }
    }
}

/**
 * Stylized 3D Compass Medallion matching the hero card's circular compass graphic
 */
@Composable
private fun Compass3DMedallion(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Outer frosted glass ring
        Surface(
            shape = CircleShape,
            color = Color(0xFF2B5B44).copy(alpha = 0.55f),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Inner dial ring
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Dial tick marks
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 30).toDouble())
                        val startR = radius - 4f
                        val endR = radius
                        val start = Offset(
                            (center.x + startR * Math.cos(angle)).toFloat(),
                            (center.y + startR * Math.sin(angle)).toFloat()
                        )
                        val end = Offset(
                            (center.x + endR * Math.cos(angle)).toFloat(),
                            (center.y + endR * Math.sin(angle)).toFloat()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = start,
                            end = end,
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Needle pointing diagonally (NE)
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Color(0xFFF1F5F3),
                    modifier = Modifier
                        .size(46.dp)
                        .rotate(15f)
                )
            }
        }
    }
}

/**
 * 2x2 Option Grid Cards with rounded edges, circular icon, bold title, and subtitle
 */
@Composable
private fun OptionGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    cardBg: Brush,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon badge
                Surface(
                    shape = CircleShape,
                    color = iconBgColor,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Bottom Navigation Bar with docked curved shape and center floating navigation button
 */
@Composable
private fun CustomBottomNavigationBar(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onConverterClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onFabClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Dock Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFFF3F8F2),
            shadowElevation = 10.dp,
            border = BorderStroke(1.5.dp, Color(0xFFB0C9B3))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Converter
                IconButton(
                    onClick = onConverterClick,
                    modifier = Modifier.testTag("bottom_bar_converter")
                ) {
                    Icon(
                        imageVector = Icons.Default.SquareFoot,
                        contentDescription = "Converter",
                        tint = Color(0xFF1B4332),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Tab 2: Records
                IconButton(
                    onClick = onRecordsClick,
                    modifier = Modifier.testTag("bottom_bar_records")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Records",
                        tint = Color(0xFF5A7365),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Space for Center Floating Button
                Spacer(modifier = Modifier.width(56.dp))

                // Tab 4: Language Switcher
                Box {
                    IconButton(
                        onClick = { showLanguageMenu = true },
                        modifier = Modifier.testTag("bottom_bar_language")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = Color(0xFF5A7365),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.testTag("language_dropdown_menu_bottom")
                    ) {
                        DropdownMenuItem(
                            text = { Text("हिन्दी") },
                            onClick = { onLanguageChanged(AppLanguage.HINDI); showLanguageMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = { onLanguageChanged(AppLanguage.ENGLISH); showLanguageMenu = false }
                        )
                    }
                }

                // Tab 5: Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("bottom_bar_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF5A7365),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Center Floating Action Button (Rises above dock)
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glowing Halo Ring
            Surface(
                shape = CircleShape,
                color = Color(0xFFC7E8CB).copy(alpha = 0.5f),
                modifier = Modifier.size(62.dp)
            ) {}

            // Button Surface with Gold Ring & Dark Green Center
            Surface(
                onClick = onFabClick,
                shape = CircleShape,
                color = Color(0xFF173D2B),
                border = BorderStroke(2.5.dp, Color(0xFFD4AF37)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("bottom_bar_measurement")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Start Live Measurement",
                        tint = Color(0xFFF3C762),
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(45f)
                    )
                }
            }
        }
    }
}

private fun checkInternetConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val network = cm?.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
