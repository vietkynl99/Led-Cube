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
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.CustomViewHolder> {
    private final String TAG = "DeviceListAdapter";
    private final ReentrantLock lock = new ReentrantLock();
    private List<Device> deviceList;
    private String connectingDeviceMac;
    private int connectingDevicePosition;
    private String savedDeviceMac;
    private int savedDevicePosition;
    private OnSubItemClickListener onSubItemClickListener;

    public DeviceListAdapter() {
        this.deviceList = new ArrayList<>();
        this.connectingDeviceMac = "";
        this.savedDeviceMac = "";
        this.connectingDevicePosition = -1;
        this.savedDevicePosition = -1;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_view_item, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device, savedDeviceMac, connectingDeviceMac);
        holder.setOnSubItemClickListener(onSubItemClickListener);
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
        this.onSubItemClickListener = onSubItemClickListener;
    }

    public void setConnectingDeviceMac(String connectingDeviceMac) {
        if (!this.connectingDeviceMac.equals(connectingDeviceMac)) {
            this.connectingDeviceMac = connectingDeviceMac;
            if (connectingDevicePosition >= 0) {
                notifyItemChanged(connectingDevicePosition);
            }
            if (!connectingDeviceMac.isEmpty()) {
                int position = findMacDevicePosition(connectingDeviceMac);
                if (position >= 0) {
                    connectingDevicePosition = position;
                    notifyItemChanged(connectingDevicePosition);
                }
            }
        }
    }

    public void setSavedDeviceMac(String savedDeviceMac) {
        if (!this.savedDeviceMac.equals(savedDeviceMac)) {
            this.savedDeviceMac = savedDeviceMac;
            if (savedDevicePosition >= 0) {
                notifyItemChanged(savedDevicePosition);
            }
            if (!savedDeviceMac.isEmpty()) {
                int position = findMacDevicePosition(savedDeviceMac);
                if (position >= 0) {
                    savedDevicePosition = position;
                    notifyItemChanged(savedDevicePosition);
                }
            }
        }
    }

    private int findMacDevicePosition(String mac) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMac().equals(mac)) {
                return i;
            }
        }
        return -1;
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
        private final ViewGroup mainItemView;
        private final TextView deviceMac, deviceIp, devicePing, deviceConnecting;
        private final ImageView deviceConnected;
        private OnSubItemClickListener onSubItemClickListener;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            mainItemView = itemView.findViewById(R.id.mainItemView);
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

        public void bind(Device device, String savedDeviceMac, String connectingDeviceMac) {
            String pingText = device.getPing() + "ms";
            deviceIp.setText(device.getIp());
            deviceMac.setText(device.getMac());
            devicePing.setText(pingText);
            deviceConnecting.setVisibility(device.getMac().equals(connectingDeviceMac) ? View.VISIBLE : View.GONE);
            deviceConnected.setVisibility(!savedDeviceMac.isEmpty() && device.getMac().equals(savedDeviceMac) ?
                    View.VISIBLE : View.GONE);
        }

        public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
            this.onSubItemClickListener = onSubItemClickListener;
        }
    }
}
