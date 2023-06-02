package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_ADD_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_FIND_SUBNET_DEVICE_FINISH;

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

import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.DeviceListAdapter;
import com.kynl.ledcube.model.NetworkDevice;
import com.kynl.ledcube.nettool.Device;
import com.kynl.ledcube.nettool.SubnetDevices;
import com.kynl.ledcube.task.NetworkDiscoveryTask;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private final String TAG = "SearchFragment";
    List<NetworkDevice> networkDeviceList;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get boardcast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_ADD_SUBNET_DEVICE: {
                        break;
                    }
                    case BROADCAST_SERVICE_FIND_SUBNET_DEVICE_FINISH: {
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

        networkDeviceList = new ArrayList<>();
        registerBroadcast();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        /* Elements */
        RecyclerView deviceListRecyclerView = view.findViewById(R.id.deviceListRecyclerView);
        ImageButton refreshBtn = view.findViewById(R.id.refreshBtn);
        TextView informationText = view.findViewById(R.id.informationText);

        /* Recycler view */
        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(networkDeviceList);
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);

        /* Refresh button */
        refreshBtn.setOnClickListener(v -> {
            Log.e(TAG, "onCreateView: Refresh button clicked");
            sendBroadcastRequestFindSubnetDevice();
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

    private void registerBroadcast() {
        // Register broadcast
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(BROADCAST_ACTION));
    }

    private void unRegisterBroadcast() {
        try {
            getContext().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    private void sendBroadcastMessage(Intent intent) {
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void sendBroadcastRequestFindSubnetDevice() {
        Log.d(TAG, "sendBroadcastRequestFindSubnetDevice: ");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_FIND_SUBNET_DEVICE);
        sendBroadcastMessage(intent);
    }
}