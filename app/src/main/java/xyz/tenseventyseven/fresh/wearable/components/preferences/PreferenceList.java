package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeslSwitchPreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.oneuiproject.oneui.preference.SeekBarPreferencePro;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.databinding.WearPreferenceListBinding;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceList extends LinearLayout {
    private static final String SEEKBAR_PREFIX = "_seekbar";
    private static final String EQUALIZER_PREVIEW_PREFIX = "_preview";
    private static final String EQUALIZER_DESCRIPTION_PREFIX = "_description";

    private GBDevice device;
    private List<DeviceSetting> settings;

    public PreferenceList(Context context) {
        super(context);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreferenceList(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSettings(Context context, GBDevice device, List<DeviceSetting> settings) {
        this.device = device;
        this.settings = settings;
        init(context);
    }

    private void init(Context context) {
        WearPreferenceListBinding binding = WearPreferenceListBinding.inflate(LayoutInflater.from(context), this, true);

        // Get fragment manager for preference list
        FragmentManager fragmentManager = FragmentManager.findFragmentManager(this);
        Fragment fragment = PreferenceListFragment.newInstance(device, settings);
        fragmentManager.beginTransaction()
                .replace(binding.preferencesContainer.getId(), fragment)
                .commit();
    }

    public static class PreferenceDependency {
        String key;
        String value;
        boolean valueDependency = false;

        public PreferenceDependency() {
        }

        public PreferenceDependency(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class PreferenceListFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
        private GBDevice device;
        private List<DeviceSetting> settings;
        private final Map<String, List<PreferenceDependency>> dependencies = new HashMap<>();
        private SharedPreferences preferences;

        // No-argument constructor
        public PreferenceListFragment() {
            // Required empty public constructor
        }

        public static PreferenceListFragment newInstance(GBDevice device, List<DeviceSetting> settings) {
            PreferenceListFragment fragment = new PreferenceListFragment();
            Bundle args = new Bundle();
            args.putParcelable("device", device);
            args.putParcelableArrayList("settings", new ArrayList<>(settings));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (getArguments() != null) {
                device = getArguments().getParcelable("device");
                settings = getArguments().getParcelableArrayList("settings");
                preferences = Application.getDevicePrefs(device).getPreferences();

                for (DeviceSetting setting : settings) {
                    dependencies.put(setting.key, new ArrayList<>());
                }
            } else {
                Log.e("PreferenceListFragment", "No arguments found");
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
            if (preferences != null) {
                preferences.registerOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
            if (preferences != null) {
                preferences.unregisterOnSharedPreferenceChangeListener(this);
            }
        }

        private String getSharedPreferencesName() {
            if (device == null) {
                Log.e("PreferenceListFragment", "Device is null");
                return null;
            }

            return "devicesettings_" + device.getAddress();
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            Context context = getContext();
            if (context == null) return;

            // Create a dummy PreferenceScreen to put our preferences to
            setPreferencesFromResource(R.xml.wear_device_preferences, rootKey);
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(context);
            setPreferenceScreen(preferenceScreen);

            String preferenceName = getSharedPreferencesName();
            if (preferenceName != null) {
                getPreferenceManager().setSharedPreferencesName(preferenceName);
                getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            }

            if (settings == null || settings.isEmpty()) {
                Log.e("PreferenceListFragment", "Settings list is empty or null");
                return;
            }

            PreferenceCategory category = new PreferenceCategory(getContext());
            preferenceScreen.addPreference(category);

            for (DeviceSetting setting : settings) {
                if (setting.type == DeviceSetting.DeviceSettingType.DIVIDER) {
                    category = new PreferenceCategory(getContext());
                    if (setting.title != 0) {
                        category.setTitle(setting.title);
                    }

                    preferenceScreen.addPreference(category);
                    continue;
                }

                try {
                    Preference preference = getPreference(setting);
                    if (preference == null) {
                        Log.e("PreferenceListFragment", "Preference is null");
                        continue;
                    }

                    if (setting.title != 0) {
                        preference.setTitle(setting.title);
                    }

                    if (setting.summary != 0 && !setting.valueAsSummary) {
                        preference.setSummary(setting.summary);
                    }

                    if (setting.icon != 0) {
                        preference.setIcon(setting.icon);
                    }

                    if (setting.defaultValue != null && !setting.defaultValue.isEmpty()) {
                        preference.setDefaultValue(setting.defaultValue);
                    }

                    category.addPreference(preference);
                    addSettingDependency(preference, setting);
                } catch (Exception e) {
                    Log.e("PreferenceListFragment", "Error adding preference: " + setting.key, e);
                }
            }

            // Update dependent preferences after all preferences have been added
            for (Map.Entry<String, List<PreferenceDependency>> entry : dependencies.entrySet()) {
                for (PreferenceDependency dependency : entry.getValue()) {
                    updatePreferenceDependent(entry.getKey(), dependency);
                }
            }
        }

        private void addSettingDependency(Preference preference, DeviceSetting setting) {
            if (setting.key == null || setting.key.isEmpty()) {
                return;
            }

            // If this setting depends on another setting, add it to the list
            if (setting.dependency != null) {
                List<PreferenceDependency> list = dependencies.get(setting.dependency);
                if (list != null) {
                    PreferenceDependency dependency = new PreferenceDependency(preference.getKey(), setting.dependencyValue);
                    if (setting.type == DeviceSetting.DeviceSettingType.EQUALIZER_PREVIEW ||
                            setting.type == DeviceSetting.DeviceSettingType.EQUALIZER_DESCRIPTION) {
                        dependency.valueDependency = true;
                    }

                    list.add(dependency);
                }
            }
        }

        private Preference getPreference(DeviceSetting setting) {
            Context context = getContext();
            if (context == null) {
                return null;
            }

            switch (setting.type) {
                case CHECKBOX:
                    CheckBoxPreference checkBoxPreference = new CheckBoxPreference(context);
                    checkBoxPreference.setKey(setting.key);
                    checkBoxPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    return checkBoxPreference;
                case SWITCH:
                    SwitchPreference switchPreference = new SwitchPreference(context);
                    switchPreference.setKey(setting.key);
                    switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });

                    if (setting.valueAsSummary) {
                        switchPreference.setSummaryProvider(preference -> {
                            if (switchPreference.isChecked()) {
                                switchPreference.seslSetSummaryColor(context.getColor(R.color.wearable_accent_primary));
                                return context.getString(R.string.function_enabled);
                            } else {
                                if (setting.summary == 0) return null;
                                switchPreference.seslSetSummaryColor(context.getColor(R.color.wearable_secondary_text));
                                return context.getString(setting.summary);
                            }
                        });
                    }

                    return switchPreference;
                case SWITCH_SCREEN:
                    SeslSwitchPreferenceScreen switchPreferenceScreen = new SeslSwitchPreferenceScreen(context);
                    switchPreferenceScreen.setKey(setting.key);
                    switchPreferenceScreen.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    switchPreferenceScreen.setOnPreferenceClickListener(preference -> {
                        launchActivity(setting);
                        return true;
                    });

                    if (setting.valueAsSummary) {
                        switchPreferenceScreen.setSummaryProvider(preference -> {
                            if (switchPreferenceScreen.isChecked()) {
                                switchPreferenceScreen.seslSetSummaryColor(context.getColor(R.color.wearable_accent_primary));
                                return context.getString(R.string.function_enabled);
                            } else {
                                if (setting.summary == 0) return null;
                                switchPreferenceScreen.seslSetSummaryColor(context.getColor(R.color.wearable_secondary_text));
                                return context.getString(setting.summary);
                            }
                        });
                    }

                    return switchPreferenceScreen;
                case DROPDOWN:
                    DropDownPreference dropDownPreference = new DropDownPreference(context);
                    dropDownPreference.setKey(setting.key);
                    dropDownPreference.setEntries(setting.entries);
                    dropDownPreference.setEntryValues(setting.entryValues);
                    dropDownPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    dropDownPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                    dropDownPreference.seslSetSummaryColor(context.getColor(R.color.wearable_accent_primary));
                    return dropDownPreference;
                case SCREEN:
                    ListPreference listPreference = new ListPreference(context) {
                        @Override
                        protected void onClick() {
                            launchActivity(setting);
                        }
                    };
                    listPreference.setKey(setting.key);
                    if (setting.valueAsSummary) {
                        listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                            onPreferenceChanged(setting.key);
                            return true;
                        });

                        if (setting.entries != 0) {
                            listPreference.setEntries(setting.entries);
                        }

                        if (setting.entryValues != 0) {
                            listPreference.setEntryValues(setting.entryValues);
                        }

                        listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                        listPreference.seslSetSummaryColor(context.getColor(R.color.wearable_accent_primary));
                    }
                    return listPreference;
                case ANC:
                    NoiseControlPreference noiseControlPreference = new NoiseControlPreference(context);
                    noiseControlPreference.setKey(setting.key);
                    noiseControlPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    noiseControlPreference.setEntryValues(setting.entryValues);
                    return noiseControlPreference;
                case SEEKBAR_PRO:
                    SeekBarPreferencePro seekBarPreferencePro = new SeekBarPreferencePro(context, null);
                    seekBarPreferencePro.setKey(setting.seekbarIsString ? setting.key + SEEKBAR_PREFIX : setting.key);
                    seekBarPreferencePro.setMin(setting.min);
                    seekBarPreferencePro.setMax(setting.max);

                    if (setting.seekbarIsString) {
                        seekBarPreferencePro.setValue(Integer.parseInt(preferences.getString(setting.key, setting.defaultValue)));
                    } else {
                        seekBarPreferencePro.setValue(preferences.getInt(setting.key, Integer.parseInt(setting.defaultValue)));
                    }

                    if (setting.leftLabel != 0) {
                        seekBarPreferencePro.setLeftLabel(context.getString(setting.leftLabel));
                    }

                    if (setting.rightLabel != 0) {
                        seekBarPreferencePro.setRightLabel(context.getString(setting.rightLabel));
                    }

                    seekBarPreferencePro.setShowTickMarks(setting.showTicks);
                    seekBarPreferencePro.setCenterBasedSeekBar(setting.centerSeekBar);
                    seekBarPreferencePro.setShowSeekBarValue(setting.showValue);

                    seekBarPreferencePro.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (setting.seekbarIsString) {
                            // This preference saves values as an integer, but setting tells us
                            // it should be saved as a string, so we need to convert it.
                            preferences.edit().putString(setting.key, String.valueOf(newValue)).commit();
                        }

                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    return seekBarPreferencePro;
                case BUTTON_GROUP:
                    ButtonGroupPreference buttonGroupPreference = new ButtonGroupPreference(context);
                    buttonGroupPreference.setKey(setting.key);
                    buttonGroupPreference.setEntriesAndValues(setting.entries, setting.entryValues);
                    buttonGroupPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    return buttonGroupPreference;
                case EQUALIZER_PREVIEW:
                case EQUALIZER_DESCRIPTION:
                    EqualizerPresetViewPreference equalizerPresetViewPreference;
                    switch (setting.type) {
                        case EQUALIZER_PREVIEW:
                            equalizerPresetViewPreference = EqualizerPresetViewPreference.preview(context);
                            equalizerPresetViewPreference.setKey(setting.key + EQUALIZER_PREVIEW_PREFIX);
                            break;
                        case EQUALIZER_DESCRIPTION:
                            equalizerPresetViewPreference = EqualizerPresetViewPreference.description(context);
                            equalizerPresetViewPreference.setKey(setting.key + EQUALIZER_DESCRIPTION_PREFIX);
                            break;
                        default:
                            Log.e("PreferenceListFragment", "Unknown equalizer setting type: " + setting.type);
                            return null;
                    }

                    equalizerPresetViewPreference.setEntriesAndValues(setting.entries, setting.entryValues);
                    equalizerPresetViewPreference.setValue(preferences.getString(setting.key, setting.defaultValue));
                    equalizerPresetViewPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });

                    return equalizerPresetViewPreference;
                default:
                    Log.w("PreferenceListFragment", "Unknown setting type: " + setting.type);
            }

            return null;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            if (key == null) {
                return;
            }

            if (!Objects.equals(getValueOfPreference(key), getValueFromSharedPreference(key))) {
                updatePreference(key);
            }

            updatePreferenceDependents(key);
        }

        private void updatePreferenceDependents(String key) {
            List<PreferenceDependency> list = dependencies.get(key);
            if (list != null) {
                for (PreferenceDependency dependency : list) {
                    updatePreferenceDependent(key, dependency);
                }
            }
        }

        private void updatePreferenceDependent(String key, PreferenceDependency dependency) {
            Preference pref = findPreference(dependency.key);
            if (pref == null) return;

            if (dependency.valueDependency) {
                setPreferenceValue(pref, getValueOfPreference(key));
            } else {
                pref.setVisible(getValueOfPreference(key).equals(dependency.value));
            }
        }

        private void setPreferenceValue(Preference pref, String value) {
            if (pref instanceof TwoStatePreference) {
                TwoStatePreference preference = (TwoStatePreference) pref;
                preference.setChecked(Boolean.parseBoolean(value));
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                listPreference.setValue(value);
            }  else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                seekBarPreferencePro.setValue(Integer.parseInt(value));
            }
        }

        private String getValueOfPreference(String key) {
            Preference pref = findPreference(key);
            if (pref == null) return "";

            if (pref instanceof TwoStatePreference) {
                TwoStatePreference twoStatePreference = (TwoStatePreference) pref;
                return String.valueOf(twoStatePreference.isChecked());
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                return listPreference.getValue();
            }  else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                return String.valueOf(seekBarPreferencePro.getValue());
            }

            return "";
        }

        private String getValueFromSharedPreference(String key) {
            Preference pref = findPreference(key);
            if (pref == null) return "";

            if (pref instanceof TwoStatePreference) {
                return String.valueOf(preferences.getBoolean(key, false));
            } else if (pref instanceof ListPreference) {
                return preferences.getString(key, "");
            } else if (pref instanceof SeekBarPreferencePro) {
                return String.valueOf(preferences.getInt(key, 0));
            }

            return "";
        }

        private void updatePreference(String key) {
            Preference pref = findPreference(key);
            if (pref instanceof TwoStatePreference) {
                TwoStatePreference twoStatePreference = (TwoStatePreference) pref;
                boolean enabled = preferences.getBoolean(key, false);
                twoStatePreference.setChecked(enabled);
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                String value = preferences.getString(key, "");
                listPreference.setValue(value);
            } else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                int value = preferences.getInt(key, 0);
                seekBarPreferencePro.setValue(value);
            }

        }

        private void onPreferenceChanged(String key) {
            invokeLater(() -> Application.deviceService(device).onSendConfiguration(key));
        }

        private void invokeLater(Runnable runnable) {
            getListView().post(runnable);
        }

        void launchActivity(DeviceSetting setting) {
            Context context = getContext();
            if (context == null) {
                return;
            }

            Intent intent = new Intent();
            if (setting.activity == null || setting.activity.isEmpty()) {
                intent = new Intent(context, PreferenceScreenActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, this.device);
                intent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, setting.type == DeviceSetting.DeviceSettingType.SWITCH_SCREEN);
                intent.putExtra(DeviceSetting.EXTRA_SETTING, setting);
            } else {
                intent.setClassName(context, setting.activity);
            }

            for (Map.Entry<String, Object> entry : setting.getExtras().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (int) value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (boolean) value);
                } else if (value instanceof Float) {
                    intent.putExtra(key, (float) value);
                } else if (value instanceof Double) {
                    intent.putExtra(key, (double) value);
                } else if (value instanceof Long) {
                    intent.putExtra(key, (long) value);
                } else if (value instanceof Short) {
                    intent.putExtra(key, (short) value);
                } else if (value instanceof Byte) {
                    intent.putExtra(key, (byte) value);
                } else if (value instanceof Character) {
                    intent.putExtra(key, (char) value);
                } else if (value instanceof Parcelable) {
                    intent.putExtra(key, (Parcelable) value);
                } else {
                    intent.putExtra(key, value.toString());
                }
            }

            getContext().startActivity(intent);
        }
    }
}
