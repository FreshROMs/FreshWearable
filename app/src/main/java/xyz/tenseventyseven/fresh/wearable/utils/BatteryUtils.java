package xyz.tenseventyseven.fresh.wearable.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableDeviceCoordinator;

public class BatteryUtils {
    public static List<String> formatDeviceBattery(Context context, GBDevice device, boolean hasStatus, boolean includeCase) {
        List<String> formatted = new ArrayList<>();
        DeviceCoordinator coordinator = device.getDeviceCoordinator();

        boolean isBuds = coordinator.getDeviceKind() == WearableDeviceCoordinator.DeviceKind.EARBUDS;
        int batteryCount = coordinator.getBatteryCount(device);

        if (isBuds && batteryCount > 1) {
            formatEarbudsBattery(device, batteryCount, formatted, context, includeCase);
        } else if (batteryCount > 0) {
            formatSingleBattery(device, hasStatus, formatted, context);
        }

        return formatted;
    }

    public static List<String> formatDeviceBattery(Context context, GBDevice device) {
        return formatDeviceBattery(context, device, true, true);
    }

    public static String formatBudsBattery(Context context, GBDevice device) {
        List<String> formatted = new ArrayList<>();
        DeviceCoordinator coordinator = device.getDeviceCoordinator();

        int batteryCount = coordinator.getBatteryCount(device);
        if (batteryCount < 2) {
            return "";
        }

        formatEarbudsBattery(device, batteryCount, formatted, context, false); // Exclude case battery
        return formatted.isEmpty() ? "" : formatted.get(0);
    }

    private static void formatEarbudsBattery(GBDevice device, int batteryCount, List<String> formatted, Context context, boolean includeCase) {
        int batteryCase = device.getBatteryLevel(0); // Case battery
        int batteryLeft = device.getBatteryLevel(1); // Left earbud battery
        int batteryRight = batteryCount > 2 ? device.getBatteryLevel(2) : -1; // Right earbud battery (if available)

        if (batteryCount == 3) {
            if (shouldCombineBudsBattery(device)) {
                int battery = getValidBatteryLevel(batteryLeft, batteryRight);
                if (includeCase) {
                    formatted.add(context.getString(R.string.wear_device_buds_battery_status_combined, battery, batteryCase));
                } else {
                    formatted.add(context.getString(R.string.wear_device_buds_battery_status_combined_no_case, battery));
                }
            } else {
                if (includeCase) {
                    formatted.add(context.getString(R.string.wear_device_buds_battery_status_separate, batteryLeft, batteryRight, batteryCase));
                } else {
                    formatted.add(context.getString(R.string.wear_device_buds_battery_status_separate_no_case, batteryLeft, batteryRight));
                }
            }
        } else if (batteryCount == 2) {
            if (includeCase) {
                formatted.add(context.getString(R.string.wear_device_buds_battery_status_single_bud, batteryLeft, batteryCase));
            } else {
                formatted.add(context.getString(R.string.wear_device_battery_status_single, batteryLeft));
            }
        }
    }


    private static void formatSingleBattery(GBDevice device, boolean hasStatus, List<String> formatted, Context context) {
        int battery = device.getBatteryLevel();
        if (battery == -1) {
            return;
        }

        formatted.add(context.getString(R.string.wear_device_battery_status_single, battery));

        if (hasStatus) {
            String status = getBatteryStatusString(device.getBatteryState());
            if (status != null) {
                formatted.add(status);
            }
        }
    }

    public static boolean shouldCombineBudsBattery(GBDevice device) {
        // If either the battery is charging and the other is not, do not combine
        if ((isBatteryCharging(device, 1) && !isBatteryCharging(device, 2)) ||
                (isBatteryCharging(device, 2) && !isBatteryCharging(device, 1))) {
            return false;
        }

        int batteryLeft = device.getBatteryLevel(1);
        int batteryRight = device.getBatteryLevel(2);
        return batteryLeft == batteryRight || Math.abs(batteryLeft - batteryRight) < 5 || batteryLeft == -1 || batteryRight == -1;
    }

    public static int getValidBatteryLevel(int batteryLeft, int batteryRight) {
        if (batteryLeft == -1) return batteryRight;
        if (batteryRight == -1) return batteryLeft;
        return Math.min(batteryLeft, batteryRight);
    }

    private static boolean isBatteryCharging(GBDevice device, int batteryIndex) {
        return device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING
                || device.getBatteryState(batteryIndex) == BatteryState.BATTERY_CHARGING_FULL;
    }

    private static String getBatteryStatusString(BatteryState batteryState) {
        switch (batteryState) {
            case BATTERY_CHARGING:
                return "Charging";
            case BATTERY_CHARGING_FULL:
                return "Fully charged";
            case BATTERY_LOW:
                return "Low battery";
            default:
                return null;
        }
    }

}
