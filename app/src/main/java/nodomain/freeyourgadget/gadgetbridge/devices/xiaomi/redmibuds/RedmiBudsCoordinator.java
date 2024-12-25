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

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds.RedmiBudsDeviceSupport;
import xyz.tenseventyseven.fresh.wearable.R;
import xyz.tenseventyseven.fresh.wearable.WearableException;

public class RedmiBudsCoordinator extends AbstractDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @Override
    public GeneralDeviceType getGeneralDeviceType() {
        return GeneralDeviceType.EARBUDS;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new RedmiBudsSettingsCustomizer(device);
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        return false;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws WearableException {
    }

    @Override
    public int getBatteryCount() {
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
    public BatteryConfig[] getBatteryConfig(GBDevice device) {
        List<BatteryConfig> batteryConfigs = new ArrayList<>();

        if (supports(RedmiBudsCapabilities.ReportsCaseBattery)) {
            batteryConfigs.add(new BatteryConfig(0, R.drawable.device_ic_galaxy_buds3_case, R.string.battery_case));
        } else if (supports(RedmiBudsCapabilities.ReportsBattery)) {
            batteryConfigs.add(new BatteryConfig(0, R.drawable.device_ic_galaxy_buds3, R.string.menuitem_headphone));
        }

        if (supports(RedmiBudsCapabilities.ReportsLeftEarbudBattery)) {
            batteryConfigs.add(new BatteryConfig(1, R.drawable.device_ic_galaxy_buds3, R.string.left_earbud));
        }

        if (supports(RedmiBudsCapabilities.ReportsRightEarbudBattery)) {
            batteryConfigs.add(new BatteryConfig(2, R.drawable.device_ic_galaxy_buds3, R.string.right_earbud));
        }

        return batteryConfigs.toArray(new BatteryConfig[0]);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (supports(RedmiBudsCapabilities.ActiveNoiseCancellationV2)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_anc_v2);
        } else if (supports(RedmiBudsCapabilities.ActiveNoiseCancellationV1)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_anc_v1);
        }

        if (supports(RedmiBudsCapabilities.AdaptiveNoiseCancellation)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_adaptive_anc);
        }

        addSettingsUnderCategory(deviceSpecificSettings, new LinkedHashMap<RedmiBudsCapabilities, Integer>() {{
            put(RedmiBudsCapabilities.GestureControl, R.xml.devicesettings_redmibuds_gestures);
        }});

        addSettingsUnderCategory(deviceSpecificSettings, new LinkedHashMap<RedmiBudsCapabilities, Integer>() {{
            put(RedmiBudsCapabilities.EqualizerV1, R.xml.devicesettings_redmibuds_equalizer_v1);
            put(RedmiBudsCapabilities.EqualizerV2, R.xml.devicesettings_redmibuds_equalizer_v2);
            put(RedmiBudsCapabilities.AdaptiveSound, R.xml.devicesettings_redmibuds_adaptive_sound);
        }});

        addSettingsUnderSubScreen(deviceSpecificSettings, DeviceSpecificSettingsScreen.EARBUDS_SETTINGS,
        new LinkedHashMap<RedmiBudsCapabilities, Integer>() {{
            put(RedmiBudsCapabilities.ReportsBattery, R.xml.devicesettings_redmibuds_read_notifications);
            put(RedmiBudsCapabilities.AutoAnswerPhoneCalls, R.xml.devicesettings_redmibuds_auto_answer);
            
            put(RedmiBudsCapabilities.DualDeviceConnection, R.xml.devicesettings_redmibuds_dual_connection);
            put(RedmiBudsCapabilities.InEarDetection, R.xml.devicesettings_redmibuds_in_ear_detection);
        }});

        //deviceSpecificSettings.addRootScreen(R.xml.devicesettings_header_developer);
        //deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds_firmware_version);

        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_header_empty);
        return deviceSpecificSettings;
    }

    public List<RedmiBudsCapabilities> getCapabilities()  {
        return Collections.emptyList();
    }

    public boolean supports(final RedmiBudsCapabilities capability) {
        return getCapabilities().contains(capability);
    }

    /**
     * Add the preference screens for capabilities under a header. The header is also only added if at least one capability is supported by the device.
     *
     * @param deviceSpecificSettings the device specific settings
     * @param capabilities           the map of capability to preference screen
     */
    private void addSettingsUnderCategory(final DeviceSpecificSettings deviceSpecificSettings,
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

        for (Map.Entry<RedmiBudsCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(capabilitiesSetting.getKey())) {
                deviceSpecificSettings.addRootScreen(capabilitiesSetting.getValue());
            }
        }
    }

    /**
     * Add the preference screens for capabilities under a sub screen. The sub screen is also only added if at least one capability is supported by the device.
     *
     * @param deviceSpecificSettings the device specific settings
     * @param subScreen               the sub screen to add the capabilities under
     * @param capabilities           the map of capability to preference screen
     */
    private void addSettingsUnderSubScreen(final DeviceSpecificSettings deviceSpecificSettings,
                                           final DeviceSpecificSettingsScreen subScreen,
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

        deviceSpecificSettings.addRootScreen(subScreen);

        for (Map.Entry<RedmiBudsCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(capabilitiesSetting.getKey())) {
                deviceSpecificSettings.addSubScreen(subScreen, capabilitiesSetting.getValue());
            }
        }
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return RedmiBudsDeviceSupport.class;
    }

    // TODO: For device-specific coordinators, change the device name to the actual device name
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int getDefaultIconResource() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_buds_pro;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_nothingear_disabled;
    }
}
