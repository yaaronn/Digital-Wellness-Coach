package com.digitalwellness.coach.domain.model

import android.graphics.drawable.Drawable

// ─── App Usage ───────────────────────────────────────────────────────────────

data class AppUsage(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val usageTimeMs: Long,
    val lastUsed: Long,
    val openCount: Int = 0
) {
    val usageTimeMinutes: Long get() = usageTimeMs / 60_000
    val usageTimeHours: Float get() = usageTimeMs / 3_600_000f
    val formattedTime: String
        get() {
            val totalMinutes = usageTimeMs / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
}

// ─── Daily Usage ─────────────────────────────────────────────────────────────

data class DailyUsage(
    val date: Long,                         // epoch day in millis (midnight)
    val totalScreenTimeMs: Long,
    val unlockCount: Int,
    val longestSessionMs: Long,
    val appUsages: List<AppUsage> = emptyList()
) {
    val totalScreenTimeMinutes: Long get() = totalScreenTimeMs / 60_000
    val totalScreenTimeHours: Float get() = totalScreenTimeMs / 3_600_000f
    val formattedTotalTime: String
        get() {
            val hours = totalScreenTimeMs / 3_600_000
            val minutes = (totalScreenTimeMs % 3_600_000) / 60_000
            return "${hours}h ${minutes}m"
        }
}

// ─── Goal ────────────────────────────────────────────────────────────────────

data class Goal(
    val id: Long = 0,
    val type: GoalType,
    val packageName: String? = null,      // null = daily total goal
    val appName: String? = null,
    val targetTimeMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class GoalType {
    DAILY_TOTAL,
    PER_APP
}

// ─── Focus Session ───────────────────────────────────────────────────────────

data class FocusSession(
    val id: Long = 0,
    val mode: FocusMode,
    val startTime: Long,
    val endTime: Long? = null,
    val plannedDurationMs: Long,
    val isCompleted: Boolean = false,
    val blockedApps: List<String> = emptyList()
)

enum class FocusMode(val label: String, val emoji: String) {
    WORK("Work", "💼"),
    STUDY("Study", "📚"),
    SLEEP("Sleep", "🌙"),
    CUSTOM("Custom", "⚙️")
}

// ─── Achievement ─────────────────────────────────────────────────────────────

data class Achievement(
    val id: Long = 0,
    val type: AchievementType,
    val earnedAt: Long,
    val streakCount: Int = 0
)

enum class AchievementType(
    val title: String,
    val description: String,
    val emoji: String
) {
    STREAK_3("3-Day Streak", "Under your goal 3 days in a row", "🔥"),
    STREAK_7("Week Warrior", "Under your goal 7 days in a row", "⚡"),
    STREAK_15("Two-Week Champion", "Under your goal 15 days in a row", "🏆"),
    STREAK_30("Monthly Master", "Under your goal 30 days in a row", "👑"),
    EARLY_SLEEPER("Early Sleeper", "No phone after 10 PM for a week", "🌙"),
    SOCIAL_REDUCER("Social Media Reducer", "Social media under 1hr for 3 days", "📵"),
    FOCUS_MASTER("Focus Master", "Completed 10 focus sessions", "🎯"),
    PRODUCTIVITY_CHAMPION("Productivity Champion", "Addiction score under 30 for a week", "🚀")
}

// ─── Addiction Score ─────────────────────────────────────────────────────────

data class AddictionScore(
    val score: Int,                         // 0–100
    val level: AddictionLevel,
    val suggestions: List<String>
)

enum class AddictionLevel(
    val label: String,
    val range: IntRange,
    val color: String
) {
    HEALTHY("Healthy", 0..30, "#4CAF50"),
    MODERATE("Moderate", 31..55, "#FF9800"),
    HIGH("High", 56..75, "#F44336"),
    SEVERE("Severe", 76..100, "#B71C1C")
}

// ─── Habit Warning ───────────────────────────────────────────────────────────

data class HabitWarning(
    val type: WarningType,
    val message: String,
    val affectedApp: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class WarningType {
    EXCESSIVE_SOCIAL_MEDIA,
    CONTINUOUS_USAGE,
    LATE_NIGHT_USAGE,
    EXCESSIVE_UNLOCKING,
    GOAL_EXCEEDED
}

// ─── Digital Pet ─────────────────────────────────────────────────────────────

data class DigitalPet(
    val state: PetState,
    val name: String = "Buddy",
    val message: String
)

enum class PetState(val emoji: String, val label: String) {
    HAPPY("😊", "Happy"),
    NORMAL("😐", "Normal"),
    TIRED("😴", "Tired"),
    EXHAUSTED("😵", "Exhausted")
}

// ─── Notification ────────────────────────────────────────────────────────────

data class WellnessNotification(
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val scheduledAt: Long,
    val isSent: Boolean = false
)

enum class NotificationType {
    BREAK_REMINDER,
    SLEEP_REMINDER,
    GOAL_EXCEEDED,
    WEEKLY_SUMMARY,
    STREAK_MILESTONE
}

// ─── Weekly Report ───────────────────────────────────────────────────────────

data class WeeklyReport(
    val weekStart: Long,
    val weekEnd: Long,
    val totalScreenTimeMs: Long,
    val avgDailyScreenTimeMs: Long,
    val mostUsedApp: AppUsage?,
    val addictionScoreTrend: List<Int>,
    val productivityScore: Int,
    val improvementPercentage: Float,
    val topApps: List<AppUsage>
)
