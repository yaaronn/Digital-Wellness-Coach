package com.digitalwellness.coach.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.digitalwellness.coach.MainActivity
import com.digitalwellness.coach.R
import com.digitalwellness.coach.domain.model.HabitWarning
import com.digitalwellness.coach.domain.model.WeeklyReport
import com.digitalwellness.coach.domain.model.WarningType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellnessNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_HABITS    = "wellness_habits"
        const val CHANNEL_GOALS     = "wellness_goals"
        const val CHANNEL_REMINDERS = "wellness_reminders"
        const val CHANNEL_REPORTS   = "wellness_reports"
        const val CHANNEL_SERVICE   = "wellness_service"

        const val ID_HABIT_WARNING     = 1001
        const val ID_GOAL_EXCEEDED     = 1002
        const val ID_SLEEP_REMINDER    = 1003
        const val ID_BREAK_REMINDER    = 1004
        const val ID_WEEKLY_SUMMARY    = 1005
        const val ID_FOREGROUND_SVC    = 1006
    }

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channels = listOf(
            NotificationChannel(CHANNEL_HABITS,    "Habit Alerts",       NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications about unhealthy usage habits" },
            NotificationChannel(CHANNEL_GOALS,     "Goal Alerts",        NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when you exceed your goals" },
            NotificationChannel(CHANNEL_REMINDERS, "Reminders",          NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Break and sleep reminders" },
            NotificationChannel(CHANNEL_REPORTS,   "Weekly Reports",     NotificationManager.IMPORTANCE_LOW).apply {
                description = "Your weekly digital wellness summary" },
            NotificationChannel(CHANNEL_SERVICE,   "Background Tracker", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps usage tracking running" }
        )
        channels.forEach { notificationManager.createNotificationChannel(it) }
    }

    private fun launchIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun showHabitWarning(warning: HabitWarning) {
        val (title, channel) = when (warning.type) {
            WarningType.EXCESSIVE_SOCIAL_MEDIA -> "Social Media Check 📱" to CHANNEL_HABITS
            WarningType.CONTINUOUS_USAGE       -> "Screen Break Needed 👀" to CHANNEL_HABITS
            WarningType.LATE_NIGHT_USAGE       -> "Late Night Alert 🌙" to CHANNEL_HABITS
            WarningType.EXCESSIVE_UNLOCKING    -> "Too Many Unlocks 🔓" to CHANNEL_HABITS
            WarningType.GOAL_EXCEEDED          -> "Goal Exceeded ⚠️" to CHANNEL_GOALS
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(warning.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(warning.message))
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(ID_HABIT_WARNING + warning.type.ordinal, notification)
    }

    fun showGoalExceededAlert(totalMs: Long) {
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val notification = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Daily Goal Exceeded ⚠️")
            .setContentText("You've used your phone for ${hours}h ${minutes}m today. Time for a break!")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(ID_GOAL_EXCEEDED, notification)
    }

    fun showSleepReminder() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to Wind Down 🌙")
            .setContentText("It's late! Put your phone down and get some rest. Your digital pet will be happier tomorrow.")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(ID_SLEEP_REMINDER, notification)
    }

    fun showBreakReminder() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Take a Break 👁️")
            .setContentText("You've been on your phone for a while. Stand up, stretch, and rest your eyes for 10 minutes.")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .build()
        notificationManager.notify(ID_BREAK_REMINDER, notification)
    }

    fun showWeeklySummary(report: WeeklyReport) {
        val avgHours = report.avgDailyScreenTimeMs / 3_600_000
        val avgMinutes = (report.avgDailyScreenTimeMs % 3_600_000) / 60_000
        val topAppName = report.mostUsedApp?.appName ?: "unknown"

        val notification = NotificationCompat.Builder(context, CHANNEL_REPORTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your Weekly Wellness Report 📊")
            .setContentText("Avg daily: ${avgHours}h ${avgMinutes}m • Top app: $topAppName • Score: ${report.productivityScore}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Weekly Summary:\n" +
                        "• Avg daily usage: ${avgHours}h ${avgMinutes}m\n" +
                        "• Most used app: $topAppName\n" +
                        "• Productivity score: ${report.productivityScore}/100\n" +
                        "• Tap to see full report"
                    )
            )
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(ID_WEEKLY_SUMMARY, notification)
    }

    fun buildForegroundNotification() = NotificationCompat.Builder(context, CHANNEL_SERVICE)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Digital Wellness Coach")
        .setContentText("Monitoring your screen time in the background")
        .setContentIntent(launchIntent())
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setSilent(true)
        .build()
}
