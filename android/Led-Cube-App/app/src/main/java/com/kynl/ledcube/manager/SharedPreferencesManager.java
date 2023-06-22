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
//    private boolean autoDetect = autoDetectDefault;
//    private boolean syncBrightness = syncBrightnessDefault;

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
        readLastScanInformation();
    }

    /* Common */
    public void saveBoolean(String key, boolean value) {
        if (context == null) {
            Log.e(TAG, "saveBoolean: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean readBoolean(String key, boolean defaultValue) {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return defaultValue;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }

    public void restoreDefaultSettings() {
        setAutoDetect(autoDetectDefault);
        setSyncBrightness(syncBrightnessDefault);
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
        return readBoolean("autoDetect", autoDetectDefault);
    }

    public void setAutoDetect(boolean autoDetect) {
        saveBoolean("autoDetect", autoDetect);
    }

    public boolean isSyncBrightness() {
        return readBoolean("syncBrightness", syncBrightnessDefault);
    }

    public void setSyncBrightness(boolean syncBrightness) {
        saveBoolean("syncBrightness", syncBrightness);
    }

    /* ServerManager */
    public void saveSynced(boolean synced) {
        if (context == null) {
            Log.e(TAG, "saveSynced: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("synced", synced);
        editor.apply();
    }

    public boolean readOldSynced() {
        if (context == null) {
            Log.e(TAG, "readOldSynced: Context is null");
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean("synced", false);
    }
}
