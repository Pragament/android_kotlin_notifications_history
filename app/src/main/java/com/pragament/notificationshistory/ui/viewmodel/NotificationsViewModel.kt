package com.pragament.notificationshistory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.notificationshistory.data.dao.AppInfo
import com.pragament.notificationshistory.data.entity.NotificationEntity
import com.pragament.notificationshistory.data.repository.NotificationRepository
import com.pragament.notificationshistory.ui.components.FilterState
import com.pragament.notificationshistory.ui.components.StatusFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val availableApps: List<AppInfo> = emptyList(),
    val unreadCount: Int = 0,
    val filterState: FilterState = FilterState(),
    val isLoading: Boolean = true,
    val selectedNotification: NotificationEntity? = null,
    val showSnoozeDialog: Boolean = false,
    val snoozeTargetId: Long? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.asStateFlow()

    private val _showSnoozeDialog = MutableStateFlow<Long?>(null)
    val showSnoozeDialog = _showSnoozeDialog.asStateFlow()

    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.getAllNotifications(),
        repository.getDistinctApps(),
        repository.getUnreadCount(),
        _filterState
    ) { notifications, apps, unreadCount, filter ->
        val filteredNotifications = notifications.filter { notification ->
            val matchesApp = filter.selectedApps.isEmpty() || 
                filter.selectedApps.contains(notification.packageName)
            
            val matchesStatus = when (filter.statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.READ -> notification.isRead
                StatusFilter.UNREAD -> !notification.isRead
                StatusFilter.SNOOZED -> notification.isSnoozed
            }
            
            matchesApp && matchesStatus
        }
        
        NotificationsUiState(
            notifications = filteredNotifications,
            availableApps = apps,
            unreadCount = unreadCount,
            filterState = filter,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotificationsUiState()
    )

    fun updateFilter(filterState: FilterState) {
        _filterState.value = filterState
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAsUnread(id: Long) {
        viewModelScope.launch {
            repository.markAsUnread(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotificationById(id)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }

    fun showSnoozeDialog(notificationId: Long) {
        _showSnoozeDialog.value = notificationId
    }

    fun dismissSnoozeDialog() {
        _showSnoozeDialog.value = null
    }

    fun snoozeNotification(id: Long, snoozeUntil: Long) {
        viewModelScope.launch {
            repository.snoozeNotification(id, snoozeUntil)
            _showSnoozeDialog.value = null
        }
    }

    fun unsnoozeNotification(id: Long) {
        viewModelScope.launch {
            repository.unsnoozeNotification(id)
        }
    }

    fun toggleReadStatus(notification: NotificationEntity) {
        viewModelScope.launch {
            if (notification.isRead) {
                repository.markAsUnread(notification.id)
            } else {
                repository.markAsRead(notification.id)
            }
        }
    }
}
