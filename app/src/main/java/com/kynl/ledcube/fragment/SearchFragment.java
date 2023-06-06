package com.kynl.ledcube.fragment;

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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.DeviceListAdapter;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.service.NetworkService;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class SearchFragment extends Fragment {
    private final String TAG = "SearchFragment";
    private String savedIpAddress, savedMacAddress;
    private String lastScanTime, lastScanDevicesList;
    private final Gson gson = new Gson();
    private final Handler handler = new Handler();
    private DeviceListAdapter deviceListAdapter;
    private ImageButton refreshBtn;
    private TextView informationText;
    private ProgressBar progressBar;

    // Debounce
    private boolean isDebouncing = false;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_UPDATE_SUBNET_PROGRESS: {
                        if (progressBar != null) {
                            int percent = intent.getIntExtra("percent", -1);
                            if (percent >= 0) {
                                progressBar.setProgress(Math.max(percent, 10));
                            }
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE: {
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                updateLastScanList();
                                setRefreshButtonEnable(true);
                            });
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_SERVER_STATUS_CHANGED: {
                        ServerManager.ServerState serverState = (ServerManager.ServerState) intent.getSerializableExtra("serverState");
                        if (serverState == ServerManager.ServerState.SERVER_STATE_DISCONNECTED) {
                            deviceListAdapter.resetConnectingDevice();
                        } else {
                            readSavedDeviceInformation();
                            deviceListAdapter.setConnectedDeviceMac(savedMacAddress);
                        }
                        if (serverState == ServerManager.ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED) {
                            deviceListAdapter.setConnectedDeviceState(Device.DeviceState.STATE_CONNECTED_BUT_NOT_PAIRED);
                        } else if (serverState == ServerManager.ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                            deviceListAdapter.setConnectedDeviceState(Device.DeviceState.STATE_CONNECTED_AND_PAIRED);
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_STATE_CHANGED: {
                        NetworkService.NetworkServiceState networkServiceState = (NetworkService.NetworkServiceState) intent.getSerializableExtra("serviceState");
                        if (networkServiceState == NetworkService.NetworkServiceState.STATE_NONE) {
                            setRefreshButtonEnable(true);
                            deviceListAdapter.resetConnectingDevice();
                            if (progressBar != null) {
                                progressBar.setProgress(0);
                            }
                        } else {
                            setRefreshButtonEnable(false);
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_UPDATE_STATUS: {
                        NetworkService.NetworkServiceState networkServiceState = (NetworkService.NetworkServiceState) intent.getSerializableExtra("serviceState");
                        if (networkServiceState == NetworkService.NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE) {
                            deviceListAdapter.setConnectingDevice(ServerManager.getInstance().getMacAddress());
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    };

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        savedIpAddress = "";
        savedMacAddress = "";
        lastScanTime = "";
        lastScanDevicesList = "";
        readSavedDeviceInformation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        /* Elements */
        RecyclerView deviceListRecyclerView = view.findViewById(R.id.deviceListRecyclerView);
        refreshBtn = view.findViewById(R.id.refreshBtn);
        informationText = view.findViewById(R.id.informationText);
        progressBar = view.findViewById(R.id.progressBar);

        /* Recycler view */
        deviceListAdapter = new DeviceListAdapter();
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);
        deviceListAdapter.setOnSubItemClickListener((ip, mac) -> {
            // Debounce
            if (!isDebouncing) {
                isDebouncing = true;
                deviceListAdapter.setConnectingDevice(mac);
                handler.postDelayed(() -> {
                    isDebouncing = false;
//                    sendBroadcastRequestConnectDevice(ip, mac);
                    sendBroadcastRequestPairDevice(ip, mac);
                }, 1000);
            }
        });

        /* Refresh button */
        refreshBtn.setOnClickListener(v -> refreshDeviceList());

        updateLastScanList();
        registerBroadcast();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sendBroadcastRequestUpdateStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterBroadcast();
    }

    private void updateLastScanList() {
        readLastScanInformation();
        if (!lastScanDevicesList.isEmpty() && !lastScanTime.isEmpty()) {
            ArrayList<Device> devicesList = convertStringToDevicesList(lastScanDevicesList);
            setInformationText("Last scan: " + lastScanTime);
            deviceListAdapter.syncList(devicesList);
            readSavedDeviceInformation();
            deviceListAdapter.setConnectedDeviceMac(savedMacAddress);
        }
    }

    private void readSavedDeviceInformation() {
        Context context = getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            savedIpAddress = prefs.getString("savedIpAddress", "");
            savedMacAddress = prefs.getString("savedMacAddress", "");
        }

        Log.e(TAG, "readDeviceInformation: savedIpAddress[" + savedIpAddress + "] savedMacAddress[" + savedMacAddress + "]");
    }


    private void readLastScanInformation() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "readLastScanInformation: Context is null");
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        lastScanTime = prefs.getString("lastScanTime", "");
        lastScanDevicesList = prefs.getString("lastScanDevicesList", "");

        Log.i(TAG, "readLastScanInformation: lastScanTime[" + lastScanTime + "]");
    }


    private ArrayList<Device> convertStringToDevicesList(String jsonString) {
        Type type = new TypeToken<ArrayList<Device>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }


    private void refreshDeviceList() {
        Log.d(TAG, "refreshDeviceList: ");
        setRefreshButtonEnable(false);
//        setInformationText("Scanning...");
        sendBroadcastRequestFindSubnetDevice();
    }

    private void setRefreshButtonEnable(boolean enable) {
        if (refreshBtn != null) {
            refreshBtn.setEnabled(enable);
        }
    }

    private void setInformationText(String text) {
        if (informationText != null) {
            informationText.setText(text);
        }
    }

    private void registerBroadcast() {
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver,
                    new IntentFilter(BROADCAST_ACTION));
        } else {
            Log.e(TAG, "registerBroadcast: Context is null");
        }
    }

    private void unRegisterBroadcast() {
        Context context = getContext();
        if (context != null) {
            try {
                context.unregisterReceiver(mBroadcastReceiver);
            } catch (Exception ignored) {

            }
        } else {
            Log.e(TAG, "unRegisterBroadcast: Context is null");
        }
    }

    private void sendBroadcastMessage(Intent intent) {
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else {
            Log.e(TAG, "sendBroadcastMessage: Context is null");
        }
    }

    private void sendBroadcastRequestFindSubnetDevice() {
        Log.e(TAG, "sendBroadcastRequestFindSubnetDevice: ");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_FIND_SUBNET_DEVICE);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastRequestConnectDevice(String ip, String mac) {
        Log.e(TAG, "sendBroadcastRequestConnectDevice: IP[" + ip + "] MAC[" + mac + "]");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_CONNECT_DEVICE);
        intent.putExtra("ip", ip);
        intent.putExtra("mac", mac);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastRequestPairDevice(String ip, String mac) {
        Log.e(TAG, "sendBroadcastRequestPairDevice: IP[" + ip + "] MAC[" + mac + "]");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_PAIR_DEVICE);
        intent.putExtra("ip", ip);
        intent.putExtra("mac", mac);
        sendBroadcastMessage(intent);
    }

    private void sendBroadcastRequestUpdateStatus() {
        Log.e(TAG, "sendBroadcastRequestUpdateStatus: ");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_UPDATE_STATUS);
        sendBroadcastMessage(intent);
    }
}