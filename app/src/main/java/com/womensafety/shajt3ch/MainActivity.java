package com.womensafety.shajt3ch;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 456;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 789;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            boolean openRegister = getIntent().getBooleanExtra("OPEN_REGISTER", false);
            if (openRegister) {
                loadFragment(new Register());
            } else {
                loadFragment(new HomeFragment());
            }
        }

        // Sequential permission checks to avoid overlapping dialogs
        if (checkAndRequestPermissions()) {
            checkOverlayPermission();
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAccessibilityPermission, 3000);
    }

    private void checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled(this, VolumeAccessibilityService.class)) {
            new AlertDialog.Builder(this)
                    .setTitle("Volume SOS Required")
                    .setMessage("To detect Volume Button patterns (3x Up for SOS), you must enable 'Women Safety Accessibility' in Settings.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        String serviceId = context.getPackageName() + "/" + service.getName();
        String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (splitter.next().equalsIgnoreCase(serviceId)) return true;
        }
        return false;
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("Overlay Permission Required")
                        .setMessage("To detect screen taps in the background, please allow 'Display over other apps'.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkBackgroundLocationPermission();
            }
        }
    }

    private void checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Required")
                        .setMessage("To track your location accurately when the app is in the background, please select 'Allow all the time' in the Location settings.")
                        .setPositiveButton("Configure", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, 
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 
                                    BACKGROUND_LOCATION_PERMISSION_CODE);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    public boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        listPermissionsNeeded.add(Manifest.permission.CALL_PHONE);
        listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> remainingPermissions = new ArrayList<>();
        for (String p : listPermissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(p);
            }
        }

        if (!remainingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, remainingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            List<String> denied = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Fine location is often "denied" if user chooses Approximate. 
                    // We check if Coarse is granted.
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            denied.add("Precise Location");
                        }
                    } else if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                        // Notifications are not essential for logic but recommended
                        Log.w("MainActivity", "Notification permission denied.");
                    } else {
                        denied.add(permissions[i].substring(permissions[i].lastIndexOf('.') + 1));
                    }
                }
            }

            if (!denied.isEmpty()) {
                String msg = "Permissions required: " + TextUtils.join(", ", denied);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } else {
                // Initial permissions granted, proceed to chain other checks
                checkOverlayPermission();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_power) {
            toggleService();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleService() {
        if (checkAndRequestPermissions()) {
            Intent serviceIntent = new Intent(this, MyService.class);
            if (isServiceRunning(MyService.class)) {
                stopService(serviceIntent);
                Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
            }
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (currentFragment instanceof HomeFragment) {
                loadFragment(new HomeFragment());
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.newbutton, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        displaySelectedScreen(item.getItemId());
        return true;
    }

    private void displaySelectedScreen(int itemId) {
        Fragment fragment = null;
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_inst) {
            showInstructions();
        } else if (itemId == R.id.nav_verify) {
            fragment = new Verify();
        } else if (itemId == R.id.nav_register) {
            fragment = new Register();
        } else if (itemId == R.id.nav_display) {
            fragment = new Display();
        } else if (itemId == R.id.nav_journey) {
            fragment = new JourneyFragment();
        } else if (itemId == R.id.nav_community) {
            fragment = new CommunityFragment();
        }
        if (fragment != null) { loadFragment(fragment); }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    private void showInstructions() {
        View alertLayout = LayoutInflater.from(this).inflate(R.layout.popup_layout, null);
        new AlertDialog.Builder(this)
            .setTitle("App Instructions")
            .setView(alertLayout)
            .setPositiveButton("Got it", null)
            .show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
    }
}
