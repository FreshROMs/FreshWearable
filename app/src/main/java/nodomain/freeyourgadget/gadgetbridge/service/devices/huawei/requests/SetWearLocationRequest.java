/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetWearLocationRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetWearLocationRequest.class);

    public SetWearLocationRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.WearLocationRequest.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsWearLocation(supportProvider.getDevice());
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        String locationString = Application
            .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
            .getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION, "left");
        byte location = (byte) (locationString.equals("left") ? 1 : 0);
        try {
            return new DeviceConfig.WearLocationRequest(paramsProvider, location).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Set Wear Location");
    }
}
