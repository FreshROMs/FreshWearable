/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmiwatch3;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class RedmiWatch3Coordinator extends XiaomiCoordinator {
    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BOTH;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Redmi Watch 3 [A-Z0-9]{4}$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmiwatch3;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return 10; // TODO:verify
    }
}
