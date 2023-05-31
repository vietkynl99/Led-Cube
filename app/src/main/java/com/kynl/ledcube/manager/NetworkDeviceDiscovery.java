package com.kynl.ledcube.manager;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.kynl.ledcube.model.NetworkDevice;

import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

public class NetworkDeviceDiscovery {

    private static final String TAG = "NetworkDeviceDiscovery";
    private Context context;
    private final ReentrantLock myLock = new ReentrantLock();
    private final int startAddress = 2;
    private final int endAddress = 254;
    private boolean scanning;
    private int scannedDeviceCount;
    private List<NetworkDevice> foundDeviceList;

    public NetworkDeviceDiscovery(Context context) {
        this.context = context;
        resetDiscoverState();
    }

    public boolean isScanning() {
        return scanning;
    }

    public int getScannedDeviceCount() {
        return scannedDeviceCount;
    }

    public List<NetworkDevice> getFoundDeviceCount() {
        return foundDeviceList;
    }

    private void resetDiscoverState() {
        scanning = false;
        scannedDeviceCount = 0;
        foundDeviceList = new ArrayList<>();
    }

    private String getIpAddress() {
        if (context == null) {
            Log.e(TAG, "getIpAddress: Context is null");
            return "";
        }
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
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

    public void discoverDevices() {
        Log.e(TAG, "discoverDevices: Start discover devices...");

        String myIp = getIpAddress();
        Log.e(TAG, "discoverDevices: Get ip address: " + myIp);
        if (myIp.equals("")) {
            Log.e(TAG, "discoverDevices: Device is not connect to internet!");
            return;
        }
        String myIpHeader = getIpAddressHeader();

        resetDiscoverState();
        scanning = true;

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
//                        ChannelPipeline pipeline = ch.pipeline();
//                        pipeline.addLast(new StringDecoder());
//                        ChannelPipeline entries = pipeline.addLast(new SimpleChannelInboundHandler<String>() {
//                            @Override
//                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
//                                Log.e(TAG, "channelRead0: " + msg);
//                            }
//                        });
                    }
                });

        for (int index = startAddress; index <= endAddress; index++) {
            String ip = myIpHeader + index;
            InetSocketAddress address = new InetSocketAddress(ip, 80);
            ChannelFuture channelFuture = bootstrap.connect(address);
            channelFuture.addListener((ChannelFutureListener) future -> {
                myLock.lock();
                scannedDeviceCount++;
                if (future.isSuccess() && !ip.equals(myIp)) {
                    foundDeviceList.add(new NetworkDevice(ip));
                    Log.e(TAG, "Found " + address.getHostName() + ". Scanned: " + scannedDeviceCount + " found: " + foundDeviceList.size());
                }

                if (scannedDeviceCount >= (endAddress - startAddress + 1)) {
                    Log.e(TAG, "startTest: Done!!!");
                    scanning = false;
                    eventLoopGroup.shutdownGracefully();
                }
                myLock.unlock();
            });
        }


    }


}
