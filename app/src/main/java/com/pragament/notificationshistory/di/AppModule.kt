package com.pragament.notificationshistory.di

import android.content.Context
import androidx.room.Room
import com.pragament.notificationshistory.data.dao.NotificationDao
import com.pragament.notificationshistory.data.database.AppDatabase
import com.pragament.notificationshistory.data.preference.SupabasePrefs
import com.pragament.notificationshistory.data.remote.SupabaseManager
import com.pragament.notificationshistory.data.remote.SupabaseRealtimeClient
import com.pragament.notificationshistory.data.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    @Singleton
    fun provideSupabasePrefs(
        @ApplicationContext context: Context
    ): SupabasePrefs {
        return SupabasePrefs(context)
    }

    @Provides
    @Singleton
    fun provideSupabaseManager(
        prefs: SupabasePrefs
    ): SupabaseManager {
        return SupabaseManager(prefs)
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtimeClient(
        @ApplicationContext context: Context,
        prefs: SupabasePrefs,
        repository: NotificationRepository
    ): SupabaseRealtimeClient {
        return SupabaseRealtimeClient(context, prefs, repository)
    }
}
