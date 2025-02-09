/*  Copyright (C) 2024 Jonathan Gobbo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import xyz.tenseventyseven.fresh.AppException;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds.RedmiBudsDeviceSupport;

public abstract class RedmiBudsCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws AppException {

    }

    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return RedmiBudsDeviceSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        int batteryCount = 0;

        if (supports(RedmiBudsCapabilities.ReportsBattery) ||
                supports(RedmiBudsCapabilities.ReportsCaseBattery)) {
            batteryCount++;
        }

        if (supports(RedmiBudsCapabilities.ReportsLeftEarbudBattery)) {
            batteryCount++;
        }

        if (supports(RedmiBudsCapabilities.ReportsRightEarbudBattery)) {
            batteryCount++;
        }

        return batteryCount;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (supports(RedmiBudsCapabilities.ActiveNoiseCancellationV3)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_anc_v3);
        } else if (supports(RedmiBudsCapabilities.ActiveNoiseCancellationV2)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_anc_v2);
        } else if (supports(RedmiBudsCapabilities.ActiveNoiseCancellationV1)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_anc_v1);
        }

        deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS, R.xml.devicesettings_headphones);

        addSettingsUnderHeader(deviceSpecificSettings, device, R.xml.devicesettings_header_sound_vibration, new LinkedHashMap<RedmiBudsCapabilities, Integer>() {{
            put(RedmiBudsCapabilities.EqualizerV1, R.xml.devicesettings_redmibuds_equalizer_v1);
            put(RedmiBudsCapabilities.EqualizerV2, R.xml.devicesettings_redmibuds_equalizer_v2);
            put(RedmiBudsCapabilities.AdaptiveSound, R.xml.devicesettings_redmibuds_adaptive_sound);
        }});

        addSettingsUnderHeader(deviceSpecificSettings, device, R.xml.devicesettings_header_system, new LinkedHashMap<RedmiBudsCapabilities, Integer>() {{
            put(RedmiBudsCapabilities.GestureControl, R.xml.devicesettings_redmibuds_gestures);
            put(RedmiBudsCapabilities.InEarDetection, R.xml.devicesettings_redmibuds_in_ear_detection);
            put(RedmiBudsCapabilities.AutoAnswerPhoneCalls, R.xml.devicesettings_redmibuds_auto_answer_calls);
            put(RedmiBudsCapabilities.DualDeviceConnection, R.xml.devicesettings_redmibuds_dual_connection);
        }});

        return deviceSpecificSettings;
    }

    /**
     * Add the preference screens for capabilities under a header. The header is also only added if at least one capability is supported by the device.
     *
     * @param deviceSpecificSettings the device specific settings
     * @param header                 the header to add, if any capability supported
     * @param capabilities           the map of capability to preference screen
     */
    private void addSettingsUnderHeader(final DeviceSpecificSettings deviceSpecificSettings,
                                        final GBDevice device,
                                        final int header,
                                        final Map<RedmiBudsCapabilities, Integer> capabilities) {
        final Set<RedmiBudsCapabilities> supportedCapabilities = new HashSet<>(capabilities.keySet());
        for (RedmiBudsCapabilities capability : capabilities.keySet()) {
            if (!supports(capability)) {
                supportedCapabilities.remove(capability);
            }
        }

        if (supportedCapabilities.isEmpty()) {
            // None of the capabilities in the map are supported
            return;
        }

        deviceSpecificSettings.addRootScreen(header);

        for (Map.Entry<RedmiBudsCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(capabilitiesSetting.getKey())) {
                deviceSpecificSettings.addRootScreen(capabilitiesSetting.getValue());
            }
        }
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new RedmiBudsSettingsCustomizer(device);
    }

    @Override
    public BatteryConfig[] getBatteryConfig(GBDevice device) {
        List<BatteryConfig> batteryConfigs = new ArrayList<>();

        if (supports(RedmiBudsCapabilities.ReportsCaseBattery)) {
            batteryConfigs.add(new BatteryConfig(0, R.drawable.ic_buds3_case_solid, R.string.battery_case));
        } else if (supports(RedmiBudsCapabilities.ReportsBattery)) {
            batteryConfigs.add(new BatteryConfig(0, R.drawable.ic_buds3_left_right, R.string.menuitem_headphone));
        }

        if (supports(RedmiBudsCapabilities.ReportsLeftEarbudBattery)) {
            batteryConfigs.add(new BatteryConfig(1, R.drawable.ic_buds3_left_right, R.string.left_earbud));
        }

        if (supports(RedmiBudsCapabilities.ReportsRightEarbudBattery)) {
            batteryConfigs.add(new BatteryConfig(2, R.drawable.ic_buds3_left_right, R.string.right_earbud));
        }

        return batteryConfigs.toArray(new BatteryConfig[0]);
    }

    public List<RedmiBudsCapabilities> getCapabilities()  {
        return Collections.emptyList();
    }

    public boolean supports(final RedmiBudsCapabilities capability) {
        return getCapabilities().contains(capability);
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_nothingear;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_nothingear_disabled;
    }

    @Override
    public DeviceKind getDeviceKind() {
        return DeviceKind.EARBUDS;
    }

    @Override
    public int getDeviceIconResource() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_buds_pro;
    }

    @Override
    public int getDeviceImageResource() {
        return R.drawable.headset_redmi_buds_5_black;
    }

    @Override
    public int getDeviceImageResource(GBDevice device) {
        return super.getDeviceImageResource(device);
    }

    @Override
    public RedmiBudsSettingsCoordinator getDeviceSettings() {
        return new RedmiBudsSettingsCoordinator(this);
    }
}
