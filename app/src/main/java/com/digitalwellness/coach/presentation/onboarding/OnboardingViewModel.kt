package com.digitalwellness.coach.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwellness.coach.data.datasource.UsageStatsDataSource
import com.digitalwellness.coach.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val usageDataSource: UsageStatsDataSource
) : ViewModel() {

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission

    init {
        viewModelScope.launch {
            _isOnboardingCompleted.value = preferences.isOnboardingCompleted().first()
            _hasUsagePermission.value = usageDataSource.hasUsagePermission()
        }
    }

    fun checkPermissions() {
        _hasUsagePermission.value = usageDataSource.hasUsagePermission()
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            preferences.setUsagePermissionGranted(_hasUsagePermission.value)
            _isOnboardingCompleted.value = true
        }
    }

    fun openUsageSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
