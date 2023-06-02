package com.kynl.ledcube.model;

import java.io.Serializable;
import java.net.InetAddress;

public class Device implements Serializable {
    public String ip;
    public String hostname;
    public String mac;
    public float time;

    public Device(InetAddress ip) {
        this.ip = ip.getHostAddress();
        this.hostname = ip.getCanonicalHostName();
        this.mac = "";
        this.time = 0;
    }

    public Device(String ip, String mac) {
        this.ip = ip;
        this.mac = mac;
        this.hostname = "";
        this.time = 0;
    }

    @Override
    public String toString() {
        return "Device{ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", mac='" + mac + '\'' + '}';
    }

    public String getIp() {
        return ip;
    }

    public String getHostname() {
        return hostname;
    }

    public String getMac() {
        return mac;
    }

    public float getTime() {
        return time;
    }
}

