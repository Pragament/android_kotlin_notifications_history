package com.pragament.notificationshistory.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pragament.notificationshistory.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Update
    suspend fun update(notification: NotificationEntity)

    @Delete
    suspend fun delete(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 1 ORDER BY timestamp DESC")
    fun getReadNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isSnoozed = 1 ORDER BY snoozeUntil ASC")
    fun getSnoozedNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getNotificationsByApp(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getNotificationsByDateRange(startTime: Long, endTime: Long): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: Long, isRead: Boolean)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isSnoozed = :isSnoozed, snoozeUntil = :snoozeUntil WHERE id = :id")
    suspend fun updateSnoozeStatus(id: Long, isSnoozed: Boolean, snoozeUntil: Long?)

    @Query("SELECT * FROM notifications WHERE isSnoozed = 1 AND snoozeUntil <= :currentTime")
    suspend fun getExpiredSnoozes(currentTime: Long): List<NotificationEntity>

    @Query("UPDATE notifications SET isSnoozed = 0, snoozeUntil = null WHERE isSnoozed = 1 AND snoozeUntil <= :currentTime")
    suspend fun clearExpiredSnoozes(currentTime: Long)

    @Query("SELECT DISTINCT packageName, appName FROM notifications ORDER BY appName")
    fun getDistinctApps(): Flow<List<AppInfo>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}

data class AppInfo(
    val packageName: String,
    val appName: String
)
