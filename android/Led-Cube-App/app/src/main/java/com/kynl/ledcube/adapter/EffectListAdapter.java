package com.kynl.ledcube.adapter;


import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
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
        holder.bind(position == selectedPosition, element);
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
        if (selectedPosition < 0 || selectedPosition >= effectItemList.size()) {
            Log.i(TAG, "setSelectedPosition: " + selectedPosition);
            if (this.selectedPosition >= 0 && this.selectedPosition < effectItemList.size()) {
                notifyItemChanged(this.selectedPosition);
            }
            this.selectedPosition = selectedPosition;
        } else {
            Log.i(TAG, "setSelectedPosition: type " + effectItemList.get(selectedPosition).getType());
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

    public void select(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if (effectItemList.get(i).getType() == type) {
                setSelectedPosition(i);
                return;
            }
        }
        setSelectedPosition(-1);
    }


    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final View highlight_view;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.effect_item_icon);
            highlight_view = itemView.findViewById(R.id.highlight_view);
        }

        public void bind(boolean highlight, EffectItem effectItem) {
            highlight_view.setVisibility(highlight ? View.VISIBLE : View.INVISIBLE);
            icon.setImageResource(effectItem.getIconId());
            if (!(highlight && effectItem.useOriginIconColor())) {
                Drawable drawable = icon.getDrawable().mutate();
                Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
                int color = ContextCompat.getColor(itemView.getContext(), highlight ? R.color.color_primary : R.color.icon_color);
                DrawableCompat.setTint(wrappedDrawable, color);
                icon.setImageDrawable(wrappedDrawable);
            }
        }
    }
}
