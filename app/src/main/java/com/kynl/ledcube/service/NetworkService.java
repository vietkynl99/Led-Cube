package com.kynl.ledcube.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kynl.ledcube.manager.NetworkDeviceDiscovery;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";

    public NetworkService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(TAG, "onCreate: create service");

        NetworkDeviceDiscovery networkDeviceDiscovery = new NetworkDeviceDiscovery(getApplicationContext());
        networkDeviceDiscovery.discoverDevices();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}