package com.digitalwellness.coach.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.digitalwellness.coach.presentation.analytics.AnalyticsScreen
import com.digitalwellness.coach.presentation.dashboard.DashboardScreen
import com.digitalwellness.coach.presentation.focus.FocusScreen
import com.digitalwellness.coach.presentation.goals.GoalsScreen
import com.digitalwellness.coach.presentation.onboarding.OnboardingScreen
import com.digitalwellness.coach.presentation.profile.ProfileScreen
import com.digitalwellness.coach.presentation.reports.ReportsScreen

// ─── Routes ───────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Analytics : Screen("analytics")
    object Goals : Screen("goals")
    object Focus : Screen("focus")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Analytics, "Analytics", Icons.Default.BarChart),
    BottomNavItem(Screen.Goals, "Goals", Icons.Default.TrackChanges),
    BottomNavItem(Screen.Focus, "Focus", Icons.Default.CenterFocusStrong),
    BottomNavItem(Screen.Reports, "Reports", Icons.Default.Assessment),
    BottomNavItem(Screen.Profile, "Profile", Icons.Default.Person)
)

// ─── NavGraph ─────────────────────────────────────────────────────────────────

@Composable
fun WellnessNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(paddingValues = paddingValues)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(paddingValues = paddingValues)
            }
            composable(Screen.Goals.route) {
                GoalsScreen(paddingValues = paddingValues)
            }
            composable(Screen.Focus.route) {
                FocusScreen(paddingValues = paddingValues)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(paddingValues = paddingValues)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(paddingValues = paddingValues)
            }
        }
    }
}
