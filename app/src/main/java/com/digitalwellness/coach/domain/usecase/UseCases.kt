package com.digitalwellness.coach.domain.usecase

import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

// ─── Dashboard Use Cases ──────────────────────────────────────────────────────

class GetTodayStatsUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(): Flow<DailyUsage?> {
        val today = getStartOfDay()
        return usageRepository.getDailyUsage(today)
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class GetWeeklyComparisonUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(): Flow<List<DailyUsage>> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val start = cal.timeInMillis
        return usageRepository.getWeeklyUsage(start, end)
    }
}

class GetDailyGoalProgressUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<Float> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return combine(
            usageRepository.getDailyUsage(today),
            preferencesRepository.getDailyGoalMs()
        ) { usage, goalMs ->
            if (goalMs == 0L) 0f
            else ((usage?.totalScreenTimeMs ?: 0L).toFloat() / goalMs).coerceIn(0f, 1f)
        }
    }
}

// ─── Analytics Use Cases ─────────────────────────────────────────────────────

class GetAppUsageListUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(date: Long): Flow<List<AppUsage>> =
        usageRepository.getAppUsageForDate(date)
}

class GetMonthlyTrendUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(): Flow<List<DailyUsage>> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val start = cal.timeInMillis
        return usageRepository.getMonthlyUsage(start, end)
    }
}

// ─── Addiction Score Use Case ─────────────────────────────────────────────────

class CalculateAddictionScoreUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(dailyUsages: List<DailyUsage>): AddictionScore {
        if (dailyUsages.isEmpty()) return AddictionScore(0, AddictionLevel.HEALTHY, emptyList())

        val avgScreenTimeHours = dailyUsages.map { it.totalScreenTimeHours }.average().toFloat()
        val avgUnlocks = dailyUsages.map { it.unlockCount }.average().toFloat()
        val longestSessionHours = dailyUsages.map { it.longestSessionMs / 3_600_000f }.maxOrNull() ?: 0f

        // Weighted scoring
        val screenTimeScore = when {
            avgScreenTimeHours <= 3f -> (avgScreenTimeHours / 3f) * 30
            avgScreenTimeHours <= 5f -> 30 + ((avgScreenTimeHours - 3f) / 2f) * 25
            avgScreenTimeHours <= 7f -> 55 + ((avgScreenTimeHours - 5f) / 2f) * 25
            else -> 80 + ((avgScreenTimeHours - 7f) / 5f) * 20
        }
        val unlockScore = (avgUnlocks / 150f * 20).coerceAtMost(20f)
        val sessionScore = (longestSessionHours / 3f * 10).coerceAtMost(10f)

        val totalScore = (screenTimeScore + unlockScore + sessionScore).toInt().coerceIn(0, 100)

        val level = AddictionLevel.entries.first { totalScore in it.range }

        val suggestions = generateSuggestions(avgScreenTimeHours, avgUnlocks, longestSessionHours, level)

        return AddictionScore(totalScore, level, suggestions)
    }

    private fun generateSuggestions(
        avgHours: Float,
        avgUnlocks: Float,
        longestSession: Float,
        level: AddictionLevel
    ): List<String> {
        val suggestions = mutableListOf<String>()

        when (level) {
            AddictionLevel.HEALTHY -> {
                suggestions.add("Great job! Keep maintaining these healthy habits.")
                suggestions.add("Consider enabling Focus Mode during work hours.")
            }
            AddictionLevel.MODERATE -> {
                suggestions.add("Try scheduling phone-free periods each day.")
                if (avgUnlocks > 80) suggestions.add("Reduce unnecessary phone checks by keeping it face-down.")
                suggestions.add("Enable Do Not Disturb during meals and bedtime.")
            }
            AddictionLevel.HIGH -> {
                suggestions.add("Set a daily screen time goal of ${(avgHours * 0.7).toInt()} hours.")
                suggestions.add("Delete or limit your most-used social media apps.")
                suggestions.add("Use the Focus Mode feature during key parts of your day.")
                if (longestSession > 1.5f) suggestions.add("Take a break every 45 minutes of screen time.")
            }
            AddictionLevel.SEVERE -> {
                suggestions.add("Your phone usage is significantly affecting your wellbeing.")
                suggestions.add("Set strict app limits and use the App Lock feature.")
                suggestions.add("Consider a digital detox — one screen-free day per week.")
                suggestions.add("Talk to someone you trust about reducing phone dependency.")
                suggestions.add("Replace 30 minutes of phone time with a physical activity.")
            }
        }

        return suggestions
    }
}

