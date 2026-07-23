package com.digitalwellness.coach.presentation.reports

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.AppUsage
import com.digitalwellness.coach.domain.model.WeeklyReport
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    paddingValues: PaddingValues,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TopAppBar(
            title = { Text("Weekly Report", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            actions = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.report == null) {
            EmptyReportPlaceholder()
        } else {
            val report = uiState.report!!
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { ReportSummaryCard(report) }
                item {
                    WeeklyBarChartCard(
                        screenTimes = uiState.dailyScreenTimes,
                        dayLabels = uiState.dayLabels
                    )
                }
                item { ScoreTrendCard(trend = report.addictionScoreTrend, labels = uiState.dayLabels) }
                item { TopAppsCard(apps = report.topApps) }
                item { ProductivityCard(report) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: WeeklyReport) {
    val improvement = report.improvementPercentage
    val isImproving = improvement > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Assessment, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "This Week at a Glance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReportStat(
                    label = "Total",
                    value = formatMs(report.totalScreenTimeMs),
                    icon = "📱"
                )
                ReportStat(
                    label = "Daily Avg",
                    value = formatMs(report.avgDailyScreenTimeMs),
                    icon = "📊"
                )
                ReportStat(
                    label = "vs Last Week",
                    value = "${if (isImproving) "-" else "+"}${improvement.roundToInt()}%",
                    icon = if (isImproving) "📉" else "📈",
                    valueColor = if (isImproving) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String, icon: String, valueColor: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onPrimaryContainer else valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
        )
    }
}

@Composable
private fun WeeklyBarChartCard(screenTimes: List<Long>, dayLabels: List<String>) {
    val maxMs = screenTimes.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Screen Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                screenTimes.zip(dayLabels).forEachIndexed { index, (ms, label) ->
                    val fraction = ms.toFloat() / maxMs
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(800, delayMillis = index * 60),
                        label = "bar$index"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            formatMs(ms),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height((100 * animatedFraction).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendCard(trend: List<Int>, labels: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Addiction Score Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Lower is healthier. Track your progress over the week.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.zip(labels).forEach { (score, label) ->
                    val color = when {
                        score <= 30 -> Color(0xFF4CAF50)
                        score <= 55 -> Color(0xFFFF9800)
                        score <= 75 -> Color(0xFFF44336)
                        else -> Color(0xFFB71C1C)
                    }
                    val animatedScore by animateIntAsState(
                        targetValue = score,
                        animationSpec = tween(600),
                        label = "score"
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$animatedScore",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((score * 0.8f).toInt().dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color.copy(alpha = 0.8f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppUsage>) {
    if (apps.isEmpty()) return
    val totalMs = apps.sumOf { it.usageTimeMs }.coerceAtLeast(1L)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Most Used Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            apps.forEachIndexed { index, app ->
                val fraction = app.usageTimeMs.toFloat() / totalMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(100.dp)) {
                        Text(
                            app.formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductivityCard(report: WeeklyReport) {
    val productivityColor = when {
        report.productivityScore >= 70 -> Color(0xFF4CAF50)
        report.productivityScore >= 45 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = productivityColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(productivityColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${report.productivityScore}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = productivityColor
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Productivity Score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        report.productivityScore >= 70 -> "Great job — you're in control of your screen time!"
                        report.productivityScore >= 45 -> "Decent week. Room to improve your digital habits."
                        else -> "High screen time detected. Try setting stricter goals."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                    lineHeight = 16.sp
                )
                report.mostUsedApp?.let { app ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Top app: ${app.appName} (${app.formattedTime})",
                        style = MaterialTheme.typography.labelSmall,
                        color = productivityColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReportPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text("No data yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use your phone for a few days to generate your first report.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
