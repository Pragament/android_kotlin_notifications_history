package com.pragament.notificationshistory.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozeUntil: Long? = null,
    val iconBase64: String? = null,
    val category: String? = null,
    val notificationKey: String? = null
)
