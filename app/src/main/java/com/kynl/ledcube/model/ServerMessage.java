package com.kynl.ledcube.model;

import com.kynl.ledcube.common.CommonUtils;

import org.json.JSONObject;

public class ServerMessage {

    // Server Event
    public enum EventType {
        EVENT_NONE,
        /* Request */
        EVENT_REQUEST_CHECK_CONNECTION,
        EVENT_REQUEST_PAIR_DEVICE,
        /* Response */
        EVENT_RESPONSE_CHECK_CONNECTION,
        EVENT_RESPONSE_PAIR_DEVICE_IGNORED,
        EVENT_RESPONSE_PAIR_DEVICE_PAIRED,
        EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL,
        /* Max */
        EVENT_TYPE_MAX
    }

    private int key;
    private EventType type;
    private String data;

    public ServerMessage() {
        this.key = 0;
        this.type = EventType.EVENT_NONE;
        this.data = "";
    }

    public ServerMessage(int key, EventType type, String data) {
        this.key = key;
        this.type = type;
        this.data = data;
    }

    public ServerMessage(String jsonString) {
        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (Exception e) {
            this.key = 0;
            this.type = EventType.EVENT_NONE;
            this.data = "";
            return;
        }
        // key
        try {
            this.key = json.getInt("key");
        } catch (Exception e) {
            this.key = 0;
        }
        // type
        try {
            int intValue = json.getInt("type");
            this.type = EventType.values()[intValue];
        } catch (Exception e) {
            this.type = EventType.EVENT_NONE;
        }
        // data
        try {
            this.data = json.getString("data");
        } catch (Exception e) {
            this.data = "";
        }
    }

    public int getKey() {
        return key;
    }

    public String getKeyAsString() {
        return String.valueOf(key);
    }

    public void setKey(int key) {
        this.key = key;
    }

    public EventType getType() {
        return type;
    }

    public String getTypeAsString() {
        return String.valueOf(type.ordinal());
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
