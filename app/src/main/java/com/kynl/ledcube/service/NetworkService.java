package com.kynl.ledcube.service;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_CONNECT_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;
import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;
import static com.kynl.ledcube.manager.ServerManager.ServerState.SERVER_STATE_DISCONNECTED;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.nettool.SubnetDevices;

import java.util.ArrayList;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    private final int RETRY_ON_STARTUP_MAX = 3;
    private String savedIpAddress, savedMacAddress;
    private NetworkServiceState networkServiceState;
    private int retryCount;
    private boolean autoDetect;

    private enum NetworkServiceState {
        STATE_NONE,
        STATE_TRY_TO_CONNECT_DEVICE,
        STATE_FIND_SUBNET_DEVICES
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_REQUEST_FIND_SUBNET_DEVICE: {
                        findSubnetDevicesList();
                        break;
                    }
                    case BROADCAST_REQUEST_CONNECT_DEVICE: {
//                        tryToConnectDevice();
                    }
                    default:
                        break;
                }
            }
        }
    };


    public NetworkService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate: create service");

        savedIpAddress = "";
        savedMacAddress = "";
        networkServiceState = NetworkServiceState.STATE_NONE;
        retryCount = 0;
        autoDetect = true;

        readSharedPreferences();

        /* ServerManager */
        ServerManager.getInstance().init(getApplicationContext());

        /* Server status changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.i(TAG, "Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "] networkServiceState[" + networkServiceState + "]");
            // Sent request is done
            if (connectionState == CONNECTION_STATE_NONE) {
                if (networkServiceState == NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE) {
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
                }
            }
        });

        /* Found subnet devices */
        ServerManager.getInstance().setOnSubnetDeviceFoundListener(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.d(TAG, "onDeviceFound: " + device.toString());
                sendBroadcastAddSubnetDevice(device);
            }

            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                Log.i(TAG, "onFinished: Found " + devicesFound.size());
                networkServiceState = NetworkServiceState.STATE_NONE;
                sendBroadcastFinishFindSubnetDevices(devicesFound);
                if (autoDetect) {
                    autoDetectDeviceInSubnetList(devicesFound);
                }
            }
        });

        if (!savedIpAddress.isEmpty()) {
            tryToConnectDevice(savedIpAddress, savedMacAddress);
        } else {
            findSubnetDevicesList();
        }

        /* Broadcast */
        registerBroadcast();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
        unRegisterBroadcast();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerBroadcast() {
        // Register broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(BROADCAST_ACTION));
    }

    private void unRegisterBroadcast() {
        try {
            getApplicationContext().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    private void sendBroadcastMessage(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendBroadcastAddSubnetDevice(Device device) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_ADD_SUBNET_DEVICE);
        intent.putExtra("ip", device.getIp());
        intent.putExtra("mac", device.getMac());
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastFinishFindSubnetDevices(ArrayList<Device> devicesList) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE);
        intent.putExtra("devicesList", removeInvalidDevices(devicesList));
        sendBroadcastMessage(intent);
    }

    private void tryToConnectDevice(String ipAddress, String macAddress) {
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "tryToConnectDevice: IP is empty");
            return;
        }
        if (networkServiceState != NetworkServiceState.STATE_NONE) {
            Log.e(TAG, "tryToConnectDevice: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "tryToConnectDevice: ");
        retryCount = 0;
        networkServiceState = NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE;
        ServerManager.getInstance().setIpAddress(savedIpAddress);
        if (!macAddress.isEmpty()) {
            ServerManager.getInstance().setMacAddress(macAddress);
        }
        ServerManager.getInstance().sendCheckConnectionRequest();
    }

    private void autoDetectDeviceInSubnetList(ArrayList<Device> devices) {
        if (networkServiceState != NetworkServiceState.STATE_NONE) {
            Log.e(TAG, "autoDetectDeviceInSubnetList: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "autoDetectDeviceInSubnetList: ");
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
            Log.i(TAG, "autoDetectDeviceInSubnetList: Cannot find device which has same MAC address in list");
        }
    }

    private void findSubnetDevicesList() {
        if (networkServiceState != NetworkServiceState.STATE_NONE) {
            Log.e(TAG, "findSubnetDevicesList: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "findSubnetDevicesList: ");
        networkServiceState = NetworkServiceState.STATE_FIND_SUBNET_DEVICES;
        ServerManager.getInstance().setIpAddress("");
        ServerManager.getInstance().setMacAddress("");
        ServerManager.getInstance().findSubnetDevices();
    }

    private ArrayList<Device> removeInvalidDevices(ArrayList<Device> devices) {
        for (int i = devices.size() - 1; i >= 0; i--) {
            Device device = devices.get(i);
            if (device.getIp().isEmpty() || device.getMac().isEmpty()) {
                Log.d(TAG, "removeInvalidDevices: Remove IP " + device.getIp());
                devices.remove(i);
            }
        }
        return devices;
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

        Log.e(TAG, "saveSharedPreferences: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }

    private void readSharedPreferences() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        savedIpAddress = prefs.getString("savedIpAddress", "");
        savedMacAddress = prefs.getString("savedMacAddress", "");

        Log.e(TAG, "readSharedPreferences: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }
}