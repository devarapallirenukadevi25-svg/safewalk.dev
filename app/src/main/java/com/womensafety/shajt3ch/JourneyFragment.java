package com.womensafety.shajt3ch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

public class JourneyFragment extends Fragment {

    private EditText etDestination, etPin;
    private Button btnStart, btnStop;
    private TextView tvStatus;
    private TextInputLayout pinLayout;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey, container, false);

        etDestination = view.findViewById(R.id.etDestination);
        etPin = view.findViewById(R.id.etPin);
        btnStart = view.findViewById(R.id.btnStartJourney);
        btnStop = view.findViewById(R.id.btnStopJourney);
        tvStatus = view.findViewById(R.id.tvStatus);
        pinLayout = view.findViewById(R.id.pinLayout);

        sharedPreferences = requireActivity().getSharedPreferences("ServiceSettings", Context.MODE_PRIVATE);

        btnStart.setOnClickListener(v -> startJourney());
        btnStop.setOnClickListener(v -> stopJourney());

        updateUI();

        return view;
    }

    private void updateUI() {
        boolean isActive = sharedPreferences.getBoolean("is_journey_active", false);
        if (isActive) {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            pinLayout.setVisibility(View.GONE);
            etDestination.setEnabled(false);
            
            String dest = sharedPreferences.getString("journey_dest", "");
            etDestination.setText(dest);
            tvStatus.setText("Status: Journey Guardian Active");
        } else {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            pinLayout.setVisibility(View.VISIBLE);
            etDestination.setEnabled(true);
            tvStatus.setText("Status: Inactive");
        }
    }

    private void startJourney() {
        String dest = etDestination.getText().toString();
        String pin = etPin.getText().toString();

        if (dest.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pin.length() < 4) {
            Toast.makeText(getActivity(), "Please set a 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPreferences.edit()
                .putBoolean("is_journey_active", true)
                .putString("journey_dest", dest)
                .putString("journey_pin", pin)
                .apply();

        Intent intent = new Intent(getActivity(), MyService.class);
        intent.setAction(MyService.ACTION_START_JOURNEY);
        intent.putExtra("DESTINATION", dest);
        intent.putExtra("PIN", pin);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getActivity().startForegroundService(intent);
        } else {
            getActivity().startService(intent);
        }

        updateUI();
        Toast.makeText(getActivity(), "Journey Guardian Started", Toast.LENGTH_SHORT).show();
    }

    private void stopJourney() {
        sharedPreferences.edit().putBoolean("is_journey_active", false).apply();

        Intent intent = new Intent(getActivity(), MyService.class);
        intent.setAction(MyService.ACTION_STOP_JOURNEY);
        getActivity().startService(intent);

        updateUI();
        etPin.setText("");
        Toast.makeText(getActivity(), "Journey Stopped", Toast.LENGTH_SHORT).show();
    }
}
