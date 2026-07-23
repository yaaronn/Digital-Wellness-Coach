package com.digitalwellness.coach.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─── Type Converters ──────────────────────────────────────────────────────────

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

// ─── AppUsageEntity ───────────────────────────────────────────────────────────

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                     // midnight timestamp
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val lastUsed: Long,
    val openCount: Int = 0
)

// ─── DailyUsageEntity ────────────────────────────────────────────────────────

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey val date: Long,         // midnight timestamp (unique per day)
    val totalScreenTimeMs: Long,
    val unlockCount: Int,
    val longestSessionMs: Long
)

// ─── GoalEntity ───────────────────────────────────────────────────────────────

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                   // GoalType enum name
    val packageName: String?,
    val appName: String?,
    val targetTimeMs: Long,
    val createdAt: Long,
    val isActive: Boolean = true
)

// ─── FocusSessionEntity ───────────────────────────────────────────────────────

@Entity(tableName = "focus_sessions")
@TypeConverters(Converters::class)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,                   // FocusMode enum name
    val startTime: Long,
    val endTime: Long?,
    val plannedDurationMs: Long,
    val isCompleted: Boolean = false,
    val blockedApps: List<String> = emptyList()
)

// ─── AchievementEntity ────────────────────────────────────────────────────────

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                   // AchievementType enum name
    val earnedAt: Long,
    val streakCount: Int = 0
)

// ─── NotificationEntity ───────────────────────────────────────────────────────

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                   // NotificationType enum name
    val title: String,
    val message: String,
    val scheduledAt: Long,
    val isSent: Boolean = false
)
