package com.kynl.ledcube.service;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAUSE_NETWORK_SCAN;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_SEND_DATA;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.nettool.SubnetDevices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.common.CommonUtils.ConnectionState;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    private final int networkScanTime = 10;
    private Handler mHandler;
    private Runnable mRunnable;
    private final Gson gson = new Gson();
    private final int retryMax = 1;
    private String lastScanTime, lastScanDevicesList;
    private NetworkServiceState networkServiceState;
    private int retryCount;
    private boolean autoDetect;
    private long lastFreeTime;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_REQUEST_FIND_SUBNET_DEVICE: {
                        requestFindSubnetDevicesList();
                        break;
                    }
                    case BROADCAST_REQUEST_PAIR_DEVICE: {
                        String ip = intent.getStringExtra("ip");
                        String mac = intent.getStringExtra("mac");
                        if (!ip.isEmpty() && !mac.isEmpty()) {
                            requestPairDevice(ip, mac);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_SEND_DATA: {
                        String data = intent.getStringExtra("data");
                        if (!data.isEmpty()) {
                            requestSendData(data);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_UPDATE_STATUS: {
                        BroadcastManager.getInstance(getApplicationContext()).sendUpdateStatus(networkServiceState);
                        break;
                    }
                    case BROADCAST_REQUEST_PAUSE_NETWORK_SCAN: {
                        if (mHandler != null && mRunnable != null) {
                            mHandler.removeCallbacks(mRunnable);
                        }
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

        lastScanTime = "";
        lastScanDevicesList = "";
        networkServiceState = NetworkServiceState.STATE_NONE;
        retryCount = 0;
        autoDetect = true;
        lastFreeTime = System.currentTimeMillis();

        /* ServerManager */
        ServerManager.getInstance().init(getApplicationContext());

        /* Server status changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.i(TAG, ">>> Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "]");
            // Sent request is done
            if (connectionState == ConnectionState.CONNECTION_STATE_NONE) {
                switch (networkServiceState) {
                    case STATE_TRY_TO_CONNECT_DEVICE: {
                        if (serverState == ServerState.SERVER_STATE_DISCONNECTED) {
                            // Cannot connect to server
                            retryCount++;
                            if (retryCount >= retryMax) {
                                Log.i(TAG, "Server status changed: Cannot connect (retry: " + retryCount + " ) -> Stop");
                                setNetworkServiceState(NetworkServiceState.STATE_NONE);
                            } else {
                                Log.i(TAG, "Server status changed: Cannot connect (retry: " + retryCount + " ) -> Retry");
                                ServerManager.getInstance().sendCheckConnectionRequest();
                            }
                        } else {
                            // Able to connect to server
                            setNetworkServiceState(NetworkServiceState.STATE_NONE);
                        }
                        break;
                    }
                    case STATE_PAIR_DEVICE: {
                        if (serverState == ServerState.SERVER_STATE_DISCONNECTED) {
                            Log.i(TAG, "Server status changed: Can not pair to " + ServerManager.getInstance().getIpAddress() + " " + ServerManager.getInstance().getMacAddress());
                        } else if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                            Log.i(TAG, "Server status changed: Pair successfully!");
                        }
                        setNetworkServiceState(NetworkServiceState.STATE_NONE);
                        break;
                    }
                    case STATE_SEND_DATA: {
                        if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                            Log.d(TAG, "Server status changed: Send data successfully");
                        } else {
                            Log.e(TAG, "Server status changed: Send data failed");
                        }
                        setNetworkServiceState(NetworkServiceState.STATE_NONE);
                        break;
                    }
                    default: {
                        Log.e(TAG, "Server status changed: Unhandled event " + networkServiceState);
                        break;
                    }
                }
                BroadcastManager.getInstance(getApplicationContext()).sendServerStatusChanged(serverState, connectionState);
            }
        });

        /* Server data changed */
        ServerManager.getInstance().setOnServerDataChangeListener(serverData -> {
            Log.d(TAG, "ServerDataChanged: " + serverData);
            BroadcastManager.getInstance(getApplicationContext()).sendUpdateServerData(serverData);
        });

        /* Found subnet devices */
        ServerManager.getInstance().setOnSubnetDeviceFoundListener(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.d(TAG, "onDeviceFound: " + device);
                BroadcastManager.getInstance(getApplicationContext()).sendAddSubnetDevice(device);
            }

            public void onProcessed(int processed, int total) {
                int percent = 100 * processed / total;
                BroadcastManager.getInstance(getApplicationContext()).sendUpdateSubnetProgress(percent);
            }

            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                Log.i(TAG, "onFinished: Found " + devicesFound.size());
                lastScanTime = getCurrentTimeString();
                lastScanDevicesList = convertDevicesListToString(devicesFound);
                saveLastScanInformation();
                setNetworkServiceState(NetworkServiceState.STATE_NONE);
                BroadcastManager.getInstance(getApplicationContext()).sendFinishFindSubnetDevices();
                if (autoDetect) {
                    requestAutoDetectDeviceInSubnetList(devicesFound);
                }
            }
        });

        /* Broadcast */
        registerBroadcast();

        /* Runnable */
        mHandler = new Handler();
        mRunnable = () -> {
            // If it have free time for 5 seconds, then start checking the connection
            if (!isBusy()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFreeTime > networkScanTime * 500) {
                    requestConnectToSavedDevice();
                }
            }
            mHandler.postDelayed(mRunnable, networkScanTime * 1000);
        };

        // Try to connect in first time
        if (ServerManager.getInstance().hasSavedDevice()) {
            requestConnectToSavedDevice();
        } else {
            requestFindSubnetDevicesList();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        // Runnable
        mHandler.postDelayed(mRunnable, networkScanTime * 1000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
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

    private void setNetworkServiceState(NetworkServiceState networkServiceState) {
        if (this.networkServiceState != NetworkServiceState.STATE_NONE &&
                networkServiceState == NetworkServiceState.STATE_NONE) {
            lastFreeTime = System.currentTimeMillis();
        }
        if (this.networkServiceState != networkServiceState) {
            this.networkServiceState = networkServiceState;
            Log.i(TAG, ">>> Service state changed: " + networkServiceState);
            BroadcastManager.getInstance(getApplicationContext()).sendNetWorkServiceStateChanged(networkServiceState);
        }
    }

    private boolean isBusy() {
        return networkServiceState != NetworkServiceState.STATE_NONE;
    }

    private void requestSendData(String data) {
        if (!ServerManager.getInstance().hasSavedDevice()) {
            Log.e(TAG, "requestSendData: No saved device");
            return;
        }
        if (isBusy()) {
            Log.e(TAG, "requestSendData: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "requestSendData: " + data);
        String ip = ServerManager.getInstance().getSavedIpAddress();
        String mac = ServerManager.getInstance().getMacAddress();
        setNetworkServiceState(NetworkServiceState.STATE_SEND_DATA);
        ServerManager.getInstance().setIpAddress(ip);
        ServerManager.getInstance().setMacAddress(mac);
        ServerManager.getInstance().sendData(data);
    }

    private void requestPairDevice(String ipAddress, String macAddress) {
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "requestPairDevice: IP is empty");
            return;
        }
        if (isBusy()) {
            Log.e(TAG, "requestPairDevice: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "requestPairDevice: " + ipAddress + " " + macAddress);
        retryCount = 0;
        setNetworkServiceState(NetworkServiceState.STATE_PAIR_DEVICE);
        ServerManager.getInstance().setIpAddress(ipAddress);
        ServerManager.getInstance().setMacAddress(macAddress);
        ServerManager.getInstance().sendPairRequest();
    }

    private void requestConnectToDevice(String ipAddress, String macAddress) {
        if (ipAddress.isEmpty()) {
            Log.e(TAG, "requestConnectToDevice: IP is empty");
            return;
        }
        if (isBusy()) {
            Log.e(TAG, "requestConnectToDevice: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "requestConnectToDevice: " + ipAddress + " " + macAddress);
        retryCount = 0;
        setNetworkServiceState(NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE);
        ServerManager.getInstance().setIpAddress(ipAddress);
        ServerManager.getInstance().setMacAddress(macAddress);
        ServerManager.getInstance().sendCheckConnectionRequest();
    }

    private void requestConnectToSavedDevice() {
        if (ServerManager.getInstance().hasSavedDevice()) {
            Log.d(TAG, "requestConnectToSavedDevice: ");
            String ip = ServerManager.getInstance().getSavedIpAddress();
            String mac = ServerManager.getInstance().getMacAddress();
            requestConnectToDevice(ip, mac);
        }
    }

    private void requestAutoDetectDeviceInSubnetList(ArrayList<Device> devices) {
        if (isBusy()) {
            Log.e(TAG, "autoDetectDeviceInSubnetList: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }

        Log.d(TAG, "autoDetectDeviceInSubnetList: ");
        String savedMacAddress = ServerManager.getInstance().getSavedMacAddress();
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

    private void requestFindSubnetDevicesList() {
        if (isBusy()) {
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