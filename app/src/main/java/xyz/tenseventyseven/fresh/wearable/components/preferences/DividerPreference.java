package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.databinding.WearDevicePreferenceBinding;
import xyz.tenseventyseven.fresh.databinding.WearDevicePreferenceDividerBinding;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class DividerPreference extends AbstractPreference {
    protected WearDevicePreferenceDividerBinding binding;

    public DividerPreference(Context context) {
        super(context);
        init(context);
    }

    public DividerPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DividerPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DividerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public DividerPreference(Context context, GBDevice device, DeviceSetting setting) {
        super(context, device, setting);
        init(context);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        binding = WearDevicePreferenceDividerBinding.inflate(getLayoutInflater(), this, true);
    }

    @Override
    public void onPreferenceChanged() {
        // Do nothing
    }
}
