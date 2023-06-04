package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_CONNECT_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_STATE_CHANGED;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_STATUS;
import static com.kynl.ledcube.common.CommonUtils.SHARED_PREFERENCES;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private String lastScanTime, lastScanDevicesList;
    private final Gson gson = new Gson();
    private DeviceListAdapter deviceListAdapter;
    private ImageButton refreshBtn;
    private TextView informationText;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_ADD_SUBNET_DEVICE: {
                        if (deviceListAdapter != null) {
                            String ip = intent.getStringExtra("ip");
                            String mac = intent.getStringExtra("mac");
                            if (!ip.isEmpty() && !mac.isEmpty()) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.runOnUiThread(() -> deviceListAdapter.insertNonExistMacAddress(new Device(ip, mac)));
                                }
                            }
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE: {
//                        Activity activity = getActivity();
//                        if (activity != null) {
//                            activity.runOnUiThread(() -> {
//                                updateLastScanList();
//                                setRefreshButtonEnable(true);
//                            });
//                        }
                        break;
                    }
                    case BROADCAST_SERVICE_STATE_CHANGED:
                    case BROADCAST_SERVICE_UPDATE_STATUS: {
                        NetworkService.NetworkServiceState networkServiceState = (NetworkService.NetworkServiceState) intent.getSerializableExtra("serviceState");
                        switch (networkServiceState) {
                            case STATE_NONE: {
                                setRefreshButtonEnable(true);
                                if (event.equals(BROADCAST_SERVICE_UPDATE_STATUS)) {
                                    updateLastScanList();
                                } else {
                                    setInformationText("Last scan: " + lastScanTime);
                                }
                                break;
                            }
                            case STATE_TRY_TO_CONNECT_DEVICE: {
                                setRefreshButtonEnable(false);
                                setInformationText("Connecting to " + ServerManager.getInstance().getIpAddress() + "...");
                                break;
                            }
                            case STATE_FIND_SUBNET_DEVICES: {
                                setRefreshButtonEnable(false);
                                setInformationText("Scanning...");
                                break;
                            }
                            default: {
                                break;
                            }
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

        lastScanTime = "";
        lastScanDevicesList = "";
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

        /* Recycler view */
        deviceListAdapter = new DeviceListAdapter();
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);
        deviceListAdapter.setOnSubItemClickListener(this::sendBroadcastRequestConnectDevice);

        /* Refresh button */
        refreshBtn.setOnClickListener(v -> refreshDeviceList());

//        updateLastScanList();
        sendBroadcastRequestUpdateStatus();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerBroadcast();
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
        }
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
        setInformationText("Scanning...");
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

    private void sendBroadcastRequestUpdateStatus() {
        Log.e(TAG, "sendBroadcastRequestUpdateStatus: ");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_UPDATE_STATUS);
        sendBroadcastMessage(intent);
    }
}