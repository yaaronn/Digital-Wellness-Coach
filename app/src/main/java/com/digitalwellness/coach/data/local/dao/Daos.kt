package com.digitalwellness.coach.data.local.dao

import androidx.room.*
import com.digitalwellness.coach.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ─── AppUsage DAO ─────────────────────────────────────────────────────────────

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageTimeMs DESC")
    fun getAppUsagesForDate(date: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, usageTimeMs DESC")
    fun getAppUsagesForRange(startDate: Long, endDate: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName ORDER BY date DESC")
    fun getAppHistory(packageName: String): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(entity: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AppUsageEntity>)

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteForDate(date: Long)

    @Query("DELETE FROM app_usage WHERE date < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}

// ─── DailyUsage DAO ──────────────────────────────────────────────────────────

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage WHERE date = :date")
    fun getDailyUsage(date: Long): Flow<DailyUsageEntity?>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getUsageForRange(startDate: Long, endDate: Long): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT 30")
    fun getRecentUsage(): Flow<List<DailyUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyUsageEntity)

    @Query("SELECT AVG(totalScreenTimeMs) FROM daily_usage WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAvgScreenTime(startDate: Long, endDate: Long): Long?

    @Query("SELECT MAX(totalScreenTimeMs) FROM daily_usage WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMaxScreenTime(startDate: Long, endDate: Long): Long?
}

// ─── Goal DAO ────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY type ASC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(entity: GoalEntity): Long

    @Update
    suspend fun updateGoal(entity: GoalEntity)

    @Query("UPDATE goals SET isActive = 0 WHERE id = :goalId")
    suspend fun deactivateGoal(goalId: Long)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)
}

// ─── FocusSession DAO ────────────────────────────────────────────────────────

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE startTime BETWEEN :start AND :end")
    fun getSessionsForRange(start: Long, end: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT SUM(endTime - startTime) FROM focus_sessions WHERE isCompleted = 1 AND startTime BETWEEN :start AND :end")
    suspend fun getTotalFocusTime(start: Long, end: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(entity: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET endTime = :endTime, isCompleted = 1 WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long, endTime: Long)
}

// ─── Achievement DAO ─────────────────────────────────────────────────────────

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY earnedAt DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE type = :type LIMIT 1")
    suspend fun getAchievementByType(type: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(entity: AchievementEntity)

    @Query("SELECT MAX(streakCount) FROM achievements WHERE type LIKE 'STREAK%'")
    suspend fun getMaxStreak(): Int?
}

// ─── Notification DAO ────────────────────────────────────────────────────────

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE isSent = 0 ORDER BY scheduledAt ASC")
    fun getPendingNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(entity: NotificationEntity): Long

    @Query("UPDATE notifications SET isSent = 1 WHERE id = :notificationId")
    suspend fun markAsSent(notificationId: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM notifications WHERE isSent = 1 AND scheduledAt < :before")
    suspend fun cleanOldNotifications(before: Long)
}
