package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_FIND_SUBNET_DEVICE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_SEND_DATA;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_STATUS_CHANGED;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.EffectListAdapter;
import com.kynl.ledcube.adapter.OptionListAdapter;
import com.kynl.ledcube.manager.EffectManager;
import com.kynl.ledcube.manager.ServerManager;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.common.CommonUtils.ConnectionState;

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private ImageView iconStatus, batteryIcon;
    private TextView textStatus, textBatteryLevel;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_SERVER_STATUS_CHANGED: {
                        ServerState serverState = (ServerState) intent.getSerializableExtra("serverState");
                        updateStatus(serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");

        EffectManager.getInstance().init(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        /* Elements */
        iconStatus = view.findViewById(R.id.iconStatus);
        textStatus = view.findViewById(R.id.textStatus);
        batteryIcon = view.findViewById(R.id.batteryIcon);
        textBatteryLevel = view.findViewById(R.id.textBattery);
        RecyclerView effectListRecyclerView = view.findViewById(R.id.effectListRecyclerView);
        RecyclerView optionListRecyclerView = view.findViewById(R.id.optionListRecyclerView);

        /* Option Recycler view */
        OptionListAdapter optionListAdapter = new OptionListAdapter(EffectManager.getInstance().getEffectItemList());
        optionListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        optionListRecyclerView.setAdapter(optionListAdapter);
        optionListAdapter.select(EffectManager.getInstance().getCurrentEffectType());

        optionListAdapter.setOnOptionValueChangeListener((effectType, optionType, value) -> {
            Log.d(TAG, "Option data changed: " + effectType + " " + optionType + " " + value);
            EffectManager.getInstance().setOptionValue(effectType, optionType, value);
            // TODO: send data to server
            String data = EffectManager.getInstance().getEffectDataAsJson(effectType);
            sendBroadcastSendData(data);
        });

        /* Effect Recycler view */
        EffectListAdapter effectListAdapter = new EffectListAdapter(EffectManager.getInstance().getEffectItemList());
        effectListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        effectListRecyclerView.setAdapter(effectListAdapter);
        effectListAdapter.select(EffectManager.getInstance().getCurrentEffectType());

        effectListAdapter.setOnEffectItemClickListener(type -> {
            EffectManager.getInstance().setCurrentEffectType(type);
            effectListAdapter.select(type);
            optionListAdapter.select(type);
        });


        registerBroadcast();

        setBatteryLevel(60);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        unRegisterBroadcast();
    }

    private void registerBroadcast() {
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
            try {
                context.unregisterReceiver(mBroadcastReceiver);
            } catch (Exception ignored) {

            }
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

    private void sendBroadcastSendData(String data) {
        Log.e(TAG, "sendBroadcastSendData: " + data);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("event", BROADCAST_REQUEST_SEND_DATA);
        intent.putExtra("data", data);
        sendBroadcastMessage(intent);
    }

    private void updateStatus(boolean connected) {
        if (iconStatus != null && textStatus != null) {
            if (connected) {
                iconStatus.setImageResource(R.drawable.sensors_48);
                textStatus.setText(getResources().getString(R.string.online));
            } else {
                iconStatus.setImageResource(R.drawable.sensors_off_48);
                if (ServerManager.getInstance().hasSavedDevice()) {
                    textStatus.setText(getResources().getString(R.string.offline));
                } else {
                    textStatus.setText(getResources().getString(R.string.no_device));
                }
            }
        }
    }

    private void setBatteryLevel(int level) {
        if (level < 0) {
            level = 0;
        }
        if (level > 100) {
            level = 100;
        }
        String text = level + "%";
        textBatteryLevel.setText(text);
        batteryIcon.setImageLevel((int) Math.round(level / 20.0));
    }
}