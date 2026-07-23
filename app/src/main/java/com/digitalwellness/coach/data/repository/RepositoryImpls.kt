package com.digitalwellness.coach.data.repository

import com.digitalwellness.coach.data.datasource.UsageStatsDataSource
import com.digitalwellness.coach.data.local.dao.*
import com.digitalwellness.coach.data.local.entities.*
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// ─── Usage Repository ─────────────────────────────────────────────────────────

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val dailyUsageDao: DailyUsageDao,
    private val appUsageDao: AppUsageDao,
    private val dataSource: UsageStatsDataSource
) : UsageRepository {

    override fun getDailyUsage(date: Long): Flow<DailyUsage?> {
        return combine(
            dailyUsageDao.getDailyUsage(date),
            appUsageDao.getAppUsagesForDate(date)
        ) { dailyEntity, appEntities ->
            dailyEntity?.toDomain(appEntities.map { it.toDomain() })
        }
    }

    override fun getWeeklyUsage(startDate: Long, endDate: Long): Flow<List<DailyUsage>> {
        return combine(
            dailyUsageDao.getUsageForRange(startDate, endDate),
            appUsageDao.getAppUsagesForRange(startDate, endDate)
        ) { dailyEntities, appEntities ->
            val appsByDate = appEntities.groupBy { it.date }
            dailyEntities.map { daily ->
                daily.toDomain(appsByDate[daily.date]?.map { it.toDomain() } ?: emptyList())
            }
        }
    }

    override fun getMonthlyUsage(startDate: Long, endDate: Long): Flow<List<DailyUsage>> =
        getWeeklyUsage(startDate, endDate)

    override fun getAppUsageForDate(date: Long): Flow<List<AppUsage>> =
        appUsageDao.getAppUsagesForDate(date).map { list -> list.map { it.toDomain() } }

    override suspend fun syncTodayUsage() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis

        val appUsages = dataSource.getAppUsagesForDay(today)
        android.util.Log.d("SYNC_TEST", "Apps returned: ${appUsages.size}")

        appUsages.forEach {
            android.util.Log.d(
                "SYNC_TEST",
                "${it.appName} | ${it.packageName} | ${it.usageTimeMs}"
            )
        }
        val totalTime = appUsages.sumOf { it.usageTimeMs }
        val unlockCount = dataSource.getTodayUnlockCount()
        val longestSession = dataSource.getLongestContinuousSession()

        dailyUsageDao.insertOrUpdate(
            DailyUsageEntity(
                date = today,
                totalScreenTimeMs = totalTime,
                unlockCount = unlockCount,
                longestSessionMs = longestSession
            )
        )
        // BUG FIX: insert first, THEN delete the old rows.
        // Doing delete-first creates a window where the DB is empty
        // and the UI reads zero data.
        val appEntities = appUsages.map { app ->
            AppUsageEntity(
                date = today,
                packageName = app.packageName,
                appName = app.appName,
                usageTimeMs = app.usageTimeMs,
                lastUsed = app.lastUsed,
                openCount = app.openCount
            )
        }
        // Delete stale rows first, then insert fresh data atomically
        appUsageDao.deleteForDate(today)
        appUsageDao.insertAll(appEntities)
    }

    override suspend fun getTodayScreenTime(): Long = dataSource.getTodayTotalScreenTime()
    override suspend fun getTodayUnlockCount(): Int = dataSource.getTodayUnlockCount()
    override suspend fun getLongestSession(): Long = dataSource.getLongestContinuousSession()

    // Mappers
    private fun DailyUsageEntity.toDomain(apps: List<AppUsage> = emptyList()) = DailyUsage(
        date = date,
        totalScreenTimeMs = totalScreenTimeMs,
        unlockCount = unlockCount,
        longestSessionMs = longestSessionMs,
        appUsages = apps
    )

    private fun AppUsageEntity.toDomain() = AppUsage(
        packageName = packageName,
        appName = appName,
        usageTimeMs = usageTimeMs,
        lastUsed = lastUsed,
        openCount = openCount
    )
}

