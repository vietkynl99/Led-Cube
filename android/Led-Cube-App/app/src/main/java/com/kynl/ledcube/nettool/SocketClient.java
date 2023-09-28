package com.kynl.ledcube.nettool;

import android.util.Log;

import com.kynl.ledcube.myinterface.OnMessageReceivedListener;
import com.kynl.ledcube.myinterface.OnSocketStateChangeListener;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class SocketClient extends WebSocketClient {
    private static final String TAG = "SocketClient";
    private OnMessageReceivedListener onMessageReceivedListener;
    private OnSocketStateChangeListener onSocketStateChangeListener;

    public SocketClient(String serverUri) throws URISyntaxException {
        super(new URI(serverUri));
        Log.i(TAG, "SocketClient: init");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "onOpen: WebSocket connection opened");
        if (onSocketStateChangeListener != null) {
            onSocketStateChangeListener.OnSocketStateChanged(true);
        }
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG, "Received message: " + message);
        if (onMessageReceivedListener != null) {
            onMessageReceivedListener.OnMessageReceived(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(TAG, "WebSocket connection closed");
        if (onSocketStateChangeListener != null) {
            onSocketStateChangeListener.OnSocketStateChanged(false);
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "get error: " + ex.getMessage());
//        ex.printStackTrace();
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener onMessageReceivedListener) {
        this.onMessageReceivedListener = onMessageReceivedListener;
    }

    public void setOnSocketStateChangeListener(OnSocketStateChangeListener onSocketStateChangeListener) {
        this.onSocketStateChangeListener = onSocketStateChangeListener;
    }
}
