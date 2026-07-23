package com.digitalwellness.coach.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.digitalwellness.coach.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wellness_prefs")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DAILY_GOAL_MS = longPreferencesKey("daily_goal_ms")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val USAGE_PERMISSION_GRANTED = booleanPreferencesKey("usage_permission_granted")
        val BLOCKED_APPS = stringPreferencesKey("blocked_apps")
        val PET_NAME = stringPreferencesKey("pet_name")
    }

    override fun isOnboardingCompleted(): Flow<Boolean> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override fun getDailyGoalMs(): Flow<Long> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map { it[Keys.DAILY_GOAL_MS] ?: 4 * 3_600_000L } // Default: 4 hours

    override suspend fun setDailyGoalMs(goalMs: Long) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_MS] = goalMs }
    }

    override fun isDarkMode(): Flow<Boolean> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map { it[Keys.DARK_MODE] ?: false }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    override fun isUsagePermissionGranted(): Flow<Boolean> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map { it[Keys.USAGE_PERMISSION_GRANTED] ?: false }

    override suspend fun setUsagePermissionGranted(granted: Boolean) {
        context.dataStore.edit { it[Keys.USAGE_PERMISSION_GRANTED] = granted }
    }

    override fun getBlockedApps(): Flow<List<String>> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map {
                val raw = it[Keys.BLOCKED_APPS] ?: ""
                if (raw.isEmpty()) emptyList() else raw.split(",")
            }

    override suspend fun setBlockedApps(packages: List<String>) {
        context.dataStore.edit { it[Keys.BLOCKED_APPS] = packages.joinToString(",") }
    }

    override fun getPetName(): Flow<String> =
        context.dataStore.data.catch { emit(emptyPreferences()) }
            .map { it[Keys.PET_NAME] ?: "Buddy" }

    override suspend fun setPetName(name: String) {
        context.dataStore.edit { it[Keys.PET_NAME] = name }
    }
}
