package com.kynl.ledcube.fragment;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kynl.ledcube.R;
import com.kynl.ledcube.manager.SharedPreferencesManager;


public class SettingsFragment extends Fragment {


    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        /* Elements */
        SwitchCompat switchAutoDetect = view.findViewById(R.id.switchAutoDetect);
        SwitchCompat switchSyncBrightness = view.findViewById(R.id.switchSyncBrightness);

        switchAutoDetect.setChecked(SharedPreferencesManager.getInstance(getContext()).isAutoDetect());
        switchSyncBrightness.setChecked(SharedPreferencesManager.getInstance(getContext()).isSyncBrightness());

        switchAutoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesManager.getInstance(getContext()).setAutoDetect(isChecked);
        });
        switchSyncBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesManager.getInstance(getContext()).setSyncBrightness(isChecked);
        });

        return view;
    }
}