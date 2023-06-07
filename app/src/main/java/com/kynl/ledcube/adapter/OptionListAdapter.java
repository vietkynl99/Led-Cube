package com.kynl.ledcube.adapter;


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

import java.util.ArrayList;
import java.util.List;

public class OptionListAdapter extends RecyclerView.Adapter<OptionListAdapter.CustomViewHolder> {
    private final List<OptionItem> optionItemList;
    private int collapsedItemPosition;
    private final OnOptionItemClickListener onOptionItemClickListener;

    public OptionListAdapter() {
        optionItemList = new ArrayList<>();
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Brightness"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Color"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Speed"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Direction"));
        optionItemList.add(new OptionItem(R.drawable.brightness_48, "Sensitivity"));
        collapsedItemPosition = -1;

        onOptionItemClickListener = position -> {
            // toggle data setting panel
            if (collapsedItemPosition >= 0 && collapsedItemPosition != position) {
                // uncollapse old position
                if (!optionItemList.get(collapsedItemPosition).isCollapse()) {
                    optionItemList.get(collapsedItemPosition).setCollapse(true);
                    notifyItemChanged(collapsedItemPosition);
                }
            }
            collapsedItemPosition = position;
            optionItemList.get(position).setCollapse(!optionItemList.get(position).isCollapse());
            notifyItemChanged(position);
        };
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

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup option_item_main_view, option_item_panel;
        private final ImageView icon, arrow;
        private final TextView option_item_text;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            option_item_main_view = itemView.findViewById(R.id.option_item_main_view);
            option_item_panel = itemView.findViewById(R.id.option_item_panel);
            icon = itemView.findViewById(R.id.option_item_icon);
            arrow = itemView.findViewById(R.id.option_arrow);
            option_item_text = itemView.findViewById(R.id.option_item_text);
        }

        public void bind(OptionItem item) {
            icon.setImageResource(item.getIconId());
            option_item_text.setText(item.getText());
            option_item_panel.setVisibility(item.isCollapse() ? View.GONE : View.VISIBLE);
            arrow.setImageResource(item.isCollapse() ? R.drawable.baseline_keyboard_arrow_right_48 :
                    R.drawable.baseline_keyboard_arrow_down_48);
        }
    }
}
