package com.kynl.ledcube.nettool;

import java.net.InetAddress;

public class Device {
    public String ip;
    public String hostname;
    public String mac;
    public float time = 0;

    public Device(InetAddress ip) {
        this.ip = ip.getHostAddress();
        this.hostname = ip.getCanonicalHostName();
    }

    @Override
    public String toString() {
        return "Device{" +
                "ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", mac='" + mac + '\'' +
                ", time=" + time +
                '}';
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

