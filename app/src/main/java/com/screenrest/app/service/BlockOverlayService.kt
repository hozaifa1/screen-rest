package com.screenrest.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.domain.usecase.GetRandomDisplayMessageUseCase
import com.screenrest.app.presentation.theme.ScreenRestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BlockOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BlockOverlayService"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        
        @Volatile
        var isOverlayActive = false
            private set
    }

    @Inject
    lateinit var getRandomDisplayMessageUseCase: GetRandomDisplayMessageUseCase

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    private var countdownJob: Job? = null
    private var remainingSeconds by mutableIntStateOf(30)
    private var displayMessage by mutableStateOf("Take a break")
    private var displayMessageArabic by mutableStateOf("")
    
    private var isInitialized = false
    private var isCountdownRunning = false
    private var isScreenOn = true
    
    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    Log.d(TAG, "Screen ON during block - countdown will resume")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    Log.d(TAG, "Screen OFF during block - countdown paused")
                }
            }
        }
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        isScreenOn = powerManager.isInteractive
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)
        
        Log.d(TAG, "BlockOverlayService created, screenOn=$isScreenOn")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isInitialized) {
            Log.w(TAG, "Service already initialized, ignoring duplicate start")
            return START_NOT_STICKY
        }
        
        val duration = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 30) ?: 30
        Log.d(TAG, "Starting overlay with duration=$duration")
        
        isInitialized = true
        isOverlayActive = true
        remainingSeconds = duration
        
        // Load message BEFORE showing overlay to prevent rotation
        lifecycleScope.launch {
            try {
                val message = getRandomDisplayMessageUseCase()
                when (message) {
                    is DisplayMessage.Custom -> {
                        displayMessage = message.text
                        displayMessageArabic = ""
                    }
                    is DisplayMessage.QuranAyah -> {
                        displayMessage = message.ayah.englishTranslation
                        displayMessageArabic = message.ayah.arabicText
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading message", e)
                displayMessage = "Take a moment to rest your eyes and reflect."
                displayMessageArabic = ""
            }
            
            // Now show overlay with loaded message
            if (overlayView == null) {
                showOverlay()
            }
            
            // Start countdown after brief delay
            delay(200)
            startCountdown(duration)
        }
        
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
                            displayMessage = displayMessage,
                            displayMessageArabic = displayMessageArabic
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv() and 0 or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
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
        if (isCountdownRunning) {
            Log.w(TAG, "Countdown already running, ignoring duplicate start")
            return
        }
        
        isCountdownRunning = true
        countdownJob?.cancel()
        
        countdownJob = lifecycleScope.launch {
            remainingSeconds = durationSeconds
            Log.d(TAG, "Countdown started: $durationSeconds seconds")
            
            while (remainingSeconds > 0 && isCountdownRunning) {
                delay(1000)
                if (isScreenOn) {
                    remainingSeconds--
                } else {
                    Log.d(TAG, "Countdown paused (screen off), remaining=$remainingSeconds")
                }
            }
            
            if (isCountdownRunning) {
                Log.d(TAG, "Countdown finished, dismissing overlay")
                finishBlock()
            }
        }
    }

    private fun finishBlock() {
        Log.d(TAG, "Block finished, removing overlay")
        isCountdownRunning = false
        isOverlayActive = false
        
        val intent = Intent("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        sendBroadcast(intent)
        
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service being destroyed")
        
        isCountdownRunning = false
        countdownJob?.cancel()
        countdownJob = null
        
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen receiver", e)
        }
        
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
        isInitialized = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
private fun BlockOverlayContent(
    remainingSeconds: Int,
    displayMessage: String,
    displayMessageArabic: String
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
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Timer countdown
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Arabic text if available (Quran verse)
            if (displayMessageArabic.isNotEmpty()) {
                Text(
                    text = displayMessageArabic,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Translation or custom message
            Text(
                text = displayMessage,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
        }
    }
}
