package com.kynl.ledcube.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.net.InetAddress;

public class Device implements Serializable {
    public String ip;
    public String mac;
    public String ping;

    public Device(InetAddress ip) {
        this.ip = ip.getHostAddress();
        this.mac = "";
        this.ping = "";
    }

    public Device(String ip, String mac, String ping) {
        this.ip = ip;
        this.mac = mac;
        this.ping = ping;
    }

    @NonNull
    @Override
    public String toString() {
        return "Device{ip='" + ip + '\'' + ", mac='" + mac + '\'' + ", ping='" + ping + '\'' + '}';
    }

    public boolean isValid() {
        return !ip.isEmpty() && !mac.isEmpty() && !ip.endsWith(".1") && !mac.equals("FAILED");
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getPing() {
        return ping;
    }
}

