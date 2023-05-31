package com.kynl.ledcube.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.DeviceListAdapter;
import com.kynl.ledcube.model.NetworkDevice;

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


        // menu
        networkDeviceList = new ArrayList<>();
        networkDeviceList.add(new NetworkDevice(0, "Device 1", "192.168.1.1"));
        networkDeviceList.add(new NetworkDevice(0, "Device 2", "192.168.1.2"));
        networkDeviceList.add(new NetworkDevice(0, "Device 3", "192.168.1.3"));
        networkDeviceList.add(new NetworkDevice(0, "Device 4", "192.168.1.4"));


//        menuRecyclerViewAdapter = new MenuRecyclerViewAdapter(menuElementIconIdList);
//        menuRecyclerViewAdapter.setOnSubItemClickListener((position, text) -> changeFragment(position));
//        RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
//        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//        menuRecyclerView.setAdapter(menuRecyclerViewAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);


        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(networkDeviceList);
        RecyclerView deviceListRecyclerView = view.findViewById(R.id.deviceListRecyclerView);
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        deviceListRecyclerView.setAdapter(deviceListAdapter);

        return view;
    }
}