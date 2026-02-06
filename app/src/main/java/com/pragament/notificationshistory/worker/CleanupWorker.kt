package com.pragament.notificationshistory.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pragament.notificationshistory.data.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NotificationRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val autoDeleteDays = inputData.getInt(KEY_AUTO_DELETE_DAYS, 0)
            
            if (autoDeleteDays > 0) {
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(autoDeleteDays.toLong())
                repository.deleteOlderThan(cutoffTime)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "cleanup_worker"
        const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    }
}
