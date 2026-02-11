package com.screenrest.app.data.repository

import com.screenrest.app.data.local.datastore.SettingsDataStore
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val breakConfig: Flow<BreakConfig>
    val themeMode: Flow<ThemeMode>
    val themeColor: Flow<ThemeColor>
    val onboardingCompleted: Flow<Boolean>
    val usageTrackingEnabled: Flow<Boolean>
    val lastBreakTimestamp: Flow<Long>
    
    suspend fun updateBreakConfig(config: BreakConfig)
    suspend fun updateThemeMode(theme: ThemeMode)
    suspend fun updateThemeColor(color: ThemeColor)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setUsageTrackingEnabled(enabled: Boolean)
    suspend fun updateLastBreakTimestamp(timestamp: Long)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    override val breakConfig: Flow<BreakConfig> = settingsDataStore.breakConfig
    override val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode
    override val themeColor: Flow<ThemeColor> = settingsDataStore.themeColor
    override val onboardingCompleted: Flow<Boolean> = settingsDataStore.onboardingCompleted
    override val usageTrackingEnabled: Flow<Boolean> = settingsDataStore.usageTrackingEnabled
    override val lastBreakTimestamp: Flow<Long> = settingsDataStore.lastBreakTimestamp
    
    override suspend fun updateBreakConfig(config: BreakConfig) {
        settingsDataStore.updateBreakConfig(config)
    }
    
    override suspend fun updateThemeMode(theme: ThemeMode) {
        settingsDataStore.updateThemeMode(theme)
    }
    
    override suspend fun updateThemeColor(color: ThemeColor) {
        settingsDataStore.updateThemeColor(color)
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        settingsDataStore.setOnboardingCompleted(completed)
    }
    
    override suspend fun setUsageTrackingEnabled(enabled: Boolean) {
        settingsDataStore.setUsageTrackingEnabled(enabled)
    }
    
    override suspend fun updateLastBreakTimestamp(timestamp: Long) {
        settingsDataStore.updateLastBreakTimestamp(timestamp)
    }
}