// ─── Habit Detection Use Case ─────────────────────────────────────────────────

class DetectHabitsUseCase @Inject constructor() {
    operator fun invoke(todayUsage: DailyUsage): List<HabitWarning> {
        val warnings = mutableListOf<HabitWarning>()

        // Excessive social media
        val socialApps = listOf("instagram", "facebook", "tiktok", "twitter", "snapchat", "youtube")
        val socialTimeMs = todayUsage.appUsages
            .filter { app -> socialApps.any { it in app.packageName.lowercase() } }
            .sumOf { it.usageTimeMs }
        if (socialTimeMs > 3 * 3_600_000L) {
            warnings.add(HabitWarning(
                type = WarningType.EXCESSIVE_SOCIAL_MEDIA,
                message = "You've spent ${socialTimeMs / 3_600_000}h+ on social media today. Consider taking a break."
            ))
        }

        // Continuous usage
        if (todayUsage.longestSessionMs > 3_600_000L) {
            val hours = todayUsage.longestSessionMs / 3_600_000
            warnings.add(HabitWarning(
                type = WarningType.CONTINUOUS_USAGE,
                message = "You had a continuous screen session of ${hours}h. A 10-minute break helps your eyes and mind."
            ))
        }

        // Excessive unlocking
        if (todayUsage.unlockCount > 100) {
            warnings.add(HabitWarning(
                type = WarningType.EXCESSIVE_UNLOCKING,
                message = "You've unlocked your phone ${todayUsage.unlockCount} times today. Try keeping it out of reach."
            ))
        }

        return warnings
    }
}

// ─── Goal Use Cases ──────────────────────────────────────────────────────────

class ManageGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    fun getActiveGoals(): Flow<List<Goal>> = goalRepository.getActiveGoals()

    suspend fun createDailyGoal(targetHours: Float): Long {
        val goal = Goal(
            type = GoalType.DAILY_TOTAL,
            targetTimeMs = (targetHours * 3_600_000).toLong()
        )
        return goalRepository.insertGoal(goal)
    }

    suspend fun createAppGoal(packageName: String, appName: String, targetMinutes: Int): Long {
        val goal = Goal(
            type = GoalType.PER_APP,
            packageName = packageName,
            appName = appName,
            targetTimeMs = targetMinutes * 60_000L
        )
        return goalRepository.insertGoal(goal)
    }

    suspend fun deleteGoal(goalId: Long) = goalRepository.deleteGoal(goalId)
}

// ─── AI Recommendations Use Case ─────────────────────────────────────────────

class GetAIRecommendationsUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(weeklyUsages: List<DailyUsage>): List<String> {
        if (weeklyUsages.isEmpty()) return listOf("Start tracking your usage to get personalized recommendations!")

        val recommendations = mutableListOf<String>()

        // Peak hour analysis
        val avgScreenTime = weeklyUsages.map { it.totalScreenTimeHours }.average()
        val previousWeekAvg = avgScreenTime * 0.9 // Simulated comparison

        if (avgScreenTime > previousWeekAvg * 1.25) {
            recommendations.add("📈 Your screen time has increased by ${((avgScreenTime / previousWeekAvg - 1) * 100).toInt()}% this week. Consider enabling Focus Mode.")
        }

        // Most used app recommendation
        val allAppUsages = weeklyUsages.flatMap { it.appUsages }
        val topApp = allAppUsages
            .groupBy { it.packageName }
            .maxByOrNull { it.value.sumOf { u -> u.usageTimeMs } }

        topApp?.let {
            val totalMs = it.value.sumOf { u -> u.usageTimeMs }
            val avgDailyHours = totalMs / weeklyUsages.size / 3_600_000f
            if (avgDailyHours > 1f) {
                recommendations.add("📱 You spend an average of ${String.format("%.1f", avgDailyHours)}h daily on ${it.value.first().appName}. Try setting an app limit.")
            }
        }

