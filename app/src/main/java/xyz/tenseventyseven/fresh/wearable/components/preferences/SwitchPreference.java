package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.databinding.WearDevicePreferenceBinding;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class SwitchPreference extends AbstractPreference {
    protected WearDevicePreferenceBinding binding;
    protected boolean value;
    protected boolean defaultValue;

    public SwitchPreference(Context context) {
        super(context);
    }

    public SwitchPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreference(Context context, GBDevice device, DeviceSetting setting) {
        super(context, device, setting);
        this.defaultValue = Boolean.parseBoolean(setting.defaultValue);
        init(context);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        value = sharedPreferences.getBoolean(getKey(), this.defaultValue);
        binding = WearDevicePreferenceBinding.inflate(getLayoutInflater(), this, true);

        if (this.setting.icon != 0) {
            binding.preferenceIcon.setImageResource(this.setting.icon);
        } else {
            binding.preferenceIconLayout.setVisibility(GONE);
        }

        if (this.setting.title != 0) {
            binding.preferenceTitle.setText(this.setting.title);
        } else {
            binding.preferenceTitle.setVisibility(GONE);
        }

        if (this.setting.summary != 0) {
            binding.preferenceSummary.setText(this.setting.summary);
        } else {
            binding.preferenceSummary.setVisibility(GONE);
        }

        binding.preference.setOnClickListener(v -> onPreferenceClicked());
        seslSetRoundCorners(SeslRoundedCorner.ROUNDED_CORNER_NONE);
        onAfterInit(context);
    }

    void onAfterInit(Context context) {
        binding.preferenceSwitch.setVisibility(VISIBLE);
        binding.preferenceSwitch.setChecked(value);
        binding.preferenceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            value = isChecked;
            onPreferenceChanged();
        });

        if (setting.type == DeviceSetting.DeviceSettingType.SWITCH_SCREEN) {
            binding.switchDivider.setVisibility(VISIBLE);
        }
    }

    @SuppressLint("ApplySharedPref") // This is why we put this in a separate thread
    @Override
    public void onPreferenceChanged() {
        new Thread(() -> {
            sharedPreferences.edit().putBoolean(getKey(), value).commit();
            super.onPreferenceChanged();
        }).start();
    }

    @Override
    public void onPreferenceChangedNotify() {
        value = sharedPreferences.getBoolean(getKey(), this.defaultValue);
        binding.preferenceSwitch.setChecked(value);
        binding.preferenceCheckbox.setChecked(value);
    }

    @Override
    public void onPreferenceClicked() {
        if (setting.type == DeviceSetting.DeviceSettingType.SWITCH_SCREEN) {
            super.launchActivity(true);
            return;
        }

        value = !value;
        binding.preferenceSwitch.setChecked(value);
        super.onPreferenceClicked();
    }

    @Override
    public void setText(String text) {
        binding.preferenceTitle.setText(text);
    }

    @Override
    public void setSummary(String summary) {
        binding.preferenceSummary.setText(summary);
    }

    @Override
    public void setText(int resId) {
        binding.preferenceTitle.setText(resId);
    }

    @Override
    public void setSummary(int resId) {
        binding.preferenceSummary.setText(resId);
    }

    @Override
    public void setSummaryColor(int color) {
        binding.preferenceSummary.setTextColor(color);
    }

    @Override
    public void setSummaryColorRes(int resId) {
        binding.preferenceSummary.setTextColor(getContext().getColor(resId));
    }

    @Override
    public void seslSetRoundCorners(int corners) {
        setRoundedCorners(corners);
        binding.preferenceLayout.setRoundedCorners(corners);
    }
}
