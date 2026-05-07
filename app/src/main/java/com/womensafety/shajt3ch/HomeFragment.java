package com.womensafety.shajt3ch;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class HomeFragment extends Fragment {

    private Button btStartService;
    private TextView tvText;
    private SwitchMaterial switchVolume, switchShake;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btStartService = view.findViewById(R.id.btStartService);
        tvText = view.findViewById(R.id.tvText);
        switchVolume = view.findViewById(R.id.switchVolume);
        switchShake = view.findViewById(R.id.switchShake);

        sharedPreferences = requireActivity().getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);

        switchVolume.setChecked(sharedPreferences.getBoolean("volume_enabled", true));
        switchShake.setChecked(sharedPreferences.getBoolean("shake_enabled", true));

        switchVolume.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean("volume_enabled", isChecked).apply());

        switchShake.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean("shake_enabled", isChecked).apply());

        updateUI();

        btStartService.setOnClickListener(v -> {
            boolean isRunning = checkServiceRunning();
            if (!isRunning) {
                if (((MainActivity) requireActivity()).checkAndRequestPermissions()) {
                    sharedPreferences.edit().putBoolean("protection_enabled", true).apply();
                    Intent intent = new Intent(getActivity(), MyService.class);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        requireActivity().startForegroundService(intent);
                    } else {
                        requireActivity().startService(intent);
                    }
                    btStartService.setText("STOP SERVICE");
                    tvText.setVisibility(View.VISIBLE);
                }
            } else {
                sharedPreferences.edit().putBoolean("protection_enabled", false).apply();
                // We stop the whole service if Journey isn't active, otherwise we just stop protection logic
                // For simplicity, we restart the service to refresh its state
                requireActivity().stopService(new Intent(getActivity(), MyService.class));
                btStartService.setText("START SERVICE");
                tvText.setVisibility(View.GONE);
            }
        });

        return view;
    }

    private void updateUI() {
        boolean isEnabled = sharedPreferences.getBoolean("protection_enabled", false);
        if (checkServiceRunning() && isEnabled) {
            btStartService.setText("STOP SERVICE");
            tvText.setVisibility(View.VISIBLE);
        } else {
            btStartService.setText("START SERVICE");
            tvText.setVisibility(View.GONE);
        }
    }

    private boolean checkServiceRunning() {
        ActivityManager manager = (ActivityManager) requireActivity().getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(50);
            if (services != null) {
                String serviceName = getString(R.string.my_service_name);
                for (ActivityManager.RunningServiceInfo service : services) {
                    if (serviceName.equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
