package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;

import java.util.ArrayList;
import java.util.List;

public class EffectManager {
    private final String TAG = "EffectManager";
    private static EffectManager instance;
    private Context context;
    private List<EffectItem> effectItemList;
    private EffectItem.EffectType currentEffectType;

    private EffectManager() {
    }

    public static synchronized EffectManager getInstance() {
        if (instance == null) {
            instance = new EffectManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;

        // default value
        currentEffectType = EffectItem.EffectType.RGB;

        // Read old value from SHARED PREFERENCES
        readOldEffectType();

        effectItemList = new ArrayList<>();

        // RGB
        List<OptionItem> rgbOptionItemList = new ArrayList<>();
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.DIRECTION));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.RGB, rgbOptionItemList));

        // Music
        List<OptionItem> musicOptionItemList = new ArrayList<>();
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.DIRECTION));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.MUSIC, musicOptionItemList));

        // Wave
        List<OptionItem> waveOptionItemList = new ArrayList<>();
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.WAVE, waveOptionItemList));

        // Flash
        List<OptionItem> flashOptionItemList = new ArrayList<>();
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.FLASH, flashOptionItemList));
    }

    public List<EffectItem> getEffectItemList() {
        return effectItemList;
    }

    public EffectItem.EffectType getCurrentEffectType() {
        return currentEffectType;
    }

    public void setCurrentEffectType(EffectItem.EffectType currentEffectType) {
        if (this.currentEffectType != currentEffectType) {
            this.currentEffectType = currentEffectType;
            saveEffectType();
        }
    }

    private void readOldEffectType() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String currentEffectTypeStr = prefs.getString("currentEffectType", "");
        if (!currentEffectTypeStr.isEmpty()) {
            try {
                currentEffectType = EffectItem.EffectType.valueOf(currentEffectTypeStr);
            } catch (Exception ignored) {
            }
        }

        Log.i(TAG, "readOldEffectType: currentEffectType[" + currentEffectType + "]");
    }

    private void saveEffectType() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentEffectType", currentEffectType.toString());
        editor.apply();

        Log.d(TAG, "saveEffectType: currentEffectType[" + currentEffectType + "]");
    }
}
