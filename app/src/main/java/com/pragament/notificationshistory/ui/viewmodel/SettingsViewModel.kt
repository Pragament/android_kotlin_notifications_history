package com.pragament.notificationshistory.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isNotificationAccessEnabled: Boolean = false,
    val autoDeleteDays: Int = 0, // 0 = never
    val excludedApps: Set<String> = emptySet()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkNotificationAccess()
    }

    fun checkNotificationAccess() {
        viewModelScope.launch {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(application)
            val isEnabled = enabledListeners.contains(application.packageName)
            _uiState.value = _uiState.value.copy(isNotificationAccessEnabled = isEnabled)
        }
    }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(intent)
    }

    fun setAutoDeleteDays(days: Int) {
        _uiState.value = _uiState.value.copy(autoDeleteDays = days)
        // TODO: Save to DataStore preferences
    }

    fun addExcludedApp(packageName: String) {
        _uiState.value = _uiState.value.copy(
            excludedApps = _uiState.value.excludedApps + packageName
        )
        // TODO: Save to DataStore preferences
    }

    fun removeExcludedApp(packageName: String) {
        _uiState.value = _uiState.value.copy(
            excludedApps = _uiState.value.excludedApps - packageName
        )
        // TODO: Save to DataStore preferences
    }
}
