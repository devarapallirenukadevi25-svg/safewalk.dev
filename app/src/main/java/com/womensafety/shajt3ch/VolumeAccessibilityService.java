package com.womensafety.shajt3ch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class VolumeAccessibilityService extends AccessibilityService {

    private int upCount = 0;
    private int downCount = 0;
    private long lastUpTime = 0;
    private long lastDownTime = 0;
    private boolean upPressed = false;
    private boolean downPressed = false;

    private static final long RESET_MS = 2000;
    private static final String TAG = "VolumeSOS";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility Service Connected!");
        
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(getApplicationContext(), "Safe Walk: Volume Detection Ready", Toast.LENGTH_LONG).show());

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        
        vibrate(200);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Check if volume features are enabled in settings
        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("volume_enabled", true);
        
        if (!isEnabled) {
            return super.onKeyEvent(event);
        }

        int keyCode = event.getKeyCode();
        int action = event.getAction();
        long now = System.currentTimeMillis();

        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                upPressed = true;
                if (downPressed) {
                    triggerBothPressed();
                } else {
                    if (now - lastUpTime > RESET_MS) upCount = 0;
                    upCount++;
                    lastUpTime = now;
                    Log.d(TAG, "Volume Up Press: " + upCount);
                    vibrate(60);
                    showDebugToast("Vol Up: " + upCount);
                    if (upCount == 3) {
                        triggerSOS();
                        upCount = 0;
                    }
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                downPressed = true;
                if (upPressed) {
                    triggerBothPressed();
                } else {
                    if (now - lastDownTime > RESET_MS) downCount = 0;
                    downCount++;
                    lastDownTime = now;
                    Log.d(TAG, "Volume Down Press: " + downCount);
                    vibrate(60);
                    showDebugToast("Vol Down: " + downCount);
                    if (downCount == 5) {
                        triggerAlarm();
                        downCount = 0;
                    }
                }
                return true;
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) upPressed = false;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) downPressed = false;
        }

        return super.onKeyEvent(event);
    }

    private void vibrate(long ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(ms);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration failed", e);
        }
    }

    private void showDebugToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void triggerSOS() {
        Log.d(TAG, "Executing SOS ACTION");
        vibrate(500);
        sendIntent(MyService.ACTION_SOS);
    }

    private void triggerAlarm() {
        Log.d(TAG, "Executing ALARM ACTION");
        vibrate(500);
        sendIntent(MyService.ACTION_ALARM);
    }

    private void triggerBothPressed() {
        Log.d(TAG, "Executing LOCATION ACTION");
        vibrate(300);
        sendIntent(MyService.ACTION_LOCATION);
        upCount = 0;
        downCount = 0;
    }

    private void sendIntent(String action) {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MyService with action " + action, e);
        }
    }
}
