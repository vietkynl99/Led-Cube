package com.kynl.ledcube.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kynl.ledcube.model.ServerMessage;
import com.kynl.ledcube.myinterface.OnServerStatusChangedListener;
import com.kynl.ledcube.nettool.SubnetDevices;

import static com.kynl.ledcube.common.CommonUtils.HTTP_FORMAT;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_CHECK_CONNECTION;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_PAIR_DEVICE;

public class ServerManager {
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

    private final String TAG = "ServerManager";
    private String ipAddress, macAddress;
    private int apiKey = 0;
    private static ServerManager instance;
    private Context context;
    private RequestQueue requestQueue;
    private ServerState serverState;
    private ConnectionState connectionState;
    private OnServerStatusChangedListener onServerStatusChangedListener;
    SubnetDevices subnetDevices;
    private SubnetDevices.OnSubnetDeviceFound onSubnetDeviceFoundListener;
    private boolean isFindingSubnetDevices;

    private ServerManager() {
    }

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public void init(Context context) {
        Log.i(TAG, "init: ");
        this.context = context;
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context);
        }
        readOldSetting();
        serverState = ServerState.SERVER_STATE_DISCONNECTED;
        connectionState = ConnectionState.CONNECTION_STATE_NONE;
        onServerStatusChangedListener = null;
        ipAddress = "";
        macAddress = "";
        subnetDevices = null;
        isFindingSubnetDevices = false;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void sendCheckConnectionRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_CHECK_CONNECTION, ""));
    }

    public void sendPairRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_PAIR_DEVICE, ""));
    }

    public void setOnServerStatusChangedListener(OnServerStatusChangedListener onServerStatusChangedListener) {
        this.onServerStatusChangedListener = onServerStatusChangedListener;
    }

    private void sendRequestToServer(ServerMessage sentData) {
        Log.d(TAG, "sendRequestToServer: key[" + sentData.getKey() + "] type[" + sentData.getType() + "] data[" + sentData.getData() + "]");
        if (context == null) {
            Log.e(TAG, "sendRequestToServer: Context is null!");
            return;
        }
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "sendRequestToServer: Error! IP Address is empty");
            return;
        }
        String serverAddress = HTTP_FORMAT + ipAddress;
        String url = Uri.parse(serverAddress)
                .buildUpon()
                .appendQueryParameter("key", sentData.getKeyAsString())
                .appendQueryParameter("type", sentData.getTypeAsString())
                .appendQueryParameter("data", sentData.getData())
                .build()
                .toString();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.i(TAG, "onResponse: " + response);
                    ServerMessage message = new ServerMessage(response);
                    if (message.isValidResponseMessage()) {
                        getResponseFromServer(false, "", message);
                    } else {
                        getResponseFromServer(true, "Invalid response data.", new ServerMessage());
                    }
                },
                error -> {
//                    Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Can not connect to server " + ipAddress;
                    getResponseFromServer(true, errorMessage, new ServerMessage());
                });

        connectionState = sentData.getType() == EVENT_REQUEST_PAIR_DEVICE ? ConnectionState.CONNECTION_STATE_PENDING_PAIR :
                ConnectionState.CONNECTION_STATE_PENDING_REQUEST;
        notifyServerStatusChanged();
        requestQueue.add(stringRequest);
    }

    private void getResponseFromServer(boolean isError, String errorMessage, ServerMessage receivedData) {
        if (isError) {
            Log.e(TAG, "getResponseFromServer: get error: " + errorMessage);
            serverState = ServerState.SERVER_STATE_DISCONNECTED;
        } else {
            Log.i(TAG, "getResponseFromServer: type[" + receivedData.getType() + "] data[" + receivedData.getData() + "]");
            switch (receivedData.getType()) {
                case EVENT_RESPONSE_CHECK_CONNECTION: {
                    serverState = receivedData.getData().equals("1") ? ServerState.SERVER_STATE_CONNECTED_AND_PAIRED : ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                        Log.i(TAG, "getResponseFromServer: The device has been paired.");
                    } else {
                        Log.i(TAG, "getResponseFromServer: The connection is successful, but the device is not paired.");
                    }
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_PAIRED: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    Log.i(TAG, "getResponseFromServer: The device has been paired.");
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_IGNORED: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL: {
                    try {
                        apiKey = Integer.parseInt(receivedData.getData());
                        serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                        saveOldSetting();
                        Log.i(TAG, "getResponseFromServer: Paired -> apiKey = " + apiKey);
                    } catch (NumberFormatException e) {
                        serverState = ServerState.SERVER_STATE_DISCONNECTED;
                        Log.e(TAG, "getResponseFromServer: Invalid apiKey string: " + receivedData.getData());
                    }
                    break;
                }
                default: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.e(TAG, "getResponseFromServer: Unhandled event type: " + receivedData.getType());
                    break;
                }
            }
        }
        connectionState = ConnectionState.CONNECTION_STATE_NONE;

        notifyServerStatusChanged();
    }

    private void notifyServerStatusChanged() {
        if (onServerStatusChangedListener != null) {
            onServerStatusChangedListener.onServerStateChanged(serverState, connectionState);
        }
    }

    private void saveOldSetting() {
        // server address
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("apiKey", apiKey);
        editor.apply();
    }

    private void readOldSetting() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        apiKey = prefs.getInt("apiKey", 0);

        Log.e(TAG, "readOldSetting: apiKey = " + apiKey);
    }

    public void setOnSubnetDeviceFoundListener(SubnetDevices.OnSubnetDeviceFound onSubnetDeviceFoundListener) {
        this.onSubnetDeviceFoundListener = onSubnetDeviceFoundListener;
    }

    public void findSubnetDevices() {
        if (onSubnetDeviceFoundListener == null) {
            Log.e(TAG, "findSubnetDevices: Error! Listener is null");
            return;
        }
        Log.d(TAG, "findSubnetDevices: Started!");
        subnetDevices = SubnetDevices.fromLocalAddress().findDevices(onSubnetDeviceFoundListener);
    }

    public void cancelFindSubnetDevices() {
        if (subnetDevices != null) {
            if (isFindingSubnetDevices) {
                Log.i(TAG, "stopFindSubnetDevices: Canceled!");
                subnetDevices.cancel();
            }
        }
    }
}
