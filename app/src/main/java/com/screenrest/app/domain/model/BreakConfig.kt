package com.screenrest.app.domain.model

data class BreakConfig(
    val usageThresholdSeconds: Int = 300,
    val blockDurationSeconds: Int = 30,
    val trackingMode: TrackingMode = TrackingMode.CONTINUOUS,
    val locationEnabled: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationRadiusMeters: Float = 100f,
    val quranMessagesEnabled: Boolean = true
)
