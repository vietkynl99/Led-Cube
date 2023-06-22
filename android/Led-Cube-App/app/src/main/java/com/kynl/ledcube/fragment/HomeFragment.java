package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_RESPONSE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_SERVER_DATA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
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
import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.EffectManager;
import com.kynl.ledcube.manager.ServerManager;

import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.manager.SharedPreferencesManager;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private OptionListAdapter optionListAdapter;
    private EffectListAdapter effectListAdapter;
    private ImageView iconStatus, batteryIcon;
    private TextView textStatus, textBatteryLevel;
    private int batteryLevel;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_SERVER_RESPONSE: {
                        ServerState serverState = (ServerState) intent.getSerializableExtra("serverState");
                        updateStatus(serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED);
                        break;
                    }
                    case BROADCAST_SERVICE_UPDATE_SERVER_DATA: {
                        int batteryLevel = intent.getIntExtra("batteryLevel", -1);
                        if (batteryLevel >= 0) {
                            setBatteryLevel(batteryLevel);
                        }
                    }
                    case BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS: {
                        restoreDefaultSettings();
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

        batteryLevel = 0;
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
        optionListAdapter = new OptionListAdapter(EffectManager.getInstance().getEffectItemList());
        optionListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        optionListRecyclerView.setAdapter(optionListAdapter);
        optionListAdapter.select(EffectManager.getInstance().getCurrentEffectType());

        optionListAdapter.setOnOptionValueChangeListener((effectType, optionType, value) -> {
            Log.d(TAG, "Option data changed: " + effectType + " " + optionType + " " + value);
            EffectManager.getInstance().setOptionValue(effectType, optionType, value);
            if (optionType == OptionItem.OptionType.BRIGHTNESS &&
                    SharedPreferencesManager.getInstance().isSyncBrightness()) {
                EffectManager.getInstance().synchronizeAllEffects(optionType, value);
            }
            // Send data to server
            String data = EffectManager.getInstance().getEffectDataAsJson(effectType);
            BroadcastManager.getInstance().sendRequestSendData(data);
        });

        /* Effect Recycler view */
        effectListAdapter = new EffectListAdapter(EffectManager.getInstance().getEffectItemList());
        effectListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        effectListRecyclerView.setAdapter(effectListAdapter);
        effectListAdapter.select(EffectManager.getInstance().getCurrentEffectType());

        effectListAdapter.setOnEffectItemClickListener(type -> {
            EffectItem.EffectType preType = EffectManager.getInstance().getCurrentEffectType();
            if (type != preType) {
                selectEffectType(type);
            }
        });

        BroadcastManager.getInstance().registerBroadcast(mBroadcastReceiver);

        setBatteryLevel(-1);

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
        BroadcastManager.getInstance().unRegisterBroadcast(mBroadcastReceiver);
    }

    private void selectEffectType(EffectItem.EffectType type) {
        if (effectListAdapter != null && optionListAdapter != null) {
            EffectManager.getInstance().setCurrentEffectType(type);
            effectListAdapter.select(type);
            optionListAdapter.select(type);
            // Send data to server
            String data = EffectManager.getInstance().getEffectDataAsJson(type);
            BroadcastManager.getInstance().sendRequestSendData(data);
        }
    }

    private void restoreDefaultSettings() {
        Log.d(TAG, "restoreDefaultSettings: ");
        EffectManager.getInstance().setDefaultValue();
        selectEffectType(EffectManager.getInstance().getCurrentEffectType());
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
        if (batteryLevel != level) {
            batteryLevel = level;
            if (level < 0) {
                String text = "--%";
                textBatteryLevel.setText(text);
                batteryIcon.setImageLevel(0);
                return;
            }
            level = Math.min(100, level);
            String text = level + "%";
            textBatteryLevel.setText(text);
            batteryIcon.setImageLevel((int) Math.round(level / 20.0));
        }
    }
}