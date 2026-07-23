package com.digitalwellness.coach.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.UsageRepository
import com.digitalwellness.coach.domain.usecase.CalculateAddictionScoreUseCase
import com.digitalwellness.coach.domain.usecase.DetectHabitsUseCase
import com.digitalwellness.coach.notifications.WellnessNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class HabitCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val detectHabits: DetectHabitsUseCase,
    private val calculateScore: CalculateAddictionScoreUseCase,
    private val notificationManager: WellnessNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val today = usageRepository.getDailyUsage(cal.timeInMillis).first()

            today?.let { usage ->
                val warnings = detectHabits(usage)
                warnings.forEach { warning ->
                    notificationManager.showHabitWarning(warning)
                }

                // Late night check
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hour >= 23 || hour < 1) {
                    notificationManager.showSleepReminder()
                }

                // Goal check
                val totalMs = usage.totalScreenTimeMs
                if (totalMs > 4 * 3_600_000L) {
                    notificationManager.showGoalExceededAlert(totalMs)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "habit_check_worker"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<HabitCheckWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
