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
import android.widget.Toast;

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

import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private OptionListAdapter optionListAdapter;
    private EffectListAdapter effectListAdapter;
    private ImageView iconStatus, batteryIcon;
    private TextView textStatus, textBatteryLevel, textTemperature, textHumidity;
    private int batteryLevel, temperature, humidity;
    private boolean connected;

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
                        String data = intent.getStringExtra("data");
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            setBatteryLevel(Integer.parseInt(jsonObject.getString("bat")));
                            setTemperature(Integer.parseInt(jsonObject.getString("temp")));
                            setHumidity(Integer.parseInt(jsonObject.getString("hum")));
                        } catch (Exception e) {
                            Log.e(TAG, "handleResponseFromServer: Invalid data: " + data);
                        }
                        break;
                    }
                    case BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS: {
                        restoreDefaultSettings();
                        break;
                    }
                    default: {
                        break;
                    }
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
        temperature = 0;
        humidity = 0;
        connected = false;
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
        textTemperature = view.findViewById(R.id.textTemperature);
        textHumidity = view.findViewById(R.id.textHumidity);
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
            } else {
                selectEffectType(EffectItem.EffectType.OFF);
            }
        });

        BroadcastManager.getInstance().registerBroadcast(mBroadcastReceiver);

        setBatteryLevel(-1);
        setTemperature(-1);
        setHumidity(-1);

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
        //TODO: reload all lists
        EffectManager.getInstance().createDefaultList();
        selectEffectType(EffectManager.getInstance().getCurrentEffectType());
    }

    private void updateStatus(boolean connected) {
        if (this.connected != connected) {
            if (isVisible()) {
                if (connected) {
                    Toast.makeText(getContext(), "Connected to device!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Disconnected to device!", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
        this.connected = connected;
    }

    private void setBatteryLevel(int level) {
        if (batteryLevel != level) {
            batteryLevel = level;
            String text = level >= 0 ? String.valueOf(level) : "--";
            text = text + getResources().getString(R.string.unit_percent);
            if (textBatteryLevel != null) {
                textBatteryLevel.setText(text);
            }
            if (batteryIcon != null) {
                batteryIcon.setImageLevel(level >= 0 ? (int) Math.round(level / 20.0) : 0);
            }
        }
    }

    private void setTemperature(int temp) {
        if (temperature != temp) {
            temperature = temp;
            String text = temp >= 0 ? String.valueOf(temp) : "--";
            text = text + getResources().getString(R.string.unit_temperature);
            if (textTemperature != null) {
                textTemperature.setText(text);
            }
        }
    }

    private void setHumidity(int hum) {
        if (humidity != hum) {
            humidity = hum;
            String text = hum >= 0 ? String.valueOf(hum) : "--";
            text = text + getResources().getString(R.string.unit_percent);
            if (textHumidity != null) {
                textHumidity.setText(text);
            }
        }
    }
}