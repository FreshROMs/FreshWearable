package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_CAMERA_REMOTE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_INACTIVITY;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_PASSWORD;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_SCREEN_ON_ON_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_SLEEP_MODE_SCHEDULE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.FEAT_WIDGETS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeslSwitchPreferenceScreen;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.widgets.WidgetScreensListActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceShortcut;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableSettingCoordinator;

public class XiaomiSettingsCoordinator extends WearableSettingCoordinator {
    private XiaomiCoordinator coordinator;
    private GBDevice device;

    public XiaomiSettingsCoordinator(XiaomiCoordinator coordinator, GBDevice device) {
        this.coordinator = coordinator;
        this.device = device;
    }

    private static final String WIDGETS_SHORTCUT_KEY = "widgets";
    private static final String DISPLAY_ITEMS_SHORTCUT_KEY = "display_items";
    private static final String ALARMS_SHORTCUT_KEY = "alarms";
    private static final String WATCHFACE_SHORTCUT_KEY = "watchface";

    @Override
    public void onShortcutClicked(Context context, GBDevice device, String key) {
        switch (key) {
            case ALARMS_SHORTCUT_KEY:
                Intent alarms = new Intent(context, ConfigureAlarms.class);
                alarms.putExtra(GBDevice.EXTRA_DEVICE, device);
                context.startActivity(alarms);
                break;
            case WATCHFACE_SHORTCUT_KEY:
                Class<? extends Activity> appsManagementActivity = coordinator.getAppsManagementActivity();
                if (appsManagementActivity != null) {
                    Intent intent = new Intent(context, appsManagementActivity);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    context.startActivity(intent);
                }
                break;
            case WIDGETS_SHORTCUT_KEY:
                final Intent widgets = new Intent(context, WidgetScreensListActivity.class);
                widgets.putExtra(GBDevice.EXTRA_DEVICE, device);
                context.startActivity(widgets);
                break;
            case DISPLAY_ITEMS_SHORTCUT_KEY:
                DeviceSetting settings = getDisplayItemsSettings();
                Intent notificationsIntent = new Intent(context, PreferenceScreenActivity.class);
                notificationsIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                notificationsIntent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, false);
                notificationsIntent.putExtra(DeviceSetting.EXTRA_SETTING, settings);
                context.startActivity(notificationsIntent);
                break;
        }
    }

    private DeviceSetting getDisplayItemsSettings() {
        DeviceSetting setting = DeviceSetting.screen(
                "screen_display_items",
                R.string.wear_device_watchface_apps_screen,
                0,
                0
        );
        setting.settings = new ArrayList<>();

        return setting;
    }

    @Override
    public List<DeviceShortcut> getShortcuts() {
        List<DeviceShortcut> shortcuts = new ArrayList<>();
        shortcuts.add(new DeviceShortcut(WATCHFACE_SHORTCUT_KEY, R.string.wear_device_watchface_settings, R.drawable.home_tab_watchface));

        if (coordinator.supports(device, FEAT_DISPLAY_ITEMS)) {
            shortcuts.add(new DeviceShortcut(DISPLAY_ITEMS_SHORTCUT_KEY, R.string.wear_device_watchface_apps_screen, R.drawable.home_tab_apps));
        }

        if (coordinator.supports(device, FEAT_WIDGETS)) {
            shortcuts.add(new DeviceShortcut(WIDGETS_SHORTCUT_KEY, R.string.menuitem_widgets, R.drawable.home_tab_tile));
        }

        shortcuts.add(new DeviceShortcut(ALARMS_SHORTCUT_KEY, R.string.wear_device_watchface_alarms, R.drawable.home_tab_quick_settings));

        return shortcuts;
    }

    @Override
    public List<DeviceSetting> getSettings() {
        List<DeviceSetting> settings = new ArrayList<>();
        settings.add(getNotificationSettings());

        DeviceSetting setting = getContactsSettings();
        if (setting != null) {
            settings.add(setting);
        }

        setting = getCalendarSettings();
        if (setting != null) {
            settings.add(setting);
        }

        settings.add(DeviceSetting.divider());
        settings.add(getHealthSettings());

        List<DeviceSetting> miscSettings = getMiscSettings();
        if (!miscSettings.isEmpty()) {
            settings.add(DeviceSetting.divider());
            settings.addAll(miscSettings);
        }

        return settings;
    }

    @Override
    public List<DeviceSetting> getDeveloperOptions() {
        return super.getDeveloperOptions();
    }

    @Override
    public void onSettingChanged(GBDevice device, PreferenceScreen preferenceScreen, Preference preference, String key) {
        super.onSettingChanged(device, preferenceScreen, preference, key);
        updateSedentaryPreference(preferenceScreen);
    }

    @Override
    public void onSettingChanged(GBDevice device, SharedPreferences sharedPreferences, String key) {
        super.onSettingChanged(device, sharedPreferences, key);
        if (Objects.equals(key, "screen_on_on_notifications_dropdown")) {
            Log.d("XiaomiSettingsCoordinator", "screen_on_on_notifications_dropdown changed");
            String value = sharedPreferences.getString(key, "false");
            sharedPreferences.edit().putBoolean("notifications_ignore_when_screen_on", "true".equals(value)).apply();
        }
    }

    @Override
    public void onSettingsCreated(PreferenceScreen preferenceScreen) {
        super.onSettingsCreated(preferenceScreen);
        updateSedentaryPreference(preferenceScreen);
        updateDeviceInformation(preferenceScreen);
    }

    private void updateDeviceInformation(PreferenceScreen preferenceScreen) {
        if (!Objects.equals(preferenceScreen.getKey(), "pref_screen_about_device")) {
            return;
        }

        Preference firmwareVersion = preferenceScreen.findPreference("firmware_version");
        if (firmwareVersion != null) {
            firmwareVersion.setSummary(device.getFirmwareVersion());
        }
    }

    @Override
    public void onSettingsResumed(PreferenceScreen preferenceScreen) {
        super.onSettingsResumed(preferenceScreen);
        updateSedentaryPreference(preferenceScreen);
    }

    private void updateSedentaryPreference(PreferenceScreen preferenceScreen) {
        if (!Objects.equals(preferenceScreen.getKey(), "pref_health")) return;

        Preference sedentaryScreen = preferenceScreen.findPreference("inactivity_warnings_enable");
        if (sedentaryScreen instanceof SeslSwitchPreferenceScreen) {
            SeslSwitchPreferenceScreen switchScreen = (SeslSwitchPreferenceScreen) sedentaryScreen;
            SharedPreferences prefs = switchScreen.getSharedPreferences();
            if (prefs == null) return;
            String start = prefs.getString("inactivity_warnings_start", "06:00");
            String end = prefs.getString("inactivity_warnings_end", "22:00");
            start = formatTimeToLocale(switchScreen.getContext(), start);
            end = formatTimeToLocale(switchScreen.getContext(), end);

            switchScreen.setTitle(start + " - " + end);
            switchScreen.seslSetSummaryColor(switchScreen.getContext().getColor(R.color.wearable_accent_primary));
            switchScreen.setSummary(switchScreen.isChecked() ? R.string.function_enabled : R.string.pref_button_action_disabled);
        }
    }

    private String formatTimeToLocale(Context context, String time) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return android.text.format.DateFormat.getTimeFormat(context).format(new Date(0, 0, 0, hour, minute));
    }

    @Override
    public boolean allowPreferenceChange(PreferenceScreen screen, Preference preference, String newValue) {
        return super.allowPreferenceChange(screen, preference, newValue);
    }

    private List<DeviceSetting> getMiscSettings() {
        List<DeviceSetting> settings = new ArrayList<>();
        DeviceSetting setting;
        if (coordinator.supports(device, FEAT_CAMERA_REMOTE)) {
            setting = DeviceSetting.screen(
                    "screen_camera_remots",
                    R.string.wear_device_camera_remote_settings,
                    R.string.wear_device_camera_remote_settings_summary,
                    R.drawable.wear_ic_settings_camera_remote
            );
            setting.screenSummary = R.string.wear_device_camera_remote_settings_screen_summary;
            setting.screenHasSwitchBar = true;
            setting.screenSwitchBarKey = "camera_remote";
            setting.valueAsSummary = false;
            setting.settings = new ArrayList<>();

            settings.add(setting);
        }

        setting = DeviceSetting.screen(
                "screen_time_and_date",
                R.string.wear_device_time_and_date_settings,
                R.string.wear_device_time_and_date_settings_summary,
                R.drawable.wear_ic_settings_time_and_date
        );
        setting.settings = new ArrayList<>();

        DeviceSetting preference = DeviceSetting.dropdown(
                "time_format",
                R.string.pref_title_timeformat,
                0,
                "auto",
                R.array.pref_timeformat_entries,
                R.array.pref_timeformat_values
        );
        preference.valueAsSummary = true;
        setting.settings.add(preference);
        if (coordinator.getWorldClocksSlotCount() > 0) {
            setting.settings.add(DeviceSetting.divider());
            preference = DeviceSetting.screen(
                    "world_clocks",
                    R.string.pref_world_clocks_title,
                    R.string.pref_world_clocks_summary,
                    0,
                    "nodomain.freeyourgadget.gadgetbridge.activities.ConfigureWorldClocks"
            );
            preference.putExtra(GBDevice.EXTRA_DEVICE, device);
            setting.settings.add(preference);
        }
        settings.add(setting);

        if (coordinator.supports(device, FEAT_PASSWORD)) {
            setting = DeviceSetting.screen(
                    "screen_password_settings",
                    R.string.wear_device_password_settings,
                    R.string.wear_device_password_settings_summary,
                    R.drawable.wear_ic_settings_security
            );
            setting.screenSummary = R.string.wear_device_password_settings_screen_summary;
            setting.defaultValue = "false";
            setting.screenHasSwitchBar = true;
            setting.screenSwitchBarKey = "pref_password_enabled";
            setting.settings = new ArrayList<>();

            preference = DeviceSetting.editText(
                    "pref_password",
                    R.string.prefs_password,
                    0,
                    0,
                    "true"
            );
            preference.valueAsSummary = false;
            preference.valueKind = DeviceSetting.ValueKind.NUMBER_PASSWORD;
            preference.length = 6;
            preference.dependency = "pref_password_enabled";
            preference.dependencyValue = "true";

            setting.settings.add(preference);
            settings.add(setting);
        }

        return settings;
    }

    private DeviceSetting getHealthSettings() {
        DeviceSetting setting = DeviceSetting.screen(
                "pref_health",
                R.string.wear_device_health_settings,
                R.string.wear_device_health_settings_summary,
                R.drawable.wear_ic_settings_health
        );
        setting.settings = new ArrayList<>();

        setting.settings.add(DeviceSetting.divider(R.string.wear_device_health_settings_measurement));

        // Heart rate monitor settings
        {
            DeviceSetting screen = DeviceSetting.screen(
                    "heartrate_measurement_interval",
                    R.string.wear_device_heartrate_settings,
                    0,
                    0
            );
            screen.entries = R.array.xiaomi_wear_hr_measurement_interval_names_summary;
            screen.entryValues = R.array.xiaomi_wear_hr_measurement_interval_values;
            screen.valueAsSummary = true;
            screen.screenSummary = R.string.wear_device_heartrate_settings_screen_summary;
            screen.settings = new ArrayList<>();

            DeviceSetting preference = DeviceSetting.dropdown(
                    "heartrate_measurement_interval",
                    R.string.wear_device_heartrate_settings_interval,
                    0,
                    "0",
                    R.array.xiaomi_wear_hr_measurement_interval_names,
                    R.array.xiaomi_wear_hr_measurement_interval_values
            );
            preference.valueAsSummary = true;
            screen.settings.add(preference);

            screen.settings.add(DeviceSetting.divider());

            preference = DeviceSetting.dropdown(
                    "heartrate_alert_threshold",
                    R.string.prefs_heartrate_alert_high_threshold,
                    0,
                    "0",
                    R.array.prefs_miband_heartrate_high_alert_threshold_with_off,
                    R.array.prefs_miband_heartrate_high_alert_threshold_with_off_values
            );
            preference.valueAsSummary = true;
            preference.dependency = "heartrate_measurement_interval";
            preference.dependencyValue = "-1,60";
            preference.dependencyDisablesPref = true;
            screen.settings.add(preference);

            preference = DeviceSetting.dropdown(
                    "heartrate_alert_low_threshold",
                    R.string.prefs_heartrate_alert_low_threshold,
                    0,
                    "0",
                    R.array.prefs_miband_heartrate_low_alert_threshold,
                    R.array.prefs_miband_heartrate_low_alert_threshold_values
            );
            preference.valueAsSummary = true;
            preference.dependency = "heartrate_measurement_interval";
            preference.dependencyValue = "-1,60";
            preference.dependencyDisablesPref = true;
            screen.settings.add(preference);

            setting.settings.add(screen);
        }

        // Blood oxygen monitor settings
        if (coordinator.supportsSpo2(device)){
            DeviceSetting screen = DeviceSetting.switchScreen(
                    "spo2_all_day_monitoring_enabled",
                    R.string.wear_device_blood_oxygen_settings,
                    R.string.wear_device_blood_oxygen_settings_summary,
                    0,
                    "false"
            );
            screen.valueAsSummary = true;
            screen.screenSummary = R.string.wear_device_blood_oxygen_settings_screen_summary;

            DeviceSetting preference = DeviceSetting.dropdown(
                    "spo2_low_alert_threshold",
                    R.string.prefs_spo2_alert_threshold,
                    0,
                    "0",
                    R.array.prefs_spo2_alert_threshold,
                    R.array.prefs_spo2_alert_threshold_values
            );
            preference.valueAsSummary = true;
            preference.dependency = "spo2_all_day_monitoring_enabled";
            preference.dependencyValue = "true";
            preference.dependencyDisablesPref = true;
            screen.settings.add(preference);

            setting.settings.add(screen);
        }

        // Stress monitoring settings
        if (coordinator.supportsStressMeasurement()){
            DeviceSetting screen = DeviceSetting.switchScreen(
                    "heartrate_stress_monitoring",
                    R.string.wear_device_stress_monitoring_settings,
                    R.string.wear_device_stress_monitoring_settings_summary,
                    0,
                    "false"
            );
            screen.valueAsSummary = true;
            screen.screenSummary = R.string.wear_device_stress_monitoring_settings_screen_summary;

            DeviceSetting preference = DeviceSetting.switchSetting(
                    "heartrate_stress_relaxation_reminder",
                    R.string.prefs_relaxation_reminder_title,
                    R.string.prefs_relaxation_reminder_description,
                    0,
                    "false"
            );
            preference.dependency = "heartrate_stress_monitoring";
            preference.dependencyValue = "true";
            preference.dependencyDisablesPref = true;
            screen.settings.add(preference);

            setting.settings.add(screen);
        }

        // Sleep monitoring settings
        getSleepSettings(setting);

        setting.settings.add(DeviceSetting.divider());
        setting.settings.add(DeviceSetting.description(
                "measurement_reminder",
                R.string.wear_device_health_settings_measurement_reminder
        ));
        setting.settings.add(DeviceSetting.divider());

        // Activity settings
        getActivitySettings(setting);

        // Sedentary reminder settings
        getSedentaryReminderSettings(setting);

        return setting;
    }

    private void getActivitySettings(DeviceSetting setting) {
        setting.settings.add(DeviceSetting.divider(R.string.wear_device_activity_settings));
        DeviceSetting preference = DeviceSetting.switchSetting(
                "fitness_goal_notification",
                R.string.wear_device_activity_settings_goal_notification,
                R.string.wear_device_activity_settings_goal_notification_summary,
                0,
                "true"
        );
        preference.valueAsSummary = true;
        setting.settings.add(preference);

        preference = DeviceSetting.dropdown(
                "fitness_goal_notification_vibration",
                R.string.wear_device_activity_settings_secondary_goal,
                0,
                "standing_time",
                R.array.goal_fitness_secondary_goal_entries,
                R.array.goal_fitness_secondary_goal_values
        );
        preference.valueAsSummary = true;
        setting.settings.add(preference);

        if (coordinator.supportsPai()) {
            setting.settings.add(DeviceSetting.divider());
            preference = DeviceSetting.switchSetting(
                    "pref_vitality_score_daily",
                    R.string.pref_vitality_score_daily_title,
                    R.string.pref_vitality_score_daily_summary,
                    0,
                    "false"
            );
            preference.valueAsSummary = true;
            setting.settings.add(preference);

            setting.settings.add(DeviceSetting.divider());
            preference = DeviceSetting.switchSetting(
                    "pref_vitality_score_7_day",
                    R.string.pref_vitality_score_7_day_title,
                    R.string.pref_vitality_score_7_day_summary,
                    0,
                    "false"
            );
            preference.valueAsSummary = true;
            setting.settings.add(preference);
        }

        setting.settings.add(DeviceSetting.divider());

        preference = DeviceSetting.switchSetting(
                "workout_send_gps_to_band",
                R.string.wear_device_activity_settings_allow_device_to_get_location,
                R.string.wear_device_activity_settings_allow_device_to_get_location_summary,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        setting.settings.add(preference);

        setting.settings.add(DeviceSetting.divider());

        preference = DeviceSetting.switchSetting(
                "workout_start_on_phone",
                R.string.wear_device_activity_settings_fitness_app_integration,
                R.string.wear_device_activity_settings_fitness_app_integration_summary,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        setting.settings.add(preference);

        preference = DeviceSetting.screen(
                "workout_start_on_phone_settings",
                R.string.wear_device_activity_settings_fitness_app_integration_settings,
                0,
                0,
                "nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity"
        );
        preference.dependency = "workout_start_on_phone";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        setting.settings.add(preference);
    }

    private void getSedentaryReminderSettings(DeviceSetting setting) {
        if (!coordinator.supports(device, FEAT_INACTIVITY)) {
            return;
        }

        DeviceSetting screen = DeviceSetting.switchScreen(
                "inactivity_warnings_enable",
                R.string.wear_device_sedentary_reminder_settings,
                R.string.wear_device_sedentary_reminder_settings_summary,
                0,
                "false"
        );
        screen.valueAsSummary = false;
        screen.screenSummary = R.string.wear_device_sedentary_reminder_settings_screen_summary_xiaomi;
        screen.settings = new ArrayList<>();

        DeviceSetting preference = DeviceSetting.timePicker(
                "inactivity_warnings_start",
                R.string.wear_device_start_time,
                0,
                0,
                "06:00"
        );
        preference.valueAsSummary = true;
        preference.dependency = "inactivity_warnings_enable";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        preference = DeviceSetting.timePicker(
                "inactivity_warnings_end",
                R.string.wear_device_end_time,
                0,
                0,
                "22:00"
        );
        preference.valueAsSummary = true;
        preference.dependency = "inactivity_warnings_enable";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        screen.settings.add(DeviceSetting.divider());

        preference = DeviceSetting.switchSetting(
                "inactivity_warnings_dnd",
                R.string.wear_device_sedentary_reminder_do_not_disturb,
                0,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        preference.dependency = "inactivity_warnings_enable";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        preference = DeviceSetting.timePicker(
                "inactivity_warnings_dnd_start",
                R.string.wear_device_start_time,
                0,
                0,
                "12:00"
        );
        preference.valueAsSummary = true;
        preference.dependency = "inactivity_warnings_dnd";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        preference = DeviceSetting.timePicker(
                "inactivity_warnings_dnd_end",
                R.string.wear_device_end_time,
                0,
                0,
                "14:00"
        );
        preference.valueAsSummary = true;
        preference.dependency = "inactivity_warnings_dnd";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        setting.settings.add(DeviceSetting.divider(R.string.wear_device_sedentary_reminder_settings));
        setting.settings.add(screen);
    }

    private void getSleepSettings(DeviceSetting setting) {
        DeviceSetting screen = DeviceSetting.screen(
                "pref_sleep",
                R.string.wear_device_sleep_settings,
                R.string.wear_device_sleep_settings_summary,
                0
        );
        screen.screenSummary = R.string.wear_device_sleep_settings_screen_summary_xiaomi;
        screen.settings = new ArrayList<>();

        DeviceSetting preference = DeviceSetting.switchSetting(
                "heartrate_sleep_detection",
                R.string.wear_device_sleep_settings_advanced_sleep_tracking,
                R.string.wear_device_sleep_settings_advanced_sleep_tracking_summary,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        screen.settings.add(preference);

        preference = DeviceSetting.switchSetting(
                "heartrate_sleep_breathing_quality_monitoring",
                R.string.wear_device_sleep_settings_breathing_score,
                R.string.wear_device_sleep_settings_breathing_score_summary,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        screen.settings.add(preference);

        if (coordinator.supports(device, FEAT_SLEEP_MODE_SCHEDULE)) {
            screen.settings.add(DeviceSetting.divider());
            DeviceSetting subScreen = DeviceSetting.switchScreen(
                    "sleep_mode_schedule_enabled",
                    R.string.wear_device_sleep_settings_sleep_mode,
                    R.string.wear_device_sleep_settings_sleep_mode_summary,
                    0,
                    "false"
            );
            subScreen.screenSummary = R.string.wear_device_sleep_settings_sleep_mode_screen_summary;
            subScreen.settings = new ArrayList<>();

            subScreen.settings.add(DeviceSetting.divider(R.string.wear_device_sleep_settings_sleep_mode_schedule));

            preference = DeviceSetting.timePicker(
                    "sleep_mode_schedule_start",
                    R.string.wear_device_sleep_settings_sleep_mode_bedtime,
                    0,
                    0,
                    ""
            );
            preference.valueAsSummary = true;
            preference.dependency = "sleep_mode_schedule_enabled";
            preference.dependencyValue = "true";
            preference.dependencyDisablesPref = true;
            subScreen.settings.add(preference);

            preference = DeviceSetting.timePicker(
                    "sleep_mode_schedule_end",
                    R.string.wear_device_sleep_settings_sleep_mode_wake_up_time,
                    0,
                    0,
                    ""
            );
            preference.valueAsSummary = true;
            preference.dependency = "sleep_mode_schedule_enabled";
            preference.dependencyValue = "true";
            preference.dependencyDisablesPref = true;
            subScreen.settings.add(preference);

            screen.settings.add(subScreen);
        }

        setting.settings.add(screen);
    }

    private DeviceSetting getCalendarSettings() {
        if (!coordinator.supportsCalendarEvents()) {
            return null;
        }

        DeviceSetting screen = DeviceSetting.screen(
                "screen_sync_calendar",
                R.string.wear_device_calendar_settings,
                R.string.wear_device_calendar_settings_summary,
                R.drawable.wear_ic_settings_calendar
        );
        screen.screenHasSwitchBar = true;
        screen.screenSwitchBarKey = "sync_calendar";
        screen.valueAsSummary = false;
        screen.screenSummary = R.string.wear_device_calendar_settings_screen_summary;
        screen.settings = new ArrayList<>();

        DeviceSetting preference = DeviceSetting.editText(
                "calendar_lookahead_days",
                R.string.pref_title_calendar_lookahead,
                0,
                0,
                "true"
        );
        preference.valueKind = DeviceSetting.ValueKind.INT;
        preference.defaultValue = "7";
        preference.max = 999;
        preference.valueAsSummary = true;
        preference.dependency = "sync_calendar";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);
        screen.settings.add(DeviceSetting.divider());

        preference = DeviceSetting.switchSetting(
                "sync_birthdays",
                R.string.pref_title_sync_birthdays,
                R.string.pref_summary_sync_birthdays,
                0,
                "false"
        );
        preference.dependency = "sync_calendar";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        preference = DeviceSetting.screen(
                "blacklist_calendars",
                R.string.pref_blacklist_calendars,
                R.string.pref_blacklist_calendars_summary,
                0,
                "nodomain.freeyourgadget.gadgetbridge.activities.CalBlacklistActivity"
        );
        preference.putExtra(GBDevice.EXTRA_DEVICE, device);
        preference.dependency = "sync_calendar";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        screen.settings.add(preference);

        return screen;
    }

    private DeviceSetting getContactsSettings() {
        if (coordinator.getContactsSlotCount(device) <= 0) {
            return null;
        }

        DeviceSetting setting = DeviceSetting.screen(
                "pref_contacts",
                R.string.wear_device_contacts_settings,
                R.string.wear_device_contacts_settings_summary,
                R.drawable.wear_ic_settings_contacts,
                "nodomain.freeyourgadget.gadgetbridge.activities.ConfigureContacts"
        );
        setting.putExtra(GBDevice.EXTRA_DEVICE, device);

        return setting;
    }

    private DeviceSetting getNotificationSettings() {
        DeviceSetting setting = DeviceSetting.screen(
                "send_app_notifications_screen",
                R.string.wear_device_notifications_settings,
                R.string.wear_device_notifications_settings_summary,
                R.drawable.wear_ic_settings_notifications
        );
        setting.defaultValue = "true";
        setting.screenHasSwitchBar = true;
        setting.screenSwitchBarKey = "send_app_notifications";
        setting.valueAsSummary = false;
        setting.settings = new ArrayList<>();

        DeviceSetting preference;
        preference = DeviceSetting.screen(
                "screen_notifications_app_filter",
                R.string.wear_device_notifications_app_notifications,
                R.string.wear_device_notifications_app_notifications_summary,
                0,
                "xyz.tenseventyseven.fresh.wearable.activities.AppNotificationsPickerActivity"
        );
        preference.putExtra(GBDevice.EXTRA_DEVICE, device);
        preference.dependency = "send_app_notifications";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        setting.settings.add(preference);

        setting.settings.add(DeviceSetting.divider());

        if (coordinator.supports(device, FEAT_SCREEN_ON_ON_NOTIFICATIONS)) {
            preference = DeviceSetting.dropdown(
                    "screen_on_on_notifications_dropdown",
                    R.string.wear_device_notifications_show_notifications_options,
                    0,
                    "true",
                    R.array.wear_device_notifications_options,
                    R.array.wear_device_notifications_options_values
            );
            preference.dependency = "send_app_notifications";
            preference.dependencyValue = "true";
            preference.dependencyDisablesPref = true;
            preference.valueAsSummary = true;
            setting.settings.add(preference);
        }

        preference = DeviceSetting.switchSetting(
                "notifications_ignore_low_priority",
                R.string.wear_device_notifications_ignore_low_priority_notifications,
                R.string.wear_device_notifications_ignore_low_priority_notifications_summary,
                0,
                "false"
        );
        preference.dependency = "send_app_notifications";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        setting.settings.add(preference);

        preference = DeviceSetting.switchSetting(
                "autoremove_notifications",
                R.string.wear_device_notifications_remove_notifications_when_removed,
                R.string.wear_device_notifications_remove_notifications_when_removed_summary,
                0,
                "true"
        );
        preference.dependency = "send_app_notifications";
        preference.dependencyValue = "true";
        preference.dependencyDisablesPref = true;
        setting.settings.add(preference);


        if (coordinator.getCannedRepliesSlotCount(device) > 0) {
            setting.settings.add(getCannedMessageSettings());
        }

        setting.settings.add(DeviceSetting.divider());
        preference = DeviceSetting.switchScreen(
                DeviceSettingsPreferenceConst.PREF_DND_SYNC,
                R.string.wear_device_notifications_settings_dnd_sync,
                0,
                0,
                "false"
        );
        preference.valueAsSummary = true;
        preference.screenSummary = R.string.wear_device_notifications_settings_dnd_sync_screen_summary;
        preference.settings = new ArrayList<>();

        DeviceSetting dndMode = DeviceSetting.dropdown(
                DeviceSettingsPreferenceConst.PREF_DND_SYNC_WITH_WATCH_MODE,
                R.string.wear_device_notifications_dnd_sync_mode,
                0,
                "priority",
                R.array.phone_do_not_disturb_mode,
                R.array.phone_do_not_disturb_mode_values
        );
        dndMode.valueAsSummary = true;
        dndMode.dependency = DeviceSettingsPreferenceConst.PREF_DND_SYNC;
        dndMode.dependencyValue = "true";
        dndMode.dependencyDisablesPref = true;
        preference.settings.add(dndMode);
        preference.settings.add(DeviceSetting.divider());

        dndMode = DeviceSetting.description(
                "dnd_sync_description",
                R.string.wear_device_notifications_dnd_sync_mode_summary
        );
        preference.settings.add(dndMode);

        setting.settings.add(preference);

        setting.settings.add(DeviceSetting.divider());

        preference = DeviceSetting.dragSort(
                "pref_transliteration_languages",
                R.string.pref_title_transliteration,
                R.string.pref_summary_transliteration,
                0,
                "true"
        );
        preference.entries = R.array.pref_transliteration_languages;
        preference.entryValues = R.array.pref_transliteration_languages_values;
        setting.settings.add(preference);

        return setting;
    }

    private DeviceSetting getCannedMessageSettings() {
        DeviceSetting setting = DeviceSetting.screen(
                "screen_canned_messages_dismisscall",
                R.string.pref_title_canned_messages_dismisscall,
                R.string.pref_summary_canned_messages_dismisscall,
                0
        );
        setting.valueAsSummary = false;
        setting.settings = new ArrayList<>();

        for (int i = 0; i < coordinator.getCannedRepliesSlotCount(device); i++) {
            DeviceSetting preference = DeviceSetting.editText(
                    "canned_message_" + i,
                    0,
                    0,
                    0,
                    "true"
            );
            preference.valueAsSummary = true;
            setting.settings.add(preference);
        }

        return setting;
    }
}
