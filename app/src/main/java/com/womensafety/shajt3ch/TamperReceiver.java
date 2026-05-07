package com.womensafety.shajt3ch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class TamperReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        SharedPreferences prefs = context.getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
        boolean protectionEnabled = prefs.getBoolean("protection_enabled", false);
        boolean journeyActive = prefs.getBoolean("is_journey_active", false);
        boolean shakeEnabled = prefs.getBoolean("shake_enabled", true);

        // Process Airplane Mode logic
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isOn = intent.getBooleanExtra("state", false);
            if (isOn) {
                Log.d("TamperDetect", "Airplane mode ON - Starting service to trigger SOS.");
                startMyService(context, MyService.ACTION_SOS);
            } else {
                Log.d("TamperDetect", "Airplane mode OFF - Starting service for recovery.");
                startMyService(context, null);
            }
            return;
        }

        // Boot Completed - Ensure safety is active
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            if (protectionEnabled || shakeEnabled) {
                startMyService(context, null);
            }
            return;
        }

        // Only trigger other tamper alerts if safety modes are active
        if (!protectionEnabled && !journeyActive && !shakeEnabled) {
            return;
        }

        boolean triggerSos = false;

        switch (action) {
            case Intent.ACTION_SHUTDOWN:
                Log.d("TamperDetect", "Shutdown detected!");
                triggerSos = true;
                break;

            case LocationManager.PROVIDERS_CHANGED_ACTION:
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Log.d("TamperDetect", "GPS disabled!");
                    triggerSos = true;
                }
                break;

            case ConnectivityManager.CONNECTIVITY_ACTION:
                // Only if internet is explicitly gone
                break;
        }

        if (triggerSos) {
            startMyService(context, MyService.ACTION_SOS);
        }
    }

    private void startMyService(Context context, String action) {
        Intent serviceIntent = new Intent(context, MyService.class);
        if (action != null) serviceIntent.setAction(action);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
