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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
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
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.domain.usecase.GetRandomDisplayMessageUseCase
import com.screenrest.app.presentation.theme.ScreenRestTheme
import com.screenrest.app.presentation.theme.getThemeColorPalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
    
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    private var countdownJob: Job? = null
    private var remainingSeconds by mutableIntStateOf(30)
    private var currentDisplayMessage by mutableStateOf<DisplayMessage?>(null)
    private var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var currentThemeColor by mutableStateOf(ThemeColor.TEAL)
    
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
        
        // Load message and theme BEFORE showing overlay to prevent rotation
        lifecycleScope.launch {
            try {
                currentThemeMode = settingsRepository.themeMode.first()
                currentThemeColor = settingsRepository.themeColor.first()
                currentDisplayMessage = getRandomDisplayMessageUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading message", e)
                currentDisplayMessage = DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
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
                    ScreenRestTheme(themeMode = currentThemeMode, themeColor = currentThemeColor) {
                        BlockOverlayContent(
                            remainingSeconds = remainingSeconds,
                            displayMessage = currentDisplayMessage,
                            isDarkTheme = when (currentThemeMode) {
                                ThemeMode.DARK -> true
                                ThemeMode.LIGHT -> false
                                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                            },
                            themeColor = currentThemeColor
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
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
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
    displayMessage: DisplayMessage?,
    isDarkTheme: Boolean,
    themeColor: ThemeColor = ThemeColor.TEAL
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    // Theme-aware colors from palette
    val palette = getThemeColorPalette(themeColor)
    val gradientTop = if (isDarkTheme) palette.gradientTopDark else palette.gradientTopLight
    val gradientBottom = if (isDarkTheme) palette.gradientBottomDark else palette.gradientBottomLight
    val timerColor = if (isDarkTheme) palette.primaryLight else palette.primaryVariant
    val messageColor = if (isDarkTheme) palette.textOnDark else palette.textPrimary
    val referenceColor = if (isDarkTheme) palette.secondary else palette.primary
    val dividerColor = if (isDarkTheme) palette.dividerDark else palette.dividerLight
    val subtitleColor = if (isDarkTheme) palette.textMuted else palette.textSecondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            // Timer countdown - small and subtle at top
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = timerColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = timerColor,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Take a break",
                fontSize = 14.sp,
                color = subtitleColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                color = dividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Message content - large and prominent
            when (displayMessage) {
                is DisplayMessage.QuranAyah -> {
                    Text(
                        text = displayMessage.ayah.englishTranslation,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "\u2014 Surah ${displayMessage.ayah.surahName} (${displayMessage.ayah.surahNumber}:${displayMessage.ayah.ayahNumber})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = referenceColor
                    )
                }
                is DisplayMessage.IslamicReminder -> {
                    Text(
                        text = displayMessage.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                is DisplayMessage.Custom -> {
                    Text(
                        text = displayMessage.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                null -> {
                    Text(
                        text = "Take a moment to rest your eyes and reflect.",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                }
            }
        }
    }
}
