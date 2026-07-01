package com.pragament.notificationshistory.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pragament.notificationshistory.data.preference.SupabasePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles all HTTP calls to Supabase REST API and Edge Functions.
 */
class SupabaseManager(
    private val prefs: SupabasePrefs,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ── Edge Function: Generate OTP ──────────────────────────────────────

    data class OtpResponse(val otp: String, val group_id: String)

    suspend fun generateOtp(): Result<OtpResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "${prefs.supabaseUrl}/functions/v1/generate-otp"
            val body = gson.toJson(
                mapOf(
                    "device_id" to prefs.deviceId,
                    "device_name" to prefs.deviceName
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val otpResponse = gson.fromJson(responseBody, OtpResponse::class.java)
                // Save group_id
                prefs.groupId = otpResponse.group_id
                prefs.syncEnabled = true
                Result.success(otpResponse)
            } else {
                Result.failure(IOException("Generate OTP failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Edge Function: Link Device via OTP ───────────────────────────────

    data class LinkResponse(val group_id: String, val message: String)

    suspend fun linkDevice(otpCode: String): Result<LinkResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "${prefs.supabaseUrl}/functions/v1/link-device"
            val body = gson.toJson(
                mapOf(
                    "otp_code" to otpCode,
                    "device_id" to prefs.deviceId,
                    "device_name" to prefs.deviceName
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val linkResponse = gson.fromJson(responseBody, LinkResponse::class.java)
                prefs.groupId = linkResponse.group_id
                prefs.syncEnabled = true
                Result.success(linkResponse)
            } else {
                Result.failure(IOException("Link device failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Edge Function: Forward Notification ──────────────────────────────

    suspend fun forwardNotification(
        packageName: String,
        appName: String,
        title: String?,
        content: String?,
        timestamp: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${prefs.supabaseUrl}/functions/v1/forward-notification"
            val body = gson.toJson(
                mapOf(
                    "group_id" to prefs.groupId,
                    "sender_device_id" to prefs.deviceId,
                    "sender_device_name" to prefs.deviceName,
                    "package_name" to packageName,
                    "app_name" to appName,
                    "title" to (title ?: ""),
                    "content" to (content ?: ""),
                    "timestamp" to timestamp
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Forward failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── REST API: Forwarding Rules ───────────────────────────────────────

    data class ForwardRule(
        val id: String = "",
        val group_id: String = "",
        val app_source: String? = null,
        val text_contains: String? = null,
        val is_enabled: Boolean = true,
        val created_at: String = ""
    )

    suspend fun getForwardRules(): Result<List<ForwardRule>> = withContext(Dispatchers.IO) {
        try {
            val groupId = prefs.groupId
            if (groupId.isBlank()) return@withContext Result.success(emptyList())

            val url = "${prefs.supabaseUrl}/rest/v1/forward_rules?group_id=eq.$groupId&order=created_at.desc"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "[]"

            if (response.isSuccessful) {
                val type = object : TypeToken<List<ForwardRule>>() {}.type
                val rules: List<ForwardRule> = gson.fromJson(responseBody, type)
                Result.success(rules)
            } else {
                Result.failure(IOException("Get rules failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addForwardRule(
        appSource: String?,
        textContains: String?
    ): Result<ForwardRule> = withContext(Dispatchers.IO) {
        try {
            val url = "${prefs.supabaseUrl}/rest/v1/forward_rules"
            val body = gson.toJson(
                mapOf(
                    "group_id" to prefs.groupId,
                    "app_source" to (appSource?.ifBlank { null }),
                    "text_contains" to (textContains?.ifBlank { null }),
                    "is_enabled" to true
                )
            )
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val type = object : TypeToken<List<ForwardRule>>() {}.type
                val rules: List<ForwardRule> = gson.fromJson(responseBody, type)
                Result.success(rules.first())
            } else {
                Result.failure(IOException("Add rule failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteForwardRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${prefs.supabaseUrl}/rest/v1/forward_rules?id=eq.$ruleId"
            val request = Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 204) {
                Result.success(Unit)
            } else {
                val responseBody = response.body?.string() ?: ""
                Result.failure(IOException("Delete rule failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── REST API: Linked Devices ─────────────────────────────────────────

    data class LinkedDevice(
        val device_id: String = "",
        val device_name: String = "",
        val group_id: String = "",
        val created_at: String = ""
    )

    suspend fun getLinkedDevices(): Result<List<LinkedDevice>> = withContext(Dispatchers.IO) {
        try {
            val groupId = prefs.groupId
            if (groupId.isBlank()) return@withContext Result.success(emptyList())

            val url = "${prefs.supabaseUrl}/rest/v1/devices?group_id=eq.$groupId&order=created_at.asc"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "[]"

            if (response.isSuccessful) {
                val type = object : TypeToken<List<LinkedDevice>>() {}.type
                val devices: List<LinkedDevice> = gson.fromJson(responseBody, type)
                Result.success(devices)
            } else {
                Result.failure(IOException("Get devices failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Synchronous rule fetching (for NotificationListenerService) ──────

    fun getForwardRulesSync(): List<ForwardRule> {
        return try {
            val groupId = prefs.groupId
            if (groupId.isBlank()) return emptyList()

            val url = "${prefs.supabaseUrl}/rest/v1/forward_rules?group_id=eq.$groupId&is_enabled=eq.true"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", prefs.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${prefs.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "[]"

            if (response.isSuccessful) {
                val type = object : TypeToken<List<ForwardRule>>() {}.type
                gson.fromJson(responseBody, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
