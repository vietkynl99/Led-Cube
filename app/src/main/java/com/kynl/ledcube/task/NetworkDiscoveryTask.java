package com.kynl.ledcube.task;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.kynl.ledcube.adapter.DeviceListAdapter;
import com.kynl.ledcube.model.NetworkDevice;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NetworkDiscoveryTask extends AsyncTask<String, NetworkDiscoveryTask.ProgessData, List<NetworkDevice>> {
    private final String TAG = "NetworkDiscoveryTask";
    private Context context;
    private final ReentrantLock myLock = new ReentrantLock();
    private final int startAddress = 2;
    private final int endAddress = 254;
    private final int totalDeviceCount = Math.abs(endAddress - startAddress) + 1;
    private volatile boolean scanning;
    private int scannedDeviceCount;
    private String myIpHeader;
    private List<NetworkDevice> foundDeviceList;
    private final DeviceListAdapter deviceListAdapter;
    private final TextView informationText;

    public class ProgessData {
        private final NetworkDevice networkDevice;
        private final boolean isSuccess;
        private final int scannedDevices;

        public ProgessData(NetworkDevice networkDevice, boolean isSuccess, int scannedDevices) {
            this.networkDevice = networkDevice;
            this.isSuccess = isSuccess;
            this.scannedDevices = scannedDevices;
        }

        public NetworkDevice getNetworkDevice() {
            return networkDevice;
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public int getScannedDevices() {
            return scannedDevices;
        }
    }


    public NetworkDiscoveryTask(Context context, DeviceListAdapter deviceListAdapter, TextView informationText) {
        this.context = context;
        this.deviceListAdapter = deviceListAdapter;
        this.informationText = informationText;
        scanning = false;
        scannedDeviceCount = 0;
        foundDeviceList = new ArrayList<>();
        myIpHeader = "";
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.e(TAG, "onPreExecute: ");
        myIpHeader = getIpAddressHeader();
        if (myIpHeader.equals("")) {
            Log.e(TAG, "discoverDevices: Device is not connect to internet. Cancel task!");
            cancel(true);
        }

        scanning = true;
        scannedDeviceCount = 0;
        foundDeviceList = new ArrayList<>();
        informationText.setText("Scanning...");
    }

    @Override
    protected List<NetworkDevice> doInBackground(String... params) {
        Log.e(TAG, "doInBackground: Start discover devices...");

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                    }
                });

        // Start loop
        for (int index = startAddress; index <= endAddress; index++) {
            String ip = myIpHeader + index;
            InetSocketAddress address = new InetSocketAddress(ip, 80);
            ChannelFuture channelFuture = bootstrap.connect(address);
            channelFuture.addListener((ChannelFutureListener) future -> {
                myLock.lock();
                scannedDeviceCount++;
                NetworkDevice device = new NetworkDevice(0, "Device", ip);
                publishProgress(new ProgessData(device, future.isSuccess(), scannedDeviceCount));

                if (scannedDeviceCount >= totalDeviceCount) {
                    scanning = false;
                    eventLoopGroup.shutdownGracefully();
                }
                myLock.unlock();
            });
        }

        // wait until finish
        while (true) {
            myLock.lock();
            if (!scanning) {
                break;
            }
            myLock.unlock();
        }

        return foundDeviceList;
    }


    @Override
    protected void onProgressUpdate(ProgessData... values) {
        super.onProgressUpdate(values);
        ProgessData progessData = values[0];
        NetworkDevice device = progessData.getNetworkDevice();

        if (progessData.isSuccess()) {
            Log.e(TAG, "onProgressUpdate: found device " + device.getIp());
        }

        List<NetworkDevice> networkDeviceList = deviceListAdapter.getNetworkDeviceList();
        boolean hasInList = false;
        int position = -1;
        for (position = 0; position < networkDeviceList.size(); position++) {
            if (networkDeviceList.get(position).getIp().equals(device.getIp())) {
                hasInList = true;
            }
        }

        // update information
        int percent = 100 * progessData.getScannedDevices() / totalDeviceCount;
        informationText.setText("Scanned " + progessData.getScannedDevices() + "/" + totalDeviceCount + " (" + percent + "%)");

        // update to list
        if (progessData.isSuccess && !hasInList) {
            deviceListAdapter.insertItem(device);
        } else if (!progessData.isSuccess() && hasInList) {
            deviceListAdapter.removeItem(position);
        }
    }

    @Override
    protected void onPostExecute(List<NetworkDevice> networkDevices) {
        super.onPostExecute(networkDevices);
        Log.e(TAG, "onPostExecute: done");
        // update information
        Date currentTime = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
        String currentTimeString = dateFormat.format(currentTime);
        informationText.setText("Last scan: " + currentTimeString);
    }

    private String getIpAddressHeader() {
        if (context == null) {
            Log.e(TAG, "getIpAddress: Context is null");
            return "";
        }
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff));
    }


}
