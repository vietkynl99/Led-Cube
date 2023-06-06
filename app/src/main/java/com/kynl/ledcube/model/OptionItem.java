package com.kynl.ledcube.model;

public class OptionItem {
    private int iconId;
    private String text;

    public OptionItem(int iconId, String text) {
        this.iconId = iconId;
        this.text = text;
    }

    public int getIconId() {
        return iconId;
    }

    public String getText() {
        return text;
    }
}
