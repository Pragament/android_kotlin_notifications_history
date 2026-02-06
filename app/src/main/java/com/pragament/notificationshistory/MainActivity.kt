package com.pragament.notificationshistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pragament.notificationshistory.ui.navigation.AppNavHost
import com.pragament.notificationshistory.ui.navigation.Screen
import com.pragament.notificationshistory.ui.theme.NotificationsHistoryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NotificationsHistoryTheme {
                val navController = rememberNavController()
                var selectedTab by remember { mutableIntStateOf(0) }
                
                val navigationItems = listOf(
                    NavigationItem(
                        title = "Notifications",
                        selectedIcon = Icons.Filled.Notifications,
                        unselectedIcon = Icons.Outlined.Notifications,
                        route = Screen.Notifications.route
                    ),
                    NavigationItem(
                        title = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        route = Screen.Settings.route
                    )
                )
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            navigationItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = {
                                        selectedTab = index
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Notifications.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == index) 
                                                item.selectedIcon 
                                            else 
                                                item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    },
                                    label = { Text(item.title) }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
