package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceShortcut;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableSettingCoordinator;

public class RedmiBudsSettingsCoordinator extends WearableSettingCoordinator {
    private RedmiBudsCoordinator coordinator;
    private static final String GESTURE_SHORTCUT_KEY = "redmi_buds_gesture_shortcut";
    private static final String NOTIFICATIONS_SETTINGS_KEY = "redmi_buds_notifications_shortcut";
    private static final String AUTO_ANSWER_DELAY_KEY = "redmi_buds_auto_answer_delay";

    // Key structure constants
    private static final String PREFIX_LONG_TAP = "pref_redmi_buds_control_long_tap";
    private static final String PREFIX_LONG_TAP_SETTINGS = "pref_redmi_buds_control_long_tap_settings";
    private static final String PREFIX_GESTURE = "pref_redmi_buds_control";
    private static final String SIDE_LEFT = "_left";
    private static final String SIDE_RIGHT = "_right";

    // Settings types for long tap
    private static final String SETTING_SINGLE_TAP = "single_tap";
    private static final String SETTING_DOUBLE_TAP = "double_tap";
    private static final String SETTING_TRIPLE_TAP = "triple_tap";
    private static final String SETTING_ANC = "anc";
    private static final String SETTING_AMBIENT = "ambient";
    private static final String SETTING_OFF = "off";

