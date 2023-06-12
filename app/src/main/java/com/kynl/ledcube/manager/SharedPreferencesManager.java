package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManager";
    private static SharedPreferencesManager instance;
    private Context context;
    // Search
    private String lastScanTime = "";
    private String lastScanDevicesList = "";
    // Settings
    private static final boolean autoDetectDefault = true;
    private static final boolean syncBrightnessDefault = true;
    private boolean autoDetect = autoDetectDefault;
    private boolean syncBrightness = syncBrightnessDefault;

    private SharedPreferencesManager() {
    }

    public static synchronized SharedPreferencesManager getInstance() {
        if (instance == null) {
            instance = new SharedPreferencesManager();
        }
        return instance;
    }

    public void init(Context context) {
        Log.i(TAG, "init: ");

        this.context = context.getApplicationContext();
        readOldSettings();
        readLastScanInformation();
    }

    /* Search Fragment */
    public String getLastScanTime() {
        return lastScanTime;
    }

    public void setLastScanTime(String lastScanTime) {
        this.lastScanTime = lastScanTime;
        saveLastScanInformation();
    }

    public String getLastScanDevicesList() {
        return lastScanDevicesList;
    }

    public void setLastScanDevicesList(String lastScanDevicesList) {
        this.lastScanDevicesList = lastScanDevicesList;
        saveLastScanInformation();
    }

    private void readLastScanInformation() {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        lastScanTime = prefs.getString("lastScanTime", "");
        lastScanDevicesList = prefs.getString("lastScanDevicesList", "");
    }

    private void saveLastScanInformation() {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastScanTime", lastScanTime);
        editor.putString("lastScanDevicesList", lastScanDevicesList);
        editor.apply();

        Log.i(TAG, "saveLastScanInformation: lastScanTime[" + lastScanTime + "]");
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
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("autoDetect", autoDetect);
        editor.putBoolean("syncBrightness", syncBrightness);
        editor.apply();
    }

    public void readOldSettings() {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        autoDetect = prefs.getBoolean("autoDetect", autoDetectDefault);
        syncBrightness = prefs.getBoolean("syncBrightness", syncBrightnessDefault);
    }
}
