/*  Copyright (C) 2018-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Sebastian Kranz, Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.id115;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class ID115Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ID115Support.class);

    public BluetoothGattCharacteristic normalWriteCharacteristic = null;
    public BluetoothGattCharacteristic healthWriteCharacteristic = null;

    public ID115Support() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(ID115Constants.UUID_SERVICE_ID115);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        normalWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_NORMAL);
        healthWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_HEALTH);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        setTime(builder)
                .setWrist(builder)
                .setScreenOrientation(builder)
                .setGoal(builder)
                .setInitialized(builder);

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        try {
            new SendNotificationOperation(this, notificationSpec).perform();
        } catch (IOException ex) {
            LOG.error("Unable to send ID115 notification", ex);
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("time");
            setTime(builder);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.warn("Unable to send current time", e);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            try {
                new SendNotificationOperation(this, callSpec).perform();
            } catch (IOException ex) {
                LOG.error("Unable to send ID115 notification", ex);
            }
        } else {
            sendStopCallNotification();
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            new FetchActivityOperation(this).perform();
        } catch (IOException ex) {
            LOG.error("Unable to fetch ID115 activity data", ex);
        }
    }

    @Override
    public void onReset(int flags) {
        try {
            getQueue().clear();

            TransactionBuilder builder = performInitialized("reboot");
            builder.write(normalWriteCharacteristic, new byte[] {
                    ID115Constants.CMD_ID_DEVICE_RESTART, ID115Constants.CMD_KEY_REBOOT
            });
            builder.queue(getQueue());
        } catch(Exception e) {
        }
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    ID115Support setTime(TransactionBuilder builder) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());

        int day = c.get(Calendar.DAY_OF_WEEK);

        byte dayOfWeek;
        if (day == Calendar.SUNDAY) {
            dayOfWeek = 6;
        } else {
            dayOfWeek = (byte)(day - 2);
        }

        int year = c.get(Calendar.YEAR);
        builder.write(normalWriteCharacteristic, new byte[] {
                ID115Constants.CMD_ID_SETTINGS, ID115Constants.CMD_KEY_SET_TIME,
                (byte)(year & 0xff),
                (byte)(year >> 8),
                (byte)(1 + c.get(Calendar.MONTH)),
                (byte)c.get(Calendar.DAY_OF_MONTH),
                (byte)c.get(Calendar.HOUR_OF_DAY),
                (byte)c.get(Calendar.MINUTE),
                (byte)c.get(Calendar.SECOND),
                dayOfWeek
        });
        return this;
    }

    ID115Support setWrist(TransactionBuilder builder) {
        String value = Application.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION,
                "left");

        byte wrist;
        if ("left".equals(value)) {
            wrist = ID115Constants.CMD_ARG_LEFT;
        } else {
            wrist = ID115Constants.CMD_ARG_RIGHT;
        }

        builder.write(normalWriteCharacteristic, new byte[] {
                ID115Constants.CMD_ID_SETTINGS, ID115Constants.CMD_KEY_SET_HAND,
                wrist
        });
        return this;
    }

    ID115Support setScreenOrientation(TransactionBuilder builder) {
        String value = Application.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_SCREEN_ORIENTATION,
                "horizontal");

        byte orientation;
        if ("horizontal".equals(value)) {
            orientation = ID115Constants.CMD_ARG_HORIZONTAL;
        } else {
            orientation = ID115Constants.CMD_ARG_VERTICAL;
        }

        builder.write(normalWriteCharacteristic, new byte[] {
                ID115Constants.CMD_ID_SETTINGS, ID115Constants.CMD_KEY_SET_DISPLAY_MODE,
                orientation
        });
        return this;
    }

    private ID115Support setGoal(TransactionBuilder transaction) {
        ActivityUser activityUser = new ActivityUser();
        int value = activityUser.getStepsGoal();

        transaction.write(normalWriteCharacteristic, new byte[]{
                ID115Constants.CMD_ID_SETTINGS,
                ID115Constants.CMD_KEY_SET_GOAL,
                0,
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
                0, 0
        });
        return this;
    }

    void sendStopCallNotification() {
        try {
            TransactionBuilder builder = performInitialized("stop_call_notification");
            builder.write(normalWriteCharacteristic, new byte[] {
                    ID115Constants.CMD_ID_NOTIFY,
                    ID115Constants.CMD_KEY_NOTIFY_STOP,
                    1
            });
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.warn("Unable to stop call notification", e);
        }
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
