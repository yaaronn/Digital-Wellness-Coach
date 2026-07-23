package com.digitalwellness.coach.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsStateWithLifecycle()

    LaunchedEffect(isCompleted) {
        if (isCompleted) onComplete()
    }

    val pages = remember {
        listOf(
            OnboardingPage(
                Icons.Default.Favorite,
                "Welcome to Digital Wellness",
                "Take control of your screen time and build healthier digital habits.",
                Color(0xFF6B52F5)
            ),
            OnboardingPage(
                Icons.Default.Analytics,
                "Track Your Usage",
                "See exactly how much time you spend on each app, detect patterns, and understand your digital behaviour.",
                Color(0xFF00BFA5)
            ),
            OnboardingPage(
                Icons.Default.Psychology,
                "AI-Powered Insights",
                "Get personalized recommendations, addiction scoring, and habit detection powered by smart analytics.",
                Color(0xFFFFAB00)
            ),
            OnboardingPage(
                Icons.Default.CenterFocusStrong,
                "Focus & Achieve",
                "Use Focus Mode, set goals, earn achievements, and watch your digital pet thrive as you improve.",
                Color(0xFF4CAF50)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size + 1 }) // +1 for permissions page
    val scope = rememberCoroutineScope()

    var notificationPermissionRequested by remember { mutableStateOf(false) }
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page < pages.size) {
                OnboardingPageContent(
                    page = pages[page],
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PermissionsPage(
                    hasUsagePermission = hasUsagePermission,
                    onGrantUsage = { viewModel.openUsageSettings(context) },
                    onGrantNotification = {
                        notificationPermission?.launchPermissionRequest()
                        notificationPermissionRequested = true
                    },
                    onCheckPermissions = { viewModel.checkPermissions() }
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .animateContentSize()
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                if (pagerState.currentPage < pagerState.pageCount - 1) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = { viewModel.completeOnboarding() },
                        shape = RoundedCornerShape(50),
                        enabled = hasUsagePermission
                    ) {
                        Text(if (hasUsagePermission) "Get Started" else "Grant Access First")
                    }
                }
            }

            if (pagerState.currentPage < pagerState.pageCount - 1) {
                TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.pageCount - 1) } }) {
                    Text("Skip", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    page.color.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.background
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .padding(bottom = 160.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(page.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    page.icon, null,
                    modifier = Modifier.size(60.dp),
                    tint = page.color
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun PermissionsPage(
    hasUsagePermission: Boolean,
    onGrantUsage: () -> Unit,
    onGrantNotification: () -> Unit,
    onCheckPermissions: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { onCheckPermissions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Security, null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("One-time Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "We need a couple of permissions to track your usage. Your data never leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Usage Access
        PermissionCard(
            icon = Icons.Default.QueryStats,
            title = "Usage Access",
            description = "Required to track how much time you spend in each app. This is the core feature of Digital Wellness Coach.",
            isGranted = hasUsagePermission,
            isRequired = true,
            onGrant = onGrantUsage
        )

        Spacer(Modifier.height(12.dp))

        // Notifications
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "For break reminders, sleep alerts, goal achievements, and your weekly wellness summary.",
            isGranted = false,
            isRequired = false,
            onGrant = onGrantNotification
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (isRequired) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Granted", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(onClick = onGrant) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
