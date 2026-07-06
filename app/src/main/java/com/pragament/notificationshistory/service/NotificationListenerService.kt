package com.pragament.notificationshistory.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import com.pragament.notificationshistory.data.entity.NotificationEntity
import com.pragament.notificationshistory.data.preference.SupabasePrefs
import com.pragament.notificationshistory.data.remote.SupabaseManager
import com.pragament.notificationshistory.data.remote.SupabaseRealtimeClient
import com.pragament.notificationshistory.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var repository: NotificationRepository

    @Inject
    lateinit var supabasePrefs: SupabasePrefs

    @Inject
    lateinit var supabaseManager: SupabaseManager

    @Inject
    lateinit var realtimeClient: SupabaseRealtimeClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache forwarding rules to avoid fetching on every notification
    private var cachedRules: List<SupabaseManager.ForwardRule> = emptyList()
    private var lastRuleFetchTime: Long = 0
    private val ruleCacheTtlMs = 60_000L // Refresh rules every 60 seconds

    companion object {
        private const val TAG = "NotifListenerSvc"
    }

    override fun onCreate() {
        super.onCreate()
        // Connect to Supabase Realtime if sync is enabled
        if (supabasePrefs.syncEnabled && supabasePrefs.isLinked) {
            realtimeClient.connect()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { statusBarNotification ->
            // Skip our own notifications
            if (statusBarNotification.packageName == packageName) return

            val notification = statusBarNotification.notification
            val extras = notification.extras

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            
            // Skip if both title and content are empty
            if (title.isNullOrBlank() && content.isNullOrBlank()) return

            val appName = getAppName(statusBarNotification.packageName)
            val iconBase64 = getAppIconBase64(statusBarNotification.packageName)
            val category = notification.category

            val notificationEntity = NotificationEntity(
                packageName = statusBarNotification.packageName,
                appName = appName,
                title = title,
                content = content,
                timestamp = statusBarNotification.postTime,
                iconBase64 = iconBase64,
                category = category,
                notificationKey = statusBarNotification.key
            )

            serviceScope.launch {
                repository.insertNotification(notificationEntity)

                // Check if we should forward this notification
                if (supabasePrefs.syncEnabled && supabasePrefs.isLinked && supabasePrefs.isConfigured) {
                    checkAndForwardNotification(
                        packageName = statusBarNotification.packageName,
                        appName = appName,
                        title = title,
                        content = content,
                        timestamp = statusBarNotification.postTime
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optionally handle notification removal
        // We keep the history even after notification is dismissed
    }

    /**
     * Checks forwarding rules and forwards the notification if it matches any rule.
     */
    private suspend fun checkAndForwardNotification(
        packageName: String,
        appName: String,
        title: String?,
        content: String?,
        timestamp: Long
    ) {
        try {
            // Refresh cached rules if needed
            val now = System.currentTimeMillis()
            if (now - lastRuleFetchTime > ruleCacheTtlMs) {
                cachedRules = supabaseManager.getForwardRulesSync()
                lastRuleFetchTime = now
            }

            if (cachedRules.isEmpty()) return

            // Check if any rule matches this notification
            val matches = cachedRules.any { rule ->
                val appMatch = rule.app_source.isNullOrBlank() ||
                        packageName.contains(rule.app_source, ignoreCase = true) ||
                        appName.contains(rule.app_source, ignoreCase = true)

                val textMatch = rule.text_contains.isNullOrBlank() ||
                        (title?.contains(rule.text_contains, ignoreCase = true) == true) ||
                        (content?.contains(rule.text_contains, ignoreCase = true) == true)

                appMatch && textMatch
            }

            if (matches) {
                Log.d(TAG, "Forwarding notification from $appName")
                supabaseManager.forwardNotification(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    content = content,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/forwarding notification", e)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun getAppIconBase64(packageName: String): String? {
        return try {
            val packageManager = applicationContext.packageManager
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            bitmapToBase64(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Scale down to reduce storage size
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    override fun onDestroy() {
        realtimeClient.disconnect()
        super.onDestroy()
    }
}
