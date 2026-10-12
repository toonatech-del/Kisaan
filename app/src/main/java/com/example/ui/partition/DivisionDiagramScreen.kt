package com.example.ui.partition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.GeoLandCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionDiagramScreen(
    totalAreaSqM: Double,
    laggiHaath: Double,
    heirs: List<HeirShare>,
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit
) {
    val totalWeight = heirs.sumOf { it.shareWeight }.coerceAtLeast(0.0001)
    val totalBreakdown = GeoLandCalculator.convertToBiharUnits(totalAreaSqM, laggiHaath)
    val strings = AppStrings.get(currentLanguage)

    val colorsList = listOf(
        Color(0xFFE8F5E9),
        Color(0xFFFFFBEB),
        Color(0xFFEFF6FF),
        Color(0xFFFDF2F8),
        Color(0xFFF1F5F9),
        Color(0xFFFEF2F2)
    )

    val borderColorsList = listOf(
        Color(0xFF4CAF50),
        Color(0xFFF59E0B),
        Color(0xFF3B82F6),
        Color(0xFFEC4899),
        Color(0xFF64748B),
        Color(0xFFEF4444)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.conceptualDiagramTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("diagram_back_button")) {
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
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFFFEFCE8),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFCA8A04),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.conceptualDisclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF713F12),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Total Land: ${totalBreakdown.toHindiString()}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "(${String.format(Locale.US, "%.2f", totalBreakdown.dismil)} Dismil • ${String.format(Locale.US, "%.1f", totalAreaSqM)} sq.m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    strings.plotBlocks,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(heirs) { index, heir ->
                val fraction = heir.shareWeight / totalWeight
                val areaSqM = totalAreaSqM * fraction
                val breakdown = GeoLandCalculator.convertToBiharUnits(areaSqM, laggiHaath)
                val percentage = fraction * 100.0
                val bg = colorsList[index % colorsList.size]
                val borderCol = borderColorsList[index % borderColorsList.size]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, borderCol, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = bg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Share #${index + 1}: ${heir.name.ifBlank { "Heir #${index + 1}" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = borderCol
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", percentage)}%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (heir.relation.isNotBlank()) {
                            Text(
                                text = "Relation: ${heir.relation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Area: ${breakdown.toHindiString()}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", breakdown.dismil)} Dismil (${String.format(Locale.US, "%.0f", breakdown.sqFeet)} sq.ft)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
