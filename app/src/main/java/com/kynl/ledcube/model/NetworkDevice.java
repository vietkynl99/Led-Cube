package com.kynl.ledcube.model;

public class NetworkDevice {
    private int type;
    private String name;
    private String ip;

    public NetworkDevice(int type, String name, String ip) {
        this.type = type;
        this.name = name;
        this.ip = ip;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }
}
