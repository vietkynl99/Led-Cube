package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManager";
    private static SharedPreferencesManager instance;
    private Context context;
    private static final boolean syncedDefault = false;
    private static final boolean autoDetectDefault = true;
    private static final boolean syncBrightnessDefault = true;

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

    public void saveString(String key, String value) {
        if (context == null) {
            Log.e(TAG, "saveBoolean: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String readString(String key, String defaultValue) {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return defaultValue;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    public void restoreDefaultSettings() {
        setAutoDetect(autoDetectDefault);
        setSyncBrightness(syncBrightnessDefault);
    }

    /* Search Fragment */
    public String getLastScanTime() {
        return readString("lastScanTime", "");
    }

    public void setLastScanTime(String lastScanTime) {
        saveString("lastScanTime", lastScanTime);
    }

    public String getLastScanDevicesList() {
        return readString("lastScanDevicesList", "");
    }

    public void setLastScanDevicesList(String lastScanDevicesList) {
        saveString("lastScanDevicesList", lastScanDevicesList);
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
    public void setSynced(boolean synced) {
        saveBoolean("synced", synced);
    }

    public boolean isSynced() {
        return readBoolean("synced", syncedDefault);
    }
}
