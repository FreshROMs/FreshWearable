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
import android.widget.LinearLayout;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.utils.BatteryUtils;

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

        ImageView budsIcon = findViewById(R.id.header_battery_level_buds_image);
        ImageView caseIcon = findViewById(R.id.header_battery_level_case_image);

        ImageView leftChargeIcon = findViewById(R.id.header_battery_level_buds_left_charging);
        ImageView rightChargeIcon = findViewById(R.id.header_battery_level_buds_right_charging);
        ImageView caseChargeIcon = findViewById(R.id.header_battery_level_case_charging);

        LinearLayout budsContainer = findViewById(R.id.header_battery_level_buds_container);
        LinearLayout caseContainer = findViewById(R.id.header_battery_level_case_container);

        // Set icons based on coordinator response
        DeviceCoordinator coordinator = mDevice.getDeviceCoordinator();
        BatteryConfig[] config = coordinator.getBatteryConfig(mDevice);

        if (config.length > 1) {
            caseIcon.setImageDrawable(getContext().getDrawable(config[0].getBatteryIcon()));
            budsIcon.setImageDrawable(getContext().getDrawable(config[1].getBatteryIcon()));
        } else {
            budsIcon.setImageDrawable(getContext().getDrawable(config[0].getBatteryIcon()));
        }

        // Get battery levels
        int batteryCase = getBatteryLevel(mDevice, 0);
        int batteryLeft = getBatteryLevel(mDevice, 1);
        int batteryRight = getBatteryLevel(mDevice, 2);

        boolean chargingCase = isBatteryCharging(mDevice, 0);
        boolean chargingLeft = isBatteryCharging(mDevice, 1);
        boolean chargingRight = isBatteryCharging(mDevice, 2);

        int batteryCount = coordinator.getBatteryCount();

        // Determine visibility and text based on battery count
        switch (batteryCount) {
            case 3: // Case and two buds
                boolean shouldCombine = BatteryUtils.shouldCombineBudsBattery(mDevice);
                updateBudsDisplay(batteryLevelLeft, batteryLevelRight, leftChargeIcon, rightChargeIcon, batteryLeft, batteryRight, chargingLeft, chargingRight, shouldCombine);
                updateCaseDisplay(batteryLevelCase, caseChargeIcon, batteryCase, chargingCase, true);
                break;
            case 2: // Two batteries: combine buds and show case
                updateBudsDisplay(batteryLevelLeft, batteryLevelRight, leftChargeIcon, rightChargeIcon, batteryLeft, batteryRight, chargingLeft, chargingRight, true);
                updateCaseDisplay(batteryLevelCase, caseChargeIcon, batteryCase, chargingCase, true);
                break;
            case 1: // Only one battery: combine buds, hide case
                updateBudsDisplay(batteryLevelLeft, batteryLevelRight, leftChargeIcon, rightChargeIcon, batteryLeft, batteryRight, chargingLeft, chargingRight, true);
                updateCaseDisplay(batteryLevelCase, caseChargeIcon, batteryCase, chargingCase, false);
                break;
            default:
                // Hide all containers if no battery info is available
                budsContainer.setVisibility(GONE);
                caseContainer.setVisibility(GONE);
                break;
        }
    }

    private void updateBudsDisplay(TextView left, TextView right, ImageView leftCharge, ImageView rightCharge,
                                   int batteryLeft, int batteryRight, boolean chargingLeft, boolean chargingRight, boolean combine) {
        if (combine) {
            left.setVisibility(GONE);
            leftCharge.setVisibility(GONE);
            right.setText(getBatteryLevelString(BatteryUtils.getValidBatteryLevel(batteryLeft, batteryRight)));
        } else {
            left.setVisibility(VISIBLE);
            left.setText(getBatteryLevelString(batteryLeft));
            right.setText(getBatteryLevelString(batteryRight));
        }
        rightCharge.setVisibility(chargingRight ? VISIBLE : GONE);
    }

    private void updateCaseDisplay(TextView caseView, ImageView caseCharge, int batteryCase, boolean chargingCase, boolean visible) {
        if (visible) {
            caseView.setVisibility(VISIBLE);
            caseCharge.setVisibility(chargingCase ? VISIBLE : GONE);
            caseView.setText(getBatteryLevelString(batteryCase));
        } else {
            caseView.setVisibility(GONE);
            caseCharge.setVisibility(GONE);
        }
    }

    private String getBatteryLevelString(int batteryLevel) {
        if (batteryLevel == -1) {
            return "-";
        }

        return batteryLevel + "%";
    }

    private int getBatteryLevel(GBDevice device, int batteryIndex) {
        if (device.getBatteryState(batteryIndex) == BatteryState.UNKNOWN ||
                device.getBatteryState(batteryIndex) == BatteryState.NO_BATTERY ||
                device.getDeviceCoordinator().getBatteryCount() <= batteryIndex) {
            return -1;
        }

        return device.getBatteryLevel(batteryIndex);
    }

    private boolean isBatteryCharging(GBDevice device, int batteryIndex) {
        if (device.getDeviceCoordinator().getBatteryCount() <= batteryIndex) {
            return false;
        }

        return device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING ||
                device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING_FULL;
    }
}
