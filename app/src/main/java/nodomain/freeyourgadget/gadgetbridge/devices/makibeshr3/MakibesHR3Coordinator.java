/*  Copyright (C) 2019-2024 Andreas Shimokawa, Cre3per, Damien Gaignon,
    Daniel Dakhno, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.dao.query.QueryBuilder;
import xyz.tenseventyseven.fresh.AppException;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3.MakibesHR3DeviceSupport;

import static xyz.tenseventyseven.fresh.Application.getContext;


public class MakibesHR3Coordinator extends AbstractBLEDeviceCoordinator {

    public static final int FindPhone_ON = -1;
    public static final int FindPhone_OFF = 0;

    private static final Logger LOG = LoggerFactory.getLogger(MakibesHR3Coordinator.class);


    public static boolean shouldEnableHeadsUpScreen(SharedPreferences sharedPrefs) {
        String liftMode = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_on));

        // Makibes HR3 doesn't support scheduled intervals. Treat it as "on".
        return !liftMode.equals(getContext().getString(R.string.p_off));
    }

    public static boolean shouldEnableLostReminder(SharedPreferences sharedPrefs) {
        String lostReminder = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DISCONNECT_NOTIFICATION, getContext().getString(R.string.p_on));

        // Makibes HR3 doesn't support scheduled intervals. Treat it as "on".
        return !lostReminder.equals(getContext().getString(R.string.p_off));
    }

    /**
     * @param startOut out Only hour/minute are used.
     * @param endOut   out Only hour/minute are used.
     * @return True if quite hours are enabled.
     */
    public static boolean getQuiteHours(SharedPreferences sharedPrefs, Calendar startOut, Calendar endOut) {
        String doNotDisturb = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, getContext().getString(R.string.p_off));

        if (doNotDisturb.equals(getContext().getString(R.string.p_off))) {
            return false;
        } else {
            String start = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, "00:00");
            String end = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, "00:00");

            DateFormat df = new SimpleDateFormat("HH:mm");

            try {
                startOut.setTime(df.parse(start));
                endOut.setTime(df.parse(end));

                return true;
            } catch (Exception e) {
                LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * @return {@link #FindPhone_OFF}, {@link #FindPhone_ON}, or the duration
     */
    public static int getFindPhone(SharedPreferences sharedPrefs) {
        String findPhone = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE, getContext().getString(R.string.p_off));

        if (findPhone.equals(getContext().getString(R.string.p_off))) {
            return FindPhone_OFF;
        } else if (findPhone.equals(getContext().getString(R.string.p_on))) {
            return FindPhone_ON;
        } else { // Duration
            String duration = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION, "0");

            try {
                int iDuration;

                try {
                    iDuration = Integer.valueOf(duration);
                } catch (Exception ex) {
                    LOG.warn(ex.getMessage());
                    iDuration = 60;
                }

                return iDuration;
            } catch (Exception e) {
                LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
                return FindPhone_ON;
            }
        }
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Y808|MAKIBES HR3");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws AppException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getMakibesHR3ActivitySampleDao().queryBuilder();
        qb.where(MakibesHR3ActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MakibesHR3SampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return false;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 8;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Makibes";
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_disconnectnotification,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_transliteration
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MakibesHR3DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_makibes_hr3;
    }
}
