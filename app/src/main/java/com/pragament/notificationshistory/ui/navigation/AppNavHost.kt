package com.pragament.notificationshistory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pragament.notificationshistory.ui.screens.NotificationsScreen
import com.pragament.notificationshistory.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Notifications.route,
        modifier = modifier
    ) {
        composable(Screen.Notifications.route) {
            NotificationsScreen()
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
