package com.kynl.ledcube.adapter;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.Device;
import com.kynl.ledcube.myinterface.OnSubItemClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.channel.epoll.EpollServerChannelConfig;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.CustomViewHolder> {
    private final String TAG = "DeviceListAdapter";
    private final ReentrantLock lock = new ReentrantLock();
    private List<Device> deviceList;
    private String connectedDeviceMac;
    private Device.DeviceState connectedDeviceState;
    private OnSubItemClickListener onSubItemClickListener;

    public DeviceListAdapter() {
        this.deviceList = new ArrayList<>();
        connectedDeviceMac = "";
        connectedDeviceState = Device.DeviceState.STATE_NONE;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device, connectedDeviceMac, connectedDeviceState);
        holder.setOnSubItemClickListener(onSubItemClickListener);
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
        this.onSubItemClickListener = onSubItemClickListener;
    }

    public void setConnectedDeviceMac(String connectedDeviceMac) {
        this.connectedDeviceMac = connectedDeviceMac;
    }

    public void setConnectedDeviceState(Device.DeviceState connectedDeviceState) {
        this.connectedDeviceState = connectedDeviceState;
    }

    private boolean isExistItem(Device device) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).equals(device)) {
                return true;
            }
        }
        return false;
    }

    private int findMacDevicePosition(String mac) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMac().equals(mac)) {
                return i;
            }
        }
        return -1;
    }

    public void setConnectingDevice(String mac) {
        // find position by MAC
        lock.lock();
        int position = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMac().equals(mac)) {
                position = i;
            }
            // reset old state
            if (deviceList.get(i).getDeviceState() == Device.DeviceState.STATE_CONNECTING &&
                    !deviceList.get(i).getMac().equals(mac)) {
                deviceList.get(i).setDeviceState(Device.DeviceState.STATE_NONE);
                notifyItemChanged(i);
            }
        }
        if (position >= 0) {
            deviceList.get(position).setDeviceState(Device.DeviceState.STATE_CONNECTING);
            notifyItemChanged(position);
        }
        lock.unlock();
    }

    public void resetConnectingDevice() {
        lock.lock();
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getDeviceState() == Device.DeviceState.STATE_CONNECTING) {
                deviceList.get(i).setDeviceState(Device.DeviceState.STATE_NONE);
                notifyItemChanged(i);
            }
        }
        lock.unlock();
    }

    public void syncList(List<Device> newDeviceList) {
        // synchronize the values in the list like the new list
        // avoid changing the order in the old list (user experience)
        lock.lock();
        Log.i(TAG, "syncList: ");
        deviceList = new ArrayList<>();
        deviceList.addAll(newDeviceList);
        notifyDataSetChanged();
        lock.unlock();
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final TextView deviceMac, deviceIp, devicePing, deviceConnecting;
        private final ImageView deviceConnected;
        private OnSubItemClickListener onSubItemClickListener;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            ViewGroup mainItemView = itemView.findViewById(R.id.mainItemView);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            deviceMac = itemView.findViewById(R.id.deviceMac);
            devicePing = itemView.findViewById(R.id.devicePing);
            deviceConnecting = itemView.findViewById(R.id.deviceConnecting);
            deviceConnected = itemView.findViewById(R.id.deviceConnected);

            mainItemView.setOnClickListener(v -> {
                if (onSubItemClickListener != null) {
                    onSubItemClickListener.onSubItemClick((String) deviceIp.getText(), (String) deviceMac.getText());
                }
            });
        }

        public void bind(Device device, String connectedDeviceMac, Device.DeviceState connectedDeviceState) {
            String pingText = device.getPing() + "ms";
            Device.DeviceState state = device.getDeviceState();
            boolean isConnectedDevice = device.getMac().equals(connectedDeviceMac);
            deviceIp.setText(device.getIp());
            deviceMac.setText(device.getMac());
            devicePing.setText(pingText);
            deviceConnecting.setVisibility(state == Device.DeviceState.STATE_CONNECTING ? View.VISIBLE : View.GONE);
            // Use for connected device
            deviceConnected.setVisibility(isConnectedDevice ? View.VISIBLE : View.GONE);
            deviceConnected.setImageResource(connectedDeviceState == Device.DeviceState.STATE_CONNECTED_AND_PAIRED ?
                    R.drawable.checked_g_48 : R.drawable.checked_w_30);
        }

        public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
            this.onSubItemClickListener = onSubItemClickListener;
        }
    }
}
