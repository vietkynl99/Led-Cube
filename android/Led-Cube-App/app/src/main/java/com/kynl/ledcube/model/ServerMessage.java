package com.kynl.ledcube.model;

import org.json.JSONException;
import org.json.JSONObject;

import static com.kynl.ledcube.common.CommonUtils.SERVER_DEVICE_NAME;

import com.google.gson.Gson;

public class ServerMessage {

    // Server Event
    public enum EventType {
        EVENT_NONE(100),
        /* Request from App to Server */
        EVENT_REQUEST_CHECK_CONNECTION(200),
        EVENT_REQUEST_PAIR_DEVICE(201),
        EVENT_REQUEST_SEND_DATA(202),
        /* Response from Server to App */
        EVENT_RESPONSE_CHECK_CONNECTION(300),
        EVENT_RESPONSE_INVALID_KEY(301),
        EVENT_RESPONSE_PAIR_DEVICE_IGNORED(302),
        EVENT_RESPONSE_PAIR_DEVICE_PAIRED(303),
        EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL(304),
        EVENT_RESPONSE_GET_DATA_SUCCESSFUL(305),
        EVENT_RESPONSE_UPDATE_DATA(306);

        private final int value;

        EventType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static EventType fromValue(int value) {
            for (EventType e : EventType.values()) {
                if (e.getValue() == value) {
                    return e;
                }
            }
            return EVENT_NONE;
        }
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
            this.type = EventType.fromValue(intValue);
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
        return String.valueOf(type.getValue());
    }

    public String getData() {
        return data;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", key);
            jsonObject.put("type", type.getValue());
            jsonObject.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public boolean isValidResponseMessage() {
        return name.equals(SERVER_DEVICE_NAME) && type != EventType.EVENT_NONE;
    }
}
