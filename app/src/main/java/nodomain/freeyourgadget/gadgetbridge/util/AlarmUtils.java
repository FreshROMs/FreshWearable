/*  Copyright (C) 2019-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dmitry Markin, Petr Vaněk, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Some utility methods for dealing with Alarms.
 */
public class AlarmUtils {
    /**
     * Creates an auto-generated (not user-configurable), non-recurring alarm. This alarm
     * may not be stored in the database. Some features are not available (e.g. device id, user id).
     * @param index
     * @param smartWakeup
     * @param calendar
     * @return
     */
    public static nodomain.freeyourgadget.gadgetbridge.model.Alarm createSingleShot(int index, boolean smartWakeup, boolean snooze, Calendar calendar) {
        // TODO: add interval setting?
        return new Alarm(-1, -1, index, true, smartWakeup, null, snooze, Alarm.ALARM_ONCE, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false, Application.getContext().getString(R.string.quick_alarm), Application.getContext().getString(R.string.quick_alarm_description));
    }

    /**
     * Creates a default Alarm
     * @param device
     * @param position
     */
    public static Alarm createDefaultAlarm(GBDevice gbDevice, int position) {
        try (DBHandler db = Application.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            return createDefaultAlarm(daoSession, gbDevice, position);
        } catch (Exception e) {
            GB.log("Error accessing database", GB.ERROR, e);
            return null;
        }
    }

    /**
     * Creates a default Alarm
     * @param daoSession
     * @param position
     */
    public static Alarm createDefaultAlarm(DaoSession daoSession, GBDevice gbDevice, int position) {
        Device device = DBHelper.getDevice(gbDevice, daoSession);
        User user = DBHelper.getUser(daoSession);
        return new Alarm(device.getId(), user.getId(), position, false, false, null, false, 0, 6, 30, false, null, null);
    }

    /**
     * Creates  a Calendar object representing the time of the given alarm (not taking the
     * day/date into account.
     * @param alarm
     * @return
     */
    public static Calendar toCalendar(nodomain.freeyourgadget.gadgetbridge.model.Alarm alarm) {
        Calendar result = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        result.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        result.set(Calendar.MINUTE, alarm.getMinute());
        if (now.after(result) && alarm.getRepetition() == Alarm.ALARM_ONCE) {
            //if the alarm is in the past set it to tomorrow
            result.add(Calendar.DATE, 1);
        }
        return result;
    }

    /**
     * Returns a repetition mask suitable for {@link Alarm#repetition}
     * @param mon whether the alarm shall repeat every Monday
     * @param tue whether the alarm shall repeat every Tuesday
     * @param wed whether the alarm shall repeat every Wednesday
     * @param thu whether the alarm shall repeat every Thursday
     * @param fri whether the alarm shall repeat every Friday
     * @param sat whether the alarm shall repeat every Saturday
     * @param sun whether the alarm shall repeat every Sunday
     * @return the created repetition mask
     */
    public static int createRepetitionMask(boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat, boolean sun) {
        int repetitionMask = (mon ? Alarm.ALARM_MON : 0) |
                (tue ? Alarm.ALARM_TUE : 0) |
                (wed ? Alarm.ALARM_WED : 0) |
                (thu ? Alarm.ALARM_THU : 0) |
                (fri ? Alarm.ALARM_FRI : 0) |
                (sat ? Alarm.ALARM_SAT : 0) |
                (sun ? Alarm.ALARM_SUN : 0);
        return repetitionMask;
    }

    /**
     * Just for backward compatibility, do not call in new code.
     * @param gbDevice
     * @return
     * @deprecated use {@link DBHelper#getAlarms(GBDevice)} instead
     */
    @NonNull
    public static List<Alarm> readAlarmsFromPrefs(GBDevice gbDevice) {
        Prefs prefs = Application.getPrefs();
        Set<String> stringAlarms = prefs.getStringSet(MiBandConst.PREF_MIBAND_ALARMS, new HashSet<String>());
        List<Alarm> alarms = new ArrayList<>(stringAlarms.size());

        try (DBHandler db = Application.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(gbDevice, daoSession);
            for (String stringAlarm : stringAlarms) {
                alarms.add(createAlarmFromPreference(stringAlarm, device, user));
            }
            Collections.sort(alarms, AlarmUtils.createComparator());
            return alarms;
        } catch (Exception e) {
            GB.log("Error accessing database", GB.ERROR, e);
            return Collections.emptyList();
        }
    }

    private static Alarm createAlarmFromPreference(String fromPreferences, Device device, User user) {
        String[] tokens = fromPreferences.split(",");
        int index = Integer.parseInt(tokens[0]);
        boolean enabled = Boolean.parseBoolean(tokens[1]);
        boolean smartWakeup = Boolean.parseBoolean(tokens[2]);
        int repetition = Integer.parseInt(tokens[3]);
        int hour = Integer.parseInt(tokens[4]);
        int minute = Integer.parseInt(tokens[5]);

        return new Alarm(device.getId(), user.getId(), index, enabled, smartWakeup, null, false, repetition, hour, minute, false, null, null);
    }

    private static Comparator<Alarm> createComparator() {
        return new Comparator<Alarm>() {

            @Override
            public int compare(Alarm o1, Alarm o2) {
                int p1 = o1.getPosition();
                int p2 = o2.getPosition();

                return Integer.compare(p1, p2);
            }
        };
    }

    public static List<Alarm> mergeOneshotToDeviceAlarms(GBDevice gbDevice, Alarm oneshot, int position) {
        List<Alarm> all_alarms = new ArrayList<>();
        try {
            DBHandler db = Application.acquireDB();
            DaoSession daoSession = db.getDaoSession();
            Device device = DBHelper.getDevice(gbDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            oneshot.setPosition(position);
            oneshot.setDeviceId(device.getId());
            oneshot.setUserId(user.getId());
            daoSession.insertOrReplace(oneshot);
            all_alarms = DBHelper.getAlarms(gbDevice);
            Application.releaseDB();
        } catch (Exception e) {
            GB.log("error storing one shot quick alarm", GB.ERROR, e);
        }
        return all_alarms;
    }
}
