package com.kynl.ledcube.model;

import com.kynl.ledcube.common.CommonUtils;

import org.json.JSONObject;

import static android.provider.Settings.Global.DEVICE_NAME;
import static com.kynl.ledcube.common.CommonUtils.SERVER_DEVICE_NAME;

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

    private String name;
    private int key;
    private EventType type;
    private String data;

    public ServerMessage() {
        this.name = "";
        this.key = 0;
        this.type = EventType.EVENT_NONE;
        this.data = "";
    }

    public ServerMessage(int key, EventType type, String data) {
        this.name = "";
        this.key = key;
        this.type = type;
        this.data = data;
    }

    public ServerMessage(String jsonString) {
        this.name = "";
        this.key = 0;
        this.type = EventType.EVENT_NONE;
        this.data = "";

        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (Exception e) {
            return;
        }
        try {
            this.name = json.getString("name");
        } catch (Exception ignored) {
        }
        try {
            this.key = json.getInt("key");
        } catch (Exception ignored) {
        }
        try {
            int intValue = json.getInt("type");
            this.type = EventType.values()[intValue];
        } catch (Exception ignored) {
        }
        try {
            this.data = json.getString("data");
        } catch (Exception ignored) {
        }
    }

    public String getName() {
        return name;
    }

    public int getKey() {
        return key;
    }

    public String getKeyAsString() {
        return String.valueOf(key);
    }

    public EventType getType() {
        return type;
    }

    public String getTypeAsString() {
        return String.valueOf(type.ordinal());
    }

    public String getData() {
        return data;
    }

    public boolean isValidResponseMessage() {
        return name.equals(SERVER_DEVICE_NAME) && type != EventType.EVENT_NONE && type != EventType.EVENT_TYPE_MAX && !data.isEmpty();
    }
}
