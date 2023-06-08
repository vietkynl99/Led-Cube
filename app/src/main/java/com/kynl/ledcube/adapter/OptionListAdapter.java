package com.kynl.ledcube.adapter;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kynl.ledcube.R;
import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;
import com.kynl.ledcube.myinterface.OnOptionValueChangeListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.List;

public class OptionListAdapter extends RecyclerView.Adapter<OptionListAdapter.CustomViewHolder> {
    private final String TAG = "OptionListAdapter";
    private EffectItem effectItem;
    private final List<EffectItem> effectItemList;
    private List<OptionItem> optionItemList;
    private OnOptionValueChangeListener onOptionValueChangeListener;

    public OptionListAdapter(List<EffectItem> effectItemList) {
        this.effectItemList = effectItemList;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.option_view_item, parent, false);
        return new CustomViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.bind(optionItemList.get(position));
        holder.option_item_main_view.setOnClickListener(v -> holder.expandable_layout.toggle());
        holder.option_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int pos = holder.getAdapterPosition();
                optionItemList.get(pos).setValue(progress);
                holder.option_value_text.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int pos = holder.getAdapterPosition();
                if (onOptionValueChangeListener != null) {
                    onOptionValueChangeListener.onValueChanged(effectItem.getType(),
                            optionItemList.get(pos).getType(),
                            optionItemList.get(pos).getValue());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return optionItemList != null ? optionItemList.size() : 0;
    }

    public void setOnOptionValueChangeListener(OnOptionValueChangeListener onOptionValueChangeListener) {
        this.onOptionValueChangeListener = onOptionValueChangeListener;
    }

    public void select(EffectItem.EffectType type) {
        for (int i = 0; i < effectItemList.size(); i++) {
            if (effectItemList.get(i).getType() == type) {
                effectItem = effectItemList.get(i);
                optionItemList = effectItem.getOptionItemList();
                Log.i(TAG, "updateEffectType: Update effect type " + effectItem.getType());
                notifyDataSetChanged();
            }
        }
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup option_item_main_view;
        private final ImageView icon;
        private final TextView option_item_text, option_value_text;
        private final ExpandableLayout expandable_layout;
        private final SeekBar option_seek_bar;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            option_item_main_view = itemView.findViewById(R.id.option_item_main_view);
            expandable_layout = itemView.findViewById(R.id.expandable_layout);
            icon = itemView.findViewById(R.id.option_item_icon);
            option_item_text = itemView.findViewById(R.id.option_item_text);
            option_value_text = itemView.findViewById(R.id.option_value_text);
            option_seek_bar = itemView.findViewById(R.id.option_seek_bar);
        }

        public void bind(OptionItem item) {
            icon.setImageResource(item.getIconId());
            option_item_text.setText(item.getText());
            option_value_text.setText(String.valueOf(item.getValue()));
            option_seek_bar.setProgress(item.getValue());
            expandable_layout.collapse(false);
        }
    }
}
