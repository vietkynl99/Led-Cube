package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.kynl.ledcube.model.Device;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private final String TAG = "SearchFragment";

    private final Gson gson = new Gson();
    private DeviceListAdapter deviceListAdapter;
    private ImageButton refreshBtn;

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
                                    activity.runOnUiThread(() -> {
                                        deviceListAdapter.insertNonExistMacAddress(new Device(ip, mac));
                                    });
                                }
                            }
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_FINISH_FIND_SUBNET_DEVICE: {
                        String jsonDevicesList = intent.getStringExtra("devicesList");
                        if (!jsonDevicesList.isEmpty()) {
                            ArrayList<Device> devicesList = convertStringToDevicesList(jsonDevicesList);
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    deviceListAdapter.syncList(devicesList);
                                    setRefreshButtonEnable(true);
                                });
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

    private ArrayList<Device> convertStringToDevicesList(String jsonString) {
        Type type = new TypeToken<ArrayList<Device>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerBroadcast();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        /* Elements */
        RecyclerView deviceListRecyclerView = view.findViewById(R.id.deviceListRecyclerView);
        refreshBtn = view.findViewById(R.id.refreshBtn);
        TextView informationText = view.findViewById(R.id.informationText);

        /* Recycler view */
        deviceListAdapter = new DeviceListAdapter();
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);

        /* Refresh button */
        refreshBtn.setOnClickListener(v -> {
            refreshDeviceList();
        });

        /* Information text */
        informationText.setText("Last scan: ");

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterBroadcast();
    }

    private void refreshDeviceList() {
        Log.d(TAG, "refreshDeviceList: ");
        setRefreshButtonEnable(false);
        sendBroadcastRequestFindSubnetDevice();
    }

    private void setRefreshButtonEnable(boolean enable) {
        if (refreshBtn != null) {
            refreshBtn.setEnabled(enable);
        }
    }

    private void registerBroadcast() {
        // Register broadcast
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
            context.unregisterReceiver(mBroadcastReceiver);
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
        Log.d(TAG, "sendBroadcastRequestFindSubnetDevice: ");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_FIND_SUBNET_DEVICE);
        sendBroadcastMessage(intent);
    }
}