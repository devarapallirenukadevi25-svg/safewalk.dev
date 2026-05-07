package com.womensafety.shajt3ch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.sac.speech.Speech;
import com.sac.speech.SpeechDelegate;

import java.util.List;
import java.util.Locale;

public class MyService extends Service
        implements SpeechDelegate, Speech.stopDueToDelay, AccelerometerListener {

    private final Handler restartHandler = new Handler(Looper.getMainLooper());
    private static final String CHANNEL_ID = "WomenSafetyChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_SOS = "ACTION_SOS";
    public static final String ACTION_ALARM = "ACTION_ALARM";
    public static final String ACTION_LOCATION = "ACTION_LOCATION";
    public static final String ACTION_START_JOURNEY = "ACTION_START_JOURNEY";
    public static final String ACTION_STOP_JOURNEY = "ACTION_STOP_JOURNEY";
    public static final String ACTION_PIN_SUCCESS = "ACTION_PIN_SUCCESS";

    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private boolean waitingForTap = false;
    private final Handler patternHandler = new Handler(Looper.getMainLooper());
    private FusedLocationProviderClient fusedLocationClient;
    private MediaPlayer mediaPlayer;
    private WindowManager windowManager;
    private View overlayView;

    private boolean isJourneyActive = false;
    private String destinationName;
    private Location destinationLocation;
    private Location lastTrackedLocation;
    private long lastMovementTime;
    private LocationCallback journeyCallback;
    private boolean initialJourneySmsSent = false;
    private String journeyPin;
    private boolean pinVerifiedThisInterval = false;
    private final Handler pinCheckHandler = new Handler(Looper.getMainLooper());

    private ContentObserver airplaneModeObserver;
    private ConnectivityReceiver connectivityReceiver;
    private LocationProviderReceiver locationProviderReceiver;
    private long lastSosTriggerTime = 0;
    private boolean isSosPending = false;
    private boolean isPollingForSignal = false;
    private boolean isServiceDestroyed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceDestroyed = false;
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        ensureDatabaseSchema();
        setupAirplaneModeObserver();
        setupConnectivityReceiver();
        setupLocationProviderReceiver();
        
        try {
            // Use Application Context for Speech initialization
            Speech.init(getApplicationContext());
            Speech.getInstance().setListener(this);
            Speech.getInstance().setStopListeningAfterInactivity(30000);
            // Don't force offline as it often fails if not pre-downloaded
            Speech.getInstance().setPreferOffline(false);
            Speech.getInstance().setTransitionMinimumDelay(1200);
        } catch (Exception e) {
            Log.e("MyService", "Speech init error: " + e.getMessage());
        }

        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
        isSosPending = prefs.getBoolean("is_sos_pending", false);
        if (isSosPending) {
            startPendingSosPolling();
        }
    }

    private void setSosPending(boolean pending) {
        this.isSosPending = pending;
        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_sos_pending", pending).apply();
    }

    private void setupConnectivityReceiver() {
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    private class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = isInternetAvailable();
            Log.d("MyService", "Connectivity change: connected=" + isConnected);
            
            SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
            boolean protectionEnabled = prefs.getBoolean("protection_enabled", false);
            
            if (isConnected) {
                if (isSosPending) startPendingSosPolling();
                if (protectionEnabled) {
                    restartHandler.removeCallbacksAndMessages(null);
                    restartHandler.postDelayed(() -> startSpeechRecognition(), 2000);
                }
            } else {
                if (protectionEnabled || isJourneyActive) {
                    restartHandler.postDelayed(() -> {
                        if (!isInternetAvailable()) {
                            Log.d("MyService", "Network lost detected - Activating SOS.");
                            activateHelpMode(null);
                        }
                    }, 3000);
                }
            }
        }
    }

    private void setupLocationProviderReceiver() {
        locationProviderReceiver = new LocationProviderReceiver();
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(locationProviderReceiver, filter);
    }

    private class LocationProviderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                
                SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
                if (prefs.getBoolean("protection_enabled", false) || isJourneyActive || prefs.getBoolean("shake_enabled", true)) {
                    if (!isGpsEnabled && !isNetworkEnabled) {
                        Log.d("MyService", "Location services disabled - Activating SOS.");
                        activateHelpMode(null);
                    }
                }
            }
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network n = cm.getActiveNetwork();
            if (n != null) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) 
                        || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    private void setupAirplaneModeObserver() {
        airplaneModeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                boolean isOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
                
                boolean shakeEnabled = prefs.getBoolean("shake_enabled", true);
                boolean protectionEnabled = prefs.getBoolean("protection_enabled", false);

                if (protectionEnabled || isJourneyActive || shakeEnabled) {
                    if (isOn) {
                        Log.d("MyService", "Airplane mode ON - Triggering SOS.");
                        activateHelpMode(null);
                    } else {
                        Log.d("MyService", "Airplane mode OFF - Starting signal recovery.");
                        if (protectionEnabled) startSpeechRecognition();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> startPendingSosPolling(), 3000);
                    }
                }
            }
        };
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, airplaneModeObserver);
    }

    private synchronized void startPendingSosPolling() {
        if (!isSosPending || isPollingForSignal) return;
        isPollingForSignal = true;
        Log.d("MyService", "Aggressive polling for signal recovery...");
        
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            int attempts = 0;
            @Override
            public void run() {
                if (isServiceDestroyed) return;
                boolean isAirplaneOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                boolean hasCellular = tm != null && !TextUtils.isEmpty(tm.getNetworkOperatorName());

                if (!isSosPending) {
                    isPollingForSignal = false;
                    return;
                }

                if (!isAirplaneOn && (hasCellular || isInternetAvailable())) {
                    Log.d("MyService", "Signal recovered! Sending queued SOS SMS and initiating Call.");
                    sendSOSWithLocation(null);
                    setSosPending(false);
                    isPollingForSignal = false;
                    showToast("Signal restored. Emergency alerts sent.");
                } else if (attempts > 90) { // Poll for 3 minutes
                    isPollingForSignal = false;
                } else {
                    attempts++;
                    handler.postDelayed(this, 2000);
                }
            }
        });
    }

    private void ensureDatabaseSchema() {
        try {
            SQLiteDatabase db = openOrCreateDatabase("NumberDB", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS details(Pname TEXT, number TEXT, keyword TEXT);");
            try { db.rawQuery("SELECT keyword FROM details LIMIT 1", null).close(); } 
            catch (Exception e) { db.execSQL("ALTER TABLE details ADD COLUMN keyword TEXT;"); }
            db.close();
        } catch (Exception e) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification());
        if (intent != null && intent.getAction() != null) handleAction(intent.getAction(), intent);
        muteSystemSound();
        
        // Start listening if protection is enabled
        startSpeechRecognition();
        
        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("shake_enabled", true) && AccelerometerManager.isSupported(this)) {
            AccelerometerManager.startListening(this);
        }
        return START_STICKY;
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_SOS: activateHelpMode(null); break;
            case ACTION_ALARM: startAlarm(); break;
            case ACTION_LOCATION: sendSOSWithLocation(null); break;
            case ACTION_START_JOURNEY: destinationName = intent.getStringExtra("DESTINATION"); journeyPin = intent.getStringExtra("PIN"); startJourneyTracking(); break;
            case ACTION_STOP_JOURNEY: stopJourneyGuardian(); break;
            case ACTION_PIN_SUCCESS: pinVerifiedThisInterval = true; showToast("Identity Verified"); break;
        }
    }

    private void startJourneyTracking() {
        isJourneyActive = true; initialJourneySmsSent = false; lastMovementTime = System.currentTimeMillis(); pinVerifiedThisInterval = true; 
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(destinationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                destinationLocation = new Location("");
                destinationLocation.setLatitude(addresses.get(0).getLatitude());
                destinationLocation.setLongitude(addresses.get(0).getLongitude());
                showToast("Tracking Journey to: " + destinationName);
            }
        } catch (Exception e) {}
        LocationRequest lr = LocationRequest.create().setInterval(20000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        journeyCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult res) {
                if (res == null || !isJourneyActive) return;
                Location current = res.getLastLocation();
                if (current == null) return;
                if (!initialJourneySmsSent) { sendGuardianUpdate("Starting journey to " + destinationName + ". Track: http://maps.google.com/maps?q=" + current.getLatitude() + "," + current.getLongitude()); initialJourneySmsSent = true; }
                if (destinationLocation != null && current.distanceTo(destinationLocation) < 150) { showToast("Arrived safely!"); sendGuardianUpdate("I have arrived safely at " + destinationName); stopJourneyGuardian(); return; }
                if (lastTrackedLocation != null && current.distanceTo(lastTrackedLocation) < 15 && System.currentTimeMillis() - lastMovementTime > 300000) { activateHelpMode(null); stopJourneyGuardian(); }
                else if (lastTrackedLocation == null || current.distanceTo(lastTrackedLocation) >= 15) { lastMovementTime = System.currentTimeMillis(); }
                lastTrackedLocation = current;
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(lr, journeyCallback, Looper.getMainLooper());
        startPinCheckTimer();
    }

    private void startPinCheckTimer() {
        pinCheckHandler.removeCallbacksAndMessages(null);
        pinCheckHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isJourneyActive) return;
                if (!pinVerifiedThisInterval) { activateHelpMode(null); stopJourneyGuardian(); }
                else { pinVerifiedThisInterval = false; promptForPin(); pinCheckHandler.postDelayed(this, 300000); }
            }
        }, 300000);
    }

    private void promptForPin() { Intent intent = new Intent(this, PinActivity.class); intent.putExtra("CORRECT_PIN", journeyPin); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); }

    private void sendGuardianUpdate(String msg) {
        SQLiteDatabase db = openOrCreateDatabase("NumberDB", MODE_PRIVATE, null); String number = Register.getNumber(db); db.close();
        if (!TextUtils.isEmpty(number)) { try { SmsManager sm = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? getSystemService(SmsManager.class) : SmsManager.getDefault(); if (sm != null) sm.sendTextMessage(number, null, msg, null, null); } catch (Exception e) {} }
    }

    private void stopJourneyGuardian() { isJourneyActive = false; pinCheckHandler.removeCallbacksAndMessages(null); if (journeyCallback != null) fusedLocationClient.removeLocationUpdates(journeyCallback); showToast("Journey Stopped."); }

    private void muteSystemSound() { try { AudioManager m = (AudioManager) getSystemService(Context.AUDIO_SERVICE); if (m != null) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { m.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0); m.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0); } else { m.setStreamMute(AudioManager.STREAM_SYSTEM, true); m.setStreamMute(AudioManager.STREAM_NOTIFICATION, true); } } } catch (Exception e) {} }

    private void unmuteSystemSound() { 
        try { 
            AudioManager m = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
            if (m != null) { 
                m.setStreamVolume(AudioManager.STREAM_MUSIC, m.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0); 
                m.setStreamVolume(AudioManager.STREAM_SYSTEM, m.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0);
                m.setStreamVolume(AudioManager.STREAM_NOTIFICATION, m.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { 
                    m.adjustStreamVolume(AudioManager.STREAM_SYSTEM, 100, 0); 
                    m.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, 100, 0); 
                } else { 
                    m.setStreamMute(AudioManager.STREAM_SYSTEM, false); 
                    m.setStreamMute(AudioManager.STREAM_NOTIFICATION, false); 
                } 
            } 
        } catch (Exception e) {} 
    }

    private void createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Women Safety", NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c); } }

    private Notification getNotification() { Intent ni = new Intent(this, MainActivity.class); PendingIntent pi = PendingIntent.getActivity(this, 0, ni, PendingIntent.FLAG_IMMUTABLE); SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE); String status = (prefs.getBoolean("protection_enabled", false) ? "Protection ON" : "Guardian Active"); return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Women Safety Active").setContentText(status).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pi).setOngoing(true).build(); }

    private void showOverlay() { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return; 
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT); 
        overlayView = new View(this); 
        overlayView.setOnTouchListener((v, e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN && waitingForTap) handleTap(); return false; }); 
        windowManager.addView(overlayView, p); 
    }

    private void removeOverlay() { if (overlayView != null) { try { windowManager.removeView(overlayView); } catch(Exception e) {} overlayView = null; } }

    private synchronized void startSpeechRecognition() { 
        if (isServiceDestroyed) return;
        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE); 
        if (!prefs.getBoolean("protection_enabled", false)) return; 
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MyService", "RECORD_AUDIO permission missing");
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("MyService", "Speech recognition not available on this device");
            return;
        }

        try { 
            Speech speech = Speech.getInstance();
            if (!speech.isListening()) {
                speech.startListening(null, this);
                Log.d("MyService", "Microphone listening...");
            }
        } catch (Exception e) { 
            Log.e("MyService", "Speech start error: " + e.getMessage());
            restartHandler.removeCallbacksAndMessages(null);
            restartHandler.postDelayed(this::startSpeechRecognition, 2500); 
        } 
    }

    @Override public void onSpeechResult(String result) { 
        if (isServiceDestroyed) return;
        if (!TextUtils.isEmpty(result)) { 
            Log.d("MyService", "Speech result: " + result);
            SQLiteDatabase db = openOrCreateDatabase("NumberDB", MODE_PRIVATE, null); 
            String num = Register.getNumberByKeyword(db, result.toLowerCase().trim()); 
            db.close(); 
            if (num != null) { activateHelpMode(num); } 
            else if (result.toLowerCase().contains("hello") || result.toLowerCase().contains("help") || result.toLowerCase().contains("bachao")) { activateHelpMode(null); }
        } 
        restartHandler.removeCallbacksAndMessages(null);
        restartHandler.postDelayed(this::startSpeechRecognition, 1500); 
    }

    @Override public void onSpeechPartialResults(List<String> results) {
        if (isServiceDestroyed) return;
        if (results != null && !results.isEmpty()) {
            String partial = results.get(0).toLowerCase();
            if (partial.contains("help") || partial.contains("bachao")) { activateHelpMode(null); }
        }
    }

    private void showToast(String msg) { new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show()); }

    @Override public void onShake(float force) { 
        if (isServiceDestroyed) return;
        SharedPreferences prefs = getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE); 
        if (!prefs.getBoolean("shake_enabled", true)) return; 
        
        if (System.currentTimeMillis() - lastShakeTime < 500) return; 
        shakeCount++; 
        lastShakeTime = System.currentTimeMillis(); 
        if (shakeCount >= 2) { 
            shakeCount = 0; 
            waitingForTap = true; 
            showOverlay(); 
            showToast("Shake detected! Tap screen."); 
            new Handler(Looper.getMainLooper()).postDelayed(() -> { waitingForTap = false; removeOverlay(); }, 5000); 
        } 
    }

    @Override public void onAccelerationChanged(float x, float y, float z) {}
    private void handleTap() { waitingForTap = false; removeOverlay(); activateHelpMode(null); }

    private void activateHelpMode(@Nullable String specificNumber) {
        if (System.currentTimeMillis() - lastSosTriggerTime < 5000) return; 
        lastSosTriggerTime = System.currentTimeMillis();
        
        unmuteSystemSound(); 
        startAlarm(); 
        
        // Try to send SOS immediately
        sendSOSWithLocation(specificNumber);

        // Queue for automatic recovery once signal returns
        boolean isAirplaneModeOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (isAirplaneModeOn || !isInternetAvailable()) {
            setSosPending(true);
            showToast("Device Offline. SOS queued for automatic recovery.");
            if (!isAirplaneModeOn) {
                startPendingSosPolling(); 
            }
        }
    }

    private void startAlarm() { try { Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); if (mediaPlayer != null) mediaPlayer.release(); mediaPlayer = MediaPlayer.create(this, alert); mediaPlayer.setLooping(true); mediaPlayer.start(); } catch (Exception e) {} }

    private void sendSOSWithLocation(@Nullable String specificNumber) { 
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendSMS("SOS! I am in danger. Location unavailable.", specificNumber);
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> { 
            String msg = "SOS! I am in danger. Location: http://maps.google.com/maps?q=" + (loc != null ? loc.getLatitude() + "," + loc.getLongitude() : "Unknown"); 
            
            SharedPreferences prefs = getSharedPreferences("CommunityPrefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("isJoined", false)) { 
                SQLiteDatabase db = openOrCreateDatabase("NumberDB", MODE_PRIVATE, null); 
                Cursor c = db.rawQuery("SELECT number FROM details", null); 
                while (c.moveToNext()) sendSMS(msg + " (Community Alert)", c.getString(0)); 
                c.close(); db.close(); 
            } else { sendSMS(msg, specificNumber); }
        }).addOnFailureListener(e -> {
            sendSMS("SOS! I am in danger.", specificNumber);
        }); 
    }

    private void sendSMS(String message, @Nullable String num) { 
        String target = num; 
        if (target == null) { SQLiteDatabase db = openOrCreateDatabase("NumberDB", MODE_PRIVATE, null); target = Register.getNumber(db); db.close(); } 
        if (!TextUtils.isEmpty(target)) { 
            try { 
                SmsManager sm = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? getSystemService(SmsManager.class) : SmsManager.getDefault(); 
                if (sm != null) sm.sendTextMessage(target, null, message, null, null); 
                
                // Only trigger CALL if Airplane mode is OFF
                boolean isAirplaneModeOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                if (!isAirplaneModeOn) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + target)); 
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) startActivity(intent); 
                }
            } catch (Exception e) { Log.e("MyService", "Failed: " + e.getMessage()); } 
        } 
    }

    @Override public void onDestroy() { 
        isServiceDestroyed = true;
        super.onDestroy(); 
        restartHandler.removeCallbacksAndMessages(null);
        if (airplaneModeObserver != null) getContentResolver().unregisterContentObserver(airplaneModeObserver); 
        if (connectivityReceiver != null) unregisterReceiver(connectivityReceiver);
        if (locationProviderReceiver != null) unregisterReceiver(locationProviderReceiver);
        unmuteSystemSound(); removeOverlay(); stopJourneyGuardian(); AccelerometerManager.stopListening(); 
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.release(); } 
        try { Speech.getInstance().shutdown(); } catch(Exception e) {} 
    }
    
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStartOfSpeech() {}
    @Override public void onSpeechRmsChanged(float value) {}
    @Override public void onSpecifiedCommandPronounced(String event) { 
        if (!isServiceDestroyed) {
            restartHandler.removeCallbacksAndMessages(null);
            restartHandler.postDelayed(this::startSpeechRecognition, 1500); 
        }
    }
}
