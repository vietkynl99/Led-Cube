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

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
