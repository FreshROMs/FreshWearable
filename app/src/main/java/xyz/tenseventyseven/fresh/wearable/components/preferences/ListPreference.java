package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.annotation.SuppressLint;
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
    private DeviceSetting setting;

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

    public ListPreference(Context context, GBDevice device, String key, int title, int summary, int icon) {
        super(context, device, key, title, summary, icon);
    }

    public ListPreference(Context context, GBDevice device, DeviceSetting setting) {
        super(context, device, setting.key, setting.title, setting.summary, setting.icon);
        this.activity = setting.screen;
        this.setting = setting;
        init(context);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        binding = WearDevicePreferenceBinding.inflate(getLayoutInflater(), this, true);

        if (this.icon != 0) {
            binding.preferenceIcon.setImageResource(icon);
        } else {
            binding.preferenceIconLayout.setVisibility(GONE);
        }

        if (this.title != 0) {
            binding.preferenceTitle.setText(title);
        } else {
            binding.preferenceTitle.setVisibility(GONE);
        }

        if (this.summary != 0) {
            binding.preferenceSummary.setText(summary);
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
    public void onPreferenceClicked() {
        if (this.activity != null) {
            Intent intent = new Intent();
            if (this.activity == null) {
                intent.setClassName(getContext(), PreferenceScreenActivity.class.getName());
                intent.putExtra(GBDevice.EXTRA_DEVICE, this.device);
                intent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, false);
                intent.putExtra(DeviceSetting.EXTRA_SETTING, this.setting);
            } else {
                intent.setClassName(getContext(), this.activity);
            }

            for (Map.Entry<String, Object> entry : this.extras.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (int) value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (boolean) value);
                } else if (value instanceof Float) {
                    intent.putExtra(key, (float) value);
                } else if (value instanceof Double) {
                    intent.putExtra(key, (double) value);
                } else if (value instanceof Long) {
                    intent.putExtra(key, (long) value);
                } else if (value instanceof Short) {
                    intent.putExtra(key, (short) value);
                } else if (value instanceof Byte) {
                    intent.putExtra(key, (byte) value);
                } else if (value instanceof Character) {
                    intent.putExtra(key, (char) value);
                } else if (value instanceof Parcelable) {
                    intent.putExtra(key, (Parcelable) value);
                } else {
                    intent.putExtra(key, value.toString());
                }
            }

            getContext().startActivity(intent);
        }
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
