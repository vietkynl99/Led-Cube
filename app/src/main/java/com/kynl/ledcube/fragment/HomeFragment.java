package com.kynl.ledcube.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kynl.ledcube.R;
import com.kynl.ledcube.adapter.EffectListAdapter;
import com.kynl.ledcube.adapter.OptionListAdapter;


public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment";

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        /* Elements */
        RecyclerView effectListRecyclerView = view.findViewById(R.id.effectListRecyclerView);
        RecyclerView optionListRecyclerView = view.findViewById(R.id.optionListRecyclerView);

        /* Effect Recycler view */
        EffectListAdapter effectListAdapter = new EffectListAdapter();
        effectListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        effectListRecyclerView.setAdapter(effectListAdapter);

        effectListAdapter.setSelectedPosition(0);
        effectListAdapter.setOnEffectItemClickListener(position -> {
            Log.d(TAG, "onCreateView: Select effect " + position);
            effectListAdapter.setSelectedPosition(position);
        });

        /* Option Recycler view */
        OptionListAdapter optionListAdapter = new OptionListAdapter();
        optionListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        optionListRecyclerView.setAdapter(optionListAdapter);


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
    }
}