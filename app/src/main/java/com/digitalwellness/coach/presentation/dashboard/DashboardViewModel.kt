package com.digitalwellness.coach.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.PreferencesRepository
import com.digitalwellness.coach.domain.usecase.CalculateAddictionScoreUseCase
import com.digitalwellness.coach.domain.usecase.DetectHabitsUseCase
import com.digitalwellness.coach.domain.usecase.GetAIRecommendationsUseCase
import com.digitalwellness.coach.domain.usecase.GetDigitalPetStateUseCase
import com.digitalwellness.coach.domain.usecase.GetTodayStatsUseCase
import com.digitalwellness.coach.domain.usecase.GetWeeklyComparisonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val todayUsage: DailyUsage? = null,
    val goalProgressFraction: Float = 0f,
    val dailyGoalMs: Long = 4 * 3_600_000L,
    val weeklyUsages: List<DailyUsage> = emptyList(),
    val addictionScore: AddictionScore? = null,
    val habitWarnings: List<HabitWarning> = emptyList(),
    val digitalPet: DigitalPet? = null,
    val recommendations: List<String> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTodayStats: GetTodayStatsUseCase,
    private val getWeeklyComparison: GetWeeklyComparisonUseCase,
    private val calculateAddictionScore: CalculateAddictionScoreUseCase,
    private val detectHabits: DetectHabitsUseCase,
    private val getAIRecommendations: GetAIRecommendationsUseCase,
    private val getDigitalPetState: GetDigitalPetStateUseCase,
    private val preferences: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    getTodayStats(),
                    getWeeklyComparison(),
                    preferences.getDailyGoalMs()
                ) { todayUsage, weeklyUsages, goalMs ->
                    Triple(todayUsage, weeklyUsages, goalMs)
                }.collect { (todayUsage, weeklyUsages, goalMs) ->
                    val used = todayUsage?.totalScreenTimeMs ?: 0L
                    val goalProgress = if (goalMs > 0) (used.toFloat() / goalMs).coerceIn(0f, 1f) else 0f
                    val addiction = calculateAddictionScore(weeklyUsages)
                    val warnings = todayUsage?.let { detectHabits(it) } ?: emptyList()
                    val recommendations = getAIRecommendations(weeklyUsages)
                    val pet = getDigitalPetState(used, goalMs)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todayUsage = todayUsage,
                            goalProgressFraction = goalProgress,
                            dailyGoalMs = goalMs,
                            weeklyUsages = weeklyUsages,
                            addictionScore = addiction,
                            habitWarnings = warnings,
                            recommendations = recommendations,
                            digitalPet = pet,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
