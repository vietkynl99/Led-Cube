package com.kynl.ledcube.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.net.InetAddress;

public class Device implements Serializable {
    public String ip;
    public String hostname;
    public String mac;
    public int ping;

    public Device(InetAddress ip) {
        this.ip = ip.getHostAddress();
        this.hostname = ip.getCanonicalHostName();
        this.mac = "";
        this.ping = 0;
    }

    public Device(String ip, String mac) {
        this.ip = ip;
        this.mac = mac;
        this.hostname = "";
        this.ping = 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "Device{ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", mac='" + mac + '\'' + ", ping='" + ping + '\'' + '}';
    }

    public boolean isValid() {
        return ip != null && mac != null && !ip.isEmpty() && !mac.isEmpty() &&
                !ip.endsWith(".1") && !mac.equals("FAILED");
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

    public int getPing() {
        return ping;
    }
}

