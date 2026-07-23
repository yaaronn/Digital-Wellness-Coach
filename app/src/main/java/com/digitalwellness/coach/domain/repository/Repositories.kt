package com.digitalwellness.coach.domain.repository

import com.digitalwellness.coach.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun getDailyUsage(date: Long): Flow<DailyUsage?>
    fun getWeeklyUsage(startDate: Long, endDate: Long): Flow<List<DailyUsage>>
    fun getMonthlyUsage(startDate: Long, endDate: Long): Flow<List<DailyUsage>>
    fun getAppUsageForDate(date: Long): Flow<List<AppUsage>>
    suspend fun syncTodayUsage()
    suspend fun getTodayScreenTime(): Long
    suspend fun getTodayUnlockCount(): Int
    suspend fun getLongestSession(): Long
}

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun getActiveGoals(): Flow<List<Goal>>
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goalId: Long)
    suspend fun getGoalProgress(goalId: Long): Float
}

interface FocusRepository {
    fun getAllSessions(): Flow<List<FocusSession>>
    fun getActiveSessions(): Flow<FocusSession?>
    suspend fun startSession(session: FocusSession): Long
    suspend fun endSession(sessionId: Long)
    suspend fun getTotalFocusTime(startDate: Long, endDate: Long): Long
    suspend fun getCompletedSessionCount(): Int
}

interface AchievementRepository {
    fun getAllAchievements(): Flow<List<Achievement>>
    suspend fun unlockAchievement(achievement: Achievement)
    suspend fun hasAchievement(type: com.digitalwellness.coach.domain.model.AchievementType): Boolean
    suspend fun getCurrentStreak(): Int
}

interface NotificationRepository {
    fun getPendingNotifications(): Flow<List<WellnessNotification>>
    suspend fun scheduleNotification(notification: WellnessNotification)
    suspend fun markAsSent(notificationId: Long)
    suspend fun cancelNotification(notificationId: Long)
}

interface PreferencesRepository {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    fun getDailyGoalMs(): Flow<Long>
    suspend fun setDailyGoalMs(goalMs: Long)
    fun isDarkMode(): Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
    fun isUsagePermissionGranted(): Flow<Boolean>
    suspend fun setUsagePermissionGranted(granted: Boolean)
    fun getBlockedApps(): Flow<List<String>>
    suspend fun setBlockedApps(packages: List<String>)
    fun getPetName(): Flow<String>
    suspend fun setPetName(name: String)
}
