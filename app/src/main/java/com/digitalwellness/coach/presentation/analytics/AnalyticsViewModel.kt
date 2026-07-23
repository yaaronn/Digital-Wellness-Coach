package com.digitalwellness.coach.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.AppUsage
import com.digitalwellness.coach.domain.model.DailyUsage
import com.digitalwellness.coach.domain.usecase.GetAppUsageListUseCase
import com.digitalwellness.coach.domain.usecase.GetMonthlyTrendUseCase
import com.digitalwellness.coach.domain.usecase.GetWeeklyComparisonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class AnalyticsPeriod { DAILY, WEEKLY, MONTHLY }

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.DAILY,
    val appUsages: List<AppUsage> = emptyList(),
    val filteredApps: List<AppUsage> = emptyList(),
    val searchQuery: String = "",
    val weeklyUsages: List<DailyUsage> = emptyList(),
    val monthlyUsages: List<DailyUsage> = emptyList(),
    val totalTimeMs: Long = 0L,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAppUsageList: GetAppUsageListUseCase,
    private val getWeeklyComparison: GetWeeklyComparisonUseCase,
    private val getMonthlyTrend: GetMonthlyTrendUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData(AnalyticsPeriod.DAILY)
    }

    fun selectPeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadData(period)
    }

    fun search(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isEmpty()) state.appUsages
            else state.appUsages.filter { it.appName.contains(query, ignoreCase = true) }
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    private fun loadData(period: AnalyticsPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val today = cal.timeInMillis

            when (period) {
                AnalyticsPeriod.DAILY -> {
                    getAppUsageList(today).collect { apps ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                appUsages = apps,
                                filteredApps = apps,
                                totalTimeMs = apps.sumOf { a -> a.usageTimeMs }
                            )
                        }
                    }
                }
                AnalyticsPeriod.WEEKLY -> {
                    getWeeklyComparison().collect { weekly ->
                        val allApps = weekly.flatMap { it.appUsages }
                            .groupBy { it.packageName }
                            .map { (_, usages) -> usages.first().copy(usageTimeMs = usages.sumOf { it.usageTimeMs }) }
                            .sortedByDescending { it.usageTimeMs }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                appUsages = allApps,
                                filteredApps = allApps,
                                weeklyUsages = weekly,
                                totalTimeMs = allApps.sumOf { a -> a.usageTimeMs }
                            )
                        }
                    }
                }
                AnalyticsPeriod.MONTHLY -> {
                    getMonthlyTrend().collect { monthly ->
                        val allApps = monthly.flatMap { it.appUsages }
                            .groupBy { it.packageName }
                            .map { (_, usages) -> usages.first().copy(usageTimeMs = usages.sumOf { it.usageTimeMs }) }
                            .sortedByDescending { it.usageTimeMs }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                appUsages = allApps,
                                filteredApps = allApps,
                                monthlyUsages = monthly,
                                totalTimeMs = allApps.sumOf { a -> a.usageTimeMs }
                            )
                        }
                    }
                }
            }
        }
    }
}
