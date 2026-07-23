package com.digitalwellness.coach.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.*
import com.digitalwellness.coach.domain.repository.UsageRepository
import com.digitalwellness.coach.domain.usecase.ManageGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class GoalWithProgress(
    val goal: Goal,
    val usedMs: Long,
    val progressFraction: Float
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goalsWithProgress: List<GoalWithProgress> = emptyList(),
    val showAddDialog: Boolean = false,
    val dialogType: GoalType = GoalType.DAILY_TOTAL,
    val error: String? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val manageGoals: ManageGoalsUseCase,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val today = cal.timeInMillis

            combine(
                manageGoals.getActiveGoals(),
                usageRepository.getAppUsageForDate(today)
            ) { goals, appUsages ->
                val totalUsedMs = appUsages.sumOf { it.usageTimeMs }

                goals.map { goal ->
                    val usedMs = when (goal.type) {
                        GoalType.DAILY_TOTAL -> totalUsedMs
                        GoalType.PER_APP -> appUsages
                            .find { it.packageName == goal.packageName }
                            ?.usageTimeMs ?: 0L
                    }
                    GoalWithProgress(
                        goal = goal,
                        usedMs = usedMs,
                        progressFraction = if (goal.targetTimeMs > 0)
                            (usedMs.toFloat() / goal.targetTimeMs).coerceIn(0f, 1f)
                        else 0f
                    )
                }
            }.collect { goalsWithProgress ->
                _uiState.update {
                    it.copy(isLoading = false, goalsWithProgress = goalsWithProgress)
                }
            }
        }
    }

    fun showAddDialog(type: GoalType) {
        _uiState.update { it.copy(showAddDialog = true, dialogType = type) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun createDailyGoal(hours: Float) {
        viewModelScope.launch {
            manageGoals.createDailyGoal(hours)
            dismissDialog()
        }
    }

    fun createAppGoal(packageName: String, appName: String, minutes: Int) {
        viewModelScope.launch {
            manageGoals.createAppGoal(packageName, appName, minutes)
            dismissDialog()
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            manageGoals.deleteGoal(goalId)
        }
    }
}
