package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.model.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final String serverAddress = "http://192.168.10.102";
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerManager.getInstance().init(getApplicationContext());

        /* Pair Button */
        Button pairDeviceBtn = findViewById(R.id.pairDeviceBtn);
        pairDeviceBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: pair button clicked");
            ServerManager.getInstance().sendPairRequest();
        });

        /* Check connection button */
        Button checkConnectionBtn = findViewById(R.id.checkConnectionBtn);
        checkConnectionBtn.setOnClickListener(v -> {
            Log.i(TAG, "onCreate: check connection");
            ServerManager.getInstance().sendCheckConnectionRequest();
        });
    }

}