/*  Copyright (C) 2024 John Vincent Corcega (TenSeventy7)

    This file is part of Fresh Wearable.

    Fresh Wearable is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class NoiseControlPreference extends ListPreference {
    private String mAncValue;
    private String mOffValue;
    private String mTransparencyValue;

    View mAncIcon;
    View mOffIcon;
    View mTransparencyIcon;

    TextView mAncLabel;
    TextView mOffLabel;
    TextView mTransparencyLabel;

    public NoiseControlPreference(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public NoiseControlPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public NoiseControlPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public NoiseControlPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(@NonNull Context context, AttributeSet attrs) {
        if (attrs != null) {
            try (TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NoiseControlPreference, 0, 0)) {
                mAncValue = a.getString(R.styleable.NoiseControlPreference_ancValue);
                mOffValue = a.getString(R.styleable.NoiseControlPreference_offValue);
                mTransparencyValue = a.getString(R.styleable.NoiseControlPreference_transparencyValue);
                setValue(a.getString(R.styleable.NoiseControlPreference_selectedValue));
            }
        }

        setLayoutResource(R.layout.wear_preference_noise_controls);
    }

    @Override
    public void onClick() {
        // Override and leave empty to prevent default click behavior
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mAncIcon = holder.findViewById(R.id.noise_controls_anc_on_icon);
        mOffIcon = holder.findViewById(R.id.noise_controls_anc_off_icon);
        mTransparencyIcon = holder.findViewById(R.id.noise_controls_ambient_sound_icon);

        // Find TextViews for the labels
        mAncLabel = (TextView) holder.findViewById(R.id.noise_controls_anc_on_text);
        mOffLabel = (TextView) holder.findViewById(R.id.noise_controls_anc_off_text);
        mTransparencyLabel = (TextView) holder.findViewById(R.id.noise_controls_ambient_sound_text);

        holder.itemView.setAlpha(isEnabled() ? 1.0f : 0.5f);

        // Set click listeners regardless of enabled state
        if (isEnabled()) {
            holder.itemView.setAlpha(1.0f);

            mAncIcon.setOnClickListener(v -> setValueInternal(mAncValue));
            mAncLabel.setOnClickListener(v -> setValueInternal(mAncValue));
            mOffIcon.setOnClickListener(v -> setValueInternal(mOffValue));
            mOffLabel.setOnClickListener(v -> setValueInternal(mOffValue));
            mTransparencyIcon.setOnClickListener(v -> setValueInternal(mTransparencyValue));
            mTransparencyLabel.setOnClickListener(v -> setValueInternal(mTransparencyValue));
        } else {
            holder.itemView.setAlpha(0.5f);

            // Remove click listeners when disabled
            mAncIcon.setOnClickListener(null);
            mAncLabel.setOnClickListener(null);
            mOffIcon.setOnClickListener(null);
            mOffLabel.setOnClickListener(null);
            mTransparencyIcon.setOnClickListener(null);
            mTransparencyLabel.setOnClickListener(null);
        }

        update(); // Update the UI to reflect the current selection
    }

    private void update() {
        if (getValue() == null) {
            return;
        }

        updateIconAndLabel(mAncIcon, mAncLabel, mAncValue);
        updateIconAndLabel(mOffIcon, mOffLabel, mOffValue);
        updateIconAndLabel(mTransparencyIcon, mTransparencyLabel, mTransparencyValue);
    }

    private void updateIconAndLabel(View icon, TextView label, String value) {
        boolean isSelected = isEnabled() && value.equals(getValue());

        if (icon != null) {
            icon.setSelected(isSelected);
        }

        if (label != null) {
            label.setTextColor(isSelected ? getContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_btn_colored_background) : getContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_primary_text_color));
            label.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void setValueInternal(String value) {
        if (!value.equals(getValue()) && callChangeListener(value)) {
            setValue(value);
        }
    }

    public void setAncValue(String value) {
        mAncValue = value;
    }

    public void setOffValue(String value) {
        mOffValue = value;
    }

    public void setTransparencyValue(String value) {
        mTransparencyValue = value;
    }

    public void setEntryValues(@ArrayRes int valuesRes) {
        String[] strings = getContext().getResources().getStringArray(valuesRes);
        if (strings.length < 3) {
            Log.e("NoiseControlPreference", "setEntryValues: Array must contain 3 values");
            return;
        }

        setAncValue(strings[0]);
        setOffValue(strings[1]);
        setTransparencyValue(strings[2]);
    }
}
