package com.kynl.ledcube.fragment;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.kynl.ledcube.R;
import com.kynl.ledcube.manager.BroadcastManager;
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
        ImageButton backBtn = view.findViewById(R.id.settingBackBtn);
        SwitchCompat switchAutoDetect = view.findViewById(R.id.switchAutoDetect);
        SwitchCompat switchSyncBrightness = view.findViewById(R.id.switchSyncBrightness);

        /* Back button */
        backBtn.setOnClickListener(v -> BroadcastManager.getInstance().sendRequestChangeToHomeScreen());

        /* Settings switch */
        switchAutoDetect.setChecked(SharedPreferencesManager.getInstance().isAutoDetect());
        switchSyncBrightness.setChecked(SharedPreferencesManager.getInstance().isSyncBrightness());

        switchAutoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesManager.getInstance().setAutoDetect(isChecked);
        });
        switchSyncBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesManager.getInstance().setSyncBrightness(isChecked);
        });

        return view;
    }
}