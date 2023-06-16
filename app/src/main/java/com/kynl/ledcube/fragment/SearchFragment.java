package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_RESPONSE;
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
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.DeviceListAdapter;
import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.manager.SharedPreferencesManager;
import com.kynl.ledcube.model.Device;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;

public class SearchFragment extends Fragment {
    private final String TAG = "SearchFragment";
    private final Gson gson = new Gson();
    private final Handler handler = new Handler();
    private DeviceListAdapter deviceListAdapter;
    private ImageButton refreshBtn;
    private TextView informationText;
    private ProgressBar progressBar, circleProgressBar;

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
                                setRefreshEnable(true);
                            });
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_SERVER_RESPONSE: {
                        ServerState serverState = (ServerState) intent.getSerializableExtra("serverState");
                        if (serverState == ServerState.SERVER_STATE_DISCONNECTED) {
                            deviceListAdapter.setConnectingDeviceMac("");
                        } else {
//                            deviceListAdapter.setConnectedDeviceMac(ServerManager.getInstance().getSavedMacAddress());
                            deviceListAdapter.setSavedDeviceMac(ServerManager.getInstance().getSavedMacAddress());
                        }
//                        if (serverState == ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED) {
//                            deviceListAdapter.setConnectedDeviceState(Device.DeviceState.STATE_CONNECTED_BUT_NOT_PAIRED);
//                        } else if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
//                            deviceListAdapter.setConnectedDeviceState(Device.DeviceState.STATE_CONNECTED_AND_PAIRED);
//                        }
                        // Show message
                        String message = intent.getStringExtra("message");
                        if (!message.isEmpty() && isVisible()) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_STATE_CHANGED: {
                        NetworkServiceState networkServiceState = (NetworkServiceState) intent.getSerializableExtra("networkServiceState");
                        if (networkServiceState == NetworkServiceState.STATE_NONE) {
                            setRefreshEnable(true);
                            deviceListAdapter.setConnectingDeviceMac("");
                            if (progressBar != null) {
                                progressBar.setProgress(0);
                            }
                        } else {
                            setRefreshEnable(false);
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_UPDATE_STATUS: {
                        NetworkServiceState networkServiceState = (NetworkServiceState) intent.getSerializableExtra("networkServiceState");
                        if (networkServiceState == NetworkServiceState.STATE_TRY_TO_CONNECT_DEVICE) {
                            deviceListAdapter.setConnectingDeviceMac(ServerManager.getInstance().getMacAddress());
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        /* Elements */
        RecyclerView deviceListRecyclerView = view.findViewById(R.id.deviceListRecyclerView);
        ImageButton backBtn = view.findViewById(R.id.searchBackBtn);
        refreshBtn = view.findViewById(R.id.refreshBtn);
        informationText = view.findViewById(R.id.informationText);
        progressBar = view.findViewById(R.id.progressBar);
        circleProgressBar = view.findViewById(R.id.circleProgressBar);

        /* Back button */
        backBtn.setOnClickListener(v -> BroadcastManager.getInstance().sendRequestChangeToHomeScreen());

        /* Recycler view */
        deviceListAdapter = new DeviceListAdapter();
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);
        deviceListAdapter.setOnSubItemClickListener((ip, mac) -> {
            // Debounce
            if (!isDebouncing) {
                isDebouncing = true;
                deviceListAdapter.setConnectingDeviceMac(mac);
                setRefreshEnable(false);
                handler.postDelayed(() -> {
                    isDebouncing = false;
                    BroadcastManager.getInstance().sendRequestPairDevice(ip, mac);
                }, 1000);
            }
        });
        deviceListAdapter.setSavedDeviceMac(ServerManager.getInstance().getSavedMacAddress());
        deviceListAdapter.setConnectingDeviceMac("");

        /* Refresh button */
        refreshBtn.setOnClickListener(v -> refreshDeviceList());

        updateLastScanList();
        BroadcastManager.getInstance().registerBroadcast(mBroadcastReceiver);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        BroadcastManager.getInstance().sendRequestUpdateStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BroadcastManager.getInstance().unRegisterBroadcast(mBroadcastReceiver);
    }

    private void updateLastScanList() {
        String lastScanTime = SharedPreferencesManager.getInstance().getLastScanTime();
        String lastScanDevicesList = SharedPreferencesManager.getInstance().getLastScanDevicesList();
        if (!lastScanDevicesList.isEmpty() && !lastScanTime.isEmpty()) {
            ArrayList<Device> devicesList = convertStringToDevicesList(lastScanDevicesList);
            setInformationText("Last scan: " + lastScanTime);
            deviceListAdapter.syncList(devicesList);
            deviceListAdapter.setSavedDeviceMac(ServerManager.getInstance().getSavedMacAddress());
        }
    }

    private ArrayList<Device> convertStringToDevicesList(String jsonString) {
        Type type = new TypeToken<ArrayList<Device>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }


    private void refreshDeviceList() {
        Log.d(TAG, "refreshDeviceList: ");
        setRefreshEnable(false);
        BroadcastManager.getInstance().sendRequestFindSubnetDevice();
    }

    private void setRefreshEnable(boolean enable) {
        if (refreshBtn != null && circleProgressBar != null) {
            refreshBtn.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
            circleProgressBar.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setInformationText(String text) {
        if (informationText != null) {
            informationText.setText(text);
        }
    }
}