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

    public OptionItem(OptionType type) {
        this.type = type;
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
