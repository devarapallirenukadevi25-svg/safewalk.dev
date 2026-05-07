package com.womensafety.shajt3ch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class CommunityFragment extends Fragment {

    private SwitchMaterial switchCommunity;
    private TextView tvStatus;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        switchCommunity = view.findViewById(R.id.switchCommunity);
        tvStatus = view.findViewById(R.id.tvCommunityStatus);

        sharedPreferences = getActivity().getSharedPreferences("CommunityPrefs", Context.MODE_PRIVATE);
        boolean isJoined = sharedPreferences.getBoolean("isJoined", false);

        switchCommunity.setChecked(isJoined);
        updateStatusText(isJoined);

        switchCommunity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("isJoined", isChecked).apply();
            updateStatusText(isChecked);
            if (isChecked) {
                Toast.makeText(getActivity(), "Joined Safe Radius Community", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Left Safe Radius Community", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateStatusText(boolean isJoined) {
        if (isJoined) {
            tvStatus.setText("Status: Joined (Active in 1km Radius)");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("Status: Not Joined");
            tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
}
