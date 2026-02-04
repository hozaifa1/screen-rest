#!/usr/bin/env python3
"""
Test script to verify block screen functionality
Simulates timer expiration and tests block screen behavior
"""

import subprocess
import time
import sys

def run_adb_command(command):
    """Run ADB command and return output"""
    try:
        result = subprocess.run(
            f"adb {command}",
            shell=True,
            capture_output=True,
            text=True,
            timeout=10
        )
        return result.returncode == 0, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return False, "", "Command timed out"

def check_device_connected():
    """Check if Android device is connected"""
    print("Checking for connected devices...")
    success, output, _ = run_adb_command("devices")
    
    if not success:
        print("❌ ADB not found or not working")
        return False
    
    lines = output.strip().split('\n')
    if len(lines) <= 1 or 'device' not in output:
        print("❌ No Android device connected")
        return False
    
    print("✅ Device connected")
    return True

def install_app():
    """Install the app on the device"""
    print("\nInstalling app...")
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    success, _, error = run_adb_command(f"install -r {apk_path}")
    
    if success:
        print("✅ App installed successfully")
        return True
    else:
        print(f"❌ App installation failed: {error}")
        return False

def clear_app_data():
    """Clear app data to reset state"""
    print("\nClearing app data...")
    success, _, _ = run_adb_command("shell pm clear com.screenrest.app")
    if success:
        print("✅ App data cleared")
    return success

def grant_permissions():
    """Grant necessary permissions"""
    print("\nGranting permissions...")
    permissions = [
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.ACCESS_FINE_LOCATION",
    ]
    
    for perm in permissions:
        run_adb_command(f"shell pm grant com.screenrest.app {perm}")
    
    # Enable overlay permission
    run_adb_command("shell appops set com.screenrest.app SYSTEM_ALERT_WINDOW allow")
    print("✅ Permissions granted")

def start_app():
    """Launch the app"""
    print("\nLaunching app...")
    success, _, _ = run_adb_command(
        "shell am start -n com.screenrest.app/.MainActivity"
    )
    if success:
        print("✅ App launched")
        return True
    return False

def trigger_block_screen_test():
    """Trigger block screen using test intent"""
    print("\n" + "="*60)
    print("TESTING BLOCK SCREEN - Manual trigger")
    print("="*60)
    
    print("\nStarting BlockOverlayService with 20 second timer...")
    success, output, error = run_adb_command(
        'shell am startservice -n com.screenrest.app/.service.BlockOverlayService '
        '--ei duration_seconds 20 --es message "Test Block Screen"'
    )
    
    if success:
        print("✅ BlockOverlayService triggered")
        print("\n📱 EXPECTED BEHAVIOR:")
        print("  - Full screen dark overlay should appear immediately")
        print("  - Timer countdown should be visible (20 seconds)")
        print("  - Screen should stay locked for full duration")
        print("  - Pressing Home/Back should NOT close the screen")
        print("  - After 20 seconds, screen should auto-dismiss")
        print("\n⏱️  Waiting 25 seconds to verify auto-dismiss...")
        time.sleep(25)
        print("✅ Test completed")
    else:
        print(f"❌ Failed to trigger block screen: {error}")
        return False
    
    return True

def check_logcat_for_issues():
    """Check logcat for block screen related logs"""
    print("\n" + "="*60)
    print("CHECKING LOGCAT FOR BLOCK SCREEN ACTIVITY")
    print("="*60)
    
    print("\nFetching recent logs...")
    success, output, _ = run_adb_command(
        'logcat -d -s BlockOverlayService:* BlockActivity:* UsageTrackingService:* BlockAccessibility:*'
    )
    
    if success and output:
        print("\n--- Recent Block Screen Logs ---")
        print(output[-2000:] if len(output) > 2000 else output)
    else:
        print("⚠️  No logs found (this might be normal if block hasn't triggered)")

def main():
    """Main test execution"""
    print("\n" + "="*60)
    print("BLOCK SCREEN VERIFICATION TEST")
    print("="*60)
    
    if not check_device_connected():
        print("\n⚠️  Please connect an Android device and enable USB debugging")
        sys.exit(1)
    
    if not install_app():
        print("\n⚠️  Please build the app first: .\\gradlew.bat assembleDebug")
        sys.exit(1)
    
    clear_app_data()
    grant_permissions()
    
    print("\n" + "="*60)
    print("MANUAL SETUP REQUIRED")
    print("="*60)
    print("\n📱 Please perform these steps on your device:")
    print("  1. Enable 'Display over other apps' permission for ScreenRest")
    print("  2. Enable 'Accessibility Service' for ScreenRest")
    print("  3. Enable 'Usage Access' permission for ScreenRest")
    print("\nPress ENTER when ready to continue...")
    input()
    
    if not start_app():
        print("\n❌ Failed to launch app")
        sys.exit(1)
    
    print("\nWaiting 5 seconds for app to initialize...")
    time.sleep(5)
    
    # Run block screen test
    if not trigger_block_screen_test():
        sys.exit(1)
    
    # Check logs
    check_logcat_for_issues()
    
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print("\n✅ All automated tests completed")
    print("\n📋 MANUAL VERIFICATION CHECKLIST:")
    print("  [ ] Block screen appeared immediately when triggered")
    print("  [ ] Timer countdown was visible and accurate")
    print("  [ ] Screen stayed locked for full duration (20s)")
    print("  [ ] Home/Back buttons did NOT close the screen")
    print("  [ ] Screen auto-dismissed after timer finished")
    print("  [ ] No white flash or app closing issues")
    print("\nIf all checks pass ✅, the fix is successful!")

if __name__ == "__main__":
    main()
