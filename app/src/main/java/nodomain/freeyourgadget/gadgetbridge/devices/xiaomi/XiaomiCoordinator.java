/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.*;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.WearableException;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiUuids;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutSummaryParser;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class XiaomiCoordinator extends AbstractBLEDeviceCoordinator {
    // On plaintext devices, user id is used as auth key - numeric
    private static final Pattern AUTH_KEY_PATTERN = Pattern.compile("^[0-9]+$");

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        final List<ScanFilter> filters = new ArrayList<>();
        for (final UUID uuid : XiaomiUuids.BLE_UUIDS.keySet()) {
            final ParcelUuid service = new ParcelUuid(uuid);
            final ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
            filters.add(filter);
        }
        return filters;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return XiaomiSupport.class;
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        // At this point we don't know if it's encrypted or not, so let's accept both:
        return authKeyBytes.length == 32 || (authKey.startsWith("0x") && authKeyBytes.length == 34)
                || AUTH_KEY_PATTERN.matcher(authKey.trim()).matches();
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final XiaomiInstallHandler handler = new XiaomiInstallHandler(uri, context);
        Log.d("XiaomiCoordinator", "findInstallHandler: " + handler.isValid());
        return handler.isValid() ? handler : null;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws WearableException {
        final Long deviceId = device.getId();

        session.getXiaomiActivitySampleDao().queryBuilder()
                .where(XiaomiActivitySampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, DaoSession session) {
        return new XiaomiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiStressSampleProvider(device, session);
    }

    @Override
    public int[] getStressRanges() {
        // 1-25 = relaxed
        // 26-50 = mild
        // 51-80 = moderate
        // 81-100 = high
        return new int[]{1, 26, 51, 81};
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiTemperatureSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiHeartRateMaxSampleProvider
        return super.getHeartRateMaxSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(GBDevice device, DaoSession session) {
        return new XiaomiHeartRateRestingSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiHeartRateManualSampleProvider
        return super.getHeartRateManualSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiPaiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends RespiratoryRateSample> getRespiratoryRateSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiSleepRespiratoryRateSampleProvider
        return super.getRespiratoryRateSampleProvider(device, session);
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new WorkoutSummaryParser();
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_ALARM_SLOTS, 0);
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device, int position) {
        return true;
    }

    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsCachedAppManagement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsInstalledAppManagement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWatchfaceManagement(GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }

    @Override
    public boolean supportsAppListFetching() {
        return true;
    }

    @Override
    public boolean supportsAppReordering() {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        // TODO it does, and they're persisted - see DailySummaryParser
        return false;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPai() {
        // Vitality Score
        return true;
    }

    @Override
    public int getPaiName() {
        return R.string.pref_vitality_score_title;
    }

    @Override
    public boolean supportsPaiTime() {
        return false;
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        // TODO it does
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getMaximumReminderMessageLength() {
        // TODO does it?
        return 20;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_REMINDER_SLOTS, 0);
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_CANNED_MESSAGES_MAX, 0);
    }

    @Override
    public int getWorldClocksSlotCount() {
        // TODO how many? also, map world clocks
        return 0;
    }

    @Override
    public int getWorldClocksLabelLength() {
        // TODO no labels
        // TODO list of supported timezones
        return 5;
    }

    @Override
    public boolean supportsDisabledWorldClocks() {
        // TODO does it?
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsRemSleep() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificConnectionSettings() {
        final List<Integer> settings = new ArrayList<>();

        if (getConnectionType().equals(ConnectionType.BOTH)) {
            settings.add(R.xml.devicesettings_force_connection_type);
        }

        return ArrayUtils.addAll(
                super.getSupportedDeviceSpecificConnectionSettings(),
                ArrayUtils.toPrimitive(settings.toArray(new Integer[0]))
        );
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings settings = new DeviceSpecificSettings();

        // Quick Access
        settings.addRootScreen(R.xml.devicesettings_xiaomi_quick_access);
        settings.addRootScreen(R.xml.devicesettings_header_apps);

        // "Apps"
        if (supports(device, FEAT_DISPLAY_ITEMS)) {
            settings.addRootScreen(R.xml.devicesettings_xiaomi_apps);
        }

        settings.addRootScreen(DeviceSpecificSettingsScreen.WATCH_SETTINGS);
        String parent = DeviceSpecificSettingsScreen.WATCH_SETTINGS.getKey();

        // Notifications
        settings.addSubScreen(
                DeviceSpecificSettingsScreen.WATCH_SETTINGS,
                DeviceSpecificSettingsScreen.NOTIFICATIONS.getXml()
        );
        DeviceSpecificSettingsScreen notifications = DeviceSpecificSettingsScreen.NOTIFICATIONS;

        // TODO not implemented settings.add(R.xml.devicesettings_vibrationpatterns);
        // TODO not implemented settings.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
        settings.addSubScreen(parent, notifications, R.xml.devicesettings_send_app_notifications);
        settings.addSubScreen(parent, notifications, R.xml.devicesettings_header_sound_vibration);
        if (supports(device, FEAT_SCREEN_ON_ON_NOTIFICATIONS)) {
            settings.addSubScreen(parent, notifications, R.xml.devicesettings_screen_on_on_notifications);
        }
        settings.addSubScreen(parent, notifications, R.xml.devicesettings_autoremove_notifications);
        if (getCannedRepliesSlotCount(device) > 0) {
            settings.addSubScreen(parent, notifications, R.xml.devicesettings_canned_dismisscall_16);
        }
        settings.addSubScreen(parent, notifications, R.xml.devicesettings_transliteration);

        // Contacts
        if (getContactsSlotCount(device) > 0) {
            settings.addSubScreen(parent, R.xml.devicesettings_contacts);
        }

        // Calendar
        if (supportsCalendarEvents()) {
            settings.addSubScreen(
                    parent,
                    DeviceSpecificSettingsScreen.CALENDAR,
                    R.xml.devicesettings_sync_calendar
            );
        }

        // Health
        settings.addSubScreen(parent, R.xml.devicesettings_header_health);

        settings.addSubScreen(parent, R.xml.devicesettings_heartrate_sleep_alerts);
        if (supportsStressMeasurement() && supports(device, FEAT_STRESS)) {
            settings.addSubScreen(parent, R.xml.devicesettings_stress_monitoring);
        }

        if (supportsSpo2(device) && supports(device, FEAT_SPO2)) {
            settings.addSubScreen(parent, R.xml.devicesettings_spo_monitoring);
        }

        if (supports(device, FEAT_INACTIVITY)) {
            settings.addSubScreen(parent, R.xml.devicesettings_inactivity_dnd_no_threshold);
        }

        if (supports(device, FEAT_SLEEP_MODE_SCHEDULE)) {
            settings.addSubScreen(parent, R.xml.devicesettings_sleep_mode_schedule);
        }

        if (device.getDeviceCoordinator().supportsPai()) {
            settings.addSubScreen(parent, R.xml.devicesettings_vitality_score);
        }

        // Workout
        DeviceSpecificSettingsScreen workout = DeviceSpecificSettingsScreen.WORKOUT;
        settings.addSubScreen(parent, workout);
        settings.addSubScreen(
                parent,
                workout,
                R.xml.devicesettings_workout_start_on_phone
        );
        settings.addSubScreen(
                parent,
                workout,
                R.xml.devicesettings_workout_send_gps_to_band
        );

        if (supports(device, FEAT_GOAL_SECONDARY)) {
            settings.addSubScreen(parent, R.xml.devicesettings_goal_secondary);
        } else if (supports(device, FEAT_GOAL_NOTIFICATION)) {
            settings.addSubScreen(parent, R.xml.devicesettings_goal_notification);
        }

        settings.addSubScreen(parent, R.xml.devicesettings_header_system);
        if (supports(device, FEAT_WEAR_MODE)) {
            // TODO we should be able to get this from the band - right now it must be changed
            // at least once from the band itself
            settings.addSubScreen(parent, R.xml.devicesettings_wearmode);
        }

        // Camera Remote
        if (supports(device, FEAT_CAMERA_REMOTE)) {
            settings.addSubScreen(parent, R.xml.devicesettings_camera_remote);
        }

        // Device Actions
        if (supports(device, FEAT_DEVICE_ACTIONS)) {
            settings.addSubScreen(parent, R.xml.devicesettings_device_actions);
        }

        // DND Mode
        // settings.addSubScreen(parent, R.xml.devicesettings_phone_silent_mode);

        // Time
        DeviceSpecificSettingsScreen dateTime = DeviceSpecificSettingsScreen.DATE_TIME;
        settings.addSubScreen(parent, dateTime);
        settings.addSubScreen(
                parent,
                dateTime,
                R.xml.devicesettings_timeformat
        );

        // For devices that support world clocks
        if (getWorldClocksSlotCount() > 0) {
            settings.addSubScreen(
                    parent,
                    dateTime,
                    R.xml.devicesettings_world_clocks
            );
        }

        // Password
        if (supports(device, FEAT_PASSWORD)) {
            settings.addSubScreen(parent, R.xml.devicesettings_password);
        }

        settings.addSubScreen(parent, R.xml.devicesettings_header_developer);

        // Developer
        settings.addSubScreen(
                parent,
                DeviceSpecificSettingsScreen.DEVELOPER,
                R.xml.devicesettings_keep_activity_data_on_device
        );

        return settings;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{R.xml.devicesettings_pairingkey};
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new XiaomiSettingsCustomizer();
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "cs_CZ",
                "da_DK",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "he_IL",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "nb_NO",
                "pl_PL",
                "pt_BR",
                "pt_PT",
                "ro_RO",
                "ru_RU",
                "sv_SE",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NUMBERS_6;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.SMART,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    @Override
    public boolean supportsWidgets(final GBDevice device) {
        return getPrefs(device).getBoolean(XiaomiPreferences.FEAT_WIDGETS, false);
    }

    @Override
    public WidgetManager getWidgetManager(final GBDevice device) {
        return new XiaomiWidgetManager(device);
    }

    protected static Prefs getPrefs(final GBDevice device) {
        return new Prefs(WearableApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
    }

    public boolean supports(final GBDevice device, final String feature) {
        return getPrefs(device).getBoolean(feature, false);
    }

    public boolean checkDecryptionMac() {
        return true;
    }

    /**
     * Whether the device supports alarms. This differs from {@link #getAlarmSlotCount} since that
     * returns the number of alarms dynamically after requesting them, but some devices will crash
     * if we even attempt to request alarms and they do not support them - see
     * <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3766">#3766</a>
     */
    public boolean supportsAlarms() {
        return true;
    }

    @Override
    public GeneralDeviceType getGeneralDeviceType() {
        return GeneralDeviceType.WATCH;
    }
}
