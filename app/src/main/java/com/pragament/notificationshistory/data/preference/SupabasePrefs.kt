package com.pragament.notificationshistory.data.preference

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * SharedPreferences wrapper for storing Supabase configuration
 * and device linking state. Uses SharedPreferences (not DataStore)
 * for synchronous access from NotificationListenerService.
 */
class SupabasePrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var supabaseUrl: String
        get() = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value.trimEnd('/')).apply()

    var supabaseAnonKey: String
        get() = prefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON_KEY, value).apply()

    var deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            return newId
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, android.os.Build.MODEL) ?: android.os.Build.MODEL
        set(value) = prefs.edit().putString(KEY_DEVICE_NAME, value).apply()

    var groupId: String
        get() = prefs.getString(KEY_GROUP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROUP_ID, value).apply()

    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_ENABLED, value).apply()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    val isLinked: Boolean
        get() = groupId.isNotBlank()

    fun clearAll() {
        val currentDeviceId = deviceId // preserve device id
        prefs.edit().clear().apply()
        deviceId = currentDeviceId
    }

    companion object {
        private const val PREFS_NAME = "supabase_prefs"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_GROUP_ID = "group_id"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
    }
}
