package com.kynl.ledcube.manager;


import android.graphics.Color;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EffectManager {
    private final String TAG = "EffectManager";
    private final Gson gson = new Gson();
    private static EffectManager instance;
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

    public void init() {
        if (!readOldEffectList() || !readOldEffectType()) {
            createDefaultList();
        }
    }

    private void createDefaultEffectList() {
        effectItemList = new ArrayList<>();

        int defaultColor = Color.parseColor("#61CFEE");

        // RGB
        List<OptionItem> rgbOptionItemList = new ArrayList<>();
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.DIRECTION));
        rgbOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.RGB, rgbOptionItemList));

        // Gravity
        List<OptionItem> gravityOptionItemList = new ArrayList<>();
        gravityOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        gravityOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR, defaultColor, false));
        effectItemList.add(new EffectItem(EffectItem.EffectType.GRAVITY, gravityOptionItemList));

        // Music
        List<OptionItem> musicOptionItemList = new ArrayList<>();
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS, 20, 1, 100));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.MODE, 1, 1, 3));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR, defaultColor, false));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.SATURATION, 100, 0, 100));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.DEVIATION, 0, 0, 100));
        musicOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY, 30, 1, 100));
        effectItemList.add(new EffectItem(EffectItem.EffectType.MUSIC, musicOptionItemList));

        // Wave
        List<OptionItem> waveOptionItemList = new ArrayList<>();
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR, defaultColor, false));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        waveOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.WAVE, waveOptionItemList));

        // Flash
        List<OptionItem> flashOptionItemList = new ArrayList<>();
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.BRIGHTNESS));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.COLOR, defaultColor, false));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.SPEED));
        flashOptionItemList.add(new OptionItem(OptionItem.OptionType.SENSITIVITY));
        effectItemList.add(new EffectItem(EffectItem.EffectType.FLASH, flashOptionItemList));


        setCurrentEffectType(EffectItem.EffectType.RGB);
    }

    public void createDefaultList() {
        createDefaultEffectList();
        saveEffectList();

        setCurrentEffectType(EffectItem.EffectType.OFF);
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

    public void setOptionValue(EffectItem.EffectType effectType, OptionItem.OptionType optionType, int value) {
        if (effectType == EffectItem.EffectType.OFF) {
            return;
        }
        // find effectType position
        int effectTypePosition = findEffectTypePosition(effectType);
        if (effectTypePosition < 0) {
            Log.e(TAG, "setOptionValue: Can not find position of effect " + effectType);
            return;
        }
        // find optionType position
        List<OptionItem> optionItemList = effectItemList.get(effectTypePosition).getOptionItemList();
        int optionTypePosition = findOptionTypePosition(optionItemList, optionType);
        if (optionTypePosition < 0) {
            Log.e(TAG, "setOptionValue: Can not find position of option " + optionType);
            return;
        }
        // set value
        optionItemList.get(optionTypePosition).setValue(value);
        saveEffectList();
    }

    public void synchronizeAllEffects(OptionItem.OptionType optionType, int value) {
        for (int i = 0; i < effectItemList.size(); i++) {
            List<OptionItem> optionItemList = effectItemList.get(i).getOptionItemList();
            int position = findOptionTypePosition(optionItemList, optionType);
            if (position >= 0) {
                optionItemList.get(position).setValue(value);
            }
        }
        saveEffectList();
    }

    public String getEffectDataAsJson(EffectItem.EffectType type) {
        if (type == EffectItem.EffectType.OFF) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", EffectItem.EffectType.OFF.ordinal());
                return jsonObject.toString();
            } catch (Exception e) {
                Log.e(TAG, "getEffectDataAsJson: Error while converting to json");
                return "";
            }
        }

        int position = findEffectTypePosition(type);
        if (position < 0) {
            Log.e(TAG, "getEffectDataAsJson: Can not find position of " + type);
            return "";
        }
        try {
            List<OptionItem> optionItemList = effectItemList.get(position).getOptionItemList();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", effectItemList.get(position).getType().ordinal());
            for (OptionItem item : optionItemList) {
                jsonObject.put(item.getText().toLowerCase(), item.getValue());
            }
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getEffectDataAsJson: Error while converting to json");
            return "";
        }
    }

    public String getCurrentEffectDataAsJson() {
        EffectItem.EffectType effectType = getCurrentEffectType();
        return getEffectDataAsJson(effectType);
    }

    private int findEffectTypePosition(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if (effectItemList.get(i).getType() == type) {
                return i;
            }
        }
        return -1;
    }

    private int findOptionTypePosition(List<OptionItem> optionItemList, OptionItem.OptionType optionType) {
        for (int i = 0; i < optionItemList.size(); i++) {
            if (optionItemList.get(i).getType() == optionType) {
                return i;
            }
        }
        return -1;
    }

    private String getEffectItemListAsString() {
        return gson.toJson(effectItemList);
    }

    private List<EffectItem> convertStringToEffectList(String jsonString) {
        try {
            Type type = new TypeToken<List<EffectItem>>() {
            }.getType();
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean readOldEffectType() {
        String currentEffectTypeStr = SharedPreferencesManager.getInstance().getCurrentEffectType();
        if (!currentEffectTypeStr.isEmpty()) {
            try {
                currentEffectType = EffectItem.EffectType.valueOf(currentEffectTypeStr);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void saveEffectType() {
        SharedPreferencesManager.getInstance().setCurrentEffectType(String.valueOf(currentEffectType));
        Log.d(TAG, "saveEffectType: currentEffectType[" + currentEffectType + "]");
    }

    private boolean readOldEffectList() {
        String str = SharedPreferencesManager.getInstance().getEffectItemList();
        if (!str.isEmpty()) {
            try {
                effectItemList = convertStringToEffectList(str);
                if (effectItemList != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void saveEffectList() {
        SharedPreferencesManager.getInstance().setEffectItemList(getEffectItemListAsString());
    }
}
