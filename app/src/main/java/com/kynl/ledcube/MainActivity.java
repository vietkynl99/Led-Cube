package com.kynl.ledcube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.kynl.ledcube.fragment.HomeFragment;
import com.kynl.ledcube.fragment.SearchFragment;
import com.kynl.ledcube.fragment.SettingsFragment;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.service.NetworkService;

import static com.kynl.ledcube.manager.ServerManager.ConnectionState.CONNECTION_STATE_NONE;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private ServerManager.ServerState serverState;
    private ServerManager.ConnectionState connectionState;

    private FragmentManager fragmentManager;
    private Fragment homeFragment, searchFragment, settingsFragment;
    private String preFragmentClassName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Variable */
        serverState = ServerManager.getInstance().getServerState();
        connectionState = ServerManager.getInstance().getConnectionState();

        /* Element */
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        /* Bottom navigation */
        // fragmentManager
        fragmentManager = getSupportFragmentManager();
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        settingsFragment = new SettingsFragment();

        changeFragment(homeFragment);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int itemIndex = -1; // Khởi tạo giá trị mặc định cho itemIndex là -1
            Menu menu = bottomNavigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == itemId) {
                    itemIndex = i; // Lưu lại index của item được chọn
                    break;
                }
            }
            switch (itemIndex) {
                case 0: {
                    changeFragment(homeFragment);
                    break;
                }
                case 1: {
                    changeFragment(searchFragment);
                    break;
                }
                case 2: {
                    changeFragment(settingsFragment);
                    break;
                }
                default: {
                    Log.e(TAG, "onCreate: Index error");
                    break;
                }
            }
            return true;
        });

        /* Server state changed */
        ServerManager.getInstance().setOnServerStatusChangedListener((serverState, connectionState) -> {
            Log.i(TAG, "Server status changed: serverState[" + serverState + "] connectionState[" + connectionState + "]");
            this.serverState = serverState;
            this.connectionState = connectionState;
//            updateButtonState();
        });

        // Start Socket service
//        Log.i(TAG, "onCreate: Start service");
//        Intent intent = new Intent(this, NetworkService.class);
//        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy: ");
        // Stop service
        Intent intent = new Intent(this, NetworkService.class);
        stopService(intent);
    }

    private void changeFragment(Fragment fragment) {
        if (fragment == null) {
            Log.e(TAG, "changeFragment: Fragment is null");
            return;
        }
        String fragmentClassName = fragment.getClass().getName();
        if (!fragmentClassName.equals(preFragmentClassName)) {
            try {
                fragmentManager.beginTransaction().replace(R.id.fragment_content, fragment).commit();
                preFragmentClassName = fragmentClassName;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    private void updateButtonState() {
//        pairDeviceBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
////        pairDeviceBtn.setText(getResources().getString(connectionState == CONNECTION_STATE_PENDING_PAIR ?
////                R.string.pairing_device : R.string.pair_device));
//        refreshBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
//    }


}