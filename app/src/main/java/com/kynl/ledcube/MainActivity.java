package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kynl.ledcube.fragment.HomeFragment;
import com.kynl.ledcube.fragment.SearchFragment;
import com.kynl.ledcube.fragment.SettingsFragment;
import com.kynl.ledcube.manager.ServerManager;
import com.kynl.ledcube.service.NetworkService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private ServerManager.ServerState serverState;
    private ServerManager.ConnectionState connectionState;
    FragmentTransaction fragmentTransaction;
    private Fragment homeFragment, searchFragment, settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Variable */
        serverState = ServerManager.getInstance().getServerState();
        connectionState = ServerManager.getInstance().getConnectionState();

        /* Element */
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        /* Fragment */
        fragmentTransactionInit();

        /* Bottom navigation */
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

        // Start Socket service
        Log.i(TAG, "onCreate: Start service");
        Intent intent = new Intent(this, NetworkService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy: ");
        // Stop service
        Intent intent = new Intent(this, NetworkService.class);
        stopService(intent);
    }

    private void fragmentTransactionInit() {
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        settingsFragment = new SettingsFragment();

        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_content, homeFragment);
        fragmentTransaction.add(R.id.fragment_content, searchFragment);
        fragmentTransaction.add(R.id.fragment_content, settingsFragment);

        // Show default fragment
        fragmentTransaction.hide(searchFragment);
        fragmentTransaction.hide(settingsFragment);
        fragmentTransaction.show(homeFragment);

        fragmentTransaction.commit();
    }

    private void changeFragment(Fragment fragment) {
        if (fragment == null) {
            Log.e(TAG, "changeFragment: Fragment is null");
            return;
        }
        if (!fragment.isVisible()) {
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (homeFragment.isVisible()) {
                fragmentTransaction.hide(homeFragment);
                homeFragment.onPause();
            }
            if (searchFragment.isVisible()) {
                fragmentTransaction.hide(searchFragment);
                searchFragment.onPause();
            }
            if (settingsFragment.isVisible()) {
                fragmentTransaction.hide(settingsFragment);
                settingsFragment.onPause();
            }
            fragmentTransaction.show(fragment);
            fragment.onResume();
            fragmentTransaction.commit();
        }
    }

//    private void updateButtonState() {
//        pairDeviceBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
////        pairDeviceBtn.setText(getResources().getString(connectionState == CONNECTION_STATE_PENDING_PAIR ?
////                R.string.pairing_device : R.string.pair_device));
//        refreshBtn.setEnabled(connectionState == CONNECTION_STATE_NONE);
//    }

}