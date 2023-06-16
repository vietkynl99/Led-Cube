package com.kynl.ledcube.adapter;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.myinterface.OnEffectItemClickListener;

import java.util.List;

public class EffectListAdapter extends RecyclerView.Adapter<EffectListAdapter.CustomViewHolder> {
    private final String TAG = "EffectListAdapter";
    private final List<EffectItem> effectItemList;
    private int selectedPosition;
    private OnEffectItemClickListener onEffectItemClickListener;

    public EffectListAdapter(List<EffectItem> effectItemList) {
        this.effectItemList = effectItemList;
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
        EffectItem element = effectItemList.get(position);
        holder.bind(position == selectedPosition, position == selectedPosition ? element.getHighlightIconId() : element.getIconId());
        holder.icon.setOnClickListener(v -> {
            if (onEffectItemClickListener != null) {
                onEffectItemClickListener.onItemClick(effectItemList.get(position).getType());
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


    private void setSelectedPosition(int selectedPosition) {
        if (selectedPosition >= 0 && selectedPosition <= effectItemList.size()) {
            if (this.selectedPosition != selectedPosition) {
                Log.i(TAG, "setSelectedPosition: type " + effectItemList.get(selectedPosition).getType());
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

    public void select(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if (effectItemList.get(i).getType() == type) {
                setSelectedPosition(i);
                return;
            }
        }
        Log.e(TAG, "select: Error. Can not find type " + type);
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
