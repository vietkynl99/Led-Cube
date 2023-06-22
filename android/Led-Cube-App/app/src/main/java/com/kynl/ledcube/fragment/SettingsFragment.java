package com.kynl.ledcube.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.SharedPreferencesManager;

public class SettingsFragment extends Fragment {
    private final String TAG = "SettingsFragment";
    private SwitchCompat switchAutoDetect, switchSyncBrightness;

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
        switchAutoDetect = view.findViewById(R.id.switchAutoDetect);
        switchSyncBrightness = view.findViewById(R.id.switchSyncBrightness);

        /* Back button */
        backBtn.setOnClickListener(v -> BroadcastManager.getInstance().sendRequestChangeToHomeScreen());

        /* Settings switch */
        switchAutoDetect.setChecked(SharedPreferencesManager.getInstance().isAutoDetect());
        switchSyncBrightness.setChecked(SharedPreferencesManager.getInstance().isSyncBrightness());

        switchAutoDetect.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPreferencesManager.getInstance().setAutoDetect(isChecked));
        switchSyncBrightness.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPreferencesManager.getInstance().setSyncBrightness(isChecked));

        /* Restore default settings */
        ViewGroup view_restore_settings = view.findViewById(R.id.view_restore_settings);
        view_restore_settings.setOnClickListener(v -> openDialog());

        return view;
    }

    private void openDialog() {
        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_view);
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.CENTER;
        window.setAttributes(layoutParams);
        dialog.setCancelable(true);

        TextView btn_ok = dialog.findViewById(R.id.btn_ok);
        TextView btn_cancel = dialog.findViewById(R.id.btn_cancel);
        if (btn_ok != null) {
            btn_ok.setOnClickListener(v -> {
                dialog.dismiss();
                restoreDefaultSettings();
            });
        }
        if (btn_cancel != null) {
            btn_cancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void setDefault() {
        SharedPreferencesManager.getInstance().restoreDefaultSettings();
        if (switchAutoDetect != null) {
            switchAutoDetect.setChecked(SharedPreferencesManager.getInstance().isAutoDetect());
        }
        if (switchSyncBrightness != null) {
            switchSyncBrightness.setChecked(SharedPreferencesManager.getInstance().isSyncBrightness());
        }
    }

    private void restoreDefaultSettings() {
        Log.d(TAG, "restoreDefaultSettings: ");
        setDefault();
        // Restore settings in other activity/fragment
        BroadcastManager.getInstance().sendRequestRestoreDefaultSettings();
    }
}