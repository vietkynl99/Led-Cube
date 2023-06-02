package com.kynl.ledcube.model;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return "Device{ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", mac='" + mac + '\'' + '}';
    }

    public boolean isValid() {
        return ip != null && mac != null && !ip.isEmpty() && !mac.isEmpty() && !ip.endsWith(".1");
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

