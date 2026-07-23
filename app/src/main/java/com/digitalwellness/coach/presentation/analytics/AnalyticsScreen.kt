package com.digitalwellness.coach.presentation.analytics

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.AppUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    paddingValues: PaddingValues,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("App Analytics", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Period selector
        PeriodSelector(
            selected = uiState.selectedPeriod,
            onSelect = viewModel::selectPeriod,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Search
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Search apps…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total time header
                item {
                    TotalTimeHeader(
                        totalMs = uiState.totalTimeMs,
                        period = uiState.selectedPeriod
                    )
                }

                // Pie chart section placeholder (MPAndroidChart renders in AndroidView)
                if (uiState.filteredApps.isNotEmpty()) {
                    item {
                        AppDistributionCard(apps = uiState.filteredApps.take(6))
                    }
                }

                // App list
                items(uiState.filteredApps) { app ->
                    AppUsageRow(
                        app = app,
                        totalMs = uiState.totalTimeMs
                    )
                }

                if (uiState.filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(8.dp))
                                Text("No apps found", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        AnalyticsPeriod.DAILY to "Today",
        AnalyticsPeriod.WEEKLY to "Week",
        AnalyticsPeriod.MONTHLY to "Month"
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (period, label) ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun TotalTimeHeader(totalMs: Long, period: AnalyticsPeriod) {
    val periodLabel = when (period) {
        AnalyticsPeriod.DAILY -> "today"
        AnalyticsPeriod.WEEKLY -> "this week"
        AnalyticsPeriod.MONTHLY -> "this month"
    }
    val hours = totalMs / 3_600_000
    val minutes = (totalMs % 3_600_000) / 60_000

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Total usage $periodLabel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                Text("${hours}h ${minutes}m", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun AppDistributionCard(apps: List<AppUsage>) {
    val totalMs = apps.sumOf { it.usageTimeMs }.coerceAtLeast(1L)
    val colors = listOf(
        Color(0xFF6B52F5), Color(0xFF00BFA5), Color(0xFFFFAB00),
        Color(0xFFF44336), Color(0xFF2196F3), Color(0xFF9C27B0)
    )

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Usage Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // Stacked bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                apps.take(6).forEachIndexed { i, app ->
                    val fraction = app.usageTimeMs.toFloat() / totalMs
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(colors[i % colors.size])
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend
            apps.take(6).forEachIndexed { i, app ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[i % colors.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(app.appName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        "${(app.usageTimeMs * 100 / totalMs)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, totalMs: Long) {
    val fraction = if (totalMs > 0) app.usageTimeMs.toFloat() / totalMs else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.appName.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(app.formattedTime, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
        }
    }
}