        // Unlock pattern
        val avgUnlocks = weeklyUsages.map { it.unlockCount }.average()
        if (avgUnlocks > 80) {
            recommendations.add("🔓 You unlock your phone ~${avgUnlocks.toInt()} times daily. Try keeping it in a bag or drawer to reduce compulsive checks.")
        }

        // Positive reinforcement
        val improving = weeklyUsages.takeLast(3).map { it.totalScreenTimeMs }.zipWithNext().all { (a, b) -> b < a }
        if (improving) {
            recommendations.add("✅ Your screen time has been decreasing for 3 days. You're building great habits!")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("You're doing well! Keep tracking your usage to spot patterns.")
            recommendations.add("Try the Focus Mode feature to build productive daily routines.")
        }

        return recommendations
    }
}

// ─── Focus Use Cases ─────────────────────────────────────────────────────────

class FocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    fun getActiveSession(): Flow<FocusSession?> = focusRepository.getActiveSessions()

    suspend fun startFocus(mode: FocusMode, durationMinutes: Int, blockedApps: List<String> = emptyList()): Long {
        val session = FocusSession(
            mode = mode,
            startTime = System.currentTimeMillis(),
            plannedDurationMs = durationMinutes * 60_000L,
            blockedApps = blockedApps
        )
        return focusRepository.startSession(session)
    }

    suspend fun endFocus(sessionId: Long) = focusRepository.endSession(sessionId)

    suspend fun getCompletedCount(): Int = focusRepository.getCompletedSessionCount()
}

// ─── Weekly Report Use Case ───────────────────────────────────────────────────

class GenerateWeeklyReportUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val calculateAddictionScore: CalculateAddictionScoreUseCase
) {
    suspend operator fun invoke(weeklyUsages: List<DailyUsage>): WeeklyReport {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        val totalMs = weeklyUsages.sumOf { it.totalScreenTimeMs }
        val avgMs = if (weeklyUsages.isEmpty()) 0L else totalMs / weeklyUsages.size

        val allApps = weeklyUsages.flatMap { it.appUsages }
            .groupBy { it.packageName }
            .map { (pkg, usages) ->
                usages.first().copy(usageTimeMs = usages.sumOf { it.usageTimeMs })
            }
            .sortedByDescending { it.usageTimeMs }

        val addictionScore = calculateAddictionScore(weeklyUsages)
        val prevScore = (addictionScore.score * 1.1).toInt() // Simulated prev week
        val improvement = if (prevScore > 0) ((prevScore - addictionScore.score).toFloat() / prevScore * 100) else 0f

        return WeeklyReport(
            weekStart = cal.timeInMillis,
            weekEnd = now,
            totalScreenTimeMs = totalMs,
            avgDailyScreenTimeMs = avgMs,
            mostUsedApp = allApps.firstOrNull(),
            addictionScoreTrend = weeklyUsages.map { (it.totalScreenTimeHours * 10).toInt().coerceIn(0, 100) },
            productivityScore = (100 - addictionScore.score).coerceIn(0, 100),
            improvementPercentage = improvement,
            topApps = allApps.take(5)
        )
    }
}

// ─── Digital Pet Use Case ─────────────────────────────────────────────────────

class GetDigitalPetStateUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(todayUsageMs: Long, dailyGoalMs: Long): DigitalPet {
        val usageRatio = if (dailyGoalMs > 0) todayUsageMs.toFloat() / dailyGoalMs else 0f
        val hours = todayUsageMs / 3_600_000f

        val (state, message) = when {
            hours < 2f -> PetState.HAPPY to "I feel great! You're spending quality time away from the screen 😊"
            hours < 4f -> PetState.NORMAL to "I'm okay. Let's try to keep it under control today 😐"
            hours < 6f -> PetState.TIRED to "I'm getting tired from all this screen time... Please give me a break 😴"
            else -> PetState.EXHAUSTED to "I'm exhausted! Too much screen time today. Let's take a long break! 😵"
        }

        return DigitalPet(state = state, message = message)
    }
}
