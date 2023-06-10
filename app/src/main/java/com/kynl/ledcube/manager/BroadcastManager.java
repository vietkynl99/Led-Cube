package com.kynl.ledcube.manager;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_PAIR_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_SEND_DATA;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_STATUS_CHANGED;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_STATE_CHANGED;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kynl.ledcube.common.CommonUtils;
import com.kynl.ledcube.model.Device;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.common.CommonUtils.ConnectionState;


public class BroadcastManager {
    private final String TAG = "BroadcastManager";
    private static BroadcastManager instance;
    private final Context context;

    private BroadcastManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized BroadcastManager getInstance(Context context) {
        if (instance == null) {
            instance = new BroadcastManager(context);
        }
        return instance;
    }
    
    /* Common */
    private void sendBroadcast(Intent intent) {
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    private void sendEvent(String event) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", event);
        sendBroadcast(intent);
    }

    /* Send from NetworkService to Activity/Fragment */
    public void sendServerStatusChanged(CommonUtils.ServerState serverState, CommonUtils.ConnectionState connectionState) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_SERVER_STATUS_CHANGED);
        intent.putExtra("serverState", serverState);
        intent.putExtra("connectionState", connectionState);
        sendBroadcast(intent);
    }

    public void sendAddSubnetDevice(Device device) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_ADD_SUBNET_DEVICE);
        intent.putExtra("ip", device.getIp());
        intent.putExtra("mac", device.getMac());
        intent.putExtra("ping", device.getPing());
        sendBroadcast(intent);
    }

    public void sendUpdateSubnetProgress(int percent) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS);
        intent.putExtra("percent", percent);
        sendBroadcast(intent);
    }

    public void sendFinishFindSubnetDevices() {
        sendEvent(BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE);
    }

    public void sendNetWorkServiceStateChanged(NetworkServiceState networkServiceState) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_STATE_CHANGED);
        intent.putExtra("networkServiceState", networkServiceState);
        sendBroadcast(intent);
    }

    public void sendUpdateStatus(NetworkServiceState networkServiceState) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_SERVICE_UPDATE_STATUS);
        intent.putExtra("networkServiceState", networkServiceState);
        sendBroadcast(intent);
    }

    /* From SearchFragment to NetworkService */
    public void sendRequestFindSubnetDevice() {
        Log.e(TAG, "sendBroadcastRequestFindSubnetDevice: ");
        sendEvent(BROADCAST_REQUEST_FIND_SUBNET_DEVICE);
    }

    public void sendRequestPairDevice(String ip, String mac) {
        Log.e(TAG, "sendBroadcastRequestPairDevice: IP[" + ip + "] MAC[" + mac + "]");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_PAIR_DEVICE);
        intent.putExtra("ip", ip);
        intent.putExtra("mac", mac);
        sendBroadcast(intent);
    }

    public void sendRequestUpdateStatus() {
        Log.e(TAG, "sendBroadcastRequestUpdateStatus: ");
        sendEvent(BROADCAST_REQUEST_UPDATE_STATUS);
    }
    
    /* From HomeFragment to NetworkService */
    public void sendRequestSendData(String data) {
        Log.e(TAG, "sendBroadcastSendData: " + data);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_SEND_DATA);
        intent.putExtra("data", data);
        sendBroadcast(intent);
    }
}
