package com.digitalwellness.coach.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.digitalwellness.coach.domain.repository.UsageRepository
import com.digitalwellness.coach.domain.usecase.CalculateAddictionScoreUseCase
import com.digitalwellness.coach.domain.usecase.GenerateWeeklyReportUseCase
import com.digitalwellness.coach.domain.usecase.GetWeeklyComparisonUseCase
import com.digitalwellness.coach.notifications.WellnessNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeekly: GetWeeklyComparisonUseCase,
    private val generateReport: GenerateWeeklyReportUseCase,
    private val notificationManager: WellnessNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val weeklyUsages = getWeekly().first()
            val report = generateReport(weeklyUsages)
            notificationManager.showWeeklySummary(report)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "weekly_summary_worker"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setInitialDelay(computeInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun computeInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            if (cal.timeInMillis < now) cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            return cal.timeInMillis - now
        }
    }
}