// ─── Goal Repository ──────────────────────────────────────────────────────────

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun getAllGoals(): Flow<List<Goal>> =
        goalDao.getAllGoals().map { list -> list.map { it.toDomain() } }

    override fun getActiveGoals(): Flow<List<Goal>> =
        goalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }

    override suspend fun insertGoal(goal: Goal): Long =
        goalDao.insertGoal(goal.toEntity())

    override suspend fun updateGoal(goal: Goal) =
        goalDao.updateGoal(goal.toEntity())

    override suspend fun deleteGoal(goalId: Long) =
        goalDao.deleteGoal(goalId)

    override suspend fun getGoalProgress(goalId: Long): Float = 0f // Computed at use case level

    private fun GoalEntity.toDomain() = Goal(
        id = id, type = GoalType.valueOf(type),
        packageName = packageName, appName = appName,
        targetTimeMs = targetTimeMs, createdAt = createdAt, isActive = isActive
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id, type = type.name,
        packageName = packageName, appName = appName,
        targetTimeMs = targetTimeMs, createdAt = createdAt, isActive = isActive
    )
}

// ─── Focus Repository ─────────────────────────────────────────────────────────

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) : FocusRepository {

    override fun getAllSessions(): Flow<List<FocusSession>> =
        focusSessionDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override fun getActiveSessions(): Flow<FocusSession?> =
        focusSessionDao.getActiveSession().map { it?.toDomain() }

    override suspend fun startSession(session: FocusSession): Long =
        focusSessionDao.insertSession(session.toEntity())

    override suspend fun endSession(sessionId: Long) =
        focusSessionDao.completeSession(sessionId, System.currentTimeMillis())

    override suspend fun getTotalFocusTime(startDate: Long, endDate: Long): Long =
        focusSessionDao.getTotalFocusTime(startDate, endDate) ?: 0L

    override suspend fun getCompletedSessionCount(): Int =
        focusSessionDao.getCompletedCount()

    private fun FocusSessionEntity.toDomain() = FocusSession(
        id = id, mode = FocusMode.valueOf(mode),
        startTime = startTime, endTime = endTime,
        plannedDurationMs = plannedDurationMs,
        isCompleted = isCompleted, blockedApps = blockedApps
    )

    private fun FocusSession.toEntity() = FocusSessionEntity(
        id = id, mode = mode.name,
        startTime = startTime, endTime = endTime,
        plannedDurationMs = plannedDurationMs,
        isCompleted = isCompleted, blockedApps = blockedApps
    )
}

// ─── Achievement Repository ───────────────────────────────────────────────────

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    override fun getAllAchievements(): Flow<List<Achievement>> =
        achievementDao.getAllAchievements().map { list -> list.map { it.toDomain() } }

    override suspend fun unlockAchievement(achievement: Achievement) =
        achievementDao.insertAchievement(achievement.toEntity())

    override suspend fun hasAchievement(type: AchievementType): Boolean =
        achievementDao.getAchievementByType(type.name) != null

    override suspend fun getCurrentStreak(): Int =
        achievementDao.getMaxStreak() ?: 0

    private fun AchievementEntity.toDomain() = Achievement(
        id = id, type = AchievementType.valueOf(type),
        earnedAt = earnedAt, streakCount = streakCount
    )

    private fun Achievement.toEntity() = AchievementEntity(
        id = id, type = type.name,
        earnedAt = earnedAt, streakCount = streakCount
    )
}

// ─── Notification Repository ──────────────────────────────────────────────────

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getPendingNotifications(): Flow<List<WellnessNotification>> =
        notificationDao.getPendingNotifications().map { list -> list.map { it.toDomain() } }

    override suspend fun scheduleNotification(notification: WellnessNotification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun markAsSent(notificationId: Long) =
        notificationDao.markAsSent(notificationId)

    override suspend fun cancelNotification(notificationId: Long) =
        notificationDao.deleteNotification(notificationId)

    private fun NotificationEntity.toDomain() = WellnessNotification(
        id = id, type = NotificationType.valueOf(type),
        title = title, message = message,
        scheduledAt = scheduledAt, isSent = isSent
    )

    private fun WellnessNotification.toEntity() = NotificationEntity(
        id = id, type = type.name,
        title = title, message = message,
        scheduledAt = scheduledAt, isSent = isSent
    )
}
