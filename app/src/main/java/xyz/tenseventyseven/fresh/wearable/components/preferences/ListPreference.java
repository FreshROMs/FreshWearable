package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;

import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.databinding.WearDevicePreferenceBinding;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class ListPreference extends AbstractPreference {
    protected WearDevicePreferenceBinding binding;

    public ListPreference(Context context) {
        super(context);
    }

    public ListPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ListPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ListPreference(Context context, GBDevice device, DeviceSetting setting) {
        super(context, device, setting);
        init(context);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        binding = WearDevicePreferenceBinding.inflate(getLayoutInflater(), this, true);

        if (this.setting.icon != 0) {
            binding.preferenceIcon.setImageResource(setting.icon);
        } else {
            binding.preferenceIconLayout.setVisibility(GONE);
        }

        if (this.setting.title != 0) {
            binding.preferenceTitle.setText(setting.title);
        } else {
            binding.preferenceTitle.setVisibility(GONE);
        }

        if (this.setting.summary != 0) {
            binding.preferenceSummary.setText(setting.summary);
        } else {
            binding.preferenceSummary.setVisibility(GONE);
        }

        binding.preference.setOnClickListener(v -> onPreferenceClicked());
        seslSetRoundCorners(SeslRoundedCorner.ROUNDED_CORNER_NONE);
    }

    public void addExtra(String key, String value) {
        this.addExtra(key, value);
    }

    @Override
    public void onPreferenceChanged() {
        // Do nothing
    }

    @Override
    public void onPreferenceDependencyChangedNotify(AbstractPreference preference) {
        // Find dependent preference, check its value, then check if it matches our dependentValue
        // If it does, show this preference, else gone
        if (setting.dependency == null) {
            return;
        }

        if (preference.getValue() != null && preference.getValue().equals(setting.dependencyValue)) {
            binding.preferenceLayout.setVisibility(VISIBLE);
        } else {
            binding.preferenceLayout.setVisibility(GONE);
        }
    }

    @Override
    public void onPreferenceClicked() {
        super.launchActivity(false);
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

    @Override
    public void setVisibility(int visibility) {
        binding.preferenceLayout.setVisibility(visibility);
    }
}
