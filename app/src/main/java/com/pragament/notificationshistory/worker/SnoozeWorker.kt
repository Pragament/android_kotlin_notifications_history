package com.pragament.notificationshistory.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pragament.notificationshistory.data.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SnoozeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NotificationRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Get all expired snoozes
            val expiredSnoozes = repository.getExpiredSnoozes(currentTime)
            
            // Clear the snoozed status for expired notifications
            expiredSnoozes.forEach { notification ->
                repository.unsnoozeNotification(notification.id)
            }
            
            // Optionally: Show a notification for each unsnoozed item
            // This would require NotificationManager and creating notification channels
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "snooze_check_worker"
    }
}
