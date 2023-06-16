package com.kynl.ledcube.model;

import com.kynl.ledcube.R;

import java.util.List;

public class EffectItem {
    public enum EffectType {
        RGB,
        MUSIC,
        WAVE,
        FLASH
    }

    private final EffectType type;
    private final List<OptionItem> optionItemList;

    public EffectItem(EffectType type, List<OptionItem> optionItemList) {
        this.type = type;
        this.optionItemList = optionItemList;
    }

    public EffectType getType() {
        return type;
    }

    public List<OptionItem> getOptionItemList() {
        return optionItemList;
    }

    public int getIconId () {
        switch (type) {
            case RGB:
                return R.drawable.rgb_64;
            case MUSIC:
                return R.drawable.music_52;
            case WAVE:
                return R.drawable.wave_50;
            case FLASH:
                return R.drawable.lightning_60;
            default:
                return -1;
        }
    }

    public int getHighlightIconId () {
        switch (type) {
            case RGB:
                return R.drawable.rgb_hightlight_64;
            case MUSIC:
                return R.drawable.music_hightlight_52;
            case WAVE:
                return R.drawable.wave_hightlight_50;
            case FLASH:
                return R.drawable.lightning_hightlight_60;
            default:
                return -1;
        }
    }
}
