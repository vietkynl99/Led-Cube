package com.kynl.ledcube.model;

import com.kynl.ledcube.R;

public class OptionItem {
    public enum OptionType {
        BRIGHTNESS,
        COLOR,
        SPEED,
        DIRECTION,
        SENSITIVITY
    }

    private final OptionType type;
    private int value;

    public OptionItem(OptionType type) {
        this.type = type;
        this.value = 0;
    }

    public OptionItem(OptionType type, int value) {
        this.type = type;
        this.value = value;
    }

    public OptionType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getIconId() {
        return R.drawable.brightness_48;
    }

    public String getText() {
        switch (type) {
            case BRIGHTNESS:
                return "Brightness";
            case COLOR:
                return "Color";
            case SPEED:
                return "Speed";
            case DIRECTION:
                return "Direction";
            case SENSITIVITY:
                return "Sensitivity";
            default:
                return "";
        }
    }

}
