package com.digitalwellness.coach.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.Achievement
import com.digitalwellness.coach.domain.repository.AchievementRepository
import com.digitalwellness.coach.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val petName: String = "Buddy",
    val isDarkMode: Boolean = false,
    val dailyGoalHours: Float = 4f,
    val achievements: List<Achievement> = emptyList(),
    val currentStreak: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.getPetName(),
                preferences.isDarkMode(),
                preferences.getDailyGoalMs(),
                achievementRepository.getAllAchievements()
            ) { petName, darkMode, goalMs, achievements ->
                ProfileUiState(
                    petName = petName,
                    isDarkMode = darkMode,
                    dailyGoalHours = goalMs / 3_600_000f,
                    achievements = achievements,
                    currentStreak = 0,
                    isLoading = false
                )
            }.collect { state -> _uiState.value = state }
        }
        viewModelScope.launch {
            val streak = achievementRepository.getCurrentStreak()
            _uiState.update { it.copy(currentStreak = streak) }
        }
    }

    fun updatePetName(name: String) {
        viewModelScope.launch { preferences.setPetName(name) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun updateDailyGoal(hours: Float) {
        viewModelScope.launch { preferences.setDailyGoalMs((hours * 3_600_000).toLong()) }
    }
}
