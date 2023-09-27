package com.kynl.ledcube.manager;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.model.ServerMessage;
import com.kynl.ledcube.myinterface.OnServerDataChangeListener;
import com.kynl.ledcube.myinterface.OnServerResponseListener;
import com.kynl.ledcube.nettool.SocketClient;
import com.kynl.ledcube.nettool.SubnetDevices;

import com.kynl.ledcube.common.CommonUtils.ServerState;

import static com.kynl.ledcube.common.CommonUtils.HTTP_FORMAT;
import static com.kynl.ledcube.common.CommonUtils.MDNS_SERVER_DOMAIN;
import static com.kynl.ledcube.common.CommonUtils.MDNS_SUPPORT_ANDROID_VERSION;
import static com.kynl.ledcube.common.CommonUtils.WEBSOCKET_URL;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_CHECK_CONNECTION;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_SEND_DATA;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

public class ServerManager {
    private final String TAG = "ServerManager";
    private final ReentrantLock lock = new ReentrantLock();
    private String ipAddress, macAddress;
    private int apiKey = 0;
    private static ServerManager instance;
    private Context context;
    private RequestQueue requestQueue;
    private ServerState serverState;
    private boolean busy;
    private OnServerResponseListener onServerResponseListener;
    private OnServerDataChangeListener onServerDataChangeListener;
    private SubnetDevices subnetDevices;
    private SubnetDevices.OnSubnetDeviceFound onSubnetDeviceFoundListener;
    private boolean isFindingSubnetDevices;
    private String savedIpAddress, savedMacAddress;
    private boolean synced;
    private SocketClient socketClient;
    private Timer reconnectTimer;

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
        serverState = ServerState.SERVER_STATE_DISCONNECTED;
        busy = false;
        onServerResponseListener = null;
        ipAddress = "";
        macAddress = "";
        subnetDevices = null;
        isFindingSubnetDevices = false;
        savedIpAddress = SharedPreferencesManager.getInstance().getSavedIpAddress();
        savedMacAddress = SharedPreferencesManager.getInstance().getSavedMacAddress();
        synced = SharedPreferencesManager.getInstance().isSynced();
        apiKey = SharedPreferencesManager.getInstance().getApiKey();
        Log.e(TAG, "init: apiKey = " + apiKey);

        createSocketConnection();
    }

    private void createSocketConnection() {
        Log.i(TAG, "createNewSocketConnection: ");

        try {
            socketClient = new SocketClient(WEBSOCKET_URL);
        } catch (URISyntaxException e) {
            Log.e(TAG, "init: Cannot resolve websocket url: " + WEBSOCKET_URL);
        }
        socketClient.setOnSocketStateChangeListener(connected -> {
            Log.e(TAG, "createNewSocketConnection: connected: " + connected);
            if (!connected) {
                reconnectTimer = new Timer();
                reconnectTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "run: reconnect");
                            socketClient.reconnect();
                        }catch (Exception e) {
                            Log.e(TAG, "run: Cannot reconnect" );
                            e.printStackTrace();
                        }
                    }
                }, 5000);
            }
        });
        socketClient.setOnMessageReceivedListener(message -> {
            ServerMessage serverMessage = new ServerMessage(message);
            if (serverMessage.isValidResponseMessage()) {
                handleResponseFromServer(false, "", serverMessage);
            } else {
                handleResponseFromServer(true, "Invalid response data", new ServerMessage());
            }
        });
        Log.i(TAG, "init: Connecting to " + WEBSOCKET_URL);

        socketClient.connect();
    }

    public boolean isSupportMDNS() {
        return Build.VERSION.SDK_INT >= MDNS_SUPPORT_ANDROID_VERSION;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        if (this.synced != synced) {
            this.synced = synced;
            SharedPreferencesManager.getInstance().setSynced(synced);
        }
    }

    public boolean isBusy() {
        return busy;
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
        return isSupportMDNS() || (!savedIpAddress.isEmpty() && !savedMacAddress.isEmpty());
    }

    public void saveDevice(String ip, String mac) {
        if (!ip.isEmpty() && !mac.isEmpty()) {
            if (!ip.equals(savedIpAddress) || !mac.equals(savedMacAddress)) {
                savedIpAddress = ip;
                savedMacAddress = mac;
                SharedPreferencesManager.getInstance().setSavedIpAddress(savedIpAddress);
                SharedPreferencesManager.getInstance().setSavedMacAddress(savedMacAddress);
            }
        }
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

    public void setOnServerResponseListener(OnServerResponseListener onServerResponseListener) {
        this.onServerResponseListener = onServerResponseListener;
    }

    public void setOnServerDataChangeListener(OnServerDataChangeListener onServerDataChangeListener) {
        this.onServerDataChangeListener = onServerDataChangeListener;
    }

    private void sendRequestToServer(ServerMessage sentData) {
        Log.d(TAG, "sendRequestToServer: " + sentData.toJson());
        if (socketClient != null) {
            if (!socketClient.isOpen()) {
                Log.e(TAG, "sendRequestToServer: Socket is not opened!");
                return;
            }
            socketClient.send(sentData.toJson());
        }
//        lock.lock();
//        Log.d(TAG, "sendRequestToServer: key[" + sentData.getKey() + "] type[" + sentData.getType() + "] data[" + sentData.getData() + "]");
//        if (context == null) {
//            Log.e(TAG, "sendRequestToServer: Context is null!");
//            return;
//        }
//        if (!isSupportMDNS() && ipAddress.isEmpty()) {
//            Log.e(TAG, "sendRequestToServer: Error! IP Address is empty");
//            return;
//        }
//        if (busy) {
//            Log.e(TAG, "sendRequestToServer: Server is busy. serverState: " + serverState);
//            return;
//        }
//        busy = true;
//        String serverAddress = isSupportMDNS() ? HTTP_FORMAT + MDNS_SERVER_DOMAIN : HTTP_FORMAT + ipAddress;
//        String url = Uri.parse(serverAddress)
//                .buildUpon()
//                .appendQueryParameter("key", sentData.getKeyAsString())
//                .appendQueryParameter("type", sentData.getTypeAsString())
//                .appendQueryParameter("data", sentData.getData())
//                .build()
//                .toString();
//
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                response -> {
//                    Log.i(TAG, "onResponse: " + response);
//                    ServerMessage message = new ServerMessage(response);
//                    if (message.isValidResponseMessage()) {
//                        handleResponseFromServer(false, "", message);
//                    } else {
//                        handleResponseFromServer(true, "Invalid response data", new ServerMessage());
//                    }
//                },
//                error -> {
//                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Can not connect to server " + ipAddress;
//                    handleResponseFromServer(true, errorMessage, new ServerMessage());
//                });
//
//        requestQueue.add(stringRequest);
//        lock.unlock();
    }

    private void handleResponseFromServer(boolean isError, String errorMessage, ServerMessage receivedData) {
        lock.lock();
        String message = "";
        if (isError) {
            message = errorMessage;
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
                        message = "The device has been paired";
                    } else {
                        message = "The connection is successful, but the device is not paired";
                    }
                    Log.i(TAG, "handleResponseFromServer: " + message);
                    break;
                }
                // State is connected
                case EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL: {
                    try {
                        JSONObject jsonObject = new JSONObject(receivedData.getData());
                        String apiKeyStr = jsonObject.getString("apiKey");
                        apiKey = Integer.parseInt(apiKeyStr);
                        serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                        SharedPreferencesManager.getInstance().setApiKey(apiKey);
                        message = "Paired successfully";
                        Log.i(TAG, "handleResponseFromServer: Paired -> apiKey = " + apiKey);
                    } catch (Exception e) {
                        serverState = ServerState.SERVER_STATE_DISCONNECTED;
                        message = "Error while pairing";
                        Log.e(TAG, "handleResponseFromServer: Invalid string: " + receivedData.getData());
                    }
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_PAIRED: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    message = "The device has been paired";
                    Log.i(TAG, "handleResponseFromServer: " + message);
                    break;
                }
                case EVENT_RESPONSE_GET_DATA_SUCCESSFUL: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    break;
                }
                case EVENT_RESPONSE_UPDATE_DATA: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    String data = receivedData.getData();
                    if (data == null) {
                        Log.e(TAG, "handleResponseFromServer: data is null");
                    } else {
                        notifyServerDataChanged(data);
                    }
                    break;
                }
                // State is disconnected
                case EVENT_RESPONSE_INVALID_KEY: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    message = "Device is not paired";
                    Log.i(TAG, "handleResponseFromServer: " + message);
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_IGNORED: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    message = "Request is ignored by server";
                    Log.i(TAG, "handleResponseFromServer: " + message);
                    break;
                }
                default: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.e(TAG, "handleResponseFromServer: Unhandled event type: " + receivedData.getType());
                    break;
                }
            }
        }
