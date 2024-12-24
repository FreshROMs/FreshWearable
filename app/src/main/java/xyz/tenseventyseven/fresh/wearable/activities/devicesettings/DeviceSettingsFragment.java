/*  Copyright (C) 2019-2024 akasaka / Genjitsu Labs, Alicia Hormann, Andreas
    Böhler, Andreas Shimokawa, Arjan Schrijver, Cre3per, Damien Gaignon, Daniel
    Dakhno, Daniele Gobbetti, Davis Mosenkovs, foxstidious, José Rebelo, mamucho,
    NekoBox, opavlov, Petr Vaněk, Yoran Vulker, Yukai Li, Zhong Jianxin,
    John Vincent Corcega (TenSeventy7)

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package xyz.tenseventyseven.fresh.wearable.activities.devicesettings;

import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import xyz.tenseventyseven.fresh.wearable.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import xyz.tenseventyseven.fresh.wearable.activities.DeviceSubSettingsActivity;
import xyz.tenseventyseven.fresh.wearable.models.DeviceSpecificSettings;

public class DeviceSettingsFragment extends DeviceSettingsFragmentCommon {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSettingsFragment.class);

    public static final String FRAGMENT_TAG = "DEVICE_SETTINGS_FRAGMENT";

    @Override
    public void onSetupPreferences(String rootKey) {
        // We are always the root screen
        boolean first = true;
        for (int setting : mDeviceSpecificSettings.getRootScreens()) {
            if (first) {
                setPreferencesFromResource(setting, null);
                first = false;
            } else {
                addPreferencesFromResource(setting);
            }
        }

        // Since all root preference screens are empty, clicking them will not do anything
        // add on-click listeners
        for (final DeviceSpecificSettingsScreen value : DeviceSpecificSettingsScreen.values()) {
            final PreferenceScreen prefScreen = findPreference(value.getKey());
            if (prefScreen != null) {
                prefScreen.setOnPreferenceClickListener(p -> {
                    onNavigateToScreen(prefScreen);
                    return true;
                });
            }
        }
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        Intent intent = new Intent(getContext(), DeviceSubSettingsActivity.class);
        intent.putExtra(DeviceSubSettingsActivity.EXTRA_SCREEN_KEY, preferenceScreen.getKey());
        intent.putExtra(DeviceSubSettingsActivity.EXTRA_DEVICE_SPECIFIC_SETTINGS, mDeviceSpecificSettings);
        intent.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
        startActivity(intent);
    }

    private static DeviceSpecificSettings generateDeviceSettings(DeviceCoordinator coordinator, GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (coordinator.getSupportedLanguageSettings(device) != null) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_language_generic);
        }

        DeviceSpecificSettings coordinatorDeviceSettings = coordinator.getDeviceSpecificSettings(device);
        if (coordinatorDeviceSettings != null) {
            deviceSpecificSettings.mergeFrom(coordinatorDeviceSettings);
        }

        DeviceSpecificSettingsScreen moreSettings;
        switch (coordinator.getGeneralDeviceType()) {
            case WATCH:
            case FITNESS_TRACKER:
                moreSettings = DeviceSpecificSettingsScreen.WATCH_SETTINGS;
                break;
            case EARBUDS:
                moreSettings = DeviceSpecificSettingsScreen.EARBUDS_SETTINGS;
                break;
            case OTHER:
            default:
                moreSettings = DeviceSpecificSettingsScreen.DEVICE_SPECIFIC_SETTINGS;
                break;
        }

        deviceSpecificSettings.addSubScreen(
                moreSettings.getKey(),
                R.xml.devicesettings_header_empty
        );

        if (coordinator.getBatteryCount() > 0) {
            deviceSpecificSettings.addSubScreen(
                    moreSettings.getKey(),
                    DeviceSpecificSettingsScreen.BATTERY
            );
        }

        final int[] supportedAuthSettings = coordinator.getSupportedDeviceSpecificAuthenticationSettings();
        if (supportedAuthSettings != null && supportedAuthSettings.length > 0) {
            deviceSpecificSettings.addRootScreen(
                    DeviceSpecificSettingsScreen.AUTHENTICATION,
                    supportedAuthSettings
            );
        }

        deviceSpecificSettings.addSubScreen(
                moreSettings.getKey(),
                DeviceSpecificSettingsScreen.CONNECTION,
                coordinator.getSupportedDeviceSpecificConnectionSettings()
        );

        if (coordinator.supportsActivityTracking()) {
            deviceSpecificSettings.addSubScreen(
                    moreSettings.getKey(),
                    DeviceSpecificSettingsScreen.ACTIVITY_INFO,
                    R.xml.devicesettings_chartstabs,
                    R.xml.devicesettings_device_card_activity_card_preferences
            );
        }

        deviceSpecificSettings.addSubScreen(
                moreSettings.getKey(),
                DeviceSpecificSettingsScreen.DEVELOPER,
                R.xml.devicesettings_settings_third_party_apps
        );

        if (coordinator.getConnectionType().usesBluetoothLE()) {
            deviceSpecificSettings.addSubScreen(
                    moreSettings.getKey(),
                    DeviceSpecificSettingsScreen.DEVELOPER,
                    R.xml.devicesettings_ble_api
            );
        }

        return deviceSpecificSettings;
    }

    public static DeviceSettingsFragment newInstance(GBDevice device) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final DeviceSpecificSettings deviceSpecificSettings = generateDeviceSettings(coordinator, device);
        final DeviceSpecificSettingsCustomizer deviceSpecificSettingsCustomizer = coordinator.getDeviceSpecificSettingsCustomizer(device);
        final String settingsFileSuffix = device.getAddress();
        final DeviceSettingsFragment fragment = new DeviceSettingsFragment();

        fragment.setSettingsFileSuffix(settingsFileSuffix);
        fragment.setDeviceSpecificSettings(deviceSpecificSettings);
        fragment.setCustomizer(deviceSpecificSettingsCustomizer);
        fragment.setDevice(device);

        return fragment;
    }

    @Override
    protected void onSharedPreferenceChanged(Preference preference) {
        if (mCustomizer != null) {
            mCustomizer.onPreferenceChange(preference, DeviceSettingsFragment.this);
        }
    }
}
