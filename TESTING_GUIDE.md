# ScreenRest - Testing Guide (Part 3 UI)

## Prerequisites

Before testing, ensure you have:
- Android device or emulator (API 26+)
- ADB configured and device connected
- App built and installed

## Quick Build & Install

```powershell
# Build debug APK
.\gradlew assembleDebug

# Install on device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Clear app data for fresh start
adb shell pm clear com.screenrest.app

# Launch app
adb shell am start -n com.screenrest.app/.MainActivity
```

## Testing Checklist

### 1. Onboarding Flow (Phase 6)

**Test**: Fresh install shows onboarding
```powershell
adb shell pm clear com.screenrest.app
adb shell am start -n com.screenrest.app/.MainActivity
```

**Expected**:
- ✓ Welcome screen displays with "SR" icon and app description
- ✓ Progress bar at top shows current step (1/6)
- ✓ "Get Started" button navigates to next step

**Test**: Permission steps
- ✓ Usage Access step shows permission status card
- ✓ "Grant Permission" opens system settings
- ✓ After granting, status card turns green
- ✓ Same flow for Overlay, Notification, and Accessibility permissions

**Test**: Complete step
- ✓ Summary shows all permissions with checkmarks
- ✓ Enforcement level badge displays (FULL/STANDARD/BASIC)
- ✓ "Start Using ScreenRest" button enabled only with required permissions
- ✓ Completing onboarding navigates to Home screen

### 2. Home Screen (Phase 7)

**Test**: Dashboard display
- ✓ Top bar shows "ScreenRest" title with settings icon
- ✓ Status card shows "Tracking Paused" initially (gray)
- ✓ Enforcement level badge visible
- ✓ Config summary card shows break configuration
- ✓ Edit icon in config card navigates to Settings

**Test**: Service toggle
- ✓ "Start Tracking" button starts service
- ✓ Status card turns green and shows "Tracking Active"
- ✓ Status indicator (green dot) appears
- ✓ Notification appears in status bar
- ✓ "Pause Tracking" button stops service

**Test**: Permission warnings
- ✓ Missing permissions show warning cards
- ✓ "Fix Now" button opens appropriate system settings
- ✓ Warning cards disappear when permissions granted

### 3. Settings Screen (Phase 8)

**Test**: Navigation
```powershell
# Navigate from home to settings
```
- ✓ Settings icon in home screen opens settings
- ✓ Back button returns to home

**Test**: Break configuration
- ✓ Usage threshold slider: 5-120 minutes (step 5)
- ✓ Value updates in real-time
- ✓ Break duration slider: 5-300 seconds (step 5)
- ✓ Duration formats correctly (e.g., "30 seconds", "2 minutes")
- ✓ Setting duration > 120 seconds shows warning dialog

**Test**: Tracking mode selector
- ✓ Two modes: Continuous and Cumulative Daily
- ✓ Radio button shows selected mode
- ✓ Expand/collapse arrows work
- ✓ Expanded view shows detailed explanation
- ✓ Selecting mode updates immediately

**Test**: Messages section
- ✓ Info card explains custom messages
- ✓ "Manage Custom Messages" button navigates to messages screen

**Test**: Appearance section
- ✓ Three theme options: System, Light, Dark
- ✓ Radio buttons work
- ✓ Theme changes apply immediately
- ✓ Description text visible for each option

**Test**: About section
- ✓ Version displays (1.0.0)
- ✓ License shows (MIT)
- ✓ Source code link visible

### 4. Custom Messages (Phase 9)

**Test**: Empty state
- ✓ Empty state message displays when no messages
- ✓ FAB (+ button) visible at bottom right

**Test**: Add message
- ✓ FAB opens dialog
- ✓ Text field accepts input
- ✓ Character counter shows (0/500)
- ✓ Cannot exceed 500 characters
- ✓ "Add" button disabled when empty
- ✓ "Cancel" dismisses dialog
- ✓ "Add" creates message and closes dialog

**Test**: Message list
- ✓ Messages display in cards
- ✓ Long messages show character count
- ✓ Delete button visible on each card
- ✓ Swipe left/right shows delete action

**Test**: Delete message
- ✓ Delete button shows confirmation dialog
- ✓ "Cancel" keeps message
- ✓ "Delete" removes message
- ✓ Message disappears from list
- ✓ Empty state shows if all deleted

