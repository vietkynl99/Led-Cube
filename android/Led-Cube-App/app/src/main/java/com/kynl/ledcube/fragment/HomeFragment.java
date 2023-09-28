package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_REQUEST_RESTORE_DEFAULT_SETTINGS;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_NOTIFY_MESSAGE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_SERVER_RESPONSE;
import static com.kynl.ledcube.common.CommonUtils.BROADCAST_SERVICE_UPDATE_SERVER_DATA;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.kynl.ledcube.model.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private final Handler handler = new Handler();
    private OptionListAdapter optionListAdapter;
    private EffectListAdapter effectListAdapter;
    private ImageView iconStatus, batteryIcon;
    private TextView textStatus, textBatteryLevel, textTemperature, textHumidity, textPairing;
    private ViewGroup gamePanel;
    private RecyclerView optionListRecyclerView;
    private int batteryLevel, temperature, humidity;
    private ServerState serverState;
    private boolean isDebouncing = false;
    private ServerMessage.CommandType preCommandType;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_SERVER_RESPONSE: {
                        ServerState serverState = (ServerState) intent.getSerializableExtra("serverState");
                        updateStatus(serverState);
                        break;
                    }
                    case BROADCAST_SERVICE_NOTIFY_MESSAGE: {
                        String message = intent.getStringExtra("message");
                        if (message != null) {
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    if (isVisible()) {
                                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                        break;
                    }
                    case BROADCAST_SERVICE_UPDATE_SERVER_DATA: {
                        String data = intent.getStringExtra("data");
                        if (data != null) {
                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                setBatteryLevel(Integer.parseInt(jsonObject.getString("bat")));
                                setTemperature(Integer.parseInt(jsonObject.getString("temp")));
                                setHumidity(Integer.parseInt(jsonObject.getString("hum")));
                            } catch (Exception e) {
                                Log.e(TAG, "handleResponseFromServer: Invalid data: " + data);
                            }
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
        serverState = ServerState.SERVER_STATE_DISCONNECTED;
        preCommandType = ServerMessage.CommandType.COMMAND_MAX;
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
        optionListRecyclerView = view.findViewById(R.id.optionListRecyclerView);
        gamePanel = view.findViewById(R.id.game_panel);
        JoystickView joystickView = view.findViewById(R.id.joystick);
        Button startButton = view.findViewById(R.id.start_button);

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
        effectListRecyclerView.scrollToPosition(EffectManager.getInstance().getCurrentEffectPosition());
        effectListAdapter.select(EffectManager.getInstance().getCurrentEffectType());
        setGameModeEnable(EffectManager.getInstance().isGameMode());

        effectListAdapter.setOnEffectItemClickListener(type -> {
            EffectItem.EffectType preType = EffectManager.getInstance().getCurrentEffectType();
            if (type != preType) {
                selectEffectType(type);
            } else {
                selectEffectType(EffectItem.EffectType.OFF);
            }
            setGameModeEnable(EffectManager.getInstance().isGameMode());
        });

        /* Game Controller */
        joystickView.setOnMoveListener((angle, strength) -> {
            if (strength > 70) {
                if (angle < 30 || angle > 330) {
                    sendGameCommandData(ServerMessage.CommandType.COMMAND_GAME_RIGHT);
                } else if (angle > 60 && angle < 120) {
                    sendGameCommandData(ServerMessage.CommandType.COMMAND_GAME_UP);
                } else if (angle > 150 && angle < 210) {
                    sendGameCommandData(ServerMessage.CommandType.COMMAND_GAME_LEFT);
                } else if (angle > 240 && angle < 300) {
                    sendGameCommandData(ServerMessage.CommandType.COMMAND_GAME_DOWN);
                }
            }
        });

        startButton.setOnClickListener(v -> {
            sendGameCommandData(ServerMessage.CommandType.COMMAND_GAME_START);
        });

        /* Pair */
        textPairing = view.findViewById(R.id.textPairing);
        textPairing.setVisibility(View.INVISIBLE);
        ImageView imageDemoCube = view.findViewById(R.id.imageDemoCube);
        imageDemoCube.setOnClickListener(v -> {
            if (serverState == ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED && textPairing.getVisibility() != View.VISIBLE) {
                if (!ServerManager.getInstance().isBusy()) {
                    // Debounce
                    if (!isDebouncing) {
                        Log.d(TAG, "onCreateView: sendRequestPairDevice");
                        isDebouncing = true;
                        textPairing.setVisibility(View.VISIBLE);
                        handler.postDelayed(() -> {
                            isDebouncing = false;
                            BroadcastManager.getInstance().sendRequestPairDevice("", "");
                        }, 1000);
                    }
                }
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

    private void setGameModeEnable(boolean enable) {
        if (optionListRecyclerView != null && gamePanel != null) {
            optionListRecyclerView.setVisibility(enable ? View.GONE : View.VISIBLE);
            gamePanel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void sendGameCommandData(ServerMessage.CommandType type) {
        if (type != preCommandType || type == ServerMessage.CommandType.COMMAND_GAME_START) {
            Log.d(TAG, "sendGameCommandData: " + type);
            preCommandType = type;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("type", EffectManager.getInstance().getCurrentEffectType().ordinal());
                jsonObject.put("cmd", type.ordinal());
            } catch (JSONException e) {
                Log.e(TAG, "sendGameCommandData: get error");
                e.printStackTrace();
            }
            BroadcastManager.getInstance().sendRequestSendData(jsonObject.toString());
        }
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

    private void updateStatus(ServerState serverState) {
        Log.e(TAG, "updateStatus: " + serverState);
        boolean connected = serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED;
        if (this.serverState != serverState) {
            if (isVisible()) {
                if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                    Toast.makeText(getContext(), "Connected to device!", Toast.LENGTH_SHORT).show();
                } else if (serverState == ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED) {
                    Toast.makeText(getContext(), "Device is not paired!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Disconnected to device!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (iconStatus != null && textStatus != null) {
            iconStatus.setImageResource(connected ? R.drawable.sensors_48 : R.drawable.sensors_off_48);
            if (serverState == ServerState.SERVER_STATE_CONNECTED_AND_PAIRED) {
                textStatus.setText(getResources().getString(R.string.online));
            } else if (serverState == ServerState.SERVER_STATE_DISCONNECTED) {
                textStatus.setText(getResources().getString(R.string.offline));
            } else if (serverState == ServerState.SERVER_STATE_CONNECTED_BUT_NOT_PAIRED) {
                textStatus.setText(getResources().getString(R.string.not_paired));
            }
        }
        if (textPairing != null) {
            textPairing.setVisibility(View.INVISIBLE);
        }
        this.serverState = serverState;
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