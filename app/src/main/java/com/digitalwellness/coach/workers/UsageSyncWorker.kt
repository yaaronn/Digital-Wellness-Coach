package com.digitalwellness.coach.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.*
import com.digitalwellness.coach.domain.usecase.CalculateAddictionScoreUseCase
import com.digitalwellness.coach.domain.usecase.DetectHabitsUseCase
import com.digitalwellness.coach.domain.usecase.GetWeeklyComparisonUseCase
import com.digitalwellness.coach.notifications.WellnessNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val notificationManager: WellnessNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            usageRepository.syncTodayUsage()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "usage_sync_worker"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
