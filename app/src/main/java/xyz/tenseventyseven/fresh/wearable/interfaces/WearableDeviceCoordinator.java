package xyz.tenseventyseven.fresh.wearable.interfaces;

import androidx.annotation.DrawableRes;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface WearableDeviceCoordinator {
    enum DeviceKind {
        WATCH,
        FITNESS_TRACKER,
        HEADPHONES,
        EARBUDS,
        HEART_RATE_MONITOR,
        BLOOD_PRESSURE_MONITOR,
        SCALE,
        THERMOMETER,
        SPO2_MONITOR,
        GLUCOSE_MONITOR,
        ECG_MONITOR,
        OTHER
    }
    int COMBINED_BUDS_BATTERY_INDEX = -1;

    default DeviceKind getDeviceKind() {
        return DeviceKind.OTHER;
    }

    default boolean isHealthTrackingDevice() {
        DeviceKind type = getDeviceKind();
        return type == DeviceKind.FITNESS_TRACKER || type == DeviceKind.HEART_RATE_MONITOR || type == DeviceKind.BLOOD_PRESSURE_MONITOR || type == DeviceKind.SCALE || type == DeviceKind.THERMOMETER || type == DeviceKind.SPO2_MONITOR || type == DeviceKind.GLUCOSE_MONITOR || type == DeviceKind.ECG_MONITOR || type == DeviceKind.WATCH;
    }

    @DrawableRes
    int getDefaultIconResource();

    @DrawableRes
    default int getDeviceIconResource() {
        return getDefaultIconResource();
    }

    @DrawableRes
    default int getDeviceImageResource() {
        return getDefaultIconResource();
    }

    @DrawableRes
    default int getDeviceImageResource(GBDevice device) {
        return getDeviceImageResource();
    }

    default WearableSettingCoordinator getDeviceSettings() {
        return null;
    }

    default WearableSettingCoordinator getDeviceSettings(GBDevice device) {
        return getDeviceSettings();
    }

    default int getBatteryIconResource(int index) {
        return getDefaultIconResource();
    }

    default WidgetManager getWearableWidgetManager(GBDevice device) {
        return null;
    }
}
