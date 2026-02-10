package com.screenrest.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

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
                    
                    if (packageName == "com.android.systemui") {
                        Log.d(TAG, "System UI detected during block - closing")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    } else if (packageName != "com.screenrest.app") {
                        Log.d(TAG, "User tried to switch to: $packageName - closing")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }
    
    override fun onInterrupt() {}
}
