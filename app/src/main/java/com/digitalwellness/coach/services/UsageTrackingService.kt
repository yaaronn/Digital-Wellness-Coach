package com.digitalwellness.coach.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.work.WorkManager
import com.digitalwellness.coach.notifications.WellnessNotificationManager
import com.digitalwellness.coach.workers.HabitCheckWorker
import com.digitalwellness.coach.workers.UsageSyncWorker
import com.digitalwellness.coach.workers.WeeklySummaryWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UsageTrackingService : Service() {

    @Inject lateinit var notificationManager: WellnessNotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            WellnessNotificationManager.ID_FOREGROUND_SVC,
            notificationManager.buildForegroundNotification()
        )

        // Schedule all periodic workers
        val wm = WorkManager.getInstance(applicationContext)
        UsageSyncWorker.schedule(wm)
        HabitCheckWorker.schedule(wm)
        WeeklySummaryWorker.schedule(wm)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
