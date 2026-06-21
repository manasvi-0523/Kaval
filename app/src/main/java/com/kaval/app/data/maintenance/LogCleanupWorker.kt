package com.kaval.app.data.maintenance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaval.app.KavalApplication
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LogCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as KavalApplication
        val retentionDays = app.preferences.logRetentionDays.first()
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        app.repository.deleteOldCompletedLogs(cutoff)
        return Result.success()
    }
}

object LogCleanupScheduler {
    private const val UNIQUE_WORK = "weekly_safety_log_cleanup"

    fun schedule(context: Context) {
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.WEEK_OF_YEAR, 1)
        }
        val request = PeriodicWorkRequestBuilder<LogCleanupWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(nextSunday.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
