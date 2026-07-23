package com.digitalwellness.coach.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.digitalwellness.coach.data.local.dao.*
import com.digitalwellness.coach.data.local.entities.*

@Database(
    entities = [
        AppUsageEntity::class,
        DailyUsageEntity::class,
        GoalEntity::class,
        FocusSessionEntity::class,
        AchievementEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WellnessDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun goalDao(): GoalDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "wellness_db"
    }
}
