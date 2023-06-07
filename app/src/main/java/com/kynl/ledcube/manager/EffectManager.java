package com.kynl.ledcube.manager;

import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;

import java.util.ArrayList;
import java.util.List;

public class EffectManager {
    private static EffectManager instance;
    private List<EffectItem> effectItemList;

    private EffectManager() {
    }

    public static synchronized EffectManager getInstance() {
        if (instance == null) {
            instance = new EffectManager();
        }
        return instance;
    }

    public void init() {
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

    public List<EffectItem> getEffectElementList() {
        return effectItemList;
    }

    public List<OptionItem> getOptionItemList(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if(effectItemList.get(i).getType() == type) {
                return effectItemList.get(i).getOptionItemList();
            }
        }
        return null;
    }
}
