package xyz.tenseventyseven.fresh.wearable.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;

import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.wearable.components.preferences.AbstractPreference;
import xyz.tenseventyseven.fresh.wearable.components.preferences.CheckBoxPreference;
import xyz.tenseventyseven.fresh.wearable.components.preferences.DividerPreference;
import xyz.tenseventyseven.fresh.wearable.components.preferences.ListPreference;
import xyz.tenseventyseven.fresh.wearable.components.preferences.SwitchPreference;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class DeviceSettingsAdapter extends ArrayAdapter<DeviceSetting> {
    private final GBDevice device;
    private final List<DeviceSetting> settings;
    private final Context context;

    public DeviceSettingsAdapter(@NonNull Context context, GBDevice device, List<DeviceSetting> settings) {
        super(context, 0, settings);
        this.context = context;
        this.device = device;
        this.settings = settings;
    }

    @Override
    public DeviceSetting getItem(int position) {
        return settings.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        DeviceSetting setting = getItem(position);
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

        int corners = getCorners(position);
        preference.seslSetRoundCorners(corners);
        return preference;
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
