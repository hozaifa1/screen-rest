package com.screenrest.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.domain.model.TrackingMode
import com.screenrest.app.domain.usecase.UpdateBreakConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateBreakConfigUseCase: UpdateBreakConfigUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.breakConfig,
                settingsRepository.themeMode
            ) { config, theme ->
                config to theme
            }.collect { (config, theme) ->
                _uiState.value = _uiState.value.copy(
                    breakConfig = config,
                    themeMode = theme
                )
            }
        }
    }
    
    fun updateThreshold(minutes: Int) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(usageThresholdMinutes = minutes)
            updateBreakConfigUseCase(config)
        }
    }
    
    fun updateDuration(seconds: Int) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(blockDurationSeconds = seconds)
            
            if (seconds > 120) {
                _uiState.value = _uiState.value.copy(showLongDurationWarning = true)
            }
            
            updateBreakConfigUseCase(config)
        }
    }
    
    fun updateTrackingMode(mode: TrackingMode) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(trackingMode = mode)
            updateBreakConfigUseCase(config)
        }
    }
    
    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(theme)
        }
    }
    
    fun updateQuranMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(quranMessagesEnabled = enabled)
            updateBreakConfigUseCase(config)
        }
    }
    
    fun dismissLongDurationWarning() {
        _uiState.value = _uiState.value.copy(showLongDurationWarning = false)
    }
}

data class SettingsUiState(
    val breakConfig: BreakConfig = BreakConfig(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showLongDurationWarning: Boolean = false
)
