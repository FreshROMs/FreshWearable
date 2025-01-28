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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_NOTIFY_FULL_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_NOTIFY_FULL_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_NOTIFY_LOW_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_NOTIFY_LOW_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_SHOW_IN_NOTIFICATION;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.GBSimpleSummaryProvider;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.PreferenceCategoryMultiline;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.activities.DeviceSubSettingsActivity;
import xyz.tenseventyseven.fresh.wearable.models.DeviceSpecificSettings;
import xyz.tenseventyseven.fresh.wearable.models.Screen;

public class DeviceSubSettingsFragment extends DeviceSettingsFragmentCommon {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSubSettingsFragment.class);

    public static final String FRAGMENT_TAG = "DEVICE_SUB_SETTINGS_FRAGMENT";

    private String mParentKey = null;

    private String mScreenKey = null;

    @Override
    public void onSavedInstanceState(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mScreenKey = arguments.getString("screenKey");
            mParentKey = arguments.getString("parentKey");
        }
    }

    @Override
    public void onSetupPreferences(String rootKey) {
        LOG.debug("onCreatePreferences parentKey: {}, screenKey: {}", mParentKey, mScreenKey);
        if (mScreenKey == null || mScreenKey.isEmpty()) {
            LOG.debug("No screen key provided, cannot create preferences");
            return;
        }

        /*
         * Our new Screen class can now reference nested screens by key,
         * so we can make it faster by providing the parent key when we navigate to a sub-screen.
         */
        boolean success = setupScreenFromKey() || setupScreenFromParent() || setupScreenFromAll();
        if (!success) {
            return;
        }

        addDynamicSettings();

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
    public void onResume() {
        super.onResume();
        if (mCustomizer != null) {
            mCustomizer.update(this);
        }
    }

    private boolean setupScreenFromKey() {
        // First attempt to find a known screen for this key
        final Screen root = mDeviceSpecificSettings.getRoot();
        Screen screen = root.getScreen(mScreenKey);

        if (screen == null) {
            return false;
        }

        boolean first = true;
        for (int setting : screen.getSettings()) {
            if (first) {
                try {
                    setPreferencesFromResource(setting, mScreenKey);
                } catch (Exception ignore) {
                    setPreferencesFromResource(setting, null);
                }

                first = false;
            } else {
                addPreferencesFromResource(setting);
            }
        }

        return true;
    }

    private boolean setupScreenFromParent() {
        if (mParentKey == null || mParentKey.isEmpty()) {
            return false;
        }

        final Screen root = mDeviceSpecificSettings.getRoot();
        Screen parent = root.getScreen(mParentKey);
        if (parent == null) {
            return false;
        }

        final List<Integer> allScreens = mDeviceSpecificSettings.getAllScreens();
        for (int setting : allScreens) {
            try {
                setPreferencesFromResource(setting, mScreenKey);
            } catch (Exception ignore) {
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean setupScreenFromAll() {
        // Now, this is ugly: search all the xml files for the rootKey
        // This means that this device is using the deprecated getSupportedDeviceSpecificSettings,
        // or that we're on a sub-screen
        final List<Integer> allScreens = mDeviceSpecificSettings.getAllScreens();
        for (int setting : allScreens) {
            try {
                setPreferencesFromResource(setting, mScreenKey);
            } catch (Exception ignore) {
                continue;
            }
            return true;
        }

        return false;
    }

    private void addDynamicSettings() {
        if (mScreenKey.equals(DeviceSpecificSettingsScreen.BATTERY.getKey())) {
            addBatterySettings();
        }
    }

    private void addBatterySettings() {
        final DeviceCoordinator coordinator = mDevice.getDeviceCoordinator();
        final PreferenceScreen batteryScreen = getPreferenceScreen();
        if (batteryScreen == null) {
            return;
        }
        final BatteryConfig[] batteryConfigs = coordinator.getBatteryConfig(mDevice);
        for (final BatteryConfig batteryConfig : batteryConfigs) {
            if (batteryConfigs.length > 1 || coordinator.addBatteryPollingSettings()) {
                final Preference prefHeader = new PreferenceCategory(requireContext());
                prefHeader.setKey("pref_battery_header_" + batteryConfig.getBatteryIndex());
                prefHeader.setIconSpaceReserved(false);
                if (batteryConfig.getBatteryLabel() != GBDevice.BATTERY_LABEL_DEFAULT) {
                    prefHeader.setTitle(batteryConfig.getBatteryLabel());
                } else {
                    prefHeader.setTitle(requireContext().getString(R.string.battery_i, batteryConfig.getBatteryIndex()));
                }
                batteryScreen.addPreference(prefHeader);
            }

            final SwitchPreferenceCompat showInNotification = new SwitchPreferenceCompat(requireContext());
            showInNotification.setLayoutResource(R.layout.preference_checkbox);
            showInNotification.setKey(PREF_BATTERY_SHOW_IN_NOTIFICATION + batteryConfig.getBatteryIndex());
            showInNotification.setTitle(R.string.show_in_notification);
            showInNotification.setIconSpaceReserved(false);
            showInNotification.setDefaultValue(true);
            batteryScreen.addPreference(showInNotification);

            final SwitchPreferenceCompat notifyLowEnabled = new SwitchPreferenceCompat(requireContext());
            notifyLowEnabled.setLayoutResource(R.layout.preference_checkbox);
            notifyLowEnabled.setKey(PREF_BATTERY_NOTIFY_LOW_ENABLED + batteryConfig.getBatteryIndex());
            notifyLowEnabled.setTitle(R.string.battery_low_notify_enabled);
            notifyLowEnabled.setDefaultValue(true);
            notifyLowEnabled.setIconSpaceReserved(false);
            batteryScreen.addPreference(notifyLowEnabled);

            final EditTextPreference notifyLowThreshold = new EditTextPreference(requireContext());
            notifyLowThreshold.setKey(PREF_BATTERY_NOTIFY_LOW_THRESHOLD + batteryConfig.getBatteryIndex());
            notifyLowThreshold.setTitle(R.string.battery_low_threshold);
            notifyLowThreshold.setDialogTitle(R.string.battery_low_threshold);
            notifyLowThreshold.setIconSpaceReserved(false);
            notifyLowThreshold.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 100, true));
                editText.setSelection(editText.getText().length());
            });
            notifyLowThreshold.setSummaryProvider(new GBSimpleSummaryProvider(
                    requireContext().getString(R.string.default_percentage, batteryConfig.getDefaultLowThreshold()),
                    R.string.battery_percentage_str
            ));

            batteryScreen.addPreference(notifyLowThreshold);

            final SwitchPreferenceCompat notifyFullEnabled = new SwitchPreferenceCompat(requireContext());
            notifyFullEnabled.setLayoutResource(R.layout.preference_checkbox);
            notifyFullEnabled.setKey(PREF_BATTERY_NOTIFY_FULL_ENABLED + batteryConfig.getBatteryIndex());
            notifyFullEnabled.setTitle(R.string.battery_full_notify_enabled);
            notifyFullEnabled.setDefaultValue(true);
            notifyFullEnabled.setIconSpaceReserved(false);
            batteryScreen.addPreference(notifyFullEnabled);

            final EditTextPreference notifyFullThreshold = new EditTextPreference(requireContext());
            notifyFullThreshold.setKey(PREF_BATTERY_NOTIFY_FULL_THRESHOLD + batteryConfig.getBatteryIndex());
            notifyFullThreshold.setTitle(R.string.battery_full_threshold);
            notifyFullThreshold.setDialogTitle(R.string.battery_full_threshold);
            notifyFullThreshold.setIconSpaceReserved(false);
            notifyFullThreshold.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 100, true));
                editText.setSelection(editText.getText().length());
            });
            notifyFullThreshold.setSummaryProvider(new GBSimpleSummaryProvider(
                    requireContext().getString(R.string.default_percentage, batteryConfig.getDefaultFullThreshold()),
                    R.string.battery_percentage_str
            ));
            batteryScreen.addPreference(notifyFullThreshold);
        }

        if (coordinator.addBatteryPollingSettings()) {
            final Preference prefHeader = new PreferenceCategoryMultiline(requireContext());
            prefHeader.setKey("pref_battery_polling_header");
            prefHeader.setIconSpaceReserved(false);
            prefHeader.setTitle(R.string.pref_battery_polling_configuration);
            prefHeader.setSummary(R.string.pref_battery_polling_summary);
            batteryScreen.addPreference(prefHeader);

            final SwitchPreferenceCompat pollingToggle = new SwitchPreferenceCompat(requireContext());
            pollingToggle.setLayoutResource(R.layout.preference_checkbox);
            pollingToggle.setKey(PREF_BATTERY_POLLING_ENABLE);
            pollingToggle.setTitle(R.string.pref_battery_polling_enable);
            pollingToggle.setDefaultValue(true);
            pollingToggle.setIconSpaceReserved(false);
            batteryScreen.addPreference(pollingToggle);

            final EditTextPreference pollingInterval = new EditTextPreference(requireContext());
            pollingInterval.setKey(PREF_BATTERY_POLLING_INTERVAL);
            pollingInterval.setTitle(R.string.pref_battery_polling_interval);
            pollingInterval.setDialogTitle(R.string.pref_battery_polling_interval);
            pollingInterval.setIconSpaceReserved(false);
            pollingInterval.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                // Max is set to 8 days, which should be more than enough
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 11520, true));
                editText.setSelection(editText.getText().length());
            });
            pollingInterval.setSummaryProvider(new GBSimpleSummaryProvider(
                    getString(R.string.interval_fifteen_minutes),
                    R.string.pref_battery_polling_interval_format
            ));
            batteryScreen.addPreference(pollingInterval);
        }
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        Intent intent = new Intent(getContext(), DeviceSubSettingsActivity.class);
        intent.putExtra(DeviceSubSettingsActivity.EXTRA_SCREEN_KEY, preferenceScreen.getKey());
        intent.putExtra(DeviceSubSettingsActivity.EXTRA_PARENT_KEY, mScreenKey);
        intent.putExtra(DeviceSubSettingsActivity.EXTRA_DEVICE_SPECIFIC_SETTINGS, mDeviceSpecificSettings);
        intent.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
        startActivity(intent);
    }

    public static DeviceSubSettingsFragment newInstance(GBDevice device, DeviceSpecificSettings settings, String screenKey, String parentKey) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final DeviceSpecificSettingsCustomizer mDeviceSpecificSettingsCustomizer = coordinator.getDeviceSpecificSettingsCustomizer(device);
        final String settingsFileSuffix = device.getAddress();

        final DeviceSubSettingsFragment fragment = new DeviceSubSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);
        fragment.setDeviceSpecificSettings(settings);
        fragment.setCustomizer(mDeviceSpecificSettingsCustomizer);
        fragment.setScreenKey(screenKey);
        fragment.setParentKey(parentKey);
        fragment.setDevice(device);

        return fragment;
    }

    private void setScreenKey(String screenKey) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putString("screenKey", screenKey);
        setArguments(args);
    }

    private void setParentKey(String parentKey) {
        if (parentKey == null) {
            parentKey = "";
        }

        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putString("parentKey", parentKey);
        setArguments(args);
    }

    @Override
    protected void onSharedPreferenceChanged(Preference preference) {
        if (mCustomizer != null) {
            mCustomizer.onPreferenceChange(preference, DeviceSubSettingsFragment.this);
        }
    }
}
