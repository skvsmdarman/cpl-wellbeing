package com.example.cplswellbeingbynovadevs.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Auth : NavRoute("auth", "Auth")
    data object UsagePermission : NavRoute("usage_permission", "Permission")
    data object Dashboard : NavRoute("dashboard", "Home", Icons.Default.Home)
    data object Analytics : NavRoute("analytics", "Analytics", Icons.Default.Analytics)
    data object Timeline : NavRoute("timeline", "Timeline", Icons.Default.Timeline)
    data object Connections : NavRoute("connections", "Connections", Icons.Default.Favorite)
    data object Settings : NavRoute("settings", "Settings", Icons.Default.Settings)
    data object Admin : NavRoute("admin", "Admin", Icons.Default.AdminPanelSettings)

    companion object {
        val bottomNavItems: List<NavRoute>
            get() = listOf(
                Dashboard,
                Analytics,
                Timeline,
                Connections,
                Settings
            )
    }
}
