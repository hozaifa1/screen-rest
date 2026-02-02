package com.screenrest.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.screenrest.app.MainActivity
import com.screenrest.app.R
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.TrackingMode
import com.screenrest.app.presentation.block.BlockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class UsageTrackingService : LifecycleService() {
    
    companion object {
        private const val TAG = "UsageTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screenrest_tracking"
        private const val POLLING_INTERVAL_MS = 5_000L
        
        @Volatile
        var lastBreakTimestampMs: Long = System.currentTimeMillis()
            private set
        
        @Volatile
        var currentUsageMs: Long = 0L
            private set
        
        @Volatile
        var thresholdMs: Long = 0L
            private set
        
        fun resetTrackingTimestamp() {
            lastBreakTimestampMs = System.currentTimeMillis()
            currentUsageMs = 0L
        }
    }
    
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var permissionChecker: PermissionChecker
    
    private var trackingJob: Job? = null
    private var lastDayCheck: Int = -1
    private var cumulativeUsageToday: Long = 0L
    private var lastScreenOnTimestamp: Long = 0L
    private var isScreenOn: Boolean = true
    
    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lifecycleScope.launch {
                val breakConfig = settingsRepository.breakConfig.first()
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        val currentTime = System.currentTimeMillis()
                        isScreenOn = true
                        lastScreenOnTimestamp = currentTime
                        
                        if (breakConfig.trackingMode == TrackingMode.CONTINUOUS) {
                            lastBreakTimestampMs = currentTime
                            Log.d(TAG, "CONTINUOUS mode: Screen ON at $currentTime, tracking starts")
                        } else {
                            Log.d(TAG, "CUMULATIVE mode: Screen ON, starting new session")
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        if (breakConfig.trackingMode == TrackingMode.CONTINUOUS) {
                            lastBreakTimestampMs = System.currentTimeMillis()
                            Log.d(TAG, "CONTINUOUS mode: Screen OFF, reset timestamp for next screen-on")
                        } else {
                            val sessionTime = System.currentTimeMillis() - lastScreenOnTimestamp
                            cumulativeUsageToday += sessionTime
                            Log.d(TAG, "CUMULATIVE mode: Screen OFF, added ${sessionTime}ms, total=${cumulativeUsageToday}ms")
                        }
                        
                        isScreenOn = false
                    }
                }
            }
        }
    }
    
    private val blockCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.screenrest.app.ACTION_BLOCK_COMPLETE") {
                Log.d(TAG, "Block complete received, resetting tracking")
                BlockAccessibilityService.isBlockActive = false
                resetTrackingTimestamp()
                lifecycleScope.launch {
                    settingsRepository.updateLastBreakTimestamp(System.currentTimeMillis())
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceivers()
        
        isScreenOn = powerManager.isInteractive
        lastScreenOnTimestamp = System.currentTimeMillis()
        
        lifecycleScope.launch {
            lastBreakTimestampMs = settingsRepository.lastBreakTimestamp.first()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val notification = createNotification("Monitoring screen time...")
        startForeground(NOTIFICATION_ID, notification)
        
        startTracking()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
            unregisterReceiver(blockCompleteReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
    }
    
    private fun registerReceivers() {
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)
        
        val blockFilter = IntentFilter("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(blockCompleteReceiver, blockFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(blockCompleteReceiver, blockFilter)
        }
    }
    
    private fun startTracking() {
        lastDayCheck = getCurrentDay()
        
        trackingJob?.cancel()
        trackingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    performTrackingCycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in tracking cycle", e)
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun performTrackingCycle() {
        val breakConfig = settingsRepository.breakConfig.first()
        thresholdMs = breakConfig.usageThresholdMinutes * 60_000L
        
        checkDailyReset(breakConfig)
        
        currentUsageMs = calculateCurrentUsage(breakConfig)
        val currentUsageMinutes = (currentUsageMs / 60_000).toInt()
        val currentUsageSeconds = ((currentUsageMs % 60_000) / 1000).toInt()
        
        val thresholdReached = currentUsageMs >= thresholdMs
        
        Log.d(TAG, "==================== TRACKING CYCLE ====================")
        Log.d(TAG, "Current Usage: ${currentUsageMs}ms (${currentUsageMinutes}m ${currentUsageSeconds}s)")
        Log.d(TAG, "Threshold: ${thresholdMs}ms (${breakConfig.usageThresholdMinutes}m)")
        Log.d(TAG, "Threshold Reached: $thresholdReached")
        Log.d(TAG, "Screen On: $isScreenOn")
        Log.d(TAG, "Block Active: ${BlockAccessibilityService.isBlockActive}")
        Log.d(TAG, "Location Enabled: ${breakConfig.locationEnabled}")
        Log.d(TAG, "Tracking Mode: ${breakConfig.trackingMode}")
        Log.d(TAG, "=======================================================")
        
        if (thresholdReached && !BlockAccessibilityService.isBlockActive) {
            Log.w(TAG, "⚠️ THRESHOLD REACHED! Attempting to trigger block...")
            
            if (breakConfig.locationEnabled) {
                val inLocation = isInTargetLocation(breakConfig)
                Log.d(TAG, "Location check: $inLocation")
                if (inLocation) {
                    Log.w(TAG, "✅ TRIGGERING BLOCK (location OK)")
                    triggerBlock(breakConfig)
                } else {
                    Log.w(TAG, "❌ BLOCK SKIPPED (outside location)")
                    updateNotification("Outside target location - ${formatTime(currentUsageMs)}")
                }
            } else {
                Log.w(TAG, "✅ TRIGGERING BLOCK (no location check)")
                triggerBlock(breakConfig)
            }
        } else {
            if (thresholdReached) {
                Log.d(TAG, "Threshold reached but block already active")
            }
            val remainingMs = thresholdMs - currentUsageMs
            val remainingMinutes = (remainingMs / 60_000).toInt()
            val remainingSeconds = ((remainingMs % 60_000) / 1000).toInt()
            updateNotification("Used: ${formatTime(currentUsageMs)} | Break in ${remainingMinutes}m ${remainingSeconds}s")
        }
    }
    
    private fun calculateCurrentUsage(breakConfig: BreakConfig): Long {
        return when (breakConfig.trackingMode) {
            TrackingMode.CONTINUOUS -> {
                if (isScreenOn) {
                    System.currentTimeMillis() - lastBreakTimestampMs
                } else {
                    0L
                }
            }
            TrackingMode.CUMULATIVE_DAILY -> {
                cumulativeUsageToday + (if (isScreenOn) System.currentTimeMillis() - lastScreenOnTimestamp else 0L)
            }
        }
    }
    
    private fun checkDailyReset(breakConfig: BreakConfig) {
        val currentDay = getCurrentDay()
        if (currentDay != lastDayCheck && breakConfig.trackingMode == TrackingMode.CUMULATIVE_DAILY) {
            cumulativeUsageToday = 0L
            lastDayCheck = currentDay
        }
    }
    
    private fun getCurrentDay(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.DAY_OF_YEAR)
    }
    
    private suspend fun isInTargetLocation(breakConfig: BreakConfig): Boolean {
        if (!permissionChecker.checkLocationPermission()) return false
        if (breakConfig.locationLat == null || breakConfig.locationLng == null) return false
        
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val targetLocation = Location("").apply {
                    latitude = breakConfig.locationLat
                    longitude = breakConfig.locationLng
                }
                val distance = location.distanceTo(targetLocation)
                distance <= breakConfig.locationRadiusMeters
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            true
        }
    }
    
    private fun triggerBlock(breakConfig: BreakConfig) {
        Log.w(TAG, "========== TRIGGERING BLOCK SCREEN ==========")
        Log.w(TAG, "Block Duration: ${breakConfig.blockDurationSeconds} seconds")
        
        try {
            BlockAccessibilityService.isBlockActive = true
            Log.d(TAG, "Set isBlockActive = true")
            
            val intent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("BLOCK_DURATION_SECONDS", breakConfig.blockDurationSeconds)
            }
            
            Log.w(TAG, "Starting BlockActivity with intent: $intent")
            startActivity(intent)
            Log.w(TAG, "✅ BlockActivity started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO START BLOCK ACTIVITY", e)
            BlockAccessibilityService.isBlockActive = false
        }
    }
    
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    private fun createNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenRest Active")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your screen time usage"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
