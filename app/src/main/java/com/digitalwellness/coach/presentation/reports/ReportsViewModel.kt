package com.digitalwellness.coach.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.WeeklyReport
import com.digitalwellness.coach.domain.usecase.CalculateAddictionScoreUseCase
import com.digitalwellness.coach.domain.usecase.GenerateWeeklyReportUseCase
import com.digitalwellness.coach.domain.usecase.GetWeeklyComparisonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = true,
    val report: WeeklyReport? = null,
    val dailyScreenTimes: List<Long> = emptyList(),      // ms per day, 7 entries
    val dayLabels: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getWeeklyComparison: GetWeeklyComparisonUseCase,
    private val generateReport: GenerateWeeklyReportUseCase,
    private val calculateScore: CalculateAddictionScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            getWeeklyComparison().collect { weeklyUsages ->
                val report = generateReport(weeklyUsages)
                val dayLabels = weeklyUsages.map { usage ->
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = usage.date
                    listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        dailyScreenTimes = weeklyUsages.map { u -> u.totalScreenTimeMs },
                        dayLabels = dayLabels
                    )
                }
            }
        }
    }

    fun refresh() { load() }
}
