package xyz.tenseventyseven.fresh.wearable.activities;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class DashboardUtils {
    public static DeviceSetting getDeveloperOptions(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        DeviceSetting screen = DeviceSetting.screen(
                "device_developer_options",
                R.string.wear_device_developer_options,
                0,
                R.drawable.wear_ic_settings_developer_options
        );
        screen.settings = new ArrayList<>();

        List<DeviceSetting> deviceOptions = coordinator.getDeviceSettings(device).getDeveloperOptions();
        if (deviceOptions != null && !deviceOptions.isEmpty()) {
            screen.settings.addAll(deviceOptions);
        }

        DeviceCoordinator.ConnectionType connectionType = coordinator.getConnectionType();
        if (connectionType == DeviceCoordinator.ConnectionType.BLE) {
            screen.settings.add(DeviceSetting.divider(R.string.connection_over_ble));
            screen.settings.add(DeviceSetting.switchSetting(
                    "prefs_key_device_auto_reconnect",
                    R.string.auto_reconnect_ble_title,
                    R.string.auto_reconnect_ble_summary,
                    0,
                    "true"
            ));
        }

        if (connectionType == DeviceCoordinator.ConnectionType.BT_CLASSIC) {
            screen.settings.add(DeviceSetting.divider(R.string.connection_over_bt_classic));
            screen.settings.add(DeviceSetting.switchSetting(
                    "prefs_key_device_reconnect_on_acl",
                    R.string.autoconnect_from_device_title,
                    R.string.autoconnect_from_device_summary,
                    0,
                    "false"
            ));
        }

        screen.settings.add(DeviceSetting.divider(R.string.pref_header_intent_api));
        screen.settings.add(DeviceSetting.switchSetting(
                "third_party_apps_set_settings",
                R.string.pref_title_third_party_app_device_settings,
                R.string.pref_summary_third_party_app_device_settings,
                0,
                "false"
        ));

        if (coordinator.getConnectionType().usesBluetoothLE()) {
            screen.settings.add(DeviceSetting.divider(R.string.prefs_title_ble_intent_api));
            screen.settings.add(DeviceSetting.switchSetting(
                    "prefs_device_ble_api_characteristic_read_write",
                    R.string.prefs_summary_gatt_client_allow_gatt_interactions,
                    R.string.prefs_title_gatt_client_allow_gatt_interactions,
                    0,
                    "false"
            ));

            screen.settings.add(DeviceSetting.switchSetting(
                    "prefs_device_ble_api_characteristic_notify",
                    R.string.prefs_title_gatt_client_notification_intents,
                    R.string.prefs_summary_gatt_client_notification_intents,
                    0,
                    "false"
            ));

            screen.settings.add(DeviceSetting.editText(
                    "prefs_device_ble_api_filter_char",
                    R.string.prefs_title_gatt_client_filter_char,
                    R.string.prefs_summary_gatt_client_filter_char,
                    0,
                    ""
            ));

            screen.settings.add(DeviceSetting.editText(
                    "prefs_device_ble_api_package",
                    R.string.prefs_title_gatt_client_api_package,
                    R.string.prefs_summary_gatt_client_api_package,
                    0,
                    ""
            ));
        }

        if (screen.settings.isEmpty()) {
            return null;
        }

        return screen;
    }

    public static DeviceSetting getBatterySettings(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (coordinator.getBatteryCount(device) <= 0) {
            // No use showing battery settings if there are no batteries
            return null;
        }

        final BatteryConfig[] batteryConfigs = coordinator.getBatteryConfig(device);
        DeviceSetting screen = DeviceSetting.screen(
                "pref_screen_battery",
                R.string.wear_device_battery_notifications,
                0,
                R.drawable.wear_ic_settings_battery_notifications
        );
        screen.screenSummary = R.string.wear_device_battery_notifications_screen_summary;
        screen.settings = new ArrayList<>();

        for (final BatteryConfig batteryConfig : batteryConfigs) {
            if (batteryConfigs.length > 1 || coordinator.addBatteryPollingSettings()) {
                screen.settings.add(DeviceSetting.divider(
                        batteryConfig.getBatteryLabel() != GBDevice.BATTERY_LABEL_DEFAULT
                                ? batteryConfig.getBatteryLabel()
                                : R.string.battery_i
                ));
            }

            screen.settings.add(DeviceSetting.switchSetting(
                    PREF_BATTERY_SHOW_IN_NOTIFICATION + batteryConfig.getBatteryIndex(),
                    R.string.show_in_notification,
                    0,
                    0,
                    "true"
            ));

            screen.settings.add(DeviceSetting.switchSetting(
                    PREF_BATTERY_NOTIFY_LOW_ENABLED + batteryConfig.getBatteryIndex(),
                    R.string.battery_low_notify_enabled,
                    0,
                    0,
                    "true"
            ));

            DeviceSetting threshold = DeviceSetting.editText(
                    PREF_BATTERY_NOTIFY_LOW_THRESHOLD + batteryConfig.getBatteryIndex(),
                    R.string.battery_low_threshold,
                    R.string.battery_percentage_str,
                    0,
                    String.valueOf(batteryConfig.getDefaultLowThreshold())
            );
            threshold.valueAsSummary = true;
            threshold.valueKind = DeviceSetting.ValueKind.INT;
            threshold.min = 0;
            threshold.max = 100;
            screen.settings.add(threshold);

            screen.settings.add(DeviceSetting.switchSetting(
                    PREF_BATTERY_NOTIFY_FULL_ENABLED + batteryConfig.getBatteryIndex(),
                    R.string.battery_full_notify_enabled,
                    0,
                    0,
                    "true"
            ));

            DeviceSetting fullThreshold = DeviceSetting.editText(
                    PREF_BATTERY_NOTIFY_FULL_THRESHOLD + batteryConfig.getBatteryIndex(),
                    R.string.battery_full_threshold,
                    R.string.battery_percentage_str,
                    0,
                    String.valueOf(batteryConfig.getDefaultFullThreshold())
            );
            fullThreshold.valueAsSummary = true;
            fullThreshold.valueKind = DeviceSetting.ValueKind.INT;
            fullThreshold.min = 0;
            fullThreshold.max = 100;
            screen.settings.add(fullThreshold);
        }

        if (coordinator.addBatteryPollingSettings()) {
            screen.settings.add(DeviceSetting.divider(R.string.pref_battery_polling_configuration));
            screen.settings.add(DeviceSetting.switchSetting(
                    PREF_BATTERY_POLLING_ENABLE,
                    R.string.pref_battery_polling_enable,
                    0,
                    0,
                    "true"
            ));

            DeviceSetting interval = DeviceSetting.editText(
                    PREF_BATTERY_POLLING_INTERVAL,
                    R.string.pref_battery_polling_interval,
                    R.string.pref_battery_polling_interval_format,
                    0,
                    "15"
            );
            interval.min = 0;
            interval.max = 11520;
            interval.valueAsSummary = true;
            interval.valueKind = DeviceSetting.ValueKind.INT;
            screen.settings.add(interval);
        }

        return screen;
    }

    public static DeviceSetting getAboutDeviceSettings() {
        DeviceSetting screen = DeviceSetting.screen(
                "pref_screen_about_device",
                R.string.wear_device_about_device,
                R.string.wear_device_about_device_summary,
                R.drawable.wear_ic_settings_about_device
        );
        screen.settings = new ArrayList<>();

        screen.settings.add(DeviceSetting.aboutDeviceHeader());
        screen.settings.add(DeviceSetting.divider());

        screen.settings.add(
                DeviceSetting.info(
                        "firmware_version",
                        R.string.wear_device_about_device_firmware_version,
                        0,
                        0
                )
        );

        return screen;
    }
}
