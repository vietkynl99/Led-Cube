package com.kynl.ledcube.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kynl.ledcube.model.ServerData;
import com.kynl.ledcube.model.ServerMessage;
import com.kynl.ledcube.myinterface.OnServerDataChangeListener;
import com.kynl.ledcube.myinterface.OnServerStatusChangedListener;
import com.kynl.ledcube.nettool.SubnetDevices;

import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.common.CommonUtils.ConnectionState;

import static com.kynl.ledcube.common.CommonUtils.HTTP_FORMAT;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_CHECK_CONNECTION;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_SEND_DATA;

import org.json.JSONObject;

public class ServerManager {
    private final String TAG = "ServerManager";
    private String ipAddress, macAddress;
    private int apiKey = 0;
    private static ServerManager instance;
    private Context context;
    private RequestQueue requestQueue;
    private ServerState serverState;
    private ConnectionState connectionState;
    private OnServerStatusChangedListener onServerStatusChangedListener;
    private OnServerDataChangeListener onServerDataChangeListener;
    SubnetDevices subnetDevices;
    private SubnetDevices.OnSubnetDeviceFound onSubnetDeviceFoundListener;
    private boolean isFindingSubnetDevices;
    private String savedIpAddress, savedMacAddress;

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
        readApiKey();
        serverState = ServerState.SERVER_STATE_DISCONNECTED;
        connectionState = ConnectionState.CONNECTION_STATE_NONE;
        onServerStatusChangedListener = null;
        ipAddress = "";
        macAddress = "";
        subnetDevices = null;
        isFindingSubnetDevices = false;
        savedIpAddress = "";
        savedMacAddress = "";

        readSavedDeviceInformation();
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

    public String getSavedIpAddress() {
        return savedIpAddress;
    }

    public String getSavedMacAddress() {
        return savedMacAddress;
    }

    public boolean hasSavedDevice() {
        return !savedIpAddress.isEmpty() && !savedMacAddress.isEmpty();
    }

    public void saveDevice(String ip, String mac) {
        if (!ip.isEmpty() && !mac.isEmpty()) {
            if (!ip.equals(savedIpAddress) || !mac.equals(savedMacAddress)) {
                savedIpAddress = ip;
                savedMacAddress = mac;
                saveDeviceInformation();
            }
        }
    }

    private void saveDeviceInformation() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedIpAddress", savedIpAddress);
        editor.putString("savedMacAddress", savedMacAddress);
        editor.apply();

        Log.e(TAG, "saveDeviceInformation: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }

    private void readSavedDeviceInformation() {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        savedIpAddress = prefs.getString("savedIpAddress", "");
        savedMacAddress = prefs.getString("savedMacAddress", "");

        Log.e(TAG, "readDeviceInformation: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }

    public void sendCheckConnectionRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_CHECK_CONNECTION, ""));
    }

