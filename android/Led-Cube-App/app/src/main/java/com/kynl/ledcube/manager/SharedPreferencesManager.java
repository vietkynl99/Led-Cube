package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.LAST_SCAN_DATE_TIME_FORMAT;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManager";
    private static SharedPreferencesManager instance;
    private Context context;
    private static final boolean syncedDefault = false;
    private static final boolean autoDetectDefault = true;
    private static final boolean syncBrightnessDefault = true;
    private static final int apiKeyDefault = 0;

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
    private void saveBoolean(String key, boolean value) {
        if (context == null) {
            Log.e(TAG, "saveBoolean: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return defaultValue;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }

    private void saveString(String key, String value) {
        if (context == null) {
            Log.e(TAG, "saveBoolean: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String readString(String key, String defaultValue) {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return defaultValue;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    private void saveInt(String key, int value) {
        if (context == null) {
            Log.e(TAG, "saveBoolean: Context is null");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private int readInt(String key, int defaultValue) {
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return defaultValue;
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getInt(key, defaultValue);
    }

    public void restoreDefaultSettings() {
        setAutoDetect(autoDetectDefault);
        setSyncBrightness(syncBrightnessDefault);
    }

    /* Search Fragment */
    public String getLastScanTimeString() {
        return readString("lastScanTime", "");
    }
     public Date getLastScanTime() {
         String lastScanTime = getLastScanTimeString();
         if (!lastScanTime.isEmpty()) {
             SimpleDateFormat formatter = new SimpleDateFormat(LAST_SCAN_DATE_TIME_FORMAT, Locale.US);
             try {
                 Date date = formatter.parse(lastScanTime);
                 if (date != null) {
                     return date;
                 }
             } catch (ParseException ignored) {
             }
         }
         return null;
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

    public void setSavedIpAddress(String savedIpAddress) {
        saveString("savedIpAddress", savedIpAddress);
    }

    public String getSavedIpAddress() {
        return readString("savedIpAddress", "");
    }

    public void setSavedMacAddress(String savedMacAddress) {
        saveString("savedMacAddress", savedMacAddress);
    }

    public String getSavedMacAddress() {
        return readString("savedMacAddress", "");
    }

    public void setApiKey(int apiKey) {
        saveInt("apiKey", apiKey);
    }

    public int getApiKey() {
        return readInt("apiKey", apiKeyDefault);
    }

    /* Effect Manager */
    public String getCurrentEffectType() {
        return readString("currentEffectType", "");
    }

    public void setCurrentEffectType(String currentEffectType) {
        saveString("currentEffectType", currentEffectType);
    }

    public String getEffectItemList() {
        return readString("effectItemList", "");
    }

    public void setEffectItemList(String effectItemList) {
        saveString("effectItemList", effectItemList);
    }

}
