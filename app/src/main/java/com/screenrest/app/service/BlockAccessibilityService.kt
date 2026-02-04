package com.screenrest.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.screenrest.app.presentation.block.BlockActivity

class BlockAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "BlockAccessibility"
        
        @Volatile
        var isBlockActive = false
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (!isBlockActive) return
            
            event?.let {
                if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val packageName = it.packageName?.toString() ?: return
                    
                    // User trying to switch away from our app during block
                    if (packageName != "com.screenrest.app") {
                        Log.w(TAG, "User tried to switch to: $packageName - relaunching block")
                        relaunchBlockScreens()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    private fun relaunchBlockScreens() {
        try {
            // Relaunch overlay service if not active
            if (!BlockOverlayService.isOverlayActive) {
                Log.d(TAG, "Overlay not active, restarting overlay service")
                val overlayIntent = Intent(this, BlockOverlayService::class.java).apply {
                    putExtra(BlockOverlayService.EXTRA_DURATION_SECONDS, 30)
                    putExtra(BlockOverlayService.EXTRA_MESSAGE, "Take a break")
                }
                startService(overlayIntent)
            }
            
            // Also bring activity to front
            val activityIntent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(activityIntent)
            Log.d(TAG, "Block screens relaunched")
        } catch (e: Exception) {
            Log.e(TAG, "Error relaunching block screens", e)
        }
    }
}
