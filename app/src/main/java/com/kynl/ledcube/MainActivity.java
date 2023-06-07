package com.kynl.ledcube;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kynl.ledcube.fragment.HomeFragment;
import com.kynl.ledcube.fragment.SearchFragment;
import com.kynl.ledcube.fragment.SettingsFragment;
import com.kynl.ledcube.service.NetworkService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Fragment homeFragment, searchFragment, settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Variable */

        /* Element */

        /* Fragment Transaction*/
        fragmentTransactionInit();

        /* Bottom navigation */
        initBottomNavigation();

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

    private void initBottomNavigation() {
        MeowBottomNavigation bottomNavigation = findViewById(R.id.meowBottomNavigation);

        bottomNavigation.add(new MeowBottomNavigation.Model(1, R.drawable.home_w_48));
        bottomNavigation.add(new MeowBottomNavigation.Model(2, R.drawable.search_w_50));
        bottomNavigation.add(new MeowBottomNavigation.Model(3, R.drawable.settings_w_48));

        bottomNavigation.setOnClickMenuListener(item -> {
            Log.e(TAG, "onCreate: clicked " + item.getId());
            switch (item.getId()) {
                case 1: {
                    changeFragment(homeFragment);
                    break;
                }
                case 2: {
                    changeFragment(searchFragment);
                    break;
                }
                case 3: {
                    changeFragment(settingsFragment);
                    break;
                }
            }
        });
        bottomNavigation.setOnShowListener(item -> {
        });
        bottomNavigation.setOnReselectListener(item -> {
        });

        bottomNavigation.show(1, false);
    }

    private void fragmentTransactionInit() {
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        settingsFragment = new SettingsFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
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
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
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

}