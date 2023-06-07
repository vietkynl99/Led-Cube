package com.kynl.ledcube.adapter;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.OptionItem;
import com.kynl.ledcube.myinterface.OnOptionItemClickListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.List;

public class OptionListAdapter extends RecyclerView.Adapter<OptionListAdapter.CustomViewHolder> {
    private final List<OptionItem> optionItemList;

    public OptionListAdapter() {
        optionItemList = new ArrayList<>();
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Brightness"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Color"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Speed"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Direction"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Sensitivity"));
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
        holder.option_item_main_view.setOnClickListener(v -> holder.expandable_layout.toggle());
    }

    @Override
    public int getItemCount() {
        return optionItemList != null ? optionItemList.size() : 0;
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup option_item_main_view;
        private final ImageView icon;
        private final TextView option_item_text;
        private final ExpandableLayout expandable_layout;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            option_item_main_view = itemView.findViewById(R.id.option_item_main_view);
            expandable_layout = itemView.findViewById(R.id.expandable_layout);
            icon = itemView.findViewById(R.id.option_item_icon);
            option_item_text = itemView.findViewById(R.id.option_item_text);
        }

        public void bind(OptionItem item) {
            icon.setImageResource(item.getIconId());
            option_item_text.setText(item.getText());
        }
    }
}
