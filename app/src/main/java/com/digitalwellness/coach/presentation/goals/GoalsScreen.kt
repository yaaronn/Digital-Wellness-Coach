package com.digitalwellness.coach.presentation.goals

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.Goal
import com.digitalwellness.coach.domain.model.GoalType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    paddingValues: PaddingValues,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Motivational header
                    item { GoalsHeader(goalsCount = uiState.goalsWithProgress.size) }

                    // Add goal buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.showAddDialog(GoalType.DAILY_TOTAL) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Today, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Daily Goal")
                            }
                            OutlinedButton(
                                onClick = { viewModel.showAddDialog(GoalType.PER_APP) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Apps, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("App Goal")
                            }
                        }
                    }

                    if (uiState.goalsWithProgress.isEmpty()) {
                        item { EmptyGoalsPlaceholder() }
                    } else {
                        items(uiState.goalsWithProgress) { gwp ->
                            GoalCard(
                                goalWithProgress = gwp,
                                onDelete = { viewModel.deleteGoal(gwp.goal.id) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // Add Goal Dialog
        if (uiState.showAddDialog) {
            AddGoalDialog(
                type = uiState.dialogType,
                onDismiss = viewModel::dismissDialog,
                onCreateDaily = viewModel::createDailyGoal,
                onCreateApp = viewModel::createAppGoal
            )
        }
    }
}

@Composable
private fun GoalsHeader(goalsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎯", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Your Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    if (goalsCount == 0) "Set goals to track your limits"
                    else "$goalsCount active goal${if (goalsCount > 1) "s" else ""} running",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goalWithProgress: GoalWithProgress,
    onDelete: () -> Unit
) {
    val goal = goalWithProgress.goal
    val progress = goalWithProgress.progressFraction
    val usedMs = goalWithProgress.usedMs
    val remainingMs = (goal.targetTimeMs - usedMs).coerceAtLeast(0L)

    val isExceeded = progress >= 1f
    val progressColor = when {
        progress < 0.6f -> Color(0xFF4CAF50)
        progress < 0.85f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "goalProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExceeded)
                Color(0xFFF44336).copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (goal.type == GoalType.DAILY_TOTAL) Icons.Default.Today else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isExceeded) Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            if (goal.type == GoalType.DAILY_TOTAL) "Daily Screen Time"
                            else goal.appName ?: "App Goal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Limit: ${formatMs(goal.targetTimeMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(progressColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(progressColor)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Used: ${formatMs(usedMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (isExceeded) {
                    Text(
                        "⚠️ Limit exceeded!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "${formatMs(remainingMs)} left  •  ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎯", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "No goals yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Set daily or per-app limits to stay on track",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AddGoalDialog(
    type: GoalType,
    onDismiss: () -> Unit,
    onCreateDaily: (Float) -> Unit,
    onCreateApp: (String, String, Int) -> Unit
) {
    var hoursInput by remember { mutableStateOf("4") }
    var minutesInput by remember { mutableStateOf("30") }
    var packageInput by remember { mutableStateOf("") }
    var appNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                if (type == GoalType.DAILY_TOTAL) Icons.Default.Today else Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                if (type == GoalType.DAILY_TOTAL) "Set Daily Goal" else "Set App Goal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type == GoalType.DAILY_TOTAL) {
                    Text(
                        "Set your maximum daily screen time target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Target hours") },
                        suffix = { Text("hrs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Enter the app name and your daily time limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        label = { Text("App name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = packageInput,
                        onValueChange = { packageInput = it },
                        label = { Text("Package (e.g. com.instagram.android)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Limit (minutes)") },
                        suffix = { Text("min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (type == GoalType.DAILY_TOTAL) {
                        onCreateDaily(hoursInput.toFloatOrNull() ?: 4f)
                    } else {
                        onCreateApp(
                            packageInput.trim(),
                            appNameInput.trim(),
                            minutesInput.toIntOrNull() ?: 30
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatMs(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
