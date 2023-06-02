package com.kynl.ledcube;

import android.app.Application;
import android.util.Log;

import com.kynl.ledcube.manager.ServerManager;

public class MyApplication extends Application {
    private final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

    }
}
