package com.kynl.ledcube.fragment;

import static com.kynl.ledcube.common.CommonUtils.BROADCAST_ACTION;
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

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";
    private ImageView iconStatus;
    private TextView textStatus;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
//            Log.i(TAG, "onReceive: Get board cast event: " + event);
            if (event != null) {
                switch (event) {
                    case BROADCAST_SERVICE_SERVER_STATUS_CHANGED: {
                        ServerManager.ServerState serverState = (ServerManager.ServerState) intent.getSerializableExtra("serverState");
                        updateStatus(serverState == ServerManager.ServerState.SERVER_STATE_CONNECTED_AND_PAIRED);
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

        updateStatus(false);

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
}