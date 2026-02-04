package com.screenrest.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screenrest.app.presentation.theme.ScreenRestTheme
import kotlinx.coroutines.*

class BlockOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BlockOverlayService"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_MESSAGE = "message"
        
        @Volatile
        var isOverlayActive = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    private var countdownJob: Job? = null
    private var remainingSeconds by mutableIntStateOf(30)
    private var displayMessage by mutableStateOf("Take a break")

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "BlockOverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 30) ?: 30
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Take a break"
        
        Log.d(TAG, "Starting overlay with duration=$duration, message=$message")
        
        remainingSeconds = duration
        displayMessage = message
        
        if (overlayView == null) {
            showOverlay()
        }
        
        startCountdown(duration)
        
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        try {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Cannot draw overlays - permission not granted")
                stopSelf()
                return
            }

            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@BlockOverlayService)
                setViewTreeViewModelStoreOwner(this@BlockOverlayService)
                setViewTreeSavedStateRegistryOwner(this@BlockOverlayService)
                
                setContent {
                    ScreenRestTheme {
                        BlockOverlayContent(
                            remainingSeconds = remainingSeconds,
                            displayMessage = displayMessage
                        )
                    }
                }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            windowManager?.addView(overlayView, params)
            isOverlayActive = true
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            
            Log.d(TAG, "Overlay view added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            stopSelf()
        }
    }

    private fun startCountdown(durationSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            remainingSeconds = durationSeconds
            
            repeat(durationSeconds) {
                delay(1000)
                remainingSeconds--
                
                if (remainingSeconds <= 0) {
                    finishBlock()
                }
            }
        }
    }

    private fun finishBlock() {
        Log.d(TAG, "Block finished, removing overlay")
        isOverlayActive = false
        BlockAccessibilityService.isBlockActive = false
        
        val intent = Intent("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        sendBroadcast(intent)
        
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
        
        try {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
        
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        isOverlayActive = false
        Log.d(TAG, "BlockOverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
private fun BlockOverlayContent(
    remainingSeconds: Int,
    displayMessage: String
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = displayMessage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Rest your eyes and relax",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
