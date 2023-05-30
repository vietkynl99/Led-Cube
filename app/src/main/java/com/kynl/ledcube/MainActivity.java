package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.kynl.ledcube.manager.ServerManager;

import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;
import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_PENDING_PAIR;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final String serverAddress = "http://192.168.10.102";
    private ServerManager.ServerState serverState;
    private ServerManager.ConnectionState connectionState;

    private Button pairDeviceBtn;
    private Button checkConnectionBtn;

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
        checkConnectionBtn = findViewById(R.id.checkConnectionBtn);

        /* Pair Button */
        pairDeviceBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: pair button clicked");
            ServerManager.getInstance().sendPairRequest();
        });

        /* Check connection button */
        checkConnectionBtn.setOnClickListener(v -> {
            Log.i(TAG, "onCreate: check connection");
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
        pairDeviceBtn.setText(getResources().getString(connectionState == CONNECTION_STATE_PENDING_PAIR ?
                R.string.pairing_device : R.string.pair_device));
        checkConnectionBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
    }

}