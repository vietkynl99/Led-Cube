package com.kynl.ledcube.service;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_CONNECT_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_STATUS_CHANGED;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_STATE_CHANGED;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;
import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;
import static com.kynl.ledcube.manager.ServerManager.ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
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

import com.google.gson.Gson;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.nettool.SubnetDevices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    private final Gson gson = new Gson();
    private final int retryMax = 1;
    private String savedIpAddress, savedMacAddress;
    private String lastScanTime, lastScanDevicesList;
    private NetworkServiceState networkServiceState;
    private int retryCount;
    private boolean autoDetect;

    public enum NetworkServiceState {
        STATE_NONE,
        STATE_TRY_TO_CONNECT_DEVICE,
        STATE_FIND_SUBNET_DEVICES,
        STATE_PAIR_DEVICE
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
                        String ip = intent.getStringExtra("ip");
                        String mac = intent.getStringExtra("mac");
                        if (!ip.isEmpty() && !mac.isEmpty()) {
                            requestConnectToDevice(ip, mac);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_PAIR_DEVICE: {
                        String ip = intent.getStringExtra("ip");
                        String mac = intent.getStringExtra("mac");
                        if (!ip.isEmpty() && !mac.isEmpty()) {
                            pairDevice(ip, mac);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_UPDATE_STATUS: {
                        sendBroadcastUpdateStatus();
                        break;
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
        lastScanTime = "";
        lastScanDevicesList = "";
        networkServiceState = NetworkServiceState.STATE_NONE;
        retryCount = 0;
        autoDetect = true;

        readSavedDeviceInformation();

        /* ServerManager */
        ServerManager.getInstance().init(getApplicationContext());

        /* Server status changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.e(TAG, "Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "]");
            // Sent request is done
            if (connectionState == CONNECTION_STATE_NONE) {
                switch (networkServiceState) {
                    case STATE_TRY_TO_CONNECT_DEVICE: {
                        if (serverState == SERVER_STATE_DISCONNECTED) {
                            // Cannot connect to server
                            retryCount++;
                            if (retryCount >= retryMax) {
                                Log.i(TAG, "Server status changed: Cannot connect on startup (retry: " + retryCount + " ) -> Stop retry");
                                setNetworkServiceState(NetworkServiceState.STATE_NONE);
                            } else {
                                Log.i(TAG, "Server status changed: Cannot connect on startup (retry: " + retryCount + " ) -> Continue retry");
                                ServerManager.getInstance().sendCheckConnectionRequest();
                            }
                        } else {
                            // Able to connect to server
                            String ipAddress = ServerManager.getInstance().getIpAddress();
                            String macAddress = ServerManager.getInstance().getMacAddress();
                            if (!ipAddress.equals(savedIpAddress) || !macAddress.equals(savedMacAddress)) {
                                savedIpAddress = ipAddress;
                                savedMacAddress = macAddress;
                                saveDeviceInformation();
                            }
                            setNetworkServiceState(NetworkServiceState.STATE_NONE);
                        }
                        break;
                    }
                    case STATE_PAIR_DEVICE: {
                        if (serverState == SERVER_STATE_DISCONNECTED) {
                            Log.i(TAG, "Server status changed: Can not pair to " + ServerManager.getInstance().getIpAddress() + " " + ServerManager.getInstance().getMacAddress());
                        } else if (serverState == SERVER_STATE_CONNECTED_AND_PAIRED) {
                            Log.i(TAG, "Server status changed: Pair successfully!");
                        }
                        setNetworkServiceState(NetworkServiceState.STATE_NONE);
                        break;
                    }
                    default: {
                        Log.e(TAG, "Server status changed: Unhandled event " + networkServiceState);
                        break;
                    }
                }
                sendBroadcastServerStatusChanged(serverState, connectionState);
            }
        });

        /* Found subnet devices */
        ServerManager.getInstance().setOnSubnetDeviceFoundListener(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.d(TAG, "onDeviceFound: " + device);
                sendBroadcastAddSubnetDevice(device);
            }

            public void onProcessed(int processed, int total) {
                int percent = 100 * processed / total;
                sendBroadcastUpdateSubnetProgress(percent);
            }

            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                Log.i(TAG, "onFinished: Found " + devicesFound.size());
                lastScanTime = getCurrentTimeString();
                lastScanDevicesList = convertDevicesListToString(devicesFound);
                saveLastScanInformation();
                setNetworkServiceState(NetworkServiceState.STATE_NONE);
                sendBroadcastFinishFindSubnetDevices();
                if (autoDetect) {
                    autoDetectDeviceInSubnetList(devicesFound);
                }
            }
        });

        if (!savedIpAddress.isEmpty()) {
            requestConnectToDevice(savedIpAddress, savedMacAddress);
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

    private String convertDevicesListToString(ArrayList<Device> devices) {
        return gson.toJson(devices);
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

    private void sendBroadcastServerStatusChanged(ServerManager.ServerState serverState, ServerManager.ConnectionState connectionState) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_SERVER_STATUS_CHANGED);
        intent.putExtra("serverState", serverState);
        intent.putExtra("connectionState", connectionState);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastAddSubnetDevice(Device device) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_ADD_SUBNET_DEVICE);
        intent.putExtra("ip", device.getIp());
        intent.putExtra("mac", device.getMac());
        intent.putExtra("ping", device.getPing());
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastUpdateSubnetProgress(int percent) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS);
        intent.putExtra("percent", percent);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastFinishFindSubnetDevices() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastServiceStateChanged() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_STATE_CHANGED);
        intent.putExtra("serviceState", networkServiceState);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastUpdateStatus() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_UPDATE_STATUS);
        intent.putExtra("serviceState", networkServiceState);
        sendBroadcastMessage(intent);
    }

    private void setNetworkServiceState(NetworkServiceState networkServiceState) {
        if (this.networkServiceState != networkServiceState) {
            this.networkServiceState = networkServiceState;
            Log.e(TAG, "Service state changed: " + networkServiceState);
            sendBroadcastServiceStateChanged();
        }
    }

    private void pairDevice(String ipAddress, String macAddress) {
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "requestPairDevice: IP is empty");
            return;
        }
        if (networkServiceState != NetworkServiceState.STATE_NONE) {
            Log.e(TAG, "requestPairDevice: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "requestPairDevice: ");
        retryCount = 0;
        setNetworkServiceState(NetworkServiceState.STATE_PAIR_DEVICE);
        ServerManager.getInstance().setIpAddress(ipAddress);
        ServerManager.getInstance().setMacAddress(macAddress);
        ServerManager.getInstance().sendPairRequest();
    }

    private void requestConnectToDevice(String ipAddress, String macAddress) {
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
        setNetworkServiceState(NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE);
        ServerManager.getInstance().setIpAddress(ipAddress);
        ServerManager.getInstance().setMacAddress(macAddress);
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
            requestConnectToDevice(sameMacAddressDevice.getIp(), sameMacAddressDevice.getMac());
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
        setNetworkServiceState(NetworkServiceState.STATE_FIND_SUBNET_DEVICES);
        ServerManager.getInstance().setIpAddress("");
        ServerManager.getInstance().setMacAddress("");
        ServerManager.getInstance().findSubnetDevices();
    }

    private String getCurrentTimeString() {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.US);
        return formatter.format(now);
    }

    private void saveDeviceInformation() {
        if (savedIpAddress.isEmpty()) {
            Log.e(TAG, "saveDeviceInformation: Cannot save due to empty savedIpAddress");
            return;
        }
        if (savedMacAddress.isEmpty()) {
            Log.e(TAG, "saveDeviceInformation: Cannot save due to empty savedMacAddress");
            return;
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedIpAddress", savedIpAddress);
        editor.putString("savedMacAddress", savedMacAddress);
        editor.apply();

        Log.e(TAG, "saveDeviceInformation: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }

    private void readSavedDeviceInformation() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        savedIpAddress = prefs.getString("savedIpAddress", "");
        savedMacAddress = prefs.getString("savedMacAddress", "");

        Log.e(TAG, "readDeviceInformation: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }

    private void saveLastScanInformation() {
        if (lastScanTime.isEmpty()) {
            Log.e(TAG, "saveLastScanInformation: Cannot save due to empty lastScanTime");
            return;
        }
        if (lastScanDevicesList.isEmpty()) {
            Log.e(TAG, "saveLastScanInformation: Cannot save due to empty lastScanDevicesList");
            return;
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastScanTime", lastScanTime);
        editor.putString("lastScanDevicesList", lastScanDevicesList);
        editor.apply();

        Log.i(TAG, "saveLastScanInformation: lastScanTime[" + lastScanTime + "]");
    }
}