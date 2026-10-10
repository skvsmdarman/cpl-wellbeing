package com.example.cplswellbeingbynovadevs.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import com.example.cplswellbeingbynovadevs.service.UsageStatsHelper
import com.example.cplswellbeingbynovadevs.ui.admin.AdminDashboardScreen
import com.example.cplswellbeingbynovadevs.ui.admin.AdminViewModel
import com.example.cplswellbeingbynovadevs.ui.analytics.AnalyticsScreen
import com.example.cplswellbeingbynovadevs.ui.analytics.AnalyticsViewModel
import com.example.cplswellbeingbynovadevs.ui.auth.AuthScreen
import com.example.cplswellbeingbynovadevs.ui.auth.AuthViewModel
import com.example.cplswellbeingbynovadevs.ui.connections.ConnectionsScreen
import com.example.cplswellbeingbynovadevs.ui.connections.ConnectionsViewModel
import com.example.cplswellbeingbynovadevs.ui.dashboard.DashboardScreen
import com.example.cplswellbeingbynovadevs.ui.dashboard.DashboardViewModel
import com.example.cplswellbeingbynovadevs.ui.permission.UsagePermissionScreen
import com.example.cplswellbeingbynovadevs.ui.settings.SettingsScreen
import com.example.cplswellbeingbynovadevs.ui.settings.SettingsViewModel
import com.example.cplswellbeingbynovadevs.ui.timeline.TimelineScreen
import com.example.cplswellbeingbynovadevs.ui.timeline.TimelineViewModel

@Composable
fun AppNavGraph(
    repository: WellbeingRepository,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = viewModel { AuthViewModel(repository) }
    val dashboardViewModel: DashboardViewModel = viewModel { DashboardViewModel(repository) }
    val analyticsViewModel: AnalyticsViewModel = viewModel { AnalyticsViewModel(repository) }
    val timelineViewModel: TimelineViewModel = viewModel { TimelineViewModel(repository) }
    val connectionsViewModel: ConnectionsViewModel = viewModel { ConnectionsViewModel(repository) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(repository) }
    val adminViewModel: AdminViewModel = viewModel { AdminViewModel(repository) }

    LaunchedEffect(Unit) {
        settingsViewModel.checkForUpdates()
    }

    val startDestination = if (SupabaseClient.currentUserId != null) {
        if (!UsageStatsHelper.hasUsagePermission(context)) {
            NavRoute.UsagePermission.route
        } else {
            NavRoute.Dashboard.route
        }
    } else {
        NavRoute.Auth.route
    }

    val showBottomBar = currentRoute in listOf(
        NavRoute.Dashboard.route,
        NavRoute.Analytics.route,
        NavRoute.Timeline.route,
        NavRoute.Connections.route,
        NavRoute.Settings.route,
        NavRoute.Admin.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val userRole = SupabaseClient.currentUserRole
                val items = if (userRole == UserProfile.ROLE_ADMIN) {
                    NavRoute.bottomNavItems + listOf(NavRoute.Admin)
                } else {
                    NavRoute.bottomNavItems
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    items.filterNotNull().forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                item.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = { Text(text = item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoute.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        val hasPerm = UsageStatsHelper.hasUsagePermission(context)
                        if (!hasPerm) {
                            navController.navigate(NavRoute.UsagePermission.route) {
                                popUpTo(NavRoute.Auth.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavRoute.Dashboard.route) {
                                popUpTo(NavRoute.Auth.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(NavRoute.UsagePermission.route) {
                UsagePermissionScreen(
                    onPermissionGranted = {
                        navController.navigate(NavRoute.Dashboard.route) {
                            popUpTo(NavRoute.UsagePermission.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoute.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onAppSelected = {
                        navController.navigate(NavRoute.Analytics.route)
                    }
                )
            }

            composable(NavRoute.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(NavRoute.Timeline.route) {
                TimelineScreen(viewModel = timelineViewModel)
            }

            composable(NavRoute.Connections.route) {
                ConnectionsScreen(viewModel = connectionsViewModel)
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onSignOut = {
                        navController.navigate(NavRoute.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoute.Admin.route) {
                AdminDashboardScreen(viewModel = adminViewModel)
            }
        }
    }
}
