package com.kynl.ledcube.service;

import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;
import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;
import static com.kynl.ledcube.manager.ServerManager.ServerState.SERVER_STATE_DISCONNECTED;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.nettool.Device;
import com.kynl.ledcube.nettool.SubnetDevices;

import java.util.ArrayList;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    private final int RETRY_ON_STARTUP_MAX = 3;
    private String savedIpAddress, savedMacAddress;
    private ServerManager.ServerState serverState;
    private ServerManager.ConnectionState connectionState;
    private NetworkServiceState networkServiceState;
    private int retryCount;
    private boolean autoDetect;

    private enum NetworkServiceState {
        STATE_NONE,
        STATE_TRY_TO_CONNECT_DEVICE,
        STATE_FIND_SUBNET_DEVICES
    }

    public NetworkService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(TAG, "onCreate: create service");

        savedIpAddress = "";
        savedMacAddress = "";
        serverState = ServerManager.getInstance().getServerState();
        connectionState = ServerManager.getInstance().getConnectionState();
        networkServiceState = NetworkServiceState.STATE_NONE;
        retryCount = 0;
        autoDetect = true;

//        saveSharedPreferences();

        readSharedPreferences();

        /* ServerManager */
        ServerManager.getInstance().init(getApplicationContext());

        /* Server status changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.i(TAG, "Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "] networkServiceState[" + networkServiceState + "]");
            this.serverState = serverState;
            this.connectionState = connectionState;
            // Sent request is done
            if (connectionState == CONNECTION_STATE_NONE) {
                switch (networkServiceState) {
                    case STATE_TRY_TO_CONNECT_DEVICE: {
                        if (serverState == SERVER_STATE_DISCONNECTED) {
                            // Cannot connect to server
                            retryCount++;
                            if (retryCount >= RETRY_ON_STARTUP_MAX) {
                                Log.i(TAG, "Server status changed: Cannot connect on startup (retry: " + retryCount + " ) -> Stop retry");
                                networkServiceState = NetworkServiceState.STATE_NONE;
                            } else {
                                Log.i(TAG, "Server status changed: Cannot connect on startup (retry: " + retryCount + " ) -> Continue retry");
                                ServerManager.getInstance().sendCheckConnectionRequest();
                            }
                        } else {
                            // Able to connect to server
                            networkServiceState = NetworkServiceState.STATE_NONE;
                            String ipAddress = ServerManager.getInstance().getIpAddress();
                            String macAddress = ServerManager.getInstance().getMacAddress();
                            if (!ipAddress.equals(savedIpAddress) || !macAddress.equals(savedMacAddress)) {
                                savedIpAddress = ipAddress;
                                savedMacAddress = macAddress;
                                saveSharedPreferences();
                            }
                        }
                        break;
                    }
//                    case STATE_FIND_SUBNET_DEVICES: {
//                        break;
//                    }
//                    case STATE_NONE: {
//                        break;
//                    }
                    default: {
                        Log.e(TAG, "Server status changed: Unhandled event networkServiceState=" + networkServiceState);
                        break;
                    }
                }
            }
        });

        /* Found subnet devices */
        ServerManager.getInstance().setOnSubnetDeviceFoundListener(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.e(TAG, "onDeviceFound: " + device.time + " " + device.ip + " " + device.mac + " " + device.hostname);
            }

            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                Log.e(TAG, "onFinished: Found " + devicesFound.size());
                if (autoDetect) {
                    autoDetectDeviceInSubnetList(devicesFound);
                }
            }
        });

        if (!savedIpAddress.isEmpty()) {
            tryToConnectDevice(savedIpAddress, savedMacAddress);
        } else {
            networkServiceState = NetworkServiceState.STATE_FIND_SUBNET_DEVICES;
            ServerManager.getInstance().setIpAddress("");
            ServerManager.getInstance().findSubnetDevices();
        }
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

    private void tryToConnectDevice(String ipAddress, String macAddress) {
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "tryToConnectDevice: IP is empty");
            return;
        }
        retryCount = 0;
        networkServiceState = NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE;
        ServerManager.getInstance().setIpAddress(savedIpAddress);
        if (!macAddress.isEmpty()) {
            ServerManager.getInstance().setMacAddress(macAddress);
        }
        ServerManager.getInstance().sendCheckConnectionRequest();
    }

    private void autoDetectDeviceInSubnetList(ArrayList<Device> devices) {
        boolean hasMacAddressInList = false;
        Device sameMacAddressDevice = null;
        if (!savedMacAddress.isEmpty()) {
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).getMac().equals(savedMacAddress)) {
                    hasMacAddressInList = true;
                    sameMacAddressDevice = devices.get(i);
                }
            }
        }
        // connect to device which has same MAC address
        if (hasMacAddressInList && sameMacAddressDevice != null) {
            tryToConnectDevice(sameMacAddressDevice.getIp(), sameMacAddressDevice.getMac());
        } else {
            Log.e(TAG, "autoDetectDeviceInSubnetList: Cannot find device which has same MAC address in list");
        }
    }

    private void saveSharedPreferences() {
        if (savedIpAddress.isEmpty()) {
            Log.e(TAG, "saveSharedPreferences: Cannot save due to empty savedIpAddress");
            return;
        }
        if (savedMacAddress.isEmpty()) {
            Log.e(TAG, "saveSharedPreferences: Cannot save due to empty savedMacAddress");
            return;
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedIpAddress", savedIpAddress);
        editor.putString("savedMacAddress", savedMacAddress);
        editor.apply();
    }

    private void readSharedPreferences() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        savedIpAddress = prefs.getString("savedIpAddress", "");
        savedMacAddress = prefs.getString("savedMacAddress", "");

        Log.e(TAG, "readSharedPreferences: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }
}