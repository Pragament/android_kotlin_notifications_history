package com.pragament.notificationshistory.data.repository

import com.pragament.notificationshistory.data.dao.AppInfo
import com.pragament.notificationshistory.data.dao.NotificationDao
import com.pragament.notificationshistory.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    fun getAllNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    fun getUnreadNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getUnreadNotifications()

    fun getReadNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getReadNotifications()

    fun getSnoozedNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getSnoozedNotifications()

    fun getNotificationsByApp(packageName: String): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsByApp(packageName)

    fun getNotificationsByDateRange(startTime: Long, endTime: Long): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsByDateRange(startTime, endTime)

    fun getDistinctApps(): Flow<List<AppInfo>> =
        notificationDao.getDistinctApps()

    fun getUnreadCount(): Flow<Int> =
        notificationDao.getUnreadCount()

    suspend fun insertNotification(notification: NotificationEntity): Long =
        notificationDao.insert(notification)

    suspend fun updateNotification(notification: NotificationEntity) =
        notificationDao.update(notification)

    suspend fun deleteNotification(notification: NotificationEntity) =
        notificationDao.delete(notification)

    suspend fun deleteNotificationById(id: Long) =
        notificationDao.deleteById(id)

    suspend fun deleteAllNotifications() =
        notificationDao.deleteAll()

    suspend fun deleteOlderThan(timestamp: Long) =
        notificationDao.deleteOlderThan(timestamp)

    suspend fun markAsRead(id: Long) =
        notificationDao.updateReadStatus(id, true)

    suspend fun markAsUnread(id: Long) =
        notificationDao.updateReadStatus(id, false)

    suspend fun markAllAsRead() =
        notificationDao.markAllAsRead()

    suspend fun snoozeNotification(id: Long, snoozeUntil: Long) =
        notificationDao.updateSnoozeStatus(id, true, snoozeUntil)

    suspend fun unsnoozeNotification(id: Long) =
        notificationDao.updateSnoozeStatus(id, false, null)

    suspend fun getExpiredSnoozes(currentTime: Long): List<NotificationEntity> =
        notificationDao.getExpiredSnoozes(currentTime)

    suspend fun clearExpiredSnoozes(currentTime: Long) =
        notificationDao.clearExpiredSnoozes(currentTime)

    suspend fun getNotificationById(id: Long): NotificationEntity? =
        notificationDao.getNotificationById(id)
}
