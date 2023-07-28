package com.kynl.ledcube.model;

import com.kynl.ledcube.R;

public class OptionItem {
    public enum OptionType {
        BRIGHTNESS,
        MODE,
        COLOR,
        SATURATION,
        DEVIATION,
        SPEED,
        DIRECTION,
        SENSITIVITY
    }

    private final OptionType type;
    private int value;
    private final int minValue;
    private final int maxValue;
    private final boolean enableMinMax;

    public OptionItem(OptionType type) {
        this.enableMinMax = true;
        this.type = type;
        this.minValue = 1;
        this.maxValue = 100;
        this.value = minValue;
    }

    public OptionItem(OptionType type, int value, int minValue, int maxValue) {
        this.enableMinMax = true;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        if (value >= minValue && value <= maxValue) {
            this.value = value;
        } else {
            this.value = minValue;
        }
    }

    public OptionItem(OptionType type, int value, boolean enableMinMax) {
        this.enableMinMax = enableMinMax;
        this.type = type;
        this.minValue = 1;
        this.maxValue = 100;
        if (enableMinMax) {
            if (value >= minValue && value <= maxValue) {
                this.value = value;
            } else {
                this.value = minValue;
            }
        } else {
            this.value = value;
        }
    }

    public OptionType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (enableMinMax) {
            if (value >= minValue && value <= maxValue) {
                this.value = value;
            } else {
                this.value = minValue;
            }
        } else {
            this.value = value;
        }
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getIconId() {
        return R.drawable.brightness_48;
    }

    public String getText() {
        String text = type.toString();
        return text.toLowerCase().substring(0, 1).toUpperCase() + text.toLowerCase().substring(1);
    }

}
