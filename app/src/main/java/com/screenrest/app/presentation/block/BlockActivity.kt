package com.screenrest.app.presentation.block

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenrest.app.service.BlockAccessibilityService
import com.screenrest.app.presentation.theme.ScreenRestTheme

class BlockActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "BlockActivity"
    }
    
    private var countDownTimer: CountDownTimer? = null
    private var remainingSecondsState = mutableIntStateOf(30)
    private var isBlockComplete = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BlockActivity onCreate started")
        
        try {
            setupWindowFlags()
            setupImmersiveMode()
            
            val duration = intent.getIntExtra("BLOCK_DURATION_SECONDS", 30)
            remainingSecondsState.intValue = duration
            Log.d(TAG, "Block duration: $duration seconds")
            
            startCountdown(duration)
            
            setContent {
                ScreenRestTheme {
                    SimpleBlockScreen(
                        remainingSeconds = remainingSecondsState.intValue
                    )
                }
            }
            Log.d(TAG, "BlockActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in BlockActivity onCreate", e)
            finishBlock()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called - activity reused")
        setIntent(intent)
        
        if (!isBlockComplete) {
            val duration = intent?.getIntExtra("BLOCK_DURATION_SECONDS", 30) ?: 30
            remainingSecondsState.intValue = duration
            startCountdown(duration)
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        setupImmersiveMode()
    }
    
    private fun startCountdown(durationSeconds: Int) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSecondsState.intValue = (millisUntilFinished / 1000).toInt()
            }
            
            override fun onFinish() {
                remainingSecondsState.intValue = 0
                finishBlock()
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        countDownTimer?.cancel()
        if (!isBlockComplete) {
            Log.w(TAG, "Activity destroyed before block completion")
        }
    }
    
    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }
    
    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or 
                    android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    override fun onBackPressed() {
        Log.d(TAG, "Back button pressed - ignored")
        // Do nothing - prevent back button from closing the block screen
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Block home, back, and recent apps buttons
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_APP_SWITCH -> {
                Log.d(TAG, "Key blocked: $keyCode")
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    private fun finishBlock() {
        Log.d(TAG, "finishBlock called")
        isBlockComplete = true
        BlockAccessibilityService.isBlockActive = false
        
        val intent = Intent("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        sendBroadcast(intent)
        
        finish()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }
}

@Composable
private fun SimpleBlockScreen(remainingSeconds: Int) {
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
                text = "Take a break",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
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