    RedmiBudsSettingsCoordinator(RedmiBudsCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void onSettingChanged(GBDevice device, PreferenceScreen preferenceScreen, Preference preference, String key) {
        if (key.startsWith(PREFIX_LONG_TAP) && !key.endsWith("_mode_left") && !key.endsWith("_mode_right")) {
            String side = key.endsWith(SIDE_LEFT) ? SIDE_LEFT : SIDE_RIGHT;
            handleAmbientSoundChange(device, preferenceScreen, side);
        }

        if (key.equals(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AUTO_REPLY_PHONECALL)) {
            handleAutoAnswerChange(device, preferenceScreen.getSharedPreferences(), preference.getSharedPreferences().getBoolean(key, false));
        }

        if (key.equals(AUTO_ANSWER_DELAY_KEY)) {
            handleAnswerDelayChange(device, preferenceScreen, preference.getSharedPreferences().getString(key, "15"));
        }
    }

    @Override
    public void onSettingChanged(GBDevice device, SharedPreferences sharedPreferences, String key) {
        if (key.equals(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AUTO_REPLY_PHONECALL)) {
            handleAutoAnswerChange(device, sharedPreferences, sharedPreferences.getBoolean(key, false));
        }
    }

    @Override
    public void onSettingsCreated(PreferenceScreen preferenceScreen) {
        if (preferenceScreen.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT) != null) {
            handleAmbientSoundCheckboxes(SIDE_LEFT, preferenceScreen);
        }

        if (preferenceScreen.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT) != null) {
            handleAmbientSoundCheckboxes(SIDE_RIGHT, preferenceScreen);
        }
    }

    @Override
    public boolean allowPreferenceChange(PreferenceScreen screen, Preference preference, String newValue) {
        String key = preference.getKey();
        if (key.startsWith(PREFIX_LONG_TAP) && !key.endsWith("_mode_left") && !key.endsWith("_mode_right")) {
            String side = key.endsWith(SIDE_LEFT) ? SIDE_LEFT : SIDE_RIGHT;
            return allowAmbientSoundCheckboxChange(screen, side, preference, newValue);
        }
        return super.allowPreferenceChange(screen, preference, newValue);
    }

    private void handleAutoAnswerChange(GBDevice device, SharedPreferences prefs, Boolean newValue) {
        if (prefs == null) {
            return;
        }

        prefs.edit().putBoolean(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL, newValue).commit();
        Application.deviceService(device).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL);
    }

    private void handleAnswerDelayChange(GBDevice device, PreferenceScreen screen, String newValue) {
        SharedPreferences prefs = screen.getSharedPreferences();
        if (prefs == null) {
            return;
        }

        prefs.edit().putInt(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL_DELAY, Integer.parseInt(newValue)).commit();
        Application.deviceService(device).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL_DELAY);
    }

    @SuppressLint("ApplySharedPref")
    private void handleAmbientSoundChange(GBDevice device, PreferenceScreen screen, String side) {
        SharedPreferences prefs = screen.getSharedPreferences();
        if (prefs == null) {
            return;
        }

        CheckBoxPreference anc = screen.findPreference(getLongTapSoundControlKey(side, SETTING_ANC));
        CheckBoxPreference ambient = screen.findPreference(getLongTapSoundControlKey(side, SETTING_AMBIENT));
        CheckBoxPreference off = screen.findPreference(getLongTapSoundControlKey(side, SETTING_OFF));

        if (anc == null || ambient == null || off == null) {
            return;
        }

        /*
         *  Now we set the actual preference based on the selection
         *  Each action (off, transparency, anc) represent a bit in the value
         *  First bit is for off, second for anc, third for transparency
         *  There must be at least two bits set for a valid value
         *
         *  0b011 - ANC, ANC off (3)
         *  0b101 - Transparency, ANC off (5)
         *  0b110 - ANC, Transparency (6)
         *  0b111 - ANC, ANC off, Transparency (7)
         *
         * We will use the integer sum of the bits to set the value to the actual preference.
         */
        int value = 0;
        value += anc.isChecked() ? 0b010 : 0;
        value += ambient.isChecked() ? 0b100 : 0;
        value += off.isChecked() ? 0b001 : 0;

        String key = getLongTapSettingKey(side);
        prefs.edit().putString(key, String.valueOf(value)).commit();
        Application.deviceService(device).onSendConfiguration(key);
    }

    private boolean allowAmbientSoundCheckboxChange(PreferenceScreen screen, String side, Preference preference, String newValue) {
        if (Objects.equals(newValue, "true")) {
            return true;
        }

        CheckBoxPreference anc = screen.findPreference(getLongTapSoundControlKey(side, SETTING_ANC));
        CheckBoxPreference ambient = screen.findPreference(getLongTapSoundControlKey(side, SETTING_AMBIENT));
        CheckBoxPreference off = screen.findPreference(getLongTapSoundControlKey(side, SETTING_OFF));

        if (anc == null || ambient == null || off == null) {
            return true;
        }

        // Reduce by one to account for the current preference
        int checkedCount = ((anc.isChecked() ? 1 : 0) +
                (ambient.isChecked() ? 1 : 0) +
                (off.isChecked() ? 1 : 0)) - 1;

        if (checkedCount <= 1) {
            Toast.makeText(screen.getContext(),
                    R.string.wear_buds_gesture_settings_touch_and_hold_two_selections_required,
                    Toast.LENGTH_SHORT).show();
            ((CheckBoxPreference) preference).setChecked(true);
            return false;
        }

        return true;
    }

    private void handleAmbientSoundCheckboxes(String side, PreferenceScreen screen) {
        String ancKey = getLongTapSoundControlKey(side, "anc");
        String transparencyKey = getLongTapSoundControlKey(side, "ambient");
        String offKey = getLongTapSoundControlKey(side, "off");

        CheckBoxPreference anc = screen.findPreference(ancKey);
        CheckBoxPreference transparency = screen.findPreference(transparencyKey);
        CheckBoxPreference off = screen.findPreference(offKey);

        if (anc == null || transparency == null || off == null) {
            return;
        }

        SharedPreferences prefs = screen.getSharedPreferences();
        if (prefs == null) {
            return;
        }

        String setting = prefs.getString(getLongTapSettingKey(side), "7");
        int value = Integer.parseInt(setting);
        anc.setChecked((value & 0b010) != 0);
        transparency.setChecked((value & 0b100) != 0);
        off.setChecked((value & 0b001) != 0);
    }

    @Override
    public void onShortcutClicked(Context context, GBDevice device, String key) {
        switch (key) {
            case GESTURE_SHORTCUT_KEY:
                DeviceSetting settings = getGestureSettings();
                Intent intent = new Intent(context, PreferenceScreenActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                intent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, false);
                intent.putExtra(DeviceSetting.EXTRA_SETTING, settings);
                context.startActivity(intent);
                break;
            case NOTIFICATIONS_SETTINGS_KEY:
                DeviceSetting notificationsSettings = getNotificationsSettings();
                Intent notificationsIntent = new Intent(context, PreferenceScreenActivity.class);
                notificationsIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                notificationsIntent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, false);
                notificationsIntent.putExtra(DeviceSetting.EXTRA_SETTING, notificationsSettings);
                context.startActivity(notificationsIntent);
                break;
        }
    }

    @Override
    public List<DeviceShortcut> getShortcuts() {
        List<DeviceShortcut> shortcuts = new ArrayList<>();

        if (coordinator.supports(RedmiBudsCapabilities.GestureControl)) {
            shortcuts.add(new DeviceShortcut(GESTURE_SHORTCUT_KEY, R.string.wear_buds_gesture_settings, R.drawable.home_tab_touch_controls));
        }

        shortcuts.add(new DeviceShortcut(NOTIFICATIONS_SETTINGS_KEY, R.string.wear_device_notifications_settings, R.drawable.home_tab_notifications));

        return shortcuts;
    }

    @Override
    public List<DeviceSetting> getSettings() {
        List<DeviceSetting> settings = new ArrayList<>();

        List<DeviceSetting> anc = getANCSetting();
        if (!anc.isEmpty()) {
            settings.addAll(anc);
            settings.add(new DeviceSetting(DeviceSetting.DeviceSettingType.DIVIDER));
        }

        List<DeviceSetting> equalizer = getEqualizerSettings();
        if (!equalizer.isEmpty()) {
            settings.addAll(equalizer);
            settings.add(new DeviceSetting(DeviceSetting.DeviceSettingType.DIVIDER));
        }

        if (coordinator.supports(RedmiBudsCapabilities.InEarDetection)) {
            DeviceSetting setting = DeviceSetting.switchScreen(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_WEARING_DETECTION,
                    R.string.wear_buds_in_ear_detection,
                    0,
                    R.drawable.wear_ic_settings_in_ear_detection,
                    "true"
            );
            setting.valueAsSummary = true;
            setting.screenSummary = R.string.wear_buds_in_ear_detection_screen_summary;
            settings.add(setting);
        }

        if (coordinator.supports(RedmiBudsCapabilities.DualDeviceConnection)) {
            DeviceSetting setting = DeviceSetting.switchScreen(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_DOUBLE_CONNECTION,
                    R.string.wear_buds_dual_connection,
                    0,
                    R.drawable.wear_ic_settings_dual_connection,
                    "false"
            );
            setting.valueAsSummary = true;
            setting.screenSummary = R.string.wear_buds_dual_connection_screen_summary;
            settings.add(setting);
        }

        return settings;
    }

    private List<DeviceSetting> getANCSetting() {
        List<DeviceSetting> anc = new ArrayList<>();
        if (supportsAnc()) {
            DeviceSetting mainSetting = DeviceSetting.anc(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_CONTROL,
                    "0",
                    R.array.redmi_buds_wear_noise_control_values
            );
            anc.add(mainSetting);
        }

        if (coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV2) ||
                coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV3)) {
            DeviceSetting ancLevel = DeviceSetting.seekbarPro(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_NOISE_CANCELLING_STRENGTH,
                    R.string.wear_active_noise_cancelling_level,
                    "1",
                    0,
                    2
            );
            ancLevel.entryValues = R.array.redmi_buds_wear_noise_cancelling_strength_values;
            ancLevel.leftLabel = R.string.redmi_buds_anc_light;
            ancLevel.rightLabel = R.string.redmi_buds_anc_deep;
            ancLevel.showTicks = false;
            ancLevel.showValue = false;
            ancLevel.centerSeekBar = true;
            ancLevel.dependency = DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_CONTROL;
            ancLevel.dependencyValue = "1";

            DeviceSetting transparencyLevel = DeviceSetting.dropdown(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_TRANSPARENCY_STRENGTH,
                    R.string.wear_ambient_sound_mode,
                    0,
                    "0",
                    R.array.redmi_buds_wear_ambient_sound_names,
                    R.array.redmi_buds_transparency_strength_values
            );
            transparencyLevel.dependency = DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_CONTROL;
            transparencyLevel.dependencyValue = "2";

            anc.addAll(List.of(ancLevel, transparencyLevel));
        }

        if (coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV3)) {
            DeviceSetting adaptiveAnc = DeviceSetting.switchSetting(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_ADAPTIVE_NOISE_CANCELLING,
                    R.string.pref_adaptive_noise_cancelling_title,
                    R.string.pref_adaptive_noise_cancelling_summary,
                    0,
                    "true"
            );
            adaptiveAnc.dependency = DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_CONTROL;
            adaptiveAnc.dependencyValue = "1";
            anc.add(adaptiveAnc);
        }

        return anc;
    }

    private boolean supportsAnc() {
        return coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV1) ||
                coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV2) ||
                coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV3);
    }

    private List<DeviceSetting> getEqualizerSettings() {
        List<DeviceSetting> equalizer = new ArrayList<>();
        if (!coordinator.supports(RedmiBudsCapabilities.EqualizerV1) &&
                !coordinator.supports(RedmiBudsCapabilities.EqualizerV2)) {
            return equalizer;
        }

        DeviceSetting equalizerSetting = DeviceSetting.screen(
                DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET,
                R.string.sony_equalizer,
                0,
                R.drawable.wear_ic_settings_equalizer
        );
        equalizerSetting.valueAsSummary = true;
        equalizerSetting.settings = new ArrayList<>();

        DeviceSetting equalizerPreview = DeviceSetting.equalizerPreview(
                DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET,
                0, 0
        );
        equalizerPreview.dependency = DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET;

        DeviceSetting equalizerMode = DeviceSetting.buttonGroup(
                DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET,
                "0",
                0, 0
        );

        DeviceSetting equalizerDescription = DeviceSetting.equalizerDescription(
                DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET,
                0, 0
        );
        equalizerDescription.dependency = DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET;

        if (coordinator.supports(RedmiBudsCapabilities.EqualizerV1)) {
            equalizerPreview.entries = R.array.redmi_buds_wear_equalizer_v1_presets_drawables;
            equalizerPreview.entryValues = R.array.redmi_buds_wear_equalizer_v1_presets_values;

            equalizerMode.defaultValue = "0";
            equalizerMode.entries = R.array.redmi_buds_wear_equalizer_v1_presets_names;
            equalizerMode.entryValues = R.array.redmi_buds_wear_equalizer_v1_presets_values;

            equalizerDescription.entries = R.array.redmi_buds_wear_equalizer_v1_presets_descriptions;
            equalizerDescription.entryValues = R.array.redmi_buds_wear_equalizer_v1_presets_values;

            equalizerSetting.entries = R.array.redmi_buds_wear_equalizer_v1_presets_names;
            equalizerSetting.entryValues = R.array.redmi_buds_wear_equalizer_v1_presets_values;
        }

        equalizerSetting.settings.add(equalizerPreview);
        equalizerSetting.settings.add(DeviceSetting.divider());
        equalizerSetting.settings.add(equalizerMode);
        equalizerSetting.settings.add(DeviceSetting.divider());
        equalizerSetting.settings.add(equalizerDescription);

        equalizer.add(equalizerSetting);

        // TODO: Equalizer V2 support

        if (coordinator.supports(RedmiBudsCapabilities.AdaptiveSound)) {
            DeviceSetting.switchSetting(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_ADAPTIVE_SOUND,
                    R.string.wear_buds_adaptive_sound,
                    R.string.wear_buds_adaptive_sound_summary,
                    0,
                    "false"
            );
        }

        return equalizer;
    }

    private DeviceSetting getNotificationsSettings() {
        DeviceSetting notificationsScreen = DeviceSetting.screen(
                NOTIFICATIONS_SETTINGS_KEY,
                R.string.wear_device_notifications_settings,
                0,
                R.drawable.home_tab_notifications
        );
        notificationsScreen.settings = new ArrayList<>();

        DeviceSetting readAloud = DeviceSetting.switchScreen(
                DeviceSettingsPreferenceConst.PREF_SPEAK_NOTIFICATIONS_ALOUD,
                R.string.wear_buds_read_notifications_aloud,
                0,
                R.drawable.wear_ic_settings_notifications,
                "false"
        );
        readAloud.valueAsSummary = true;
        readAloud.screenSummary = R.string.wear_buds_read_notifications_aloud_screen_summary;
        readAloud.settings = new ArrayList<>();

        readAloud.settings.add(DeviceSetting.divider());
        DeviceSetting exclusiveMode = DeviceSetting.switchSetting(
                DeviceSettingsPreferenceConst.PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE,
                R.string.wear_buds_read_notifications_exclusive,
                R.string.wear_buds_read_notifications_exclusive_summary,
                0,
                "false"
        );
        readAloud.settings.add(exclusiveMode);

        notificationsScreen.settings.add(readAloud);

        if (coordinator.supports(RedmiBudsCapabilities.AutoAnswerPhoneCalls)) {
            DeviceSetting autoAnswer = DeviceSetting.switchScreen(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AUTO_REPLY_PHONECALL,
                    R.string.wear_buds_automatically_answer_calls,
                    0,
                    R.drawable.wear_ic_settings_auto_answer_calls,
                    "false"
            );
            autoAnswer.valueAsSummary = true;
            autoAnswer.screenSummary = R.string.wear_buds_automatically_answer_calls_screen_summary;
            autoAnswer.settings = new ArrayList<>();

            DeviceSetting delay = DeviceSetting.dropdown(
                    AUTO_ANSWER_DELAY_KEY,
                    R.string.wear_buds_automatically_answer_calls_delay,
                    0,
                    "15",
                    R.array.wear_auto_answer_calls_delay_names,
                    R.array.wear_auto_answer_calls_delay_values
            );

            autoAnswer.settings.add(delay);
            notificationsScreen.settings.add(autoAnswer);
        }

        return notificationsScreen;
    }

    private DeviceSetting getGestureSettings() {
        DeviceSetting screen = DeviceSetting.screen(
                GESTURE_SHORTCUT_KEY,
                R.string.wear_buds_gesture_settings,
                0,
                R.drawable.home_tab_touch_controls
        );
        screen.screenSummary = R.string.wear_buds_gesture_settings_screen_summary;
        screen.settings = new ArrayList<>();

        // Left Gestures
        screen.settings.add(DeviceSetting.divider(R.string.wear_buds_gesture_settings_category_left));
        addGestureSettings(screen, SIDE_LEFT);

        // Right Gestures
        screen.settings.add(DeviceSetting.divider(R.string.wear_buds_gesture_settings_category_right));
        addGestureSettings(screen, SIDE_RIGHT);

        return screen;
    }

    private void addGestureSettings(DeviceSetting screen, String side) {
        screen.settings.addAll(List.of(
                DeviceSetting.dropdown(
                        getGestureKey(SETTING_SINGLE_TAP, side),
                        R.string.wear_buds_gesture_settings_single_tap,
                        0,
                        "8",
                        R.array.redmi_buds_single_button_function_names,
                        R.array.redmi_buds_single_button_function_values
                ),
                DeviceSetting.dropdown(
                        getGestureKey(SETTING_DOUBLE_TAP, side),
                        R.string.wear_buds_gesture_settings_double_tap,
                        0,
                        "1",
                        R.array.redmi_buds_button_function_names,
                        R.array.redmi_buds_button_function_values
                ),
                DeviceSetting.dropdown(
                        getGestureKey(SETTING_TRIPLE_TAP, side),
                        R.string.wear_buds_gesture_settings_triple_tap,
                        0,
                        side.equals(SIDE_LEFT) ? "2" : "3",
                        R.array.redmi_buds_button_function_names,
                        R.array.redmi_buds_button_function_values
                )
        ));
        screen.settings.add(getTouchAndHoldSettings(side));
    }

    private DeviceSetting getTouchAndHoldSettings(String side) {
        String longTapKey = getLongTapKey(side);

        DeviceSetting screen = DeviceSetting.screen(
                longTapKey,
                R.string.wear_buds_gesture_settings_touch_and_hold,
                0,
                0
        );
        screen.defaultValue = "6";
        screen.entries = R.array.redmi_buds_long_button_mode_names;
        screen.entryValues = R.array.redmi_buds_long_button_mode_values;
        screen.screenSummary = R.string.wear_buds_gesture_settings_touch_and_hold_screen_summary_redmi;
        screen.valueAsSummary = true;
        screen.settings = new ArrayList<>();

        screen.settings.add(DeviceSetting.dropdown(
                longTapKey,
                R.string.wear_buds_gesture_settings_touch_and_hold_mode,
                0,
                "6",
                R.array.redmi_buds_long_button_mode_names,
                R.array.redmi_buds_long_button_mode_values
        ));

        DeviceSetting checkBoxDivider = DeviceSetting.divider(getLongTapSoundControlKey(side, "options"));
        checkBoxDivider.dependency = longTapKey;
        checkBoxDivider.dependencyValue = "6";
        screen.settings.add(checkBoxDivider);

        screen.settings.addAll(List.of(
                DeviceSetting.checkbox(
                        getLongTapSoundControlKey(side, SETTING_ANC),
                        R.string.prefs_active_noise_cancelling,
                        0,
                        R.drawable.wear_ic_active_noise_cancelling,
                        "true"
                ),
                DeviceSetting.checkbox(
                        getLongTapSoundControlKey(side, SETTING_AMBIENT),
                        R.string.prefs_ambient_sound,
                        0,
                        R.drawable.wear_ic_ambient_sound,
                        "true"
                ),
                DeviceSetting.checkbox(
                        getLongTapSoundControlKey(side, SETTING_OFF),
                        R.string.off,
                        0,
                        R.drawable.wear_ic_active_noise_cancelling_off,
                        "true"
                )
        ));

        String voiceAssistantKey = getLongTapSoundControlKey(side, "voice_assistant");
        DeviceSetting voiceAssistantDivider = DeviceSetting.divider(voiceAssistantKey);
        voiceAssistantDivider.dependency = longTapKey;
        voiceAssistantDivider.dependencyValue = "0";
        screen.settings.add(voiceAssistantDivider);

        screen.settings.add(DeviceSetting.description(
                voiceAssistantKey + "_description",
                R.string.wear_buds_gesture_settings_touch_and_hold_assistant_description
        ));

        return screen;
    }

    private String getLongTapKey(String side) {
        return PREFIX_LONG_TAP + "_mode" + side;
    }

    private String getLongTapSoundControlKey(String side, String setting) {
        return PREFIX_LONG_TAP + "_" + setting + side;
    }

    private String getLongTapSettingKey(String side) {
        return PREFIX_LONG_TAP_SETTINGS + side;
    }

    private String getGestureKey(String gesture, String side) {
        return PREFIX_GESTURE + "_" + gesture + side;
    }
}
