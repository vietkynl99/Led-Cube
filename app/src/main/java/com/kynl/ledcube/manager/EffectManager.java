package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
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

        // Effect list
        if (!readOldEffectList()) {
            createDefaultEffectList();
            saveEffectList();
        }

        // Current effect type
        if (!readOldEffectType()) {
            currentEffectType = EffectItem.EffectType.RGB;
            saveEffectType();
        }
    }

    public void createDefaultEffectList() {
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

    public void setOptionValue(EffectItem.EffectType effectType, OptionItem.OptionType optionType, int value) {
        // find effectType position
        int effectTypePosition = findEffectTypePosition(effectType);
        if (effectTypePosition < 0) {
            Log.e(TAG, "setOptionValue: Can not find position of effect " + effectType);
            return;
        }
        // find optionType position
        List<OptionItem> optionItemList = effectItemList.get(effectTypePosition).getOptionItemList();
        int optionTypePosition = -1;
        for (int i = 0; i < optionItemList.size(); i++) {
            if (optionItemList.get(i).getType() == optionType) {
                optionTypePosition = i;
            }
        }
        if (optionTypePosition < 0) {
            Log.e(TAG, "setOptionValue: Can not find position of option " + optionType);
            return;
        }
        // set value
        optionItemList.get(optionTypePosition).setValue(value);
        saveEffectList();
    }

    public String getEffectDataAsJson(EffectItem.EffectType type) {
        int position = findEffectTypePosition(type);
        if (position < 0) {
            Log.e(TAG, "getEffectDataAsJson: Can not find position of " + type);
            return "";
        }
        try {
            List<OptionItem> optionItemList = effectItemList.get(position).getOptionItemList();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", String.valueOf(effectItemList.get(position).getType()));
            for (OptionItem item : optionItemList) {
                jsonObject.put(item.getText(), item.getValue());
            }
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getEffectDataAsJson: Error while converting to json");
            return "";
        }
    }

    private int findEffectTypePosition(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if (effectItemList.get(i).getType() == type) {
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
        Log.i(TAG, "readOldEffectType: ");
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String currentEffectTypeStr = prefs.getString("currentEffectType", "");
        if (!currentEffectTypeStr.isEmpty()) {
            try {
                currentEffectType = EffectItem.EffectType.valueOf(currentEffectTypeStr);
                return true;
            } catch (Exception ignored) {
            }
        }

        Log.i(TAG, "readOldEffectType: currentEffectType[" + currentEffectType + "]");
        return false;
    }

    private void saveEffectType() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentEffectType", String.valueOf(currentEffectType));
        editor.apply();

        Log.d(TAG, "saveEffectType: currentEffectType[" + currentEffectType + "]");
    }

    private boolean readOldEffectList() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String effectItemListStr = prefs.getString("effectItemList", "");
        if (!effectItemListStr.isEmpty()) {
            try {
                effectItemList = convertStringToEffectList(effectItemListStr);
                if (effectItemList != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void saveEffectList() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("effectItemList", getEffectItemListAsString());
        editor.apply();
    }
}
