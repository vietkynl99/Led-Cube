package com.kynl.ledcube.myinterface;

import com.kynl.ledcube.model.EffectItem;
import com.kynl.ledcube.model.OptionItem;

public interface OnOptionValueChangeListener {
    void onValueChanged(EffectItem.EffectType effectType, OptionItem.OptionType optionType, int value);
}
