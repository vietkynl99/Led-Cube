package com.kynl.ledcube.common;

public class CommonUtils {
    // Shared Preferences
    public static final String SHARED_PREFERENCES = "SHARED_PREFERENCES";

    // Broadcast
    public static final String BROADCAST_ACTION = "BROADCAST_ACTION";
    public static final String BROADCAST_SERVICE_ADD_SUBNET_DEVICE = "BROADCAST_SERVICE_ADD_SUBNET_DEVICE";
    public static final String BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE = "BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE";
    public static final String BROADCAST_SERVICE_SERVER_RESPONSE = "BROADCAST_SERVICE_SERVER_RESPONSE";
    public static final String BROADCAST_SERVICE_STATE_CHANGED = "BROADCAST_SERVICE_STATE_CHANGED";
    public static final String BROADCAST_SERVICE_UPDATE_STATUS = "BROADCAST_SERVICE_UPDATE_STATUS";
    public static final String BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS = "BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS";
    public static final String BROADCAST_SERVICE_UPDATE_SERVER_DATA = "BROADCAST_SERVICE_UPDATE_SERVER_DATA";
    public static final String BROADCAST_SERVICE_NOTIFY_MESSAGE = "BROADCAST_SERVICE_NOTIFY_MESSAGE";
    public static final String BROADCAST_REQUEST_FIND_SUBNET_DEVICE = "BROADCAST_REQUEST_FIND_SUBNET_DEVICE";
    public static final String BROADCAST_REQUEST_PAIR_DEVICE = "BROADCAST_REQUEST_PAIR_DEVICE";
    public static final String BROADCAST_REQUEST_UPDATE_STATUS = "BROADCAST_REQUEST_UPDATE_STATUS";
    public static final String BROADCAST_REQUEST_PAUSE_NETWORK_SCAN = "BROADCAST_REQUEST_PAUSE_NETWORK_SCAN";
    public static final String BROADCAST_REQUEST_SEND_DATA = "BROADCAST_REQUEST_SEND_DATA";
    public static final String BROADCAST_REQUEST_CHANGE_TO_HOME_SCREEN = "BROADCAST_REQUEST_CHANGE_TO_HOME_SCREEN";
    public static final String BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS = "BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS";

    // mDNS support Android version
    public static final int MDNS_SUPPORT_ANDROID_VERSION = 31;

    // Server device name
    public static final String SERVER_DEVICE_NAME = "ledCube";
    public static final String WEBSOCKET_URL = "ws://ledCube.local:51308/ws";
    public static final int AUTO_CONNECT_TIMEOUT = 5000;

    // Date
    public static final String LAST_SCAN_DATE_TIME_FORMAT = "HH:mm:ss dd/MM/yyyy";

    // Service settings
    public static final int DEVICES_LIST_RESCAN_TIMEOUT = 60 * 60 * 1000;

    // Service
    public enum NetworkServiceState {
        STATE_NONE,
        STATE_TRY_TO_CONNECT_DEVICE,
        STATE_FIND_SUBNET_DEVICES,
        STATE_PAIR_DEVICE,
        STATE_SEND_DATA
    }

    // Server Manager
    public enum ServerState {
        SERVER_STATE_DISCONNECTED,
        SERVER_STATE_CONNECTED_BUT_NOT_PAIRED,
        SERVER_STATE_CONNECTED_AND_PAIRED
    }
}

