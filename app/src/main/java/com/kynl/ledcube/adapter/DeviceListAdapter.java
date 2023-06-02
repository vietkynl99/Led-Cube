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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.CustomViewHolder> {
    private final String TAG = "DeviceListAdapter";
    private List<Device> deviceList;
    private final ReentrantLock lock = new ReentrantLock();
    private int selectedItemPosition;
//        private OnSubItemClickListener onSubItemClickListener;

    public DeviceListAdapter() {
        this.deviceList = new ArrayList<>();
        selectedItemPosition = -1;
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
        holder.bind(device.getIp(), device.getMac());
//            holder.bind(menuIconList.get(position), position == selectedItemPosition);
//            holder.menuIconLayout.setOnClickListener(v -> {
//                // not use text
//                if (onSubItemClickListener != null) {
//                    onSubItemClickListener.onSubItemClick(position, "");
//                }
//            });
    }

    @Override
    public int getItemCount() {
        return (deviceList != null) ? deviceList.size() : 0;
    }

//        public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
//            this.onSubItemClickListener = onSubItemClickListener;
//        }

    private boolean isExistItem(Device device) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMac().equals(device.getMac()) &&
                    deviceList.get(i).getIp().equals(device.getIp())) {
                return true;
            }
        }
        return false;
    }

    public void syncList(List<Device> newDeviceList) {
        // synchronize the values in the list like the new list
        // avoid changing the order in the old list (user experience)
        lock.lock();
        Log.e(TAG, "syncList: ");
        ArrayList<Boolean> checkList;
        if (deviceList.size() > 0) {
            checkList = new ArrayList<>(Collections.nCopies(deviceList.size(), false));
        } else {
            checkList = new ArrayList<>();
        }
        for (int i = 0; i < newDeviceList.size(); i++) {
            Device newDevice = newDeviceList.get(i);
            if (isExistItem(newDevice)) {
                checkList.set(i, true);
            } else {
                deviceList.add(newDevice);
                checkList.add(true);
                notifyItemInserted(deviceList.size() - 1);
            }
        }
        // remove unchecked device
        for (int i = checkList.size() - 1; i >= 0; i--) {
            if (!checkList.get(i)) {
                deviceList.remove(i);
                notifyItemRemoved(i);
            }
        }
        lock.unlock();
    }

    public void insertNonExistMacAddress(Device device) {
        lock.lock();
        boolean isExist = false;
        int position = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMac().equals(device.getMac())) {
                isExist = true;
                position = i;
                break;
            }
        }
        if (isExist) {
            deviceList.set(position, device);
            notifyItemChanged(position);
        } else {
            deviceList.add(device);
            notifyItemInserted(deviceList.size() - 1);
        }
        lock.unlock();
    }


    static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceImage;
        TextView deviceMac, deviceIp;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            deviceMac = itemView.findViewById(R.id.deviceMac);
        }

        public void bind(String ip, String mac) {
//            deviceImage.setImageResource(iconId);
            deviceIp.setText(ip);
            deviceMac.setText(mac);
        }
    }
}
