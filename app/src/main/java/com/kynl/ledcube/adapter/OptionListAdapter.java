package com.kynl.ledcube.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;
import com.kynl.ledcube.myinterface.OnEffectItemClickListener;
import com.kynl.ledcube.myinterface.OnOptionItemClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class OptionListAdapter extends RecyclerView.Adapter<OptionListAdapter.CustomViewHolder> {
    private final String TAG = "OptionListAdapter";
    private final ReentrantLock lock = new ReentrantLock();
    private List<OptionItem> optionItemList;
    private int selectedPosition;
    private OnOptionItemClickListener onOptionItemClickListener;

    public OptionListAdapter() {
        optionItemList = new ArrayList<>();
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Brightness"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Color"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Speed"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Direction"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Sensitivity"));
        selectedPosition = -1;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.option_view_item, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        OptionItem item = optionItemList.get(position);
        holder.bind(item);
        holder.option_item_main_view.setOnClickListener(v -> {
            if (onOptionItemClickListener != null) {
                onOptionItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return optionItemList != null ? optionItemList.size() : 0;
    }

    public void setOnOptionItemClickListener(OnOptionItemClickListener onOptionItemClickListener) {
        this.onOptionItemClickListener = onOptionItemClickListener;
    }


    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup option_item_main_view;
        private final ImageView icon;
        private final TextView option_item_text;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            option_item_main_view = itemView.findViewById(R.id.option_item_main_view);
            icon = itemView.findViewById(R.id.option_item_icon);
            option_item_text = itemView.findViewById(R.id.option_item_text);
        }

        public void bind(OptionItem item) {
            icon.setImageResource(item.getIconId());
            option_item_text.setText(item.getText());
        }
    }
}