    public void sendPairRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_PAIR_DEVICE, ""));
    }

    public void sendData(String data) {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_SEND_DATA, data));
    }

    public void setOnServerStatusChangedListener(OnServerStatusChangedListener onServerStatusChangedListener) {
        this.onServerStatusChangedListener = onServerStatusChangedListener;
    }

    public void setOnServerDataChangeListener(OnServerDataChangeListener onServerDataChangeListener) {
        this.onServerDataChangeListener = onServerDataChangeListener;
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
                        handleResponseFromServer(false, "", message);
                    } else {
                        handleResponseFromServer(true, "Invalid response data", new ServerMessage());
                    }
                },
                error -> {
//                    Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Can not connect to server " + ipAddress;
                    handleResponseFromServer(true, errorMessage, new ServerMessage());
                });

        connectionState = sentData.getType() == EVENT_REQUEST_PAIR_DEVICE ? ConnectionState.CONNECTION_STATE_PENDING_PAIR :
                ConnectionState.CONNECTION_STATE_PENDING_REQUEST;
        notifyServerStatusChanged();
        requestQueue.add(stringRequest);
    }

    private void handleResponseFromServer(boolean isError, String errorMessage, ServerMessage receivedData) {
        if (isError) {
            Log.e(TAG, "handleResponseFromServer: get error: " + errorMessage);
            serverState = ServerState.SERVER_STATE_DISCONNECTED;
        } else {
            Log.i(TAG, "handleResponseFromServer: type[" + receivedData.getType() + "] data[" + receivedData.getData() + "]");
            switch (receivedData.getType()) {
                // State in data
                case EVENT_RESPONSE_CHECK_CONNECTION: {
                    try {
                        JSONObject jsonObject = new JSONObject(receivedData.getData());
                        String pair = jsonObject.getString("pair");
                        serverState = pair.equals("1") ? ServerState.SERVER_STATE_CONNECTED_AND_PAIRED :
                                ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    } catch (Exception ignored) {
                        serverState = ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    }

                    if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                        Log.i(TAG, "handleResponseFromServer: The device has been paired.");
                    } else {
                        Log.i(TAG, "handleResponseFromServer: The connection is successful, but the device is not paired.");
                    }
                    break;
                }
                // State is connected
                case EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL: {
                    try {
                        JSONObject jsonObject = new JSONObject(receivedData.getData());
                        String apiKeyStr = jsonObject.getString("apiKey");
                        apiKey = Integer.parseInt(apiKeyStr);
                        serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                        saveApiKey();
                        Log.i(TAG, "handleResponseFromServer: Paired -> apiKey = " + apiKey);
                    } catch (Exception e) {
                        serverState = ServerState.SERVER_STATE_DISCONNECTED;
                        Log.e(TAG, "handleResponseFromServer: Invalid string: " + receivedData.getData());
                    }
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_PAIRED: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    Log.i(TAG, "handleResponseFromServer: The device has been paired.");
                    break;
                }
                case EVENT_RESPONSE_GET_DATA_SUCCESSFUL: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    break;
                }
                case EVENT_RESPONSE_UPDATE_DATA: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    try {
                        JSONObject jsonObject = new JSONObject(receivedData.getData());
                        String batteryLevelStr = jsonObject.getString("batteryLevel");
                        int batteryLevel = Integer.parseInt(batteryLevelStr);
                        ServerData serverData = new ServerData(batteryLevel);
                        notifyServerDataChanged(serverData);
                    } catch (Exception e) {
                        Log.e(TAG, "handleResponseFromServer: Invalid string: " + receivedData.getData());
                    }
                    break;
                }
                // State is disconnected
                case EVENT_RESPONSE_INVALID_KEY: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.i(TAG, "handleResponseFromServer: Key is invalid");
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_IGNORED: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.i(TAG, "handleResponseFromServer: Request is ignored by server");
                    break;
                }
                default: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.e(TAG, "handleResponseFromServer: Unhandled event type: " + receivedData.getType());
                    break;
                }
            }
        }
        connectionState = ConnectionState.CONNECTION_STATE_NONE;

        if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
            saveDevice(ipAddress, macAddress);
        }

        notifyServerStatusChanged();
    }

    private void notifyServerStatusChanged() {
        if (onServerStatusChangedListener != null) {
            onServerStatusChangedListener.onServerStateChanged(serverState, connectionState);
        }
    }

    private void notifyServerDataChanged(ServerData serverData) {
        if (onServerDataChangeListener != null) {
            onServerDataChangeListener.onServerDataChanged(serverData);
        }
    }

    private void saveApiKey() {
        // server address
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("apiKey", apiKey);
        editor.apply();
    }

    private void readApiKey() {
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
        try {
            subnetDevices = SubnetDevices.fromLocalAddress().findDevices(onSubnetDeviceFoundListener);
        } catch (IllegalAccessError e) {
            Log.e(TAG, "findSubnetDevices: Error " + e.getMessage());
        }
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
