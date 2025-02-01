package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.DATA_TYPE_PLAIN;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.OPCODE_READ;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_PREAMBLE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_ACK;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_DATA;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_SESSION_CONFIG;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.PlainAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiBleProtocolV2 extends AbstractXiaomiBleProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBleProtocolV2.class);

    private final XiaomiSupport xiaomiSupport;
    private final AbstractBTLEDeviceSupport commsSupport;
    private final Context mContext;
    private final GBDevice mGbDevice;

    private BluetoothGattCharacteristic btCharacteristicRead;
    private BluetoothGattCharacteristic btCharacteristicWrite;

    private final AtomicInteger packetSequenceCounter = new AtomicInteger(0);
    private int maxWriteSize = 244; // MTU of 247 - 3 bytes for the ATT overhead

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Map<XiaomiChannelHandler.Channel, XiaomiChannelHandler> mChannelHandlers = new HashMap<>();
    private final Handler mVersionResponseTimeoutHandler = new Handler(Looper.getMainLooper());

    public XiaomiBleProtocolV2(final XiaomiBleSupport xiaomiBleSupport) {
        this.xiaomiSupport = xiaomiBleSupport.getXiaomiSupport();
        this.commsSupport = xiaomiBleSupport.getCommsSupport();
        this.mContext = xiaomiSupport.getContext();
        this.mGbDevice = xiaomiSupport.getDevice();

        mChannelHandlers.put(XiaomiChannelHandler.Channel.Version, this::handleVersionPacket);
        mChannelHandlers.put(XiaomiChannelHandler.Channel.ProtobufCommand, this.xiaomiSupport::handleCommandBytes);
        mChannelHandlers.put(XiaomiChannelHandler.Channel.Activity, this.xiaomiSupport.getHealthService().getActivityFetcher()::addChunk);
    }

    @Override
    public boolean initializeDevice(final TransactionBuilder builder) {
        btCharacteristicRead = commsSupport.getCharacteristic(XiaomiUuids.BLE_V2_CHARACTERISTIC_RX_UUID);
        btCharacteristicWrite = commsSupport.getCharacteristic(XiaomiUuids.BLE_V2_CHARACTERISTIC_TX_UUID);

        if (btCharacteristicRead == null || btCharacteristicWrite == null) {
            return false;
        }

        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (mGbDevice.getFirmwareVersion() == null) {
            mGbDevice.setFirmwareVersion(xiaomiSupport.getCachedFirmwareVersion() != null ?
                    xiaomiSupport.getCachedFirmwareVersion() :
                    "N/A");
        }

        // request highest possible MTU; device should response with the highest supported MTU anyway
        builder.requestMtu(512);
        builder.add(new SetDeviceStateAction(mGbDevice, GBDevice.State.INITIALIZING, mContext));
        builder.notify(btCharacteristicRead, true);
        builder.add(new SetDeviceStateAction(mGbDevice, GBDevice.State.AUTHENTICATING, mContext));

        writeChunks(builder, XiaomiSppPacketV1.newBuilder()
                .channel(XiaomiChannelHandler.Channel.Version)
                .needsResponse(true)
                .opCode(OPCODE_READ)
                .dataType(DATA_TYPE_PLAIN)
                .frameSerial(0)
                .build()
                .encode(null, null));
        builder.add(new PlainAction() {
            @Override
            public boolean run(final BluetoothGatt gatt) {
                mVersionResponseTimeoutHandler.postDelayed(new VersionTimeoutRunnable(), 5000L);
                return true;
            }
        });

        return true;
    }

    @Override
    public void dispose() {
        mVersionResponseTimeoutHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        final byte[] value = characteristic.getValue();

        if (btCharacteristicRead.getUuid().equals(characteristicUUID)) {
            try {
                buffer.write(value);
            } catch (IOException ex) {
                LOG.error("Exception while writing buffer: ", ex);
            }

            processBuffer();
            return true;
        }

        return false;
    }

    private void skipBuffer(int newStart) {
        final byte[] bufferState = buffer.toByteArray();
        buffer.reset();

        if (newStart < 0) {
            newStart = bufferState.length;
        }

        if (newStart >= bufferState.length) {
            return;
        }

        buffer.write(bufferState, newStart, bufferState.length - newStart);
    }

    private void processBuffer() {
        boolean shouldProcess = true;
        while (shouldProcess) {
            final byte[] bufferState = buffer.toByteArray();
            final AbstractXiaomiSppProtocol.ParseResult parseResult = processPacket(bufferState);
            LOG.debug("processBuffer(): protocol.processPacket() returned status {}", parseResult.status);
            int skipBytes;

            switch (parseResult.status) {
                case Incomplete:
                    skipBytes = 0;
                    shouldProcess = false;
                    break;
                case Complete:
                    skipBytes = parseResult.packetSize;
                    break;
                case Invalid:
                    skipBytes = findNextPacketOffset(bufferState);
                    if (skipBytes < 0) {
                        skipBytes = bufferState.length;
                    }
                    break;
                default:
                    throw new IllegalStateException(String.format("Unhandled parse state %s", parseResult.status));
            }

            if (skipBytes > 0) {
                LOG.debug("processBuffer(): skipping {} bytes for state {}", skipBytes, parseResult.status);
                skipBuffer(skipBytes);
            }
        }
    }

    protected void onPacketReceived(final XiaomiChannelHandler.Channel channel, final byte[] payload) {
        final XiaomiChannelHandler handler = mChannelHandlers.get(channel);
        if (handler != null) {
            handler.handle(payload);
        } else {
            LOG.warn("Unhandled SppPacket on channel {}", channel);
        }
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        this.maxWriteSize = mtu - 3;
    }

    @Override
    public void onAuthSuccess() {

    }

    @Override
    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        if (this.btCharacteristicWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("btCharacteristicWrite is null!");
            return;
        }

        try {
            final TransactionBuilder builder = this.commsSupport.createTransactionBuilder("send " + taskName);
            sendCommand(builder, command);
            builder.queue(this.commsSupport.getQueue());
        } catch (final Exception ex) {
            LOG.error("Caught unexpected exception while sending command, device may not have been informed!", ex);
        }
    }

    @Override
    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        if (this.btCharacteristicWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("btCharacteristicWrite is null!");
            return;
        }

        LOG.debug("sendCommand(): encoded command for task '{}': {}", builder.getTransaction().getTaskName(), GB.hexdump(command.toByteArray()));
        if (command.getType() == XiaomiAuthService.COMMAND_TYPE) {
            writeChunks(builder, encodePacket(XiaomiChannelHandler.Channel.Authentication, command.toByteArray()));
        } else {
            writeChunks(builder, encodePacket(XiaomiChannelHandler.Channel.ProtobufCommand, command.toByteArray()));
        }
        // do not queue here, that's the job of the caller
    }

    @Override
    public void sendDataChunk(final String taskName, final byte[] chunk, @Nullable final XiaomiSendCallback callback) {
        LOG.debug("sendDataChunk(): encoded data chunk for task '{}': {}", taskName, GB.hexdump(chunk));
        final TransactionBuilder builder = this.commsSupport.createTransactionBuilder("send " + taskName);
        writeChunks(builder, encodePacket(XiaomiChannelHandler.Channel.Data, chunk));
        builder.queue(commsSupport.getQueue());

        if (callback != null) {
            // callback puts a SetProgressAction onto the queue
            callback.onSend();
        }
    }

    private void handleVersionPacket(final byte[] payloadBytes) {
        // remove timeout actions from handler
        mVersionResponseTimeoutHandler.removeCallbacksAndMessages(null);

        if (payloadBytes != null && payloadBytes.length > 0) {
            LOG.debug("Received SPP protocol version: {}", GB.hexdump(payloadBytes));

            // show in details
            final GBDeviceEventUpdateDeviceInfo event = new GBDeviceEventUpdateDeviceInfo("SPP_PROTOCOL: ", GB.hexdump(payloadBytes));
            xiaomiSupport.evaluateGBDeviceEvent(event);

            // TODO handle different protocol versions
            // Right now, we expect 3
        }

        xiaomiSupport.getAuthService().startEncryptedHandshake();
    }

    private void sendAck(final int sequenceNumber) {
        final TransactionBuilder builder = commsSupport.createTransactionBuilder(String.format(Locale.ROOT, "send ack for %d", sequenceNumber));
        writeChunks(builder, new XiaomiSppPacketV2.AckPacket.Builder()
                .setSequenceNumber(sequenceNumber)
                .build()
                .encode(null));
        builder.queue(commsSupport.getQueue());
    }

    public int findNextPacketOffset(byte[] buffer) {
        for (int i = 1; i < buffer.length; i++) {
            if (buffer[i] == PACKET_PREAMBLE[0])
                return i;
        }

        return -1;
    }

    public AbstractXiaomiSppProtocol.ParseResult processPacket(byte[] rxBuf) {
        if (rxBuf.length < 8) {
            LOG.debug("processPacket(): not enough bytes in buffer to process packet (got {} of required {} bytes)",
                    rxBuf.length,
                    8);
            return new AbstractXiaomiSppProtocol.ParseResult(AbstractXiaomiSppProtocol.ParseResult.Status.Incomplete);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(rxBuf).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] headerMagic = new byte[PACKET_PREAMBLE.length];
        buffer.get(headerMagic);

        if (!Arrays.equals(PACKET_PREAMBLE, headerMagic)) {
            LOG.warn("processPacket(): invalid header magic (expected {}, got {})",
                    GB.hexdump(PACKET_PREAMBLE),
                    GB.hexdump(headerMagic));
            return new AbstractXiaomiSppProtocol.ParseResult(AbstractXiaomiSppProtocol.ParseResult.Status.Invalid);
        }

        buffer.get(); // flags and packet type
        buffer.get(); // packet sequence number
        final int packetSize = 8 + (buffer.getShort() & 0xffff);
        buffer.getShort(); // checksum

        if (rxBuf.length < packetSize) {
            LOG.debug("processPacket(): missing {} bytes (got {}/{} bytes)",
                    packetSize - rxBuf.length,
                    rxBuf.length,
                    packetSize);
            return new AbstractXiaomiSppProtocol.ParseResult(AbstractXiaomiSppProtocol.ParseResult.Status.Incomplete);
        }

        final XiaomiSppPacketV2 decodedPacket = XiaomiSppPacketV2.decode(rxBuf);
        if (decodedPacket != null) {
            switch (decodedPacket.getPacketType()) {
                case PACKET_TYPE_SESSION_CONFIG:
                    // TODO handle device's session config
                    LOG.info("Received session config, opcode={}", ((XiaomiSppPacketV2.SessionConfigPacket) decodedPacket).getOpCode());
                    xiaomiSupport.getAuthService().startEncryptedHandshake();
                    break;
                case PACKET_TYPE_DATA:
                    XiaomiSppPacketV2.DataPacket dataPacket = (XiaomiSppPacketV2.DataPacket) decodedPacket;
                    try {
                        onPacketReceived(dataPacket.getChannel(), dataPacket.getPayloadBytes(xiaomiSupport.getAuthService()));
                    } catch (final Exception ex) {
                        LOG.error("Exception while handling received packet", ex);
                    }
                    // TODO: only directly ack protobuf packets, bulk ack others
                    sendAck(decodedPacket.getSequenceNumber());
                    break;
                case PACKET_TYPE_ACK:
                    LOG.debug("receive ack for packet {}", decodedPacket.getSequenceNumber());
                    break;
                default:
                    LOG.warn("Unhandled packet with type {} (decoded type {})", decodedPacket.getPacketType(), decodedPacket.getClass().getSimpleName());
                    break;
            }
        }

        return new AbstractXiaomiSppProtocol.ParseResult(AbstractXiaomiSppProtocol.ParseResult.Status.Complete, packetSize);
    }

    public byte[] encodePacket(final XiaomiChannelHandler.Channel channel, final byte[] payloadBytes) {
        return XiaomiSppPacketV2.newDataPacketBuilder()
                .setChannel(channel)
                .setSequenceNumber(packetSequenceCounter.getAndIncrement())
                .setOpCode(XiaomiSppPacketV2.DataPacket.getOpCodeForChannel(channel))
                .setPayload(payloadBytes)
                .build()
                .encode(xiaomiSupport.getAuthService());
    }

    private void writeChunks(final TransactionBuilder builder, final byte[] value) {
        builder.writeChunkedData(btCharacteristicWrite, value, maxWriteSize);
    }

    class VersionTimeoutRunnable implements Runnable {
        @Override
        public void run() {
            LOG.warn("SPP protocol version request timed out");
            XiaomiBleProtocolV2.this.handleVersionPacket(new byte[0]);
        }
    }
}
