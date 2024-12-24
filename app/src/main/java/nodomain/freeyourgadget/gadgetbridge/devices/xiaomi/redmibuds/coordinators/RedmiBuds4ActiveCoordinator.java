/*  Copyright (C) 2024 Jonathan Gobbo
    Copyright (C) 2024 John Vincent Corcega

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.coordinators;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.RedmiBudsCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.RedmiBudsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.wearable.R;

public class RedmiBuds4ActiveCoordinator extends RedmiBudsCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Redmi Buds 4 Active$");
    }

    @Override
    public List<RedmiBudsCapabilities> getCapabilities() {
        return Arrays.asList(
                RedmiBudsCapabilities.LowLatencyMode,
                RedmiBudsCapabilities.ReportsBattery,
                RedmiBudsCapabilities.ReportsCaseBattery,
                RedmiBudsCapabilities.ReportsLeftEarbudBattery,
                RedmiBudsCapabilities.ReportsRightEarbudBattery
        );
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_4_active;
    }

    @Override
    public int getDeviceImageResource(GBDevice device) {
        switch (device.getVariant()) {
            case 1:
                return R.drawable.headset_redmi_buds_4_active_black;
            case 7:
                return R.drawable.headset_redmi_buds_4_active_white;
            case 2:
            default:
                return R.drawable.headset_redmi_buds_4_active_beige;
        }
    }
}
