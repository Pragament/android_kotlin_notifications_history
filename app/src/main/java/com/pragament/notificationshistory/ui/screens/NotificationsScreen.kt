package com.pragament.notificationshistory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pragament.notificationshistory.ui.components.FilterBottomSheet
import com.pragament.notificationshistory.ui.components.NotificationItem
import com.pragament.notificationshistory.ui.components.SnoozeDialog
import com.pragament.notificationshistory.ui.components.StatusFilter
import com.pragament.notificationshistory.ui.viewmodel.NotificationsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snoozeDialogTarget by viewModel.showSnoozeDialog.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Notifications")
                        if (uiState.unreadCount > 0) {
                            Text(
                                text = "${uiState.unreadCount} unread",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Filter button with badge
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                val hasFilter = filterState.statusFilter != StatusFilter.ALL || 
                                    filterState.selectedApps.isNotEmpty()
                                if (hasFilter) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }
                    }
                    
                    // More options
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark all as read") },
                                onClick = {
                                    viewModel.markAllAsRead()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DoneAll, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyState(
                        hasFilter = filterState.statusFilter != StatusFilter.ALL || 
                            filterState.selectedApps.isNotEmpty(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = { viewModel.toggleReadStatus(notification) },
                                onDelete = { viewModel.deleteNotification(notification.id) },
                                onSnooze = { viewModel.showSnoozeDialog(notification.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = sheetState,
            availableApps = uiState.availableApps,
            currentFilter = filterState,
            onApplyFilter = { viewModel.updateFilter(it) },
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showFilterSheet = false
                }
            }
        )
    }

    // Snooze Dialog
    snoozeDialogTarget?.let { notificationId ->
        SnoozeDialog(
            onDismiss = { viewModel.dismissSnoozeDialog() },
            onSnooze = { snoozeUntil ->
                viewModel.snoozeNotification(notificationId, snoozeUntil)
            }
        )
    }
}

@Composable
private fun EmptyState(
    hasFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasFilter) Icons.Outlined.NotificationsOff else Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (hasFilter) "No matching notifications" else "No notifications yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasFilter) 
                "Try adjusting your filters" 
            else 
                "Notifications will appear here once they arrive",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
