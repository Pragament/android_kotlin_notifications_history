package com.pragament.notificationshistory.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.pragament.notificationshistory.R
import com.pragament.notificationshistory.data.entity.NotificationEntity
import com.pragament.notificationshistory.data.preference.SupabasePrefs
import com.pragament.notificationshistory.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Connects to Supabase Realtime via WebSocket to listen for new
 * forwarded notifications inserted by other devices in the same group.
 *
 * When a notification is received from another device, it:
 * 1. Inserts it into the local Room database
 * 2. Shows a local Android notification
 */
class SupabaseRealtimeClient(
    private val context: Context,
    private val prefs: SupabasePrefs,
    private val repository: NotificationRepository
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val CHANNEL_ID = "forwarded_notifications"
        private const val CHANNEL_NAME = "Forwarded Notifications"
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val notificationIdCounter = AtomicInteger(5000)
    private val isConnected = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    init {
        createNotificationChannel()
    }

    fun connect() {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected")
            return
        }
        if (!prefs.isConfigured || !prefs.isLinked) {
            Log.d(TAG, "Not configured or not linked, skipping connection")
            return
        }

        val wsUrl = prefs.supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") +
                "/realtime/v1/websocket?apikey=${prefs.supabaseAnonKey}&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected.set(true)
                reconnectAttempt = 0

                // Join the realtime channel for forwarded_notifications
                val joinPayload = """
                    {
                        "topic": "realtime:public:forwarded_notifications:group_id=eq.${prefs.groupId}",
                        "event": "phx_join",
                        "payload": {},
                        "ref": "1"
                    }
                """.trimIndent()
                webSocket.send(joinPayload)
                Log.d(TAG, "Sent join for group: ${prefs.groupId}")

                // Start heartbeat
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                isConnected.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                isConnected.set(false)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isConnected.set(false)
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        isConnected.set(false)
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    private fun startHeartbeat(ws: WebSocket) {
        scope.launch {
            while (isConnected.get()) {
                delay(30_000)
                if (isConnected.get()) {
                    val heartbeat = """
                        {
                            "topic": "phoenix",
                            "event": "heartbeat",
                            "payload": {},
                            "ref": "heartbeat"
                        }
                    """.trimIndent()
                    ws.send(heartbeat)
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!prefs.syncEnabled) return
        reconnectAttempt++
        val delayMs = minOf(
            (1000L * (1 shl minOf(reconnectAttempt, 6))),
            MAX_RECONNECT_DELAY_MS
        )
        Log.d(TAG, "Scheduling reconnect in ${delayMs}ms (attempt $reconnectAttempt)")
        scope.launch {
            delay(delayMs)
            if (prefs.syncEnabled && !isConnected.get()) {
                connect()
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val event = json.get("event")?.asString ?: return
            val topic = json.get("topic")?.asString ?: return

            // We only care about INSERT events on our channel
            if (event == "INSERT" && topic.startsWith("realtime:public:forwarded_notifications")) {
                val payload = json.getAsJsonObject("payload")
                val record = payload?.getAsJsonObject("record") ?: return

                val senderDeviceId = record.get("sender_device_id")?.asString ?: ""

                // Skip notifications from this device
                if (senderDeviceId == prefs.deviceId) {
                    Log.d(TAG, "Skipping own notification")
                    return
                }

                val senderDeviceName = record.get("sender_device_name")?.asString ?: "Unknown"
                val packageName = record.get("package_name")?.asString ?: ""
                val appName = record.get("app_name")?.asString ?: ""
                val title = record.get("title")?.asString
                val content = record.get("content")?.asString
                val timestamp = record.get("timestamp")?.asLong ?: System.currentTimeMillis()

                // Insert into local database
                scope.launch {
                    val entity = NotificationEntity(
                        packageName = packageName,
                        appName = "$appName (from $senderDeviceName)",
                        title = title,
                        content = content,
                        timestamp = timestamp,
                        iconBase64 = null,
                        category = "forwarded",
                        notificationKey = "forwarded_${System.currentTimeMillis()}"
                    )
                    repository.insertNotification(entity)
                }

                // Show local notification
                showForwardedNotification(senderDeviceName, appName, title, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: $text", e)
        }
    }

    private fun showForwardedNotification(
        senderDeviceName: String,
        appName: String,
        title: String?,
        content: String?
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationTitle = "From $senderDeviceName: $appName"
        val notificationText = buildString {
            if (!title.isNullOrBlank()) append(title)
            if (!title.isNullOrBlank() && !content.isNullOrBlank()) append(" - ")
            if (!content.isNullOrBlank()) append(content)
        }.ifBlank { "New forwarded notification" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationIdCounter.getAndIncrement(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications forwarded from linked devices"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
