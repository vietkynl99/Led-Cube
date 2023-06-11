package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesManager {
    private final String TAG = "SharedPreferencesManager";
    private static SharedPreferencesManager instance;
    private final Context context;
    // Settings
    private static final boolean autoDetectDefault = true;
    private static final boolean syncBrightnessDefault = true;
    private boolean autoDetect, syncBrightness;

    private SharedPreferencesManager(Context context) {
        this.context = context.getApplicationContext();
        readAllSavedData();
    }

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
        return instance;
    }

    private void readAllSavedData() {
        Log.i(TAG, "readAllSavedData: ");
        readOldSettings();
    }

    /* Settings */
    public boolean isAutoDetect() {
        return autoDetect;
    }

    public void setAutoDetect(boolean autoDetect) {
        this.autoDetect = autoDetect;
        saveOldSettings();
    }

    public boolean isSyncBrightness() {
        return syncBrightness;
    }

    public void setSyncBrightness(boolean syncBrightness) {
        this.syncBrightness = syncBrightness;
        saveOldSettings();
    }

    public void saveOldSettings() {
        // server address
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("autoDetect", autoDetect);
        editor.putBoolean("syncBrightness", syncBrightness);
        editor.apply();
    }

    public void readOldSettings() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        autoDetect = prefs.getBoolean("autoDetect", autoDetectDefault);
        syncBrightness = prefs.getBoolean("syncBrightness", syncBrightnessDefault);
    }
}
