/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;

public class CmfBudsPro2Coordinator extends AbstractEarCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CMF Buds Pro 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothing_cmf_buds_pro_2;
    }

    @Override
    public boolean incrementCounter() {
        return false;
    }

    @Override
    public boolean supportsLightAncAndTransparency() {
        return true;
    }
}
