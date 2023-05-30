package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;

import com.kynl.ledcube.manager.ServerManager;

import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;
import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_PENDING_PAIR;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final String serverAddress = "http://192.168.10.102";
    private ServerManager.ServerState serverState;
    private ServerManager.ConnectionState connectionState;

    private ImageButton pairDeviceBtn, refreshBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ServerManager */
        ServerManager.getInstance().init(getApplicationContext());

        /* Variable */
        serverState = ServerManager.getInstance().getServerState();
        connectionState = ServerManager.getInstance().getConnectionState();

        /* Element */
        pairDeviceBtn = findViewById(R.id.pairDeviceBtn);
        refreshBtn = findViewById(R.id.refreshBtn);

        /* Pair Button */
        pairDeviceBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: pair button clicked");
            ServerManager.getInstance().sendPairRequest();
        });

        /* Check connection button */
        refreshBtn.setOnClickListener(v -> {
            Log.i(TAG, "onCreate: refresh button clicked");
            ServerManager.getInstance().sendCheckConnectionRequest();
        });

        /* Server state changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.i(TAG, "Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "]");
            this.serverState = serverState;
            this.connectionState = connectionState;
            updateButtonState();
        });
    }

    private void updateButtonState() {
        pairDeviceBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
//        pairDeviceBtn.setText(getResources().getString(connectionState == CONNECTION_STATE_PENDING_PAIR ?
//                R.string.pairing_device : R.string.pair_device));
        refreshBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
    }

}