//        busy = false;

        if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
            saveDevice(ipAddress, macAddress);
        }

        notifyServerResponse(serverState, message);

        lock.unlock();
    }

    private void notifyServerResponse(ServerState serverState, String message) {
        if (onServerResponseListener != null) {
            onServerResponseListener.onServerResponse(serverState, message);
        }
    }

    private void notifyServerDataChanged(String serverData) {
        if (onServerDataChangeListener != null) {
            onServerDataChangeListener.onServerDataChanged(serverData);
        }
    }

    public void setOnSubnetDeviceFoundListener(SubnetDevices.OnSubnetDeviceFound onSubnetDeviceFoundListener) {
        this.onSubnetDeviceFoundListener = onSubnetDeviceFoundListener;
    }

    public void findSubnetDevices() {
        lock.lock();
        if (onSubnetDeviceFoundListener == null) {
            Log.e(TAG, "findSubnetDevices: Error! Listener is null");
            return;
        }
        if (busy) {
            Log.e(TAG, "findSubnetDevices: Server is busy. serverState: " + serverState);
            return;
        }
        Log.d(TAG, "findSubnetDevices: Started!");
        busy = true;
        try {
            subnetDevices = SubnetDevices.fromLocalAddress().findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                @Override
                public void onDeviceFound(Device device) {
                    if (onSubnetDeviceFoundListener != null) {
                        onSubnetDeviceFoundListener.onDeviceFound(device);
                    }
                }

                @Override
                public void onProcessed(int processed, int total) {
                    if (onSubnetDeviceFoundListener != null) {
                        onSubnetDeviceFoundListener.onProcessed(processed, total);
                    }
                }

                @Override
                public void onFinished(ArrayList<Device> devicesFound) {
                    busy = false;
                    if (onSubnetDeviceFoundListener != null) {
                        onSubnetDeviceFoundListener.onFinished(devicesFound);
                    }
                }
            });
        } catch (IllegalAccessError e) {
            Log.e(TAG, "findSubnetDevices: Error " + e.getMessage());
            busy = false;
        }
        lock.unlock();
    }

    public void cancelFindSubnetDevices() {
        lock.lock();
        if (subnetDevices != null) {
            if (isFindingSubnetDevices) {
                Log.i(TAG, "stopFindSubnetDevices: Canceled!");
                subnetDevices.cancel();
            }
        }
        busy = false;
        lock.unlock();
    }
}
