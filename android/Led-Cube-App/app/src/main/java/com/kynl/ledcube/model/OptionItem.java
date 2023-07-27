package com.kynl.ledcube.model;

import com.kynl.ledcube.R;

public class OptionItem {
    public enum OptionType {
        BRIGHTNESS,
        COLOR,
        MODE,
        DEVIATION,
        SPEED,
        DIRECTION,
        SENSITIVITY
    }

    private final int defaultMinValue = 1;
    private final int defaultMaxValue = 100;
    private final OptionType type;
    private int value;
    private int minValue;
    private int maxValue;

    public OptionItem(OptionType type) {
        this.type = type;
        this.minValue = defaultMinValue;
        this.maxValue = defaultMaxValue;
        this.value = minValue;
    }

    public OptionItem(OptionType type, int value, int minValue, int maxValue) {
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        if (value >= minValue && value <= maxValue) {
            this.value = value;
        } else {
            this.value = minValue;
        }
    }

    public OptionType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (value >= minValue && value <= maxValue) {
            this.value = value;
        } else {
            this.value = minValue;
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
