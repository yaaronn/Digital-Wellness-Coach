package com.digitalwellness.coach.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.domain.model.FocusMode
import com.digitalwellness.coach.domain.model.FocusSession
import com.digitalwellness.coach.domain.repository.PreferencesRepository
import com.digitalwellness.coach.domain.usecase.FocusSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val activeSession: FocusSession? = null,
    val selectedMode: FocusMode = FocusMode.WORK,
    val selectedDurationMinutes: Int = 25,
    val elapsedMs: Long = 0L,
    val completedSessions: Int = 0,
    val blockedApps: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusUseCase: FocusSessionUseCase,
    private val preferences: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                focusUseCase.getActiveSession(),
                preferences.getBlockedApps()
            ) { session, blockedApps ->
                session to blockedApps
            }.collect { (session, blockedApps) ->
                _uiState.update {
                    it.copy(
                        activeSession = session,
                        blockedApps = blockedApps,
                        isLoading = false
                    )
                }
                if (session != null) startTimer(session.startTime)
                else stopTimer()
            }
        }

        viewModelScope.launch {
            val count = focusUseCase.getCompletedCount()
            _uiState.update { it.copy(completedSessions = count) }
        }
    }

    fun selectMode(mode: FocusMode) = _uiState.update { it.copy(selectedMode = mode) }

    fun selectDuration(minutes: Int) = _uiState.update { it.copy(selectedDurationMinutes = minutes) }

    fun startSession() {
        viewModelScope.launch {
            focusUseCase.startFocus(
                mode = _uiState.value.selectedMode,
                durationMinutes = _uiState.value.selectedDurationMinutes,
                blockedApps = _uiState.value.blockedApps
            )
        }
    }

    fun endSession() {
        viewModelScope.launch {
            _uiState.value.activeSession?.id?.let { id ->
                focusUseCase.endFocus(id)
                stopTimer()
                val count = focusUseCase.getCompletedCount()
                _uiState.update { it.copy(completedSessions = count, elapsedMs = 0L) }
            }
        }
    }

    private fun startTimer(sessionStartMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(elapsedMs = System.currentTimeMillis() - sessionStartMs) }
                delay(1000L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
