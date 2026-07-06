package com.pragament.notificationshistory.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.notificationshistory.data.preference.SupabasePrefs
import com.pragament.notificationshistory.data.remote.SupabaseManager
import com.pragament.notificationshistory.data.remote.SupabaseRealtimeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isNotificationAccessEnabled: Boolean = false,
    val autoDeleteDays: Int = 0, // 0 = never
    val excludedApps: Set<String> = emptySet(),
    // Supabase config
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val deviceName: String = "",
    val deviceId: String = "",
    val groupId: String = "",
    val syncEnabled: Boolean = false,
    val isConfigured: Boolean = false,
    val isLinked: Boolean = false,
    // OTP
    val generatedOtp: String = "",
    val otpInput: String = "",
    val isOtpLoading: Boolean = false,
    val otpMessage: String = "",
    // Forwarding rules
    val forwardRules: List<SupabaseManager.ForwardRule> = emptyList(),
    val isRulesLoading: Boolean = false,
    // Linked devices
    val linkedDevices: List<SupabaseManager.LinkedDevice> = emptyList(),
    // General
    val errorMessage: String = "",
    val successMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val supabasePrefs: SupabasePrefs,
    private val supabaseManager: SupabaseManager,
    private val realtimeClient: SupabaseRealtimeClient
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkNotificationAccess()
        loadSupabaseState()
    }

    // ── Notification Access ──────────────────────────────────────────────

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

    // ── Auto Delete ──────────────────────────────────────────────────────

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

    // ── Supabase Config ──────────────────────────────────────────────────

    private fun loadSupabaseState() {
        _uiState.value = _uiState.value.copy(
            supabaseUrl = supabasePrefs.supabaseUrl,
            supabaseAnonKey = supabasePrefs.supabaseAnonKey,
            deviceName = supabasePrefs.deviceName,
            deviceId = supabasePrefs.deviceId,
            groupId = supabasePrefs.groupId,
            syncEnabled = supabasePrefs.syncEnabled,
            isConfigured = supabasePrefs.isConfigured,
            isLinked = supabasePrefs.isLinked
        )

        // Load rules and devices if already linked
        if (supabasePrefs.isLinked) {
            refreshForwardRules()
            refreshLinkedDevices()
        }
    }

    fun updateSupabaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(supabaseUrl = url)
    }

    fun updateSupabaseAnonKey(key: String) {
        _uiState.value = _uiState.value.copy(supabaseAnonKey = key)
    }

    fun updateDeviceName(name: String) {
        _uiState.value = _uiState.value.copy(deviceName = name)
    }

    fun saveSupabaseConfig() {
        supabasePrefs.supabaseUrl = _uiState.value.supabaseUrl
        supabasePrefs.supabaseAnonKey = _uiState.value.supabaseAnonKey
        supabasePrefs.deviceName = _uiState.value.deviceName
        _uiState.value = _uiState.value.copy(
            isConfigured = supabasePrefs.isConfigured,
            successMessage = "Configuration saved"
        )
    }

    fun disconnectSupabase() {
        realtimeClient.disconnect()
        supabasePrefs.clearAll()
        _uiState.value = _uiState.value.copy(
            supabaseUrl = "",
            supabaseAnonKey = "",
            groupId = "",
            syncEnabled = false,
            isConfigured = false,
            isLinked = false,
            generatedOtp = "",
            forwardRules = emptyList(),
            linkedDevices = emptyList(),
            successMessage = "Disconnected"
        )
    }

    // ── OTP Generation & Linking ─────────────────────────────────────────

    fun generateOtp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOtpLoading = true,
                errorMessage = "",
                generatedOtp = ""
            )
            val result = supabaseManager.generateOtp()
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isOtpLoading = false,
                        generatedOtp = response.otp,
                        groupId = response.group_id,
                        isLinked = true,
                        syncEnabled = true,
                        otpMessage = "Share this OTP with the other phone. It expires in 5 minutes."
                    )
                    // Connect realtime after linking
                    realtimeClient.connect()
                    refreshLinkedDevices()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isOtpLoading = false,
                        errorMessage = "Failed to generate OTP: ${error.message}"
                    )
                }
            )
        }
    }

    fun updateOtpInput(otp: String) {
        // Only allow digits, max 6
        val filtered = otp.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(otpInput = filtered)
    }

    fun submitOtp() {
        val otpCode = _uiState.value.otpInput
        if (otpCode.length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a 6-digit OTP")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOtpLoading = true,
                errorMessage = ""
            )
            val result = supabaseManager.linkDevice(otpCode)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isOtpLoading = false,
                        groupId = response.group_id,
                        isLinked = true,
                        syncEnabled = true,
                        otpInput = "",
                        successMessage = response.message
                    )
                    // Connect realtime after linking
                    realtimeClient.connect()
                    refreshForwardRules()
                    refreshLinkedDevices()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isOtpLoading = false,
                        errorMessage = "Failed to link: ${error.message}"
                    )
                }
            )
        }
    }

    // ── Forwarding Rules ─────────────────────────────────────────────────

    fun refreshForwardRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRulesLoading = true)
            val result = supabaseManager.getForwardRules()
            result.fold(
                onSuccess = { rules ->
                    _uiState.value = _uiState.value.copy(
                        forwardRules = rules,
                        isRulesLoading = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isRulesLoading = false,
                        errorMessage = "Failed to load rules: ${it.message}"
                    )
                }
            )
        }
    }

    fun addForwardRule(appSource: String?, textContains: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRulesLoading = true)
            val result = supabaseManager.addForwardRule(appSource, textContains)
            result.fold(
                onSuccess = {
                    refreshForwardRules()
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Rule added"
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isRulesLoading = false,
                        errorMessage = "Failed to add rule: ${it.message}"
                    )
                }
            )
        }
    }

    fun deleteForwardRule(ruleId: String) {
        viewModelScope.launch {
            val result = supabaseManager.deleteForwardRule(ruleId)
            result.fold(
                onSuccess = {
                    refreshForwardRules()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete rule: ${it.message}"
                    )
                }
            )
        }
    }

    // ── Linked Devices ───────────────────────────────────────────────────

    fun refreshLinkedDevices() {
        viewModelScope.launch {
            val result = supabaseManager.getLinkedDevices()
            result.fold(
                onSuccess = { devices ->
                    _uiState.value = _uiState.value.copy(linkedDevices = devices)
                },
                onFailure = { /* Silently ignore */ }
            )
        }
    }

    // ── Message Clearing ─────────────────────────────────────────────────

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = "")
    }

    fun clearGeneratedOtp() {
        _uiState.value = _uiState.value.copy(generatedOtp = "", otpMessage = "")
    }
}
