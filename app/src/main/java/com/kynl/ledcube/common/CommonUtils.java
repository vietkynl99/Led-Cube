package com.kynl.ledcube.common;

public class CommonUtils {
    // Shared Preferences
    public static final String SHARED_PREFERENCES = "SHARED_PREFERENCES";

    // Broadcast
    public static final String BROADCAST_ACTION = "BROADCAST_ACTION";
    public static final String BROADCAST_SERVICE_ADD_SUBNET_DEVICE = "BROADCAST_SERVICE_ADD_SUBNET_DEVICE";
    public static final String BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE = "BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE";
    public static final String BROADCAST_SERVICE_SERVER_STATUS_CHANGED = "BROADCAST_SERVICE_SERVER_STATUS_CHANGED";
    public static final String BROADCAST_SERVICE_STATE_CHANGED = "BROADCAST_SERVICE_STATE_CHANGED";
    public static final String BROADCAST_SERVICE_UPDATE_STATUS = "BROADCAST_SERVICE_UPDATE_STATUS";
    public static final String BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS = "BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS";
    public static final String BROADCAST_REQUEST_FIND_SUBNET_DEVICE = "BROADCAST_REQUEST_FIND_SUBNET_DEVICE";
    public static final String BROADCAST_REQUEST_CONNECT_DEVICE = "BROADCAST_REQUEST_CONNECT_DEVICE";
    public static final String BROADCAST_REQUEST_PAIR_DEVICE = "BROADCAST_REQUEST_PAIR_DEVICE";
    public static final String BROADCAST_REQUEST_UPDATE_STATUS = "BROADCAST_REQUEST_UPDATE_STATUS";
    public static final String BROADCAST_REQUEST_PAUSE_NETWORK_SCAN = "BROADCAST_REQUEST_PAUSE_NETWORK_SCAN";
    public static final String BROADCAST_REQUEST_SEND_DATA = "BROADCAST_REQUEST_SEND_DATA";

    // Server device name
    public static final String SERVER_DEVICE_NAME = "ledCube";
    public static final String HTTP_FORMAT = "http://";

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

    public enum ConnectionState {
        CONNECTION_STATE_NONE,
        CONNECTION_STATE_PENDING_PAIR,
        CONNECTION_STATE_PENDING_REQUEST
    }

}

