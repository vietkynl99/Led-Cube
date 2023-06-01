package com.kynl.ledcube.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
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

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        networkDeviceList = new ArrayList<>();
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
//            NetworkDiscoveryTask networkDiscoveryTask = new NetworkDiscoveryTask(getContext(), deviceListAdapter, informationText);
//            networkDiscoveryTask.execute();

            findSubnetDevices();
        });

        /* Information text */
        informationText.setText("Last scan: ");

        return view;
    }


    private void findSubnetDevices() {
        Log.e(TAG, "findSubnetDevices: ");
        final long startTimeMillis = System.currentTimeMillis();
        SubnetDevices subnetDevices = SubnetDevices.fromLocalAddress().findDevices(new SubnetDevices.OnSubnetDeviceFound() {
            @Override
            public void onDeviceFound(Device device) {
                Log.e(TAG, "onDeviceFound: " + device.time + " " + device.ip + " " + device.mac + " " + device.hostname);
            }
            @Override
            public void onFinished(ArrayList<Device> devicesFound) {
                float timeTaken = (System.currentTimeMillis() - startTimeMillis) / 1000.0f;
                Log.e(TAG, "onFinished: Found " + devicesFound.size());
            }
        });

        // Below is example of how to cancel a running scan
        // subnetDevices.cancel();

    }
}