/*  Copyright (C) 2021-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Message;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyHeadphonesIoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SonyHeadphonesIoThread.class);

    private final SonyHeadphonesProtocol mProtocol;

    private final UUID btrfcommUuidV1 = UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA");
    private final UUID btrfcommUuidV2 = UUID.fromString("956C7B26-D49A-4BA8-B03F-B17D393CB6E2");

    // Track whether we got the first init reply
    private final Handler handler = new Handler();
    private int initRetries = 0;

    /**
     * Sometimes the headphones will ignore the first init request, so we retry a few times
     * TODO: Implement this in a more elegant way. Ideally, we should retry every command for which we didn't get an ACK.
     */
    private final Runnable initSendRunnable = new Runnable() {
        public void run() {
            // If we still haven't got any reply, re-send the init
            if (!mProtocol.hasProtocolImplementation()) {
                if (initRetries++ < 2) {
                    LOG.warn("Init retry {}", initRetries);

                    mProtocol.decreasePendingAcks();
                    write(mProtocol.encodeInit());
                    scheduleInitRetry();
                } else {
                    LOG.error("Failed to start headphones init after {} tries", initRetries);
                    quit();
                }
            }
        }
    };

    public SonyHeadphonesIoThread(final GBDevice gbDevice,
                                  final Context context,
                                  final SonyHeadphonesProtocol protocol,
                                  final SonyHeadphonesSupport support,
                                  final BluetoothAdapter btAdapter) {
        super(gbDevice, context, protocol, support, btAdapter);
        mProtocol = protocol;
    }

    @Override
    protected void initialize() {
        write(mProtocol.encodeInit());
        scheduleInitRetry();
        setUpdateState(GBDevice.State.INITIALIZING);
    }

    @Override
    public void quit() {
        handler.removeCallbacksAndMessages(null);
        super.quit();
    }

    @Override
    public synchronized void write(final byte[] bytes) {
        // Log the human-readable message, for debugging
        LOG.info("Writing {}", Message.fromBytes(bytes));

        super.write(bytes);
    }

    @Override
    protected byte[] parseIncoming(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream msgStream = new ByteArrayOutputStream();
        final byte[] incoming = new byte[1];

        do {
            inputStream.read(incoming);

            if (incoming[0] == Message.MESSAGE_HEADER) {
                msgStream.reset();
            }

            msgStream.write(incoming);
        } while (incoming[0] != Message.MESSAGE_TRAILER);

        LOG.trace("Raw message: {}", GB.hexdump(msgStream.toByteArray()));

        return msgStream.toByteArray();
    }

    @NonNull
    @Override
    protected UUID getUuidToConnect(@NonNull final ParcelUuid[] uuids) {
        boolean hasV1 = false;
        boolean hasV2 = false;
        boolean preferV2 = getCoordinator().preferServiceV2();
        for (final ParcelUuid uuid : uuids) {
            if (uuid.getUuid().equals(btrfcommUuidV1)) {
                LOG.info("Found Sony UUID V1");
                hasV1 = true;
            } else if (uuid.getUuid().equals(btrfcommUuidV2)) {
                LOG.info("Found Sony UUID V2");
                hasV2 = true;
            }
        }

        if (hasV2) {
            LOG.info("Using Sony UUID V2");
            return btrfcommUuidV2;
        } else if (hasV1) {
            LOG.info("Using Sony UUID V1");
            return btrfcommUuidV1;
        }

        LOG.warn("Failed to find a known Sony UUID, will fallback to {}", (preferV2 ? "V2" : "V1"));

        return preferV2 ? btrfcommUuidV2 : btrfcommUuidV1;
    }

    private void scheduleInitRetry() {
        LOG.info("Scheduling init retry");

        handler.postDelayed(initSendRunnable, 1250);
    }

    private SonyHeadphonesCoordinator getCoordinator() {
        return (SonyHeadphonesCoordinator) getDevice().getDeviceCoordinator();
    }
}
