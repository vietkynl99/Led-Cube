package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.service.NetworkService;

import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;

public class MainActivity extends AppCompatActivity  {
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
//            ServerManager.getInstance().sendPairRequest();

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

//        requestAppPermission();

        // Start Socket service
        Log.i(TAG, "onCreate: Start service");
        Intent intent = new Intent(this, NetworkService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy: ");
        // Stop service
        Intent intent = new Intent(this, NetworkService.class);
        stopService(intent);
    }

//    private void requestAppPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
//        }
//    }

    private void updateButtonState() {
        pairDeviceBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
//        pairDeviceBtn.setText(getResources().getString(connectionState == CONNECTION_STATE_PENDING_PAIR ?
//                R.string.pairing_device : R.string.pair_device));
        refreshBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
    }








}