package com.digitalwellness.coach.presentation.focus

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitalwellness.coach.domain.model.FocusMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    paddingValues: PaddingValues,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isActive = uiState.activeSession != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TopAppBar(
            title = { Text("Focus Mode", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Timer ring
            item {
                FocusTimerRing(
                    isActive = isActive,
                    elapsedMs = uiState.elapsedMs,
                    plannedMs = (uiState.activeSession?.plannedDurationMs
                        ?: (uiState.selectedDurationMinutes * 60_000L))
                )
            }

            // Start / Stop button
            item {
                AnimatedContent(
                    targetState = isActive,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "startStop"
                ) { active ->
                    if (active) {
                        Button(
                            onClick = viewModel::endSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("End Session", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = viewModel::startSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Focus", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Mode selector (only when not active)
            if (!isActive) {
                item {
                    ModeSelectorCard(
                        selectedMode = uiState.selectedMode,
                        onModeSelect = viewModel::selectMode
                    )
                }

                item {
                    DurationSelectorCard(
                        selectedMinutes = uiState.selectedDurationMinutes,
                        onDurationSelect = viewModel::selectDuration
                    )
                }
            } else {
                // Active session info
                item {
                    ActiveSessionCard(
                        mode = uiState.activeSession!!.mode,
                        startTime = uiState.activeSession!!.startTime
                    )
                }
            }

            // Stats
            item {
                FocusStatsCard(completedCount = uiState.completedSessions)
            }
        }
    }
}

@Composable
private fun FocusTimerRing(
    isActive: Boolean,
    elapsedMs: Long,
    plannedMs: Long
) {
    val progress = if (plannedMs > 0) (elapsedMs.toFloat() / plannedMs).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.3f)
    val remaining = (plannedMs - elapsedMs).coerceAtLeast(0L)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .scale(pulse)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            drawCircle(
                color = ringColor.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(strokeWidth)
            )
            if (isActive && progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(
                        center.x - radius, center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isActive) {
                Text(
                    formatCountdown(remaining),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "elapsed ${formatElapsed(elapsedMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
            } else {
                Icon(
                    Icons.Default.CenterFocusStrong, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ready",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        }
    }
}

@Composable
private fun ModeSelectorCard(
    selectedMode: FocusMode,
    onModeSelect: (FocusMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Focus Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusMode.entries.forEach { mode ->
                    val selected = mode == selectedMode
                    FilterChip(
                        selected = selected,
                        onClick = { onModeSelect(mode) },
                        label = {
                            Text("${mode.emoji} ${mode.label}")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationSelectorCard(
    selectedMinutes: Int,
    onDurationSelect: (Int) -> Unit
) {
    val durations = listOf(5 to "5m", 15 to "15m", 25 to "25m\n🍅", 45 to "45m", 60 to "1h", 90 to "1.5h")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durations.forEach { (minutes, label) ->
                    val selected = minutes == selectedMinutes
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 0.dp else 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onDurationSelect(minutes) }
                            .padding(4.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(mode: FocusMode, startTime: Long) {
    val startFormatted = remember(startTime) {
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(startTime))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mode.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "${mode.label} Session",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Started at $startFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                )
            }
            Spacer(Modifier.weight(1f))
            // Pulsing dot
            val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "dotAlpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = pulse))
            )
        }
    }
}

@Composable
private fun FocusStatsCard(completedCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Sessions Done", value = "$completedCount", emoji = "✅")
            VerticalDivider(modifier = Modifier.height(48.dp))
            StatItem(label = "Focus Time", value = "${completedCount * 25}m", emoji = "⏱️")
            VerticalDivider(modifier = Modifier.height(48.dp))
            StatItem(label = "Streak", value = "${completedCount / 4}d", emoji = "🔥")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), textAlign = TextAlign.Center)
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatElapsed(ms: Long): String {
    val m = ms / 60_000
    val s = (ms % 60_000) / 1000
    return "%02d:%02d".format(m, s)
}
