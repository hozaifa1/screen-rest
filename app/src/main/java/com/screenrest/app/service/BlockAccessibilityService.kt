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
            // Only relaunch if overlay is NOT already active
            if (BlockOverlayService.isOverlayActive) {
                Log.d(TAG, "Overlay already active, no need to relaunch")
                return
            }
            
            Log.w(TAG, "Overlay not active but should be - user switched apps during block")
            // Don't relaunch - let the overlay handle it
            // Relaunching causes duplicate service calls and message rotation
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in relaunchBlockScreens", e)
        }
    }
}
