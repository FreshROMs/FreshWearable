package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CheckBoxPreference extends SwitchPreference {
    public CheckBoxPreference(Context context) {
        super(context);
    }

    public CheckBoxPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckBoxPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CheckBoxPreference(Context context, GBDevice device, String key, int title, int summary, int icon) {
        super(context, device, key, title, summary, icon);
    }

    public CheckBoxPreference(Context context, GBDevice device, String key, int title, int summary, int icon, boolean defaultValue) {
        super(context, device, key, title, summary, icon);
        this.defaultValue = defaultValue;
        init(context);
    }

    @Override
    void onAfterInit(Context context) {
        binding.preferenceCheckbox.setVisibility(VISIBLE);
        binding.preferenceCheckbox.setChecked(value);
        binding.preferenceCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            value = isChecked;
            onPreferenceChanged();
        });
    }
}
