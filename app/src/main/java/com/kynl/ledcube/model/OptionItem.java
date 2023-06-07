package com.kynl.ledcube.model;

public class OptionItem {
    private int iconId;
    private String text;
    private boolean collapse;

    public OptionItem(int iconId, String text) {
        this.iconId = iconId;
        this.text = text;
        this.collapse = true;
    }

    public int getIconId() {
        return iconId;
    }

    public String getText() {
        return text;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCollapse() {
        return collapse;
    }

    public void setCollapse(boolean collapse) {
        this.collapse = collapse;
    }
}
