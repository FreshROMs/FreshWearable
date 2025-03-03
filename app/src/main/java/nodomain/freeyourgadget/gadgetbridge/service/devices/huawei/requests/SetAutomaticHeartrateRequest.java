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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetAutomaticHeartrateRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetAutomaticHeartrateRequest.class);

    public SetAutomaticHeartrateRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.EnableAutomaticHeartrate.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        boolean automaticHeartRateEnabled = Application
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE, false);
        boolean realtimeHeartRateEnabled = Application
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getBoolean(HuaweiConstants.PREF_HUAWEI_HEART_RATE_REALTIME_MODE, false);
        if (automaticHeartRateEnabled)
            LOG.info("Attempting to enable automatic heart rate");
        else
            LOG.info("Attempting to disable automatic heart rate");
        if(realtimeHeartRateEnabled && this.supportProvider.getHuaweiCoordinator().supportsRealtimeHeartRate()) {
            try {
                return new FitnessData.EnableRealtimeHeartRate.Request(paramsProvider, automaticHeartRateEnabled).serialize();
            } catch (HuaweiPacket.CryptoException e) {
                throw new RequestCreationException(e);
            }
        } else {
            try {
                return new FitnessData.EnableAutomaticHeartrate.Request(paramsProvider, automaticHeartRateEnabled).serialize();
            } catch (HuaweiPacket.CryptoException e) {
                throw new RequestCreationException(e);
            }
        }
    }
}
