package com.kynl.ledcube.model;

import com.kynl.ledcube.R;

import java.util.List;

public class EffectItem {
    public enum EffectType {
        OFF,
        RGB,
        MUSIC,
        WAVE,
        FLASH,
        GRAVITY,
        SNAKE,
        EFFECT_MAX
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

    public int getIconId() {
        switch (type) {
            case RGB:
                return R.drawable.effect_rgb_64;
            case MUSIC:
                return R.drawable.effect_music_52;
            case WAVE:
                return R.drawable.effect_wave_50;
            case FLASH:
                return R.drawable.effect_lightning_60;
            case GRAVITY:
                return R.drawable.effect_gravity_64;
            case SNAKE:
                return R.drawable.effect_snake_50;
            default:
                return -1;
        }
    }

    public boolean useOriginIconColor() {
        return type == EffectType.RGB;
    }
}
