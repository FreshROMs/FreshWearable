/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip3pro;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import xyz.tenseventyseven.fresh.wearable.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

class AmazfitBip3ProFWInstallHandler extends AbstractMiBandFWInstallHandler {
    AmazfitBip3ProFWInstallHandler(final Uri uri, final Context context) {
        super(uri, context);
    }

    @Override
    protected String getFwUpgradeNotice() {
        return mContext.getString(R.string.fw_upgrade_notice_amazfitbip3pro, helper.getHumanFirmwareVersion());
    }

    @Override
    protected AbstractMiBandFWHelper createHelper(final Uri uri, final Context context) throws IOException {
        return new AmazfitBip3ProFWHelper(uri, context);
    }

    @Override
    protected boolean isSupportedDeviceType(final GBDevice device) {
        return device.getType() == DeviceType.AMAZFITBIP3PRO;
    }
}
