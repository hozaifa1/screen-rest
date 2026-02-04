package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import javax.inject.Inject

class UpdateBreakConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(config: BreakConfig): Result<Unit> {
        return try {
            if (config.usageThresholdSeconds < 1 || config.usageThresholdSeconds > 86400) {
                return Result.failure(IllegalArgumentException("Usage threshold must be between 1 second and 24 hours"))
            }
            
            if (config.blockDurationSeconds < 1 || config.blockDurationSeconds > 7200) {
                return Result.failure(IllegalArgumentException("Block duration must be between 1 second and 2 hours"))
            }
            
            if (config.locationEnabled) {
                if (config.locationLat == null || config.locationLng == null) {
                    return Result.failure(IllegalArgumentException("Location coordinates required when location is enabled"))
                }
                if (config.locationRadiusMeters < 50f || config.locationRadiusMeters > 1000f) {
                    return Result.failure(IllegalArgumentException("Location radius must be between 50 and 1000 meters"))
                }
            }
            
            settingsRepository.updateBreakConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
