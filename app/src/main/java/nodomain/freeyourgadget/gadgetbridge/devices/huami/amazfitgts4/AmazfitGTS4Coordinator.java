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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitGTS4Coordinator extends ZeppOsCoordinator {
    @Override
    public String getDeviceBluetoothName() {
        return HuamiConst.AMAZFIT_GTS4_NAME;
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(
                7995648, // chinese mainland version
                7995649
        ));
    }

    @Override
    public boolean supportsContinuousFindDevice() {
        return true;
    }

    @Override
    public boolean supportsGpxUploads() {
        return true;
    }

    @Override
    public boolean supportsControlCenter() {
        return true;
    }

    @Override
    public boolean supportsToDoList() {
        return true;
    }

    @Override
    public boolean supportsWifiHotspot(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFtpServer(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return true;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_gts4;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }
}
