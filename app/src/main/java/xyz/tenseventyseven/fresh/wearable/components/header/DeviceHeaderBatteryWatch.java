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
package xyz.tenseventyseven.fresh.wearable.components.header;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.databinding.WearDeviceHeaderBatteryWatchBinding;

public class DeviceHeaderBatteryWatch extends DeviceHeaderBatteryCommon {
    private WearDeviceHeaderBatteryWatchBinding binding;

    List<BatteryState> statusStates = Arrays.asList(
            BatteryState.BATTERY_LOW,
            BatteryState.BATTERY_CHARGING,
            BatteryState.BATTERY_CHARGING_FULL
    );

    public DeviceHeaderBatteryWatch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public DeviceHeaderBatteryWatch(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public DeviceHeaderBatteryWatch(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        binding = WearDeviceHeaderBatteryWatchBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    public void refresh() {
        if (mDevice == null || binding == null) {
            return;
        }

        TextView batteryLevel = binding.headerBatteryLevelWatch;
        ImageView batteryIcon = binding.headerBatteryLevelWatchImage;

        // Get battery levels
        int battery = getBatteryLevel(mDevice);

        // Set battery level and icon
        batteryLevel.setText(getBatteryLevelString(battery));
        batteryIcon.setImageDrawable(getBatteryDrawable(battery));
        setBatteryStatus();
    }

    private void setBatteryStatus() {
        LinearLayout batteryStatusLayout = findViewById(R.id.header_battery_status_watch);
        BatteryState state = mDevice.getBatteryState();

        batteryStatusLayout.setVisibility(View.GONE);
        if (!statusStates.contains(state)) {
            return;
        }

        TextView batteryStatus = findViewById(R.id.header_battery_status_watch_text);
        batteryStatusLayout.setVisibility(View.VISIBLE);

        switch (state) {
            case BATTERY_LOW:
                batteryStatus.setText("Battery low");
                break;
            case BATTERY_CHARGING:
                batteryStatus.setText("Charging");
                break;
            case BATTERY_CHARGING_FULL:
                batteryStatus.setText("Fully charged");
                break;
        }
    }

    private Drawable getBatteryDrawable(int battery) {
        // We have 100, 80, 50, and 20% battery icons
        int batteryIconResId = R.drawable.ic_battery_20;
        if (battery >= 80) {
            batteryIconResId = R.drawable.ic_battery_full;
        } else if (battery >= 50) {
            batteryIconResId = R.drawable.ic_battery_80;
        } else if (battery >= 20) {
            batteryIconResId = R.drawable.ic_battery_50;
        }

        return getResources().getDrawable(batteryIconResId);
    }

    private String getBatteryLevelString(int batteryLevel) {
        if (batteryLevel == -1) {
            return "-";
        }

        return batteryLevel + "%";
    }

    private int getBatteryLevel(GBDevice device) {
        if (device.getBatteryState() == BatteryState.UNKNOWN ||
                device.getBatteryState() == BatteryState.NO_BATTERY) {
            return -1;
        }

        return device.getBatteryLevel();
    }
}
