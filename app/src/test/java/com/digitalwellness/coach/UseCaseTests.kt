package com.digitalwellness.coach

import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.usecase.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AddictionScoreUseCaseTest {

    private lateinit var useCase: CalculateAddictionScoreUseCase

    @Before
    fun setup() {
        useCase = CalculateAddictionScoreUseCase(
            usageRepository = FakeUsageRepository()
        )
    }

    @Test
    fun `healthy usage returns score under 31`() = runTest {
        val usage = listOf(
            makeDailyUsage(screenTimeHours = 2f, unlocks = 40)
        )
        val result = useCase(usage)
        assertTrue("Score should be healthy (<=30), was ${result.score}", result.score <= 30)
        assertEquals(AddictionLevel.HEALTHY, result.level)
    }

    @Test
    fun `moderate usage returns moderate level`() = runTest {
        val usage = List(7) { makeDailyUsage(screenTimeHours = 4f, unlocks = 80) }
        val result = useCase(usage)
        assertTrue("Score should be moderate (31-55), was ${result.score}", result.score in 31..55)
        assertEquals(AddictionLevel.MODERATE, result.level)
    }

    @Test
    fun `severe usage returns score above 75`() = runTest {
        val usage = List(7) { makeDailyUsage(screenTimeHours = 9f, unlocks = 160) }
        val result = useCase(usage)
        assertTrue("Score should be severe (>75), was ${result.score}", result.score > 75)
        assertEquals(AddictionLevel.SEVERE, result.level)
    }

    @Test
    fun `empty list returns healthy score of zero`() = runTest {
        val result = useCase(emptyList())
        assertEquals(0, result.score)
        assertEquals(AddictionLevel.HEALTHY, result.level)
    }

    @Test
    fun `suggestions are non-empty for moderate and above`() = runTest {
        val usage = List(5) { makeDailyUsage(screenTimeHours = 5f, unlocks = 100) }
        val result = useCase(usage)
        assertTrue("Should have suggestions", result.suggestions.isNotEmpty())
    }
}

class DetectHabitsUseCaseTest {

    private val detectHabits = DetectHabitsUseCase()

    @Test
    fun `excessive social media triggers warning`() {
        val usage = makeDailyUsage(
            screenTimeHours = 5f,
            unlocks = 30,
            appUsages = listOf(
                fakeApp("com.instagram.android", "Instagram", hours = 3.5f)
            )
        )
        val warnings = detectHabits(usage)
        assertTrue(warnings.any { it.type == WarningType.EXCESSIVE_SOCIAL_MEDIA })
    }

    @Test
    fun `long session triggers continuous usage warning`() {
        val usage = makeDailyUsage(
            screenTimeHours = 3f,
            unlocks = 30,
            longestSessionHours = 1.5f
        )
        val warnings = detectHabits(usage)
        assertTrue(warnings.any { it.type == WarningType.CONTINUOUS_USAGE })
    }

    @Test
    fun `excessive unlocks triggers warning`() {
        val usage = makeDailyUsage(screenTimeHours = 2f, unlocks = 120)
        val warnings = detectHabits(usage)
        assertTrue(warnings.any { it.type == WarningType.EXCESSIVE_UNLOCKING })
    }

    @Test
    fun `healthy usage has no warnings`() {
        val usage = makeDailyUsage(screenTimeHours = 1f, unlocks = 20)
        val warnings = detectHabits(usage)
        assertTrue(warnings.isEmpty())
    }
}

class DigitalPetStateUseCaseTest {

    private val useCase = GetDigitalPetStateUseCase(
        preferencesRepository = FakePreferencesRepository()
    )

    @Test
    fun `low usage gives happy pet`() {
        val pet = useCase(1 * 3_600_000L, 4 * 3_600_000L)
        assertEquals(PetState.HAPPY, pet.state)
    }

    @Test
    fun `very high usage gives exhausted pet`() {
        val pet = useCase(7 * 3_600_000L, 4 * 3_600_000L)
        assertEquals(PetState.EXHAUSTED, pet.state)
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun makeDailyUsage(
    screenTimeHours: Float,
    unlocks: Int,
    longestSessionHours: Float = 0.5f,
    appUsages: List<AppUsage> = emptyList()
): DailyUsage {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return DailyUsage(
        date = cal.timeInMillis,
        totalScreenTimeMs = (screenTimeHours * 3_600_000).toLong(),
        unlockCount = unlocks,
        longestSessionMs = (longestSessionHours * 3_600_000).toLong(),
        appUsages = appUsages
    )
}

private fun fakeApp(pkg: String, name: String, hours: Float) = AppUsage(
    packageName = pkg,
    appName = name,
    usageTimeMs = (hours * 3_600_000).toLong(),
    lastUsed = System.currentTimeMillis()
)

// ─── Fake Repos ───────────────────────────────────────────────────────────────

private class FakeUsageRepository : com.digitalwellness.coach.domain.repository.UsageRepository {
    override fun getDailyUsage(date: Long) = kotlinx.coroutines.flow.flowOf(null)
    override fun getWeeklyUsage(startDate: Long, endDate: Long) = kotlinx.coroutines.flow.flowOf(emptyList<DailyUsage>())
    override fun getMonthlyUsage(startDate: Long, endDate: Long) = kotlinx.coroutines.flow.flowOf(emptyList<DailyUsage>())
    override fun getAppUsageForDate(date: Long) = kotlinx.coroutines.flow.flowOf(emptyList<AppUsage>())
    override suspend fun syncTodayUsage() {}
    override suspend fun getTodayScreenTime() = 0L
    override suspend fun getTodayUnlockCount() = 0
    override suspend fun getLongestSession() = 0L
}

private class FakePreferencesRepository : com.digitalwellness.coach.domain.repository.PreferencesRepository {
    override fun isOnboardingCompleted() = kotlinx.coroutines.flow.flowOf(false)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override fun getDailyGoalMs() = kotlinx.coroutines.flow.flowOf(4 * 3_600_000L)
    override suspend fun setDailyGoalMs(goalMs: Long) {}
    override fun isDarkMode() = kotlinx.coroutines.flow.flowOf(false)
    override suspend fun setDarkMode(enabled: Boolean) {}
    override fun isUsagePermissionGranted() = kotlinx.coroutines.flow.flowOf(false)
    override suspend fun setUsagePermissionGranted(granted: Boolean) {}
    override fun getBlockedApps() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
    override suspend fun setBlockedApps(packages: List<String>) {}
    override fun getPetName() = kotlinx.coroutines.flow.flowOf("Buddy")
    override suspend fun setPetName(name: String) {}
}
