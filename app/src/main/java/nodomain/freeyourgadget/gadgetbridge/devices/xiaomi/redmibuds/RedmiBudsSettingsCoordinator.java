package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableSettingCoordinator;

public class RedmiBudsSettingsCoordinator extends WearableSettingCoordinator {
    private RedmiBudsCoordinator coordinator;

    RedmiBudsSettingsCoordinator(RedmiBudsCoordinator coordinator) {
        this.coordinator = coordinator;
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

        List<DeviceSetting> gestures = getGestureSettings();
        if (!gestures.isEmpty()) {
            settings.addAll(gestures);
        }

        if (coordinator.supports(RedmiBudsCapabilities.InEarDetection)) {
            DeviceSetting setting = DeviceSetting.switchScreen(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_WEARING_DETECTION,
                    R.string.nothing_prefs_inear_title,
                    R.string.nothing_prefs_inear_summary,
                    0,
                    "true"
            );
            setting.valueAsSummary = true;
            setting.screenSummary = R.string.nothing_prefs_inear_summary;
            settings.add(setting);
        }

        if (coordinator.supports(RedmiBudsCapabilities.AutoAnswerPhoneCalls)) {
            DeviceSetting setting = DeviceSetting.switchSetting(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AUTO_REPLY_PHONECALL,
                    R.string.pref_auto_reply_calls_title,
                    R.string.pref_auto_reply_calls_summary,
                    0,
                    "false"
            );
            settings.add(setting);
        }

        if (coordinator.supports(RedmiBudsCapabilities.DualDeviceConnection)) {
            DeviceSetting setting = DeviceSetting.switchSetting(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_DOUBLE_CONNECTION,
                    R.string.redmi_buds_double_connection,
                    R.string.redmi_buds_double_connection_description,
                    0,
                    "false"
            );
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
            ancLevel.seekbarIsString = true;
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
                "redmi_buds_equalizer_screen",
                R.string.sony_equalizer,
                0,
                dev.oneuiproject.oneui.R.drawable.ic_oui_equalizer_2
        );
        equalizerSetting.settings = new ArrayList<>();

        if (coordinator.supports(RedmiBudsCapabilities.EqualizerV1)) {
            DeviceSetting equalizerMode = DeviceSetting.dropdown(
                    DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET,
                    R.string.prefs_equalizer_preset,
                    0,
                    "0",
                    R.array.redmi_buds_equalizer_v1_presets_names,
                    R.array.redmi_buds_equalizer_v1_presets_values
            );

            equalizerSetting.settings.add(equalizerMode);
            equalizer.add(equalizerSetting);
        }

        return equalizer;
    }
    private List<DeviceSetting> getGestureSettings() {
        List<DeviceSetting> gestures = new ArrayList<>();
        if (!coordinator.supports(RedmiBudsCapabilities.GestureControl)) {
            return gestures;
        }

        DeviceSetting touchOptionsScreen = DeviceSetting.screen(
                "pref_screen_redmibuds5pro_touch_options",
                R.string.prefs_galaxy_touch_options,
                0,
                R.drawable.ic_touch
        );
        gestures.add(touchOptionsScreen);

        // Single Tap Category
        DeviceSetting singleTapCategory = DeviceSetting.divider(R.string.single_tap);
        touchOptionsScreen.settings.add(singleTapCategory);

        DeviceSetting singleTapLeft = DeviceSetting.dropdown(
                "pref_redmi_buds_control_single_tap_left",
                R.string.prefs_left,
                0,
                "8",
                R.array.redmi_buds_single_button_function_names,
                R.array.redmi_buds_single_button_function_values
        );
        touchOptionsScreen.settings.add(singleTapLeft);

        DeviceSetting singleTapRight = DeviceSetting.dropdown(
                "pref_redmi_buds_control_single_tap_right",
                R.string.prefs_right,
                0,
                "8",
                R.array.redmi_buds_single_button_function_names,
                R.array.redmi_buds_single_button_function_values
        );
        touchOptionsScreen.settings.add(singleTapRight);

        // Double Tap Category
        DeviceSetting doubleTapCategory = DeviceSetting.divider(R.string.double_tap);
        touchOptionsScreen.settings.add(doubleTapCategory);

        DeviceSetting doubleTapLeft = DeviceSetting.dropdown(
                "pref_redmi_buds_control_double_tap_left",
                R.string.prefs_left,
                0,
                "1",
                R.array.redmi_buds_button_function_names,
                R.array.redmi_buds_button_function_values
        );
        touchOptionsScreen.settings.add(doubleTapLeft);

        DeviceSetting doubleTapRight = DeviceSetting.dropdown(
                "pref_redmi_buds_control_double_tap_right",
                R.string.prefs_right,
                0,
                "1",
                R.array.redmi_buds_button_function_names,
                R.array.redmi_buds_button_function_values
        );
        touchOptionsScreen.settings.add(doubleTapRight);

        // Triple Tap Category
        DeviceSetting tripleTapCategory = DeviceSetting.divider(R.string.triple_tap);
        touchOptionsScreen.settings.add(tripleTapCategory);

        DeviceSetting tripleTapLeft = DeviceSetting.dropdown(
                "pref_redmi_buds_control_triple_tap_left",
                R.string.prefs_left,
                0,
                "2",
                R.array.redmi_buds_button_function_names,
                R.array.redmi_buds_button_function_values
        );
        touchOptionsScreen.settings.add(tripleTapLeft);

        DeviceSetting tripleTapRight = DeviceSetting.dropdown(
                "pref_redmi_buds_control_triple_tap_right",
                R.string.prefs_right,
                0,
                "3",
                R.array.redmi_buds_button_function_names,
                R.array.redmi_buds_button_function_values
        );
        touchOptionsScreen.settings.add(tripleTapRight);

        // Long Press Category
        DeviceSetting longPressCategory = DeviceSetting.divider(R.string.long_press);
        touchOptionsScreen.settings.add(longPressCategory);

        DeviceSetting longPressModeLeft = DeviceSetting.dropdown(
                "pref_redmi_buds_control_long_tap_mode_left",
                R.string.sony_button_mode_left,
                R.drawable.ic_touch,
                "6",
                R.array.redmi_buds_long_button_mode_names,
                R.array.redmi_buds_long_button_mode_values
        );
        touchOptionsScreen.settings.add(longPressModeLeft);

        DeviceSetting longPressSettingsLeft = DeviceSetting.dropdown(
                "pref_redmi_buds_control_long_tap_settings_left",
                R.string.sony_ambient_sound_control_button_modes,
                0,
                "7",
                R.array.redmi_buds_long_button_settings_names,
                R.array.redmi_buds_long_button_settings_values
        );
        longPressSettingsLeft.dependency = "pref_redmi_buds_control_long_tap_mode_left";
        longPressSettingsLeft.dependencyValue = "6";
        touchOptionsScreen.settings.add(longPressSettingsLeft);

        DeviceSetting longPressModeRight = DeviceSetting.dropdown(
                "pref_redmi_buds_control_long_tap_mode_right",
                R.string.sony_button_mode_right,
                R.drawable.ic_touch,
                "6",
                R.array.redmi_buds_long_button_mode_names,
                R.array.redmi_buds_long_button_mode_values
        );
        touchOptionsScreen.settings.add(longPressModeRight);

        DeviceSetting longPressSettingsRight = DeviceSetting.dropdown(
                "pref_redmi_buds_control_long_tap_settings_right",
                R.string.sony_ambient_sound_control_button_modes,
                0,
                "7",
                R.array.redmi_buds_long_button_settings_names,
                R.array.redmi_buds_long_button_settings_values
        );
        longPressSettingsRight.dependency = "pref_redmi_buds_control_long_tap_mode_right";
        longPressSettingsRight.dependencyValue = "6";
        touchOptionsScreen.settings.add(longPressSettingsRight);

        return gestures;
    }
}
