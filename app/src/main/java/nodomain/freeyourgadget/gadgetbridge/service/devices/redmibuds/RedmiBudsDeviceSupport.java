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
package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.HeadphoneHelper;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class RedmiBudsDeviceSupport extends AbstractHeadphoneDeviceSupport {
    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new RedmiBudsProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new RedmiBudsIOThread(getDevice(), getContext(),
                (RedmiBudsProtocol) getDeviceProtocol(),
                RedmiBudsDeviceSupport.this, getBluetoothAdapter());
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        // headphoneHelper.onSetCallState(callSpec);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
