package com.screenrest.app.presentation.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus
import com.screenrest.app.domain.usecase.CheckPermissionsUseCase
import com.screenrest.app.service.ServiceController
import com.screenrest.app.service.UsageTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val checkPermissionsUseCase: CheckPermissionsUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    init {
        observeState()
        refreshStatus()
        startTimer()
    }
    
    private fun observeState() {
        viewModelScope.launch {
            combine(
                settingsRepository.breakConfig,
                settingsRepository.usageTrackingEnabled
            ) { config, enabled ->
                config to enabled
            }.collect { (config, enabled) ->
                _uiState.value = _uiState.value.copy(
                    breakConfig = config,
                    isServiceEnabled = enabled
                )
            }
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            Log.d(TAG, "Timer started")
            while (true) {
                try {
                    updateTimerDisplay()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating timer", e)
                }
                delay(1000L)
            }
        }
    }
    
    private fun updateTimerDisplay() {
        val config = _uiState.value.breakConfig
        val thresholdMs = config.usageThresholdSeconds * 1000L
        val usageMs = UsageTrackingService.currentUsageMs
        val remainingMs = (thresholdMs - usageMs).coerceAtLeast(0L)
        
        _uiState.value = _uiState.value.copy(
            usedTimeMinutes = (usageMs / 60000).toInt(),
            usedTimeSeconds = ((usageMs % 60000) / 1000).toInt(),
            remainingTimeMinutes = (remainingMs / 60000).toInt(),
            remainingTimeSeconds = ((remainingMs % 60000) / 1000).toInt()
        )
    }
    
    fun refreshStatus() {
        try {
            val permissions = checkPermissionsUseCase()
            val enforcementLevel = checkPermissionsUseCase.calculateEnforcementLevel(permissions)
            val isRunning = ServiceController.isRunning(context)
            
            _uiState.value = _uiState.value.copy(
                permissionStatus = permissions,
                enforcementLevel = enforcementLevel,
                isServiceRunning = isRunning
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing status", e)
        }
    }
    
    fun toggleService() {
        viewModelScope.launch {
            val newState = !_uiState.value.isServiceEnabled
            settingsRepository.setUsageTrackingEnabled(newState)
            
            if (newState) {
                ServiceController.startTracking(context)
            } else {
                ServiceController.stopTracking(context)
            }
            
            refreshStatus()
        }
    }
}

data class HomeUiState(
    val breakConfig: BreakConfig = BreakConfig(),
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val enforcementLevel: EnforcementLevel = EnforcementLevel.NONE,
    val isServiceEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val usedTimeMinutes: Int = 0,
    val usedTimeSeconds: Int = 0,
    val remainingTimeMinutes: Int = 0,
    val remainingTimeSeconds: Int = 0
)
