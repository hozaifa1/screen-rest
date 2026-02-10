package com.screenrest.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val USAGE_THRESHOLD_SECONDS = intPreferencesKey("usage_threshold_seconds")
        val BLOCK_DURATION_SECONDS = intPreferencesKey("block_duration_seconds")
        val TRACKING_MODE = stringPreferencesKey("tracking_mode")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LNG = doublePreferencesKey("location_lng")
        val LOCATION_RADIUS_METERS = floatPreferencesKey("location_radius_meters")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val USAGE_TRACKING_ENABLED = booleanPreferencesKey("usage_tracking_enabled")
        val QURAN_MESSAGES_ENABLED = booleanPreferencesKey("quran_messages_enabled")
        val LAST_BREAK_TIMESTAMP = longPreferencesKey("last_break_timestamp")
    }

    val breakConfig: Flow<BreakConfig> = context.dataStore.data.map { preferences ->
        BreakConfig(
            usageThresholdSeconds = preferences[PreferencesKeys.USAGE_THRESHOLD_SECONDS] ?: 300,
            blockDurationSeconds = preferences[PreferencesKeys.BLOCK_DURATION_SECONDS] ?: 30,
            locationEnabled = preferences[PreferencesKeys.LOCATION_ENABLED] ?: false,
            locationLat = preferences[PreferencesKeys.LOCATION_LAT],
            locationLng = preferences[PreferencesKeys.LOCATION_LNG],
            locationRadiusMeters = preferences[PreferencesKeys.LOCATION_RADIUS_METERS] ?: 100f,
            quranMessagesEnabled = preferences[PreferencesKeys.QURAN_MESSAGES_ENABLED] ?: true
        )
    }

    val lastBreakTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_BREAK_TIMESTAMP] ?: System.currentTimeMillis()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val usageTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USAGE_TRACKING_ENABLED] ?: false
    }

    suspend fun updateBreakConfig(config: BreakConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_THRESHOLD_SECONDS] = config.usageThresholdSeconds
            preferences[PreferencesKeys.BLOCK_DURATION_SECONDS] = config.blockDurationSeconds
            preferences[PreferencesKeys.LOCATION_ENABLED] = config.locationEnabled
            config.locationLat?.let { preferences[PreferencesKeys.LOCATION_LAT] = it }
            config.locationLng?.let { preferences[PreferencesKeys.LOCATION_LNG] = it }
            preferences[PreferencesKeys.LOCATION_RADIUS_METERS] = config.locationRadiusMeters
            preferences[PreferencesKeys.QURAN_MESSAGES_ENABLED] = config.quranMessagesEnabled
        }
    }

    suspend fun updateLastBreakTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BREAK_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateThemeMode(theme: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setUsageTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_TRACKING_ENABLED] = enabled
        }
    }
}
