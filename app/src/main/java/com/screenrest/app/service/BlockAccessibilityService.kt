package com.screenrest.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.screenrest.app.presentation.block.BlockActivity

class BlockAccessibilityService : AccessibilityService() {
    
    companion object {
        @Volatile
        var isBlockActive = false
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (!isBlockActive) return
            
            event?.let {
                if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val packageName = it.packageName?.toString() ?: return
                    
                    if (packageName != "com.screenrest.app") {
                        relaunchBlockActivity()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BlockAccessibility", "Error in onAccessibilityEvent", e)
        }
    }
    
    override fun onInterrupt() {
        // Handle interruption if needed
    }
    
    private fun relaunchBlockActivity() {
        try {
            val intent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("BlockAccessibility", "Error relaunching BlockActivity", e)
        }
    }
}