**Test**: Persistence
```powershell
# Force stop app
adb shell am force-stop com.screenrest.app

# Relaunch
adb shell am start -n com.screenrest.app/.MainActivity
```
- ✓ Custom messages persist after app restart
- ✓ All settings persist after app restart

### 5. Break Screen Integration

**Test**: Manual break trigger
```powershell
# Set threshold to 1 minute for quick testing
# Use phone for 1 minute
```
- ✓ Break screen appears after threshold
- ✓ Countdown timer displays and counts down
- ✓ Message displays (Ayah or custom message)
- ✓ Screen stays on during break
- ✓ Cannot dismiss early (with accessibility enabled)
- ✓ Auto-dismisses when timer reaches 0
- ✓ Returns to previous screen

### 6. Service Persistence

**Test**: Boot persistence
```powershell
adb reboot
# Wait for device to boot
```
- ✓ Service starts automatically after boot (if tracking was enabled)
- ✓ Notification appears
- ✓ Settings preserved

**Test**: Force stop
```powershell
adb shell am force-stop com.screenrest.app
# Wait 30 seconds
```
- ✓ Service restarts automatically (START_STICKY)

### 7. Theme Switching

**Test**: Light/Dark mode
- ✓ Switch to Light theme - all screens use light colors
- ✓ Switch to Dark theme - all screens use dark colors
- ✓ Switch to System - follows device theme
- ✓ No layout issues in either mode
- ✓ Text remains readable in all modes

### 8. Edge Cases

**Test**: Permission revocation
```powershell
# Revoke usage access manually from system settings
```
- ✓ App detects permission loss
- ✓ Warning card appears on home screen
- ✓ Service continues running but shows "Missing permission"
- ✓ No crashes

**Test**: Screen rotation
- ✓ All screens handle rotation without data loss
- ✓ Dialogs survive rotation

**Test**: Low memory
```powershell
# Open many apps to trigger low memory
```
- ✓ App handles process death gracefully
- ✓ State restored on recreation

## Common Issues & Solutions

### Issue: Build fails
**Solution**: 
```powershell
.\gradlew clean
.\gradlew assembleDebug
```

### Issue: App crashes on launch
**Solution**: Check logcat
```powershell
adb logcat -s ScreenRest:* AndroidRuntime:E
```

### Issue: Service not starting
**Solution**: Check permissions granted
```powershell
# Check if all required permissions granted
adb shell dumpsys package com.screenrest.app | findstr permission
```

### Issue: Break screen not appearing
**Solution**: 
1. Verify overlay permission granted
2. Check notification shows "Tracking Active"
3. Verify threshold is low enough (try 1 minute)

## Manual Verification Commands

```powershell
# Check if app is installed
adb shell pm list packages | findstr screenrest

# Check app info
adb shell dumpsys package com.screenrest.app

# View app logs
adb logcat -s ScreenRest:* -v time

# Clear app data and start fresh
adb shell pm clear com.screenrest.app
adb shell am start -n com.screenrest.app/.MainActivity

# Check if service is running
adb shell dumpsys activity services com.screenrest.app

# Force stop app
adb shell am force-stop com.screenrest.app
```

## Performance Testing

**Metrics to monitor**:
- Memory usage: Should stay under 100MB
- CPU usage: Should be minimal (< 5% average)
- Battery drain: Service polling every 30 seconds
- Notification responsiveness: Updates within 30 seconds

**Check memory**:
```powershell
adb shell dumpsys meminfo com.screenrest.app
```

## Accessibility Testing

- ✓ All buttons have 48dp minimum touch target
- ✓ All interactive elements have content descriptions
- ✓ Text contrast meets WCAG guidelines
- ✓ Works with TalkBack enabled

## Final Checklist

Before considering implementation complete:
- [ ] All onboarding steps work
- [ ] Home screen displays correctly
- [ ] Settings can be changed and persist
- [ ] Custom messages can be added/deleted
- [ ] Break screen triggers and displays
- [ ] Service survives app closure and reboot
- [ ] Theme switching works
- [ ] No crashes in normal usage
- [ ] All required permissions handled gracefully
- [ ] Minimal, clean UI maintained throughout

## Next Steps After Testing

1. Fix any bugs found during testing
2. Optimize performance if needed
3. Add app icon (currently placeholder)
4. Create release build with signing
5. Prepare for distribution (Play Store or F-Droid)
