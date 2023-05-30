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

import org.json.JSONException;
import org.json.JSONObject;

import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_CHECK_CONNECTION;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.model.ServerMessage.EventType.EVENT_RESPONSE_CHECK_CONNECTION;

public class ServerManager {
    private final String SAVED_PREFERENCES = "SAVED_PREFERENCES";
    private final String TAG = "ServerManager";
    private final String serverAddress = "http://192.168.10.102";
    private int apiKey = 0;
    private static ServerManager instance;
    private Context context;
    private RequestQueue requestQueue;
    private boolean isPaired;


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
        isPaired = false;
    }

    public void sendRequest(ServerMessage.EventType type, String data) {
        sendRequestToServer(new ServerMessage(apiKey, type, data));
    }

    public void sendCheckConnectionRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_CHECK_CONNECTION, ""));
    }

    public void sendPairRequest() {
        sendRequestToServer(new ServerMessage(apiKey, EVENT_REQUEST_PAIR_DEVICE, ""));
    }

    private void sendRequestToServer(ServerMessage sentData) {
        Log.d(TAG, "sendRequestToServer: key[" + sentData.getKey() + "] type[" + sentData.getType() + "] data[" + sentData.getData() + "]");
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
                    getResponseFromServer("", sentData, new ServerMessage(response));
                },
                error -> {
                    Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Error while sending request to server!";
                    getResponseFromServer(errorMessage, sentData, new ServerMessage());
                });

        requestQueue.add(stringRequest);
    }

    private void getResponseFromServer(String errorMessage, ServerMessage sentData, ServerMessage receivedData) {
        if (!errorMessage.isEmpty()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "getResponseFromServer: type = " + receivedData.getType());
            switch (receivedData.getType()) {
                case EVENT_RESPONSE_CHECK_CONNECTION: {
                    isPaired = receivedData.getData().equals("1");
                    if (isPaired) {
                        Log.e(TAG, "getResponseFromServer: The device has been paired.");
                        Toast.makeText(context, "The device has been paired.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "getResponseFromServer: The connection is successful, but the device is not paired.");
                        Toast.makeText(context, "The connection is successful, but the device is not paired.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_PAIRED: {
                    isPaired = true;
                    Log.e(TAG, "getResponseFromServer: The device has been paired.");
                    Toast.makeText(context, "The device has been paired.", Toast.LENGTH_SHORT).show();
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_IGNORED: {
                    Toast.makeText(context, "Pair request is ignored!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case EVENT_RESPONSE_PAIR_DEVICE_SUCCESSFUL: {
                    try {
                        apiKey = Integer.parseInt(receivedData.getData());
                        saveOldSetting();
                        Log.i(TAG, "getResponseFromServer: Paired -> apiKey = " + apiKey);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "getResponseFromServer: Invalid apiKey string: " + receivedData.getData());
                    }
                    break;
                }
                default: {
                    Log.e(TAG, "getResponseFromServer: Unhandled event type: " + receivedData.getType());
                    break;
                }
            }
        }
    }

    private void saveOldSetting() {
        // server address
        SharedPreferences prefs = context.getSharedPreferences(SAVED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("apiKey", apiKey);
        editor.apply();
    }

    private void readOldSetting() {
        SharedPreferences prefs = context.getSharedPreferences(SAVED_PREFERENCES, Context.MODE_PRIVATE);
        apiKey = prefs.getInt("apiKey", 0);

        Log.e(TAG, "readOldSetting: apiKey = " + apiKey);
    }
}
