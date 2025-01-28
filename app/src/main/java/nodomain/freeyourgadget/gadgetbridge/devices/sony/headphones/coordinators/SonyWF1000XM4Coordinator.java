/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;

public class SonyWF1000XM4Coordinator extends SonyHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(".*WF-1000XM4.*");
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        final BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        final BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        final BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);

        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatteryDual,
                SonyHeadphonesCapabilities.BatteryCase,
                SonyHeadphonesCapabilities.AmbientSoundControl,
                SonyHeadphonesCapabilities.WindNoiseReduction,
                SonyHeadphonesCapabilities.EqualizerSimple,
                SonyHeadphonesCapabilities.AudioUpsampling,
                SonyHeadphonesCapabilities.ButtonModesLeftRight,
                SonyHeadphonesCapabilities.PauseWhenTakenOff,
                SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff
        );
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wf_1000xm4;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_galaxy_buds_disabled;
    }
}
