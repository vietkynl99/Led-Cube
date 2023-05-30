package com.kynl.ledcube.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kynl.ledcube.model.ServerMessage;
import com.kynl.ledcube.myinterface.OnServerStatusChangedListener;

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
    private final String serverAddress = "http://192.168.10.101";
    private int apiKey = 0;
    private static ServerManager instance;
    private Context context;
    private RequestQueue requestQueue;
    private ServerState serverState;
    private ConnectionState connectionState;
    private OnServerStatusChangedListener onServerStatusChangedListener;

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
    }

    public ServerState getServerState() {
        return serverState;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
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
        Log.e(TAG, "sendRequestToServer: key[" + sentData.getKey() + "] type[" + sentData.getType() + "] data[" + sentData.getData() + "]");
        if (context == null) {
            Log.e(TAG, "sendRequestToServer: Context is null!");
            return;
        }
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
                    Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Error while sending request to server!";
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
            connectionState = ConnectionState.CONNECTION_STATE_NONE;
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "getResponseFromServer: type[" + receivedData.getType() + "] data[" + receivedData.getData() + "]");
            switch (receivedData.getType()) {
                case EVENT_RESPONSE_CHECK_CONNECTION: {
                    serverState = receivedData.getData().equals("1") ? ServerState.SERVER_STATE_CONNECTED_AND_PAIRED : ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED;
                    if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                        Log.e(TAG, "getResponseFromServer: The device has been paired.");
                        Toast.makeText(context, "The device has been paired.", Toast.LENGTH_SHORT).show();
                    } else if (serverState == ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED) {
                        Log.e(TAG, "getResponseFromServer: The connection is successful, but the device is not paired.");
                        Toast.makeText(context, "The connection is successful, but the device is not paired.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_PAIRED: {
                    serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                    Log.e(TAG, "getResponseFromServer: The device has been paired.");
                    Toast.makeText(context, "The device has been paired.", Toast.LENGTH_SHORT).show();
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_IGNORED: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Toast.makeText(context, "Pair request is ignored!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL: {
                    try {
                        apiKey = Integer.parseInt(receivedData.getData());
                        serverState = ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
                        saveOldSetting();
                        Log.i(TAG, "getResponseFromServer: Paired -> apiKey = " + apiKey);
                        Toast.makeText(context, "Device paired successfully", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        serverState = ServerState.SERVER_STATE_DISCONNECTED;
                        Log.e(TAG, "getResponseFromServer: Invalid apiKey string: " + receivedData.getData());
                        Toast.makeText(context, "Device pairing failed due to invalid key", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                default: {
                    serverState = ServerState.SERVER_STATE_DISCONNECTED;
                    Log.e(TAG, "getResponseFromServer: Unhandled event type: " + receivedData.getType());
                    break;
                }
            }

            connectionState = ConnectionState.CONNECTION_STATE_NONE;
        }

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
}
