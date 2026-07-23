package com.digitalwellness.coach.presentation.profile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.Achievement
import com.digitalwellness.coach.domain.model.AchievementType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPetNameDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TopAppBar(
            title = { Text("Profile", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Pet & Streak hero
                item { ProfileHeroCard(uiState, onEditPetName = { showPetNameDialog = true }) }

                // Settings section
                item { SectionHeader("Settings") }

                item {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.DarkMode,
                            label = "Dark Mode",
                            trailing = {
                                Switch(
                                    checked = uiState.isDarkMode,
                                    onCheckedChange = viewModel::toggleDarkMode
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            label = "Daily Goal",
                            subtitle = "${uiState.dailyGoalHours.toInt()}h per day",
                            trailing = {
                                IconButton(onClick = { showGoalDialog = true }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            label = "Notifications",
                            subtitle = "Break & sleep reminders",
                            trailing = {
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                            }
                        )
                    }
                }

                // Achievements section
                item { SectionHeader("Achievements") }

                val allTypes = AchievementType.entries
                val earnedIds = uiState.achievements.map { it.type }.toSet()

                items(allTypes) { type ->
                    AchievementRow(
                        type = type,
                        earned = uiState.achievements.find { it.type == type }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showPetNameDialog) {
        var input by remember { mutableStateOf(uiState.petName) }
        AlertDialog(
            onDismissRequest = { showPetNameDialog = false },
            title = { Text("Name Your Pet") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Pet name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePetName(input.trim().ifEmpty { "Buddy" })
                    showPetNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPetNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showGoalDialog) {
        var input by remember { mutableStateOf(uiState.dailyGoalHours.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Update Daily Goal") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Hours per day") },
                    suffix = { Text("hrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateDailyGoal(input.toFloatOrNull() ?: 4f)
                    showGoalDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileHeroCard(uiState: ProfileUiState, onEditPetName: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("😊", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    uiState.petName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEditPetName, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f))
                }
            }
            Text(
                "Your Digital Companion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStat("🔥", "${uiState.currentStreak}", "Day Streak")
                HeroStat("🏆", "${uiState.achievements.size}", "Badges")
                HeroStat("⏰", "${uiState.dailyGoalHours.toInt()}h", "Daily Goal")
            }
        }
    }
}

@Composable
private fun HeroStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
            }
        }
        trailing()
    }
}

@Composable
private fun AchievementRow(type: AchievementType, earned: Achievement?) {
    val isEarned = earned != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                type.emoji,
                fontSize = 28.sp,
                modifier = Modifier
                    .let { if (!isEarned) it.offset() else it }
                    .run { if (!isEarned) this else this }
            )
            if (!isEarned) {
                // Greyed overlay feeling via alpha on the whole Row content
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    type.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEarned) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
                Text(
                    type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEarned) MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(0.35f)
                )
                if (earned != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Earned ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(earned.earnedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (isEarned) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.outline.copy(0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
