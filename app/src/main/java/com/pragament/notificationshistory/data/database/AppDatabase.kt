package com.pragament.notificationshistory.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pragament.notificationshistory.data.dao.NotificationDao
import com.pragament.notificationshistory.data.entity.NotificationEntity

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "notifications_history_db"
    }
}
