package com.kynl.ledcube.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.myinterface.OnEffectItemClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class EffectListAdapter extends RecyclerView.Adapter<EffectListAdapter.CustomViewHolder> {
    private final String TAG = "EffectListAdapter";
    private List<EffectItem> effectItemList;
    private int selectedPosition;
    private OnEffectItemClickListener onEffectItemClickListener;

    public EffectListAdapter() {
        effectItemList = new ArrayList<>();
        effectItemList.add(new EffectItem("Rgb", R.drawable.rgb_64, R.drawable.rgb_hightlight_64));
        effectItemList.add(new EffectItem("Music", R.drawable.music_52, R.drawable.music_hightlight_52));
        effectItemList.add(new EffectItem("Wave", R.drawable.wave_50, R.drawable.wave_hightlight_50));
        effectItemList.add(new EffectItem("Lightning", R.drawable.lightning_60, R.drawable.lightning_hightlight_60));
        selectedPosition = -1;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.effect_view_item, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        EffectItem item = effectItemList.get(position);
        holder.bind(position == selectedPosition, position == selectedPosition ? item.getHighlightIconId() : item.getIconId());
        holder.icon.setOnClickListener(v -> {
            if (onEffectItemClickListener != null) {
                onEffectItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return effectItemList != null ? effectItemList.size() : 0;
    }

    public void setOnEffectItemClickListener(OnEffectItemClickListener onEffectItemClickListener) {
        this.onEffectItemClickListener = onEffectItemClickListener;
    }


    public void setSelectedPosition(int selectedPosition) {
        if (selectedPosition >= 0 && selectedPosition <= effectItemList.size()) {
            if (this.selectedPosition != selectedPosition) {
                // notify old position
                if (this.selectedPosition >= 0) {
                    notifyItemChanged(this.selectedPosition);
                }
                this.selectedPosition = selectedPosition;
                // notify new position
                notifyItemChanged(this.selectedPosition);
            }
        }
    }


    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final View highlight_view;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.effect_item_icon);
            highlight_view = itemView.findViewById(R.id.highlight_view);
        }

        public void bind(boolean highlight, int iconId) {
            highlight_view.setVisibility(highlight ? View.VISIBLE : View.INVISIBLE);
            icon.setImageResource(iconId);
        }
    }
}
