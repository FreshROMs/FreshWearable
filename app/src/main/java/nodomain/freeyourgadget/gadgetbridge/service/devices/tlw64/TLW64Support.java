/*  Copyright (C) 2020-2024 115ek, Arjan Schrijver, Damien Gaignon

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.tlw64;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.tlw64.TLW64Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.tlw64.TLW64SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.TLW64ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static org.apache.commons.lang3.math.NumberUtils.min;

public class TLW64Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TLW64Support.class);

    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic notifyCharacteristic = null;
    private List<TLW64ActivitySample> samples = new ArrayList<>();
    private byte crc = 0;
    private int firstTimestamp = 0;

    public TLW64Support() {
        super(LOG);
        addSupportedService(TLW64Constants.UUID_SERVICE_NO1);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        ctrlCharacteristic = getCharacteristic(TLW64Constants.UUID_CHARACTERISTIC_CONTROL);
        notifyCharacteristic = getCharacteristic(TLW64Constants.UUID_CHARACTERISTIC_NOTIFY);

        builder.setCallback(this);
        builder.notify(notifyCharacteristic, true);

        setTime(builder);
        setDisplaySettings(builder);
        sendSettings(builder);

        builder.write(ctrlCharacteristic, new byte[]{TLW64Constants.CMD_BATTERY});
        builder.write(ctrlCharacteristic, new byte[]{TLW64Constants.CMD_FIRMWARE_VERSION});

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case TLW64Constants.CMD_DISPLAY_SETTINGS:
                LOG.info("Display settings updated");
                return true;
            case TLW64Constants.CMD_FIRMWARE_VERSION:
                // TODO: firmware version reported "RM07JV000404" but original app: "RM07JV000404_15897"
                versionCmd.fwVersion = new String(Arrays.copyOfRange(data, 1, data.length));
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case TLW64Constants.CMD_BATTERY:
                batteryCmd.level = data[1];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + data[1]);
                return true;
            case TLW64Constants.CMD_DATETIME:
                LOG.info("Time is set to: " + (data[1] * 256 + ((int) data[2] & 0xff)) + "-" + data[3] + "-" + data[4] + " " + data[5] + ":" + data[6] + ":" + data[7]);
                return true;
            case TLW64Constants.CMD_USER_DATA:
                LOG.info("User data updated");
                return true;
            case TLW64Constants.CMD_ALARM:
                LOG.info("Alarm updated");
                return true;
            case TLW64Constants.CMD_FACTORY_RESET:
                LOG.info("Factory reset requested");
                return true;
            case TLW64Constants.CMD_FETCH_STEPS:
            case TLW64Constants.CMD_FETCH_SLEEP:
                handleActivityData(data);
                return true;
            case TLW64Constants.CMD_NOTIFICATION:
                LOG.info("Notification is displayed");
                return true;
            case TLW64Constants.CMD_ICON:
                LOG.info("Icon is displayed");
                return true;
            case TLW64Constants.CMD_DEVICE_SETTINGS:
                LOG.info("Device settings updated");
                return true;
            default:
                LOG.warn("Unhandled characteristic change: " + characteristicUUID + " code: " + Arrays.toString(data));
                return true;
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                showNotification(TLW64Constants.NOTIFICATION_SMS, notificationSpec.sender);
                setVibration(1, 1);
                break;
            case WECHAT:
                showIcon(TLW64Constants.ICON_WECHAT);
                setVibration(1, 1);
                break;
            default:
                showIcon(TLW64Constants.ICON_MAIL);
                setVibration(1, 1);
                break;
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("Set alarm");
            boolean anyAlarmEnabled = false;
            for (Alarm alarm : alarms) {
                anyAlarmEnabled |= alarm.getEnabled();
                Calendar calendar = AlarmUtils.toCalendar(alarm);

                int maxAlarms = 3;
                if (alarm.getPosition() >= maxAlarms) {
                    if (alarm.getEnabled()) {
                        GB.toast(getContext(), "Only 3 alarms are supported.", Toast.LENGTH_LONG, GB.WARN);
                    }
                    return;
                }

                byte repetition = 0x00;

                switch (alarm.getRepetition()) {
                    // TODO: case Alarm.ALARM_ONCE is not supported! Need to notify user somehow...
                    case Alarm.ALARM_MON:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_MONDAY;
                    case Alarm.ALARM_TUE:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_TUESDAY;
                    case Alarm.ALARM_WED:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_WEDNESDAY;
                    case Alarm.ALARM_THU:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_THURSDAY;
                    case Alarm.ALARM_FRI:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_FRIDAY;
                    case Alarm.ALARM_SAT:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_SATURDAY;
                    case Alarm.ALARM_SUN:
                        repetition |= TLW64Constants.ARG_SET_ALARM_REMINDER_REPEAT_SUNDAY;
                        break;

                    default:
                        LOG.warn("invalid alarm repetition " + alarm.getRepetition());
                        break;
                }

                byte[] alarmMessage = new byte[]{
                        TLW64Constants.CMD_ALARM,
                        (byte) repetition,
                        (byte) calendar.get(Calendar.HOUR_OF_DAY),
                        (byte) calendar.get(Calendar.MINUTE),
                        (byte) (alarm.getEnabled() ? 2 : 0),    // vibration duration
                        (byte) (alarm.getEnabled() ? 10 : 0),   // vibration count
                        (byte) (alarm.getEnabled() ? 2 : 0),    // unknown
                        (byte) 0x00,
                        (byte) (alarm.getPosition() + 1)
                };
                builder.write(ctrlCharacteristic, alarmMessage);
            }
            builder.queue(getQueue());
            if (anyAlarmEnabled) {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);
            } else {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException ex) {
            GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            showNotification(TLW64Constants.NOTIFICATION_CALL, callSpec.name);
            setVibration(1, 30);
        } else {
            stopNotification();
            setVibration(0, 0);
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        sendFetchCommand(TLW64Constants.CMD_FETCH_STEPS);
    }

    @Override
    public void onReset(int flags) {
        if (flags == GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) {
            try {
                TransactionBuilder builder = performInitialized("factoryReset");
                byte[] msg = new byte[]{
                        TLW64Constants.CMD_FACTORY_RESET,
                };
                builder.write(ctrlCharacteristic, msg);
                builder.queue(getQueue());
            } catch (IOException e) {
                GB.toast(getContext(), "Error during factory reset: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            setVibration(1, 3);
        }
    }

    private void setVibration(int duration, int count) {
        try {
            TransactionBuilder builder = performInitialized("vibrate");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_ALARM,
                    (byte) 0x00,
                    (byte) 0x00,
                    (byte) 0x00,
                    (byte) duration,
                    (byte) count,
                    (byte) 0x07,       // unknown, sniffed by original app
                    (byte) 0x01
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to set vibration", e);
        }
    }

    private void setTime(TransactionBuilder transaction) {
        Calendar c = GregorianCalendar.getInstance();
        byte[] datetimeBytes = new byte[]{
                TLW64Constants.CMD_DATETIME,
                (byte) (c.get(Calendar.YEAR) / 256),
                (byte) (c.get(Calendar.YEAR) % 256),
                (byte) (c.get(Calendar.MONTH) + 1),
                (byte) c.get(Calendar.DAY_OF_MONTH),
                (byte) c.get(Calendar.HOUR_OF_DAY),
                (byte) c.get(Calendar.MINUTE),
                (byte) c.get(Calendar.SECOND)
        };
        transaction.write(ctrlCharacteristic, datetimeBytes);
    }

    private void setDisplaySettings(TransactionBuilder transaction) {
        byte[] displayBytes = new byte[]{
                TLW64Constants.CMD_DISPLAY_SETTINGS,
                (byte) 0x00,   // 1 - display distance in kilometers, 2 - in miles
                (byte) 0x00    // 1 - display 24-hour clock, 2 - for 12-hour with AM/PM
        };
        String units = Application.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));
        if (units.equals(getContext().getString(R.string.p_unit_metric))) {
            displayBytes[1] = 1;
        } else {
            displayBytes[1] = 2;
        }

        String timeformat = Application.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, "auto");
        switch (timeformat) {
            case "24h":
                displayBytes[2] = 1;
                break;
            case "am/pm":
                displayBytes[2] = 2;
                break;
            case "auto":
            default:
                if (DateFormat.is24HourFormat(getContext())) {
                    displayBytes[2] = 1;
                } else {
                    displayBytes[2] = 2;
                }
        }

        transaction.write(ctrlCharacteristic, displayBytes);
        return;
    }

    private void sendSettings(TransactionBuilder builder) {
        // TODO Create custom settings page for changing hardcoded values

        // set user data
        ActivityUser activityUser = new ActivityUser();
        byte[] userBytes = new byte[]{
                TLW64Constants.CMD_USER_DATA,
                (byte) 0x00,  // unknown
                (byte) 0x00,  // step length [cm]
                (byte) 0x00,  // unknown
                (byte) activityUser.getWeightKg(),
                (byte) 0x05,  // screen on time / display timeout
                (byte) 0x00,  // unknown
                (byte) 0x00,  // unknown
                (byte) (activityUser.getStepsGoal() / 256),
                (byte) (activityUser.getStepsGoal() % 256),
                (byte) 0x00,  // raise hand to turn on screen, ON = 1, OFF = 0
                (byte) 0xff,  // unknown
                (byte) 0x00,  // unknown
                (byte) activityUser.getAge(),
                (byte) 0x00,  // gender
                (byte) 0x00,  // lost function, ON = 1, OFF = 0 TODO: find out what this does
                (byte) 0x02   // unknown
        };

        if (Application.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED, false)) {
            userBytes[10] = (byte) 0x01;
        }

        if (activityUser.getGender() == ActivityUser.GENDER_FEMALE) {
            userBytes[14] = 2; // female
            // default and factor from https://livehealthy.chron.com/determine-stride-pedometer-height-weight-4518.html
            if (activityUser.getHeightCm() != 0)
                userBytes[2] = (byte) Math.ceil(activityUser.getHeightCm() * 0.413);
            else
                userBytes[2] = 70; // default
        } else {
            userBytes[14] = 1; // male
            if (activityUser.getHeightCm() != 0)
                userBytes[2] = (byte) Math.ceil(activityUser.getHeightCm() * 0.415);
            else
                userBytes[2] = 78; // default
        }

        builder.write(ctrlCharacteristic, userBytes);

        // device settings
        byte[] deviceBytes = new byte[]{
                TLW64Constants.CMD_DEVICE_SETTINGS,
                (byte) 0x00,   // 1 - turns on inactivity alarm
                (byte) 0x3c,   // unknown, sniffed by original app
                (byte) 0x02,   // unknown, sniffed by original app
                (byte) 0x03,   // unknown, sniffed by original app
                (byte) 0x01,   // unknown, sniffed by original app
                (byte) 0x00    // unknown, sniffed by original app
        };

        if (Application.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE_NOSHED, false)) {
            deviceBytes[1] = (byte) 0x01;
        }

        builder.write(ctrlCharacteristic, deviceBytes);
    }

    private void showIcon(int iconId) {
        try {
            TransactionBuilder builder = performInitialized("showIcon");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_ICON,
                    (byte) iconId
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing icon: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void showNotification(int type, String text) {
        try {
            TransactionBuilder builder = performInitialized("showNotification");
            int length;
            byte[] bytes;
            byte[] msg;

            // send text
            bytes = text.getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = TLW64Constants.CMD_NOTIFICATION;
            msg[1] = TLW64Constants.NOTIFICATION_HEADER;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            // send notification type
            msg = new byte[2];
            msg[0] = TLW64Constants.CMD_NOTIFICATION;
            msg[1] = (byte) type;
            builder.write(ctrlCharacteristic, msg);

            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing notificaton: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void stopNotification() {
        try {
            TransactionBuilder builder = performInitialized("clearNotification");
            byte[] msg = new byte[]{
                    TLW64Constants.CMD_NOTIFICATION,
                    TLW64Constants.NOTIFICATION_STOP
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to stop notification", e);
        }
    }

    private void sendFetchCommand(byte type) {
        samples.clear();
        crc = 0;
        firstTimestamp = 0;
        try {
            TransactionBuilder builder = performInitialized("fetchActivityData");
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
            byte[] msg = new byte[]{
                    type,
                    (byte) 0xfa
            };
            builder.write(ctrlCharacteristic, msg);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void handleActivityData(byte[] data) {
        if (data[1] == (byte) 0xfd) {
            LOG.info("CRC received: {}, calculated: {}", data[2] & 0xff, crc & 0xff);
            if (data[2] != crc) {
                GB.toast(getContext(), "Incorrect CRC. Try fetching data again.", Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
                if (getDevice().isBusy()) {
                    getDevice().unsetBusyTask();
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
            } else if (!samples.isEmpty()) {
                try (DBHandler dbHandler = Application.acquireDB()) {
                    Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                    Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
                    TLW64SampleProvider provider = new TLW64SampleProvider(getDevice(), dbHandler.getDaoSession());
                    for (int i = 0; i < samples.size(); i++) {
                        samples.get(i).setDeviceId(deviceId);
                        samples.get(i).setUserId(userId);
                        if (data[0] == TLW64Constants.CMD_FETCH_STEPS) {
                            samples.get(i).setRawKind(ActivityKind.ACTIVITY.getCode());
                            samples.get(i).setRawIntensity(samples.get(i).getSteps());
                        } else if (data[0] == TLW64Constants.CMD_FETCH_SLEEP) {
                            if (samples.get(i).getRawIntensity() < 7) {
                                samples.get(i).setRawKind(ActivityKind.DEEP_SLEEP.getCode());
                            } else
                                samples.get(i).setRawKind(ActivityKind.LIGHT_SLEEP.getCode());
                        }
                        provider.addGBActivitySample(samples.get(i));
                    }
                    LOG.info("Activity data saved");
                    if (data[0] == TLW64Constants.CMD_FETCH_STEPS) {
                        sendFetchCommand(TLW64Constants.CMD_FETCH_SLEEP);
                    } else {
                        GB.updateTransferNotification(null, "", false, 100, getContext());
                        if (getDevice().isBusy()) {
                            getDevice().unsetBusyTask();
                            getDevice().sendDeviceUpdateIntent(getContext());
                            GB.signalActivityDataFinish(getDevice());
                        }
                    }
                } catch (Exception ex) {
                    GB.toast(getContext(), "Error saving activity data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                    GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
                }
            }
        } else {
            TLW64ActivitySample sample = new TLW64ActivitySample();

            Calendar timestamp = GregorianCalendar.getInstance();
            timestamp.set(Calendar.YEAR, data[1] * 256 + (data[2] & 0xff));
            timestamp.set(Calendar.MONTH, (data[3] - 1) & 0xff);
            timestamp.set(Calendar.DAY_OF_MONTH, data[4] & 0xff);
            timestamp.set(Calendar.HOUR_OF_DAY, data[5] & 0xff);
            timestamp.set(Calendar.SECOND, 0);

            int startProgress = 0;
            if (data[0] == TLW64Constants.CMD_FETCH_STEPS) {
                timestamp.set(Calendar.MINUTE, 0);
                sample.setSteps(data[6] * 256 + (data[7] & 0xff));
                //noinspection lossy-conversions
                crc ^= (data[6] ^ data[7]);
            } else if (data[0] == TLW64Constants.CMD_FETCH_SLEEP) {
                timestamp.set(Calendar.MINUTE, data[6] & 0xff);
                sample.setRawIntensity(data[7] * 256 + (data[8] & 0xff));
                //noinspection lossy-conversions
                crc ^= (data[7] ^ data[8]);
                startProgress = 33;
            }

            sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000L));
            samples.add(sample);

            if (firstTimestamp == 0)
                firstTimestamp = sample.getTimestamp();
            int progress = startProgress + 33 * (sample.getTimestamp() - firstTimestamp) /
                    ((int) (Calendar.getInstance().getTimeInMillis() / 1000L) - firstTimestamp);
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, progress, getContext());
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
