package com.kynl.ledcube.model;


import androidx.annotation.NonNull;

public class ServerData {
    private int batteryLevel;

    public ServerData(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    @NonNull
    @Override
    public String toString() {
        return "batteryLevel[" + String.valueOf(batteryLevel) + "]";
    }
}
