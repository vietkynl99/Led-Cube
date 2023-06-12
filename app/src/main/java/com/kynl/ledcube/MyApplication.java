package com.kynl.ledcube;

import android.app.Application;
import android.util.Log;

import com.kynl.ledcube.manager.BroadcastManager;
import com.kynl.ledcube.manager.EffectManager;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.manager.SharedPreferencesManager;

public class MyApplication extends Application {
    private final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

        SharedPreferencesManager.getInstance().init(getApplicationContext());
        BroadcastManager.getInstance().init(getApplicationContext());
        ServerManager.getInstance().init(getApplicationContext());
        EffectManager.getInstance().init(getApplicationContext());
    }
}
