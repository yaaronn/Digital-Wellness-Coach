package com.digitalwellness.coach.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item { DashboardHeader() }

                // Digital Pet + Score
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.digitalPet?.let { pet ->
                            DigitalPetCard(pet = pet, modifier = Modifier.weight(1f))
                        }
                        uiState.addictionScore?.let { score ->
                            AddictionScoreCard(score = score, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Main stats ring
                item {
                    MainScreenTimeCard(
                        todayUsage = uiState.todayUsage,
                        goalProgress = uiState.goalProgressFraction,
                        goalMs = uiState.dailyGoalMs
                    )
                }

                // Quick stats row
                uiState.todayUsage?.let { usage ->
                    item {
                        QuickStatsRow(usage = usage)
                    }
                }

                // Weekly bar chart
                if (uiState.weeklyUsages.isNotEmpty()) {
                    item {
                        WeeklyBarCard(weeklyUsages = uiState.weeklyUsages)
                    }
                }

                // Habit warnings
                if (uiState.habitWarnings.isNotEmpty()) {
                    item {
                        HabitWarningsCard(warnings = uiState.habitWarnings)
                    }
                }

                // AI Recommendations
                if (uiState.recommendations.isNotEmpty()) {
                    item {
                        AIRecommendationsCard(recommendations = uiState.recommendations)
                    }
                }

                // Addiction suggestions
                uiState.addictionScore?.let { score ->
                    if (score.suggestions.isNotEmpty()) {
                        item { SuggestionsCard(score = score) }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Text("Digital Wellness", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DigitalPetCard(pet: DigitalPet, modifier: Modifier = Modifier) {
    val bgColor = when (pet.state) {
        PetState.HAPPY -> Color(0xFF4CAF50)
        PetState.NORMAL -> Color(0xFF2196F3)
        PetState.TIRED -> Color(0xFFFF9800)
        PetState.EXHAUSTED -> Color(0xFFF44336)
    }.copy(alpha = 0.12f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(pet.state.emoji, fontSize = 36.sp)
            Spacer(Modifier.height(4.dp))
            Text(pet.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(pet.state.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Spacer(Modifier.height(8.dp))
            Text(
                pet.message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AddictionScoreCard(score: AddictionScore, modifier: Modifier = Modifier) {
    val levelColor = when (score.level) {
        AddictionLevel.HEALTHY -> Color(0xFF4CAF50)
        AddictionLevel.MODERATE -> Color(0xFFFF9800)
        AddictionLevel.HIGH -> Color(0xFFF44336)
        AddictionLevel.SEVERE -> Color(0xFFB71C1C)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Digital Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Spacer(Modifier.height(8.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                CircularProgressIndicator(
                    progress = { score.score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = levelColor,
                    trackColor = levelColor.copy(alpha = 0.2f),
                    strokeWidth = 6.dp
                )
                Text(
                    "${score.score}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                color = levelColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    score.level.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MainScreenTimeCard(
    todayUsage: DailyUsage?,
    goalProgress: Float,
    goalMs: Long
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Today's Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
                Spacer(Modifier.height(24.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = center
                        // Track
                        drawCircle(
                            color = Color(0x20808080),
                            radius = radius,
                            style = Stroke(strokeWidth)
                        )
                        // Progress arc
                        drawArc(
                            color = if (goalProgress > 0.9f) Color(0xFFF44336) else Color(0xFF6B52F5),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(
                                center.x - radius, center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            todayUsage?.formattedTotalTime ?: "0h 0m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "of ${goalMs / 3_600_000}h goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (goalProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${(goalProgress * 100).toInt()}% of daily limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(usage: DailyUsage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            icon = Icons.Default.LockOpen,
            label = "Unlocks",
            value = "${usage.unlockCount}",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Timer,
            label = "Longest",
            value = formatMs(usage.longestSessionMs),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Apps,
            label = "Apps Used",
            value = "${usage.appUsages.size}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }
    }
}

@Composable
private fun WeeklyBarCard(weeklyUsages: List<DailyUsage>) {
    val maxMs = weeklyUsages.maxOfOrNull { it.totalScreenTimeMs } ?: 1L
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cal = Calendar.getInstance()

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val sortedUsages = weeklyUsages.sortedBy { it.date }
                sortedUsages.forEachIndexed { i, usage ->
                    val fraction = if (maxMs > 0) usage.totalScreenTimeMs.toFloat() / maxMs else 0f
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(800, delayMillis = i * 50),
                        label = "bar$i"
                    )
                    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = usage.date }
                        .get(Calendar.DAY_OF_WEEK) - 2
                    val label = dayLabels.getOrElse(dayOfWeek.coerceIn(0, 6)) { "—" }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.height(80.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height((80 * animatedFraction).dp.coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitWarningsCard(warnings: List<HabitWarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Habit Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D4037))
            }
            Spacer(Modifier.height(12.dp))
            warnings.forEach { warning ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .offset(y = 6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(warning.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037))
                }
            }
        }
    }
}

@Composable
private fun AIRecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("AI Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            recommendations.forEach { rec ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        rec,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsCard(score: AddictionScore) {
    val levelColor = when (score.level) {
        AddictionLevel.HEALTHY -> Color(0xFF4CAF50)
        AddictionLevel.MODERATE -> Color(0xFFFF9800)
        AddictionLevel.HIGH -> Color(0xFFF44336)
        AddictionLevel.SEVERE -> Color(0xFFB71C1C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = levelColor.copy(0.08f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Personalized Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            score.suggestions.forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(Icons.Default.TipsAndUpdates, null, modifier = Modifier.size(16.dp), tint = levelColor)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

