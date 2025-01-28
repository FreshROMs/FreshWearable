/*  Copyright (C) 2024 Jonathan Gobbo
    Copyright (C) 2024 John Vincent Corcega

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import dev.oneuiproject.oneui.widget.Toast;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.components.ButtonGroupPreference;
import xyz.tenseventyseven.fresh.wearable.components.NoiseControlPreference;

public class RedmiBudsSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    final GBDevice device;

    final ArrayList<String> longTapControlPreferencesLeft = new ArrayList<>(Arrays.asList(
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_AMBIENT,
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_ANC,
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_OFF
    ));

    final ArrayList<String> longTapControlPreferencesRight = new ArrayList<>(Arrays.asList(
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_AMBIENT,
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_ANC,
            DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_OFF
    ));

    public RedmiBudsSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
        RedmiBudsCoordinator coordinator = (RedmiBudsCoordinator) device.getDeviceCoordinator();

        // Check if the device supports ANCv2 as it is only available on ANCv2 devices
        if (coordinator.supports(RedmiBudsCapabilities.ActiveNoiseCancellationV2)) {
            // Hide or Show ANC/Transparency settings according to the current ambient sound control mode
            if (preference.getKey().equals(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_CONTROL)) {
                String mode = ((NoiseControlPreference) preference).getValue();
                final Preference ancLevel = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_NOISE_CANCELLING_STRENGTH);
                final Preference transparencyLevel = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_AMBIENT_SOUND_STRENGTH);
                final Preference adaptiveAnc = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_ADAPTIVE_NOISE_CANCELLING);
//            final Preference customizedAnc = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_PERSONALIZED_NOISE_CANCELLING);
                if (ancLevel != null) {
                    ancLevel.setVisible(mode.equals("1"));
                }
                if (transparencyLevel != null) {
                    transparencyLevel.setVisible(mode.equals("2"));
                }
                if (adaptiveAnc != null) {
                    adaptiveAnc.setVisible(mode.equals("1"));
                }
            }
        }

        if (coordinator.supports(RedmiBudsCapabilities.GestureControl)) {
            handleLongTapChange(preference, handler);
        }
    }

    private void handleLongTapChange(Preference preference, DeviceSpecificSettingsHandler handler) {
        // Check if the preference is a long tap control preference for the left or right earbud
        if (!longTapControlPreferencesLeft.contains(preference.getKey())
                && !longTapControlPreferencesRight.contains(preference.getKey())) {
            return;
        }
        // 0 - left; 1 - right
        int position = longTapControlPreferencesLeft.contains(preference.getKey()) ? 0 : 1;
        boolean ambient, anc, off;
        switch (position) {
            case 0:
                ambient = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_AMBIENT, true);
                anc = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_ANC, true);
                off = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_OFF, true);
                break;
            case 1:
            default:
                ambient = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_AMBIENT, true);
                anc = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_ANC, true);
                off = handler.getPrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_OFF, true);
                break;
        }

        // If only one option remains enabled, show a toast and prevent the user from disabling it
        if ((!ambient && !anc) || (!ambient && !off) || (!anc && !off) ) {
            ((CheckBoxPreference) preference).setChecked(true);
            Toast.makeText(handler.getContext(), "At least two options must be selected", Toast.LENGTH_SHORT).show();
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
        int value = (off ? 1 : 0) | (anc ? 2 : 0) | (ambient ? 4 : 0);
        SharedPreferences preferences = handler.getPrefs().getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        String key = position == 0 ? DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_SETTINGS_LEFT : DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_SETTINGS_RIGHT;

        editor.putString(key, String.valueOf(value));
        editor.commit();
        handler.notifyPreferenceChanged(key);
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {

        final ListPreference longPressLeft = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT);
        final ListPreference longPressRight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT);

        final Preference longPressLeftSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_SETTINGS_LEFT_CATEGORY);
        final Preference longPressRightSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_SETTINGS_RIGHT_CATEGORY);

        final Preference longPressLeftVoice = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_VOICE_LEFT);
        final Preference longPressRightVoice = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_VOICE_RIGHT);

        final ButtonGroupPreference equalizerPreset = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET);

        if (longPressLeft != null) {
            final Preference.OnPreferenceChangeListener longPressLeftButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressLeftSettings != null) {
                    longPressLeftSettings.setVisible(mode.equals("6"));
                    longPressLeftVoice.setVisible(mode.equals("0"));
                }
                return true;
            };
            longPressLeftButtonListener.onPreferenceChange(longPressLeft, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT, longPressLeftButtonListener);
        }

        if (longPressRight != null) {
            final Preference.OnPreferenceChangeListener longPressRightButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressRightSettings != null) {
                    longPressRightSettings.setVisible(mode.equals("6"));
                    longPressRightVoice.setVisible(mode.equals("0"));
                }
                return true;
            };
            longPressRightButtonListener.onPreferenceChange(longPressRight, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT, longPressRightButtonListener);
        }

        if (equalizerPreset != null) {

            final Preference.OnPreferenceChangeListener equalizerPresetListener = (preference, newVal) -> {

                final List<Preference> prefsToDisable = Arrays.asList(
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_62),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_125),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_250),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_500),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_1k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_2k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_4k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_8k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_12k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_BAND_16k)
                );

                String mode = newVal.toString();
                for (Preference pref : prefsToDisable) {
                    if (pref != null) {
                        pref.setEnabled(mode.equals("10"));
                    }
                }
                return true;
            };
            equalizerPresetListener.onPreferenceChange(equalizerPreset, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET, "0"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET, equalizerPresetListener);
        }
    }

    @Override
    public void update(DeviceSpecificSettingsHandler handler) {
        RedmiBudsCoordinator coordinator = (RedmiBudsCoordinator) device.getDeviceCoordinator();
        if (coordinator.supports(RedmiBudsCapabilities.EqualizerV1)) {
            Preference screen = handler.findPreference(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER);
            if (screen != null) {
                // Update summary to show the current preset
                Prefs prefs = handler.getPrefs();
                String preset = prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_EQUALIZER_PRESET, "0");

                // Find index of the preset in the array to get the name
                String[] values = handler.getContext().getResources().getStringArray(R.array.redmi_buds_equalizer_presets_values);
                int presetIndex = Arrays.asList(values).indexOf(preset);

                String[] presets = handler.getContext().getResources().getStringArray(R.array.redmi_buds_equalizer_presets_names);

                if (presetIndex >= 0 && presetIndex < presets.length) {
                    screen.setSummary(presets[presetIndex]);
                }
            }
        }

        if (coordinator.supports(RedmiBudsCapabilities.GestureControl)) {
            Preference left = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT_SUMMARY);
            Preference right = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT_SUMMARY);

            // Find index of the preset in the array to get the name
            String[] values = handler.getContext().getResources().getStringArray(R.array.redmi_buds_long_button_mode_values);
            String[] names = handler.getContext().getResources().getStringArray(R.array.redmi_buds_long_button_mode_names);
            int leftValue = Integer.parseInt(handler.getPrefs().getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_LEFT, "6"));
            int rightValue = Integer.parseInt(handler.getPrefs().getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_CONTROL_LONG_TAP_MODE_RIGHT, "6"));

            int leftIndex = Arrays.asList(values).indexOf(String.valueOf(leftValue));
            int rightIndex = Arrays.asList(values).indexOf(String.valueOf(rightValue));

            if (left != null && leftIndex >= 0) {
                left.setSummary(names[leftIndex]);
            }

            if (right != null && rightIndex >= 0) {
                right.setSummary(names[rightIndex]);
            }
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(device, 0);
    }

    public static final Creator<RedmiBudsSettingsCustomizer> CREATOR = new Creator<RedmiBudsSettingsCustomizer>() {
        @Override
        public RedmiBudsSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(RedmiBudsSettingsCustomizer.class.getClassLoader());
            return new RedmiBudsSettingsCustomizer(device);
        }

        @Override
        public RedmiBudsSettingsCustomizer[] newArray(final int size) {
            return new RedmiBudsSettingsCustomizer[size];
        }
    };
}
