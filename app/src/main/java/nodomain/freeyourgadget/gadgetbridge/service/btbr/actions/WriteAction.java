/*  Copyright (C) 2022-2024 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr.actions;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.BtBRAction;

/**
 * Invokes a write operation on a given socket.
 * The result status will be made available asynchronously through the
 * {@link SocketCallback}
 */
public class WriteAction extends BtBRAction {
    private static final Logger LOG = LoggerFactory.getLogger(WriteAction.class);

    private final byte[] value;
    private OutputStream mOutputStream = null;

    public WriteAction(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean run(BluetoothSocket socket) {
        try {
            mOutputStream = socket.getOutputStream();
            if (mOutputStream == null) {
                LOG.error("mOutStream is null");
                return false;
            }
            return writeValue(value);
        } catch (IOException e) {
            LOG.error("Can not get the output stream");
        }
        return false;
    }

    protected boolean writeValue(byte[] value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("writing to socket: " + Logging.formatBytes(value));
        }
        try {
            mOutputStream.write(value);
            mOutputStream.flush();
            return true;
        } catch (IOException e) {
            LOG.error("Error writing to socket: ", e);
        }
        return false;
    }

    protected final byte[] getValue() {
        return value;
    }

    @Override
    public boolean expectsResult() {
        return true;
    }
}
