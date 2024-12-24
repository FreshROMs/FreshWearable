/*  Copyright (C) 2019-2024 Andreas Shimokawa, José Rebelo, Petr Vaněk,
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
package xyz.tenseventyseven.fresh.wearable.activities;

import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.DeviceSubSettingsFragment;
import xyz.tenseventyseven.fresh.wearable.models.DeviceSpecificSettings;

public class DeviceSubSettingsActivity extends AbstractSettingsActivityV2 {
    public static final String EXTRA_SCREEN_KEY = "EXTRA_SCREEN_KEY";
    public static final String EXTRA_PARENT_KEY = "EXTRA_PARENT_KEY";
    public static final String EXTRA_DEVICE_SPECIFIC_SETTINGS = "EXTRA_DEVICE_SPECIFIC_SETTINGS";

    private DeviceSubSettingsFragment mFragment;

    @Override
    protected String fragmentTag() {
        return DeviceSubSettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        final GBDevice device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        final DeviceSpecificSettings settings = getIntent().getParcelableExtra(EXTRA_DEVICE_SPECIFIC_SETTINGS);
        final String screenKey = getIntent().getStringExtra(EXTRA_SCREEN_KEY);
        final String parentKey = getIntent().getStringExtra(EXTRA_PARENT_KEY);

        mFragment = DeviceSubSettingsFragment.newInstance(device, settings, screenKey, parentKey);
        return mFragment;
    }
}
