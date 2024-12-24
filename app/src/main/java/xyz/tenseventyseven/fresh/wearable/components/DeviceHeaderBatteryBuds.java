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
package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import xyz.tenseventyseven.fresh.wearable.R;

public class DeviceHeaderBatteryBuds extends DeviceHeaderBatteryCommon {

    public DeviceHeaderBatteryBuds(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public DeviceHeaderBatteryBuds(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DeviceHeaderBatteryBuds(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.component_device_header_battery_buds, this);
    }

    @Override
    public void refresh() {
        TextView batteryLevelLeft = findViewById(R.id.header_battery_level_buds_left);
        TextView batteryLevelRight = findViewById(R.id.header_battery_level_buds_right);
        TextView batteryLevelCase = findViewById(R.id.header_battery_level_case);

        ImageView batteryIconLeft = findViewById(R.id.header_battery_level_buds_left_image);
        ImageView batteryIconRight = findViewById(R.id.header_battery_level_buds_right_image);
        ImageView batteryIconCase = findViewById(R.id.header_battery_level_case_image);

        // Get battery levels
        int batteryCase = getBatteryLevel(mDevice, 0);
        int batteryLeft = getBatteryLevel(mDevice, 1);
        int batteryRight = getBatteryLevel(mDevice, 2);

        // Set battery levels
        batteryLevelCase.setText(getBatteryLevelString(batteryCase));
        batteryLevelLeft.setText(getBatteryLevelString(batteryLeft));
        batteryLevelRight.setText(getBatteryLevelString(batteryRight));

        Drawable chargeIcon = mContext.getDrawable(R.drawable.ic_bolt);
        batteryLevelCase.setCompoundDrawablesWithIntrinsicBounds(isBatteryCharging(mDevice, 0) ? chargeIcon : null, null, null, null);
        batteryLevelLeft.setCompoundDrawablesWithIntrinsicBounds(isBatteryCharging(mDevice, 1) ? chargeIcon : null, null, null, null);
        batteryLevelRight.setCompoundDrawablesWithIntrinsicBounds(isBatteryCharging(mDevice, 2) ? chargeIcon: null, null, null, null);

        batteryIconCase.setAlpha(batteryCase == -1 ? 0.3f : 0.6f);
        batteryIconLeft.setAlpha(batteryLeft == -1 ? 0.3f : 0.6f);
        batteryIconRight.setAlpha(batteryRight == -1 ? 0.3f : 0.6f);
    }

    private String getBatteryLevelString(int batteryLevel) {
        if (batteryLevel == -1) {
            return "-";
        }

        return batteryLevel + "%";
    }

    private int getBatteryLevel(GBDevice device, int batteryIndex) {
        if (device.getBatteryState(batteryIndex) == BatteryState.UNKNOWN ||
                device.getBatteryState(batteryIndex) == BatteryState.NO_BATTERY) {
            return -1;
        }

        return device.getBatteryLevel(batteryIndex);
    }

    private boolean isBatteryCharging(GBDevice device, int batteryIndex) {
        return device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING ||
                device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING_FULL;
    }
}
