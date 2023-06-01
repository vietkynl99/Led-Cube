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
import com.kynl.ledcube.model.NetworkDevice;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.CustomViewHolder> {
    private final String TAG = "DeviceListAdapter";
    private List<NetworkDevice> networkDeviceList;
    private int selectedItemPosition;
//        private OnSubItemClickListener onSubItemClickListener;

    public DeviceListAdapter(List<NetworkDevice> networkDeviceList) {
        this.networkDeviceList = networkDeviceList;
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
        NetworkDevice device = networkDeviceList.get(position);
        holder.bind(device.getType(), device.getName(), device.getIp());
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
        return (networkDeviceList != null) ? networkDeviceList.size() : 0;
    }

//        public void setOnSubItemClickListener(OnSubItemClickListener onSubItemClickListener) {
//            this.onSubItemClickListener = onSubItemClickListener;
//        }

    public List<NetworkDevice> getNetworkDeviceList() {
        return networkDeviceList;
    }

    public void updateNewList(List<NetworkDevice> networkDeviceList) {
        Log.e(TAG, "updateList: ");
        this.networkDeviceList = networkDeviceList;
        notifyDataSetChanged();
    }

    public void insertItem(NetworkDevice networkDevice) {
        networkDeviceList.add(networkDevice);
        notifyItemInserted(networkDeviceList.size() - 1);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < networkDeviceList.size() - 1) {
            networkDeviceList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public boolean isExistIp(String ip) {
        boolean exist = false;
        int position = -1;
        for (position = 0; position < networkDeviceList.size(); position++) {
            if (networkDeviceList.get(position).getIp().equals(ip)) {
                exist = true;
            }
        }
        return exist;
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceImage;
        TextView deviceName, deviceIp;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceIp = itemView.findViewById(R.id.deviceIp);
        }

        public void bind(int type, String name, String ip) {
//            deviceImage.setImageResource(iconId);
            deviceName.setText(name);
            deviceIp.setText(ip);
        }
    }
}
