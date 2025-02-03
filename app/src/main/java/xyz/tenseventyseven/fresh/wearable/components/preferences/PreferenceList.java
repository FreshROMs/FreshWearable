package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;

import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.databinding.WearPreferenceListBinding;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceList extends LinearLayout {
    private WearPreferenceListBinding binding;
    private GBDevice device;
    private List<DeviceSetting> settings;

    public PreferenceList(Context context) {
        super(context);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreferenceList(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSettings(Context context, GBDevice device, List<DeviceSetting> settings) {
        this.device = device;
        this.settings = settings;
        init(context);
    }

    private void init(Context context) {
        binding = WearPreferenceListBinding.inflate(LayoutInflater.from(context), this, true);

        for (int i = 0; i < settings.size(); i++) {
            DeviceSetting setting = settings.get(i);
            AbstractPreference preference;
            switch (Objects.requireNonNull(setting).type) {
                case CHECKBOX:
                    preference = new CheckBoxPreference(context, this.device, setting);
                    break;
                case SWITCH:
                case SWITCH_SCREEN:
                    preference = new SwitchPreference(context, this.device, setting);
                    break;
                case DIVIDER:
                    preference = new DividerPreference(context);
                    break;
                case SCREEN:
                    preference = new ListPreference(context, this.device, setting);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown setting type: " + setting.type);
            }

            int corners = getCorners(i);
            preference.seslSetRoundCorners(corners);
            binding.preferenceList.addView(preference);
        }
    }

    private int getCorners(int position) {
        int corners = SeslRoundedCorner.ROUNDED_CORNER_NONE;

        boolean isFirstItem = position == 0;
        boolean isLastItem = position == settings.size() - 1;
        boolean nextIsDivider = position < settings.size() - 1 && settings.get(position + 1).type == DeviceSetting.DeviceSettingType.DIVIDER;
        boolean prevIsDivider = position > 0 && settings.get(position - 1).type == DeviceSetting.DeviceSettingType.DIVIDER;

        if (isFirstItem || prevIsDivider) {
            corners |= SeslRoundedCorner.ROUNDED_CORNER_TOP_RIGHT | SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT;
        }

        if (isLastItem || nextIsDivider) {
            corners |= SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_RIGHT | SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_LEFT;
        }

        return corners;
    }
}
