package com.kynl.ledcube.model;

public class EffectItem {
    private String name;
    private int iconId;
    private int highlightIconId;

    public EffectItem(String name, int iconId, int highlightIconId) {
        this.name = name;
        this.iconId = iconId;
        this.highlightIconId = highlightIconId;
    }

    public String getName() {
        return name;
    }

    public int getIconId() {
        return iconId;
    }

    public int getHighlightIconId() {
        return highlightIconId;
    }
}
