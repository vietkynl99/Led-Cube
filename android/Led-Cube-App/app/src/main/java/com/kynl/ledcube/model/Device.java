package com.kynl.ledcube.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class Device implements Serializable {
    private String ip;
    private String mac;
    private String ping;
    private DeviceState deviceState;

    public enum DeviceState {
        STATE_NONE,
        STATE_CONNECTING,
        STATE_CONNECTED_BUT_NOT_PAIRED,
        STATE_CONNECTED_AND_PAIRED
    }

    public Device(InetAddress ip) {
        this.ip = ip.getHostAddress();
        this.mac = "";
        this.ping = "";
        this.deviceState = DeviceState.STATE_NONE;
    }

    public Device(String ip, String mac, String ping) {
        this.ip = ip;
        this.mac = mac;
        this.ping = ping;
        this.deviceState = DeviceState.STATE_NONE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Device)) return false;
        Device other = (Device) obj;
        return ip.equals(other.ip) && mac.equals(other.mac) && ping.equals(other.ping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac, ping);
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

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public DeviceState getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(DeviceState deviceState) {
        this.deviceState = deviceState;
    }
}

