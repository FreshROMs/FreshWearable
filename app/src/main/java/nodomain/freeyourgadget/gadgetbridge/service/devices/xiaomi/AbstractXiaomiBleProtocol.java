package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public abstract class AbstractXiaomiBleProtocol {
    public abstract boolean initializeDevice(final TransactionBuilder builder);

    public abstract void dispose();

    public abstract boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic);

    public abstract void onMtuChanged(BluetoothGatt gatt, int mtu, int status);

    public abstract void onAuthSuccess();

    public abstract void sendCommand(final String taskName, final XiaomiProto.Command command);

    public abstract void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command);

    public abstract void sendDataChunk(final String taskName, final byte[] chunk, @Nullable final XiaomiSendCallback callback);
}
