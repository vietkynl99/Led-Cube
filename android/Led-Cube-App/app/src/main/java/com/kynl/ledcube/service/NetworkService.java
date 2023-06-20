package com.kynl.ledcube.service;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAUSE_NETWORK_SCAN;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_SEND_DATA;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_UPDATE_STATUS;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.EffectManager;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.manager.SharedPreferencesManager;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.nettool.SubnetDevices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    private final int networkScanTime = 10;
    private Handler mHandler;
    private Runnable mRunnable;
    private final Gson gson = new Gson();
    private final int retryMax = 1;
    private NetworkServiceState networkServiceState;
    private int retryCount;
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
                            ServerManager.getInstance().setSynced(false);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_UPDATE_STATUS: {
                        BroadcastManager.getInstance().sendUpdateStatus(networkServiceState);
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

        networkServiceState = NetworkServiceState.STATE_NONE;
        retryCount = 0;
        lastFreeTime = System.currentTimeMillis();

        /* Server response */
        ServerManager.getInstance().setOnServerResponseListener((serverState, message) -> {
            Log.i(TAG, ">>> Server status changed: serverState[" + serverState + "]");
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
                    ServerManager.getInstance().setSynced(serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED);
                    setNetworkServiceState(NetworkServiceState.STATE_NONE);
                    break;
                }
                default: {
                    Log.e(TAG, "Server status changed: Unhandled event " + networkServiceState);
                    break;
                }
            }
            BroadcastManager.getInstance().sendServerResponse(serverState, message);
        });

        /* Server data changed */
        ServerManager.getInstance().setOnServerDataChangeListener(serverData -> {
            Log.d(TAG, "ServerDataChanged: " + serverData);
            BroadcastManager.getInstance().sendUpdateServerData(serverData);
        });

        /* Found subnet devices */
        ServerManager.getInstance().setOnSubnetDeviceFoundListener(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.d(TAG, "onDeviceFound: " + device);
                BroadcastManager.getInstance().sendAddSubnetDevice(device);
            }

            public void onProcessed(int processed, int total) {
                int percent = 100 * processed / total;
                BroadcastManager.getInstance().sendUpdateSubnetProgress(percent);
            }

            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                sortDevicesListByIp(devicesFound);
                Log.i(TAG, "onFinished: Found " + devicesFound.size());
                String lastScanTime = getCurrentTimeString();
                String lastScanDevicesList = convertDevicesListToString(devicesFound);
                SharedPreferencesManager.getInstance().setLastScanTime(lastScanTime);
                SharedPreferencesManager.getInstance().setLastScanDevicesList(lastScanDevicesList);
                setNetworkServiceState(NetworkServiceState.STATE_NONE);
                BroadcastManager.getInstance().sendFinishFindSubnetDevices();
                if (SharedPreferencesManager.getInstance().isAutoDetect()) {
                    requestAutoDetectDeviceInSubnetList(devicesFound);
                }
            }
        });

        /* Broadcast */
        BroadcastManager.getInstance().registerBroadcast(mBroadcastReceiver);

        /* Runnable */
        mHandler = new Handler();
        mRunnable = () -> {
            // If it have free time for 5 seconds, then start checking the connection
            if (!ServerManager.getInstance().isBusy()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFreeTime > networkScanTime * 500) {
                    if (ServerManager.getInstance().isSynced()) {
                        requestConnectToSavedDevice();
                    } else {
                        requestSyncLastData();
                    }
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
        BroadcastManager.getInstance().unRegisterBroadcast(mBroadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sortDevicesListByIp(ArrayList<Device> devices) {
        Comparator<Device> comparator = (o1, o2) -> {
            String ip1 = o1.getIp();
            String ip2 = o2.getIp();
            String[] ip1Parts = ip1.split("\\.");
            String[] ip2Parts = ip2.split("\\.");

            for (int i = 0; i < 4; i++) {
                int part1 = Integer.parseInt(ip1Parts[i]);
                int part2 = Integer.parseInt(ip2Parts[i]);
                if (part1 < part2) {
                    return -1;
                } else if (part1 > part2) {
                    return 1;
                }
            }

            return 0;
        };

        devices.sort(comparator);
    }

    private String convertDevicesListToString(ArrayList<Device> devices) {
        return gson.toJson(devices);
    }

    private void setNetworkServiceState(NetworkServiceState networkServiceState) {
        if (this.networkServiceState != NetworkServiceState.STATE_NONE &&
                networkServiceState == NetworkServiceState.STATE_NONE) {
            lastFreeTime = System.currentTimeMillis();
        }
        if (this.networkServiceState != networkServiceState) {
            this.networkServiceState = networkServiceState;
            Log.i(TAG, ">>> Service state changed: " + networkServiceState);
            BroadcastManager.getInstance().sendNetWorkServiceStateChanged(networkServiceState);
        }
    }

    private void requestSyncLastData() {
        if (!ServerManager.getInstance().hasSavedDevice()) {
            Log.e(TAG, "requestSyncLastData: No saved device");
            return;
        }
        if (ServerManager.getInstance().isBusy()) {
            Log.e(TAG, "requestSyncLastData: Network Service is busy. Please try again. State:" + networkServiceState);
            return;
        }
        Log.d(TAG, "requestSyncLastData: ");
        String data = EffectManager.getInstance().getCurrentEffectDataAsJson();
        requestSendData(data);
    }


    private void requestSendData(String data) {
        if (!ServerManager.getInstance().hasSavedDevice()) {
            Log.e(TAG, "requestSendData: No saved device");
            return;
        }
        if (ServerManager.getInstance().isBusy()) {
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
        if (ServerManager.getInstance().isBusy()) {
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
        if (ServerManager.getInstance().isBusy()) {
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
        if (ServerManager.getInstance().isBusy()) {
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
        if (ServerManager.getInstance().isBusy()) {
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
}