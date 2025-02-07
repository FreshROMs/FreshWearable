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
import xyz.tenseventyseven.fresh.wearable.components.NoiseControlPreference;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceList extends LinearLayout {
    private WearPreferenceListBinding binding;
    private GBDevice device;
    private List<DeviceSetting> settings;
    private FragmentManager fragmentManager;

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
        binding = WearPreferenceListBinding.inflate(LayoutInflater.from(context), this, true);

        // Get fragment manager for preference list
        this.fragmentManager = FragmentManager.findFragmentManager(this);
        Fragment fragment = PreferenceListFragment.newInstance(device, settings);
        fragmentManager.beginTransaction()
                .replace(binding.preferencesContainer.getId(), fragment)
                .commit();
    }

    public static class PreferenceDependency {
        String key;
        String value;

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
        private Map<String, List<PreferenceDependency>> dependencies = new HashMap<>();
        private Map<String, String> defaultValues = new HashMap<>();
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
            } else {
                Log.d("PreferenceListFragment", "No arguments found");
            }
        }

        public PreferenceListFragment(GBDevice device, List<DeviceSetting> settings) {
            this.device = device;
            this.settings = settings;

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        private String getSharedPreferencesName() {
            if (device == null) {
                Log.e("PreferenceListFragment", "Device is null");
                return "default_shared_preferences";
            }

            return "devicesettings_" + device.getAddress();
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            // Create a PreferenceScreen
            setPreferencesFromResource(R.xml.wear_device_preferences, rootKey);
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
            setPreferenceScreen(preferenceScreen);
            getPreferenceManager().setSharedPreferencesName(getSharedPreferencesName());
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

            // Log the settings
            if (settings == null || settings.isEmpty()) {
                Log.e("PreferenceListFragment", "Settings list is empty or null");
                return;
            }

            // Add preferences to the list
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
                        defaultValues.put(setting.key, setting.defaultValue);
                    } else {
                        defaultValues.put(setting.key, "");
                    }

                    category.addPreference(preference);
                    addSettingDependency(setting);
                } catch (Exception e) {
                    Log.e("PreferenceListFragment", "Error adding preference: " + setting.key, e);
                }
            }
        }

        private void addSettingDependency(DeviceSetting setting) {
            if (setting.key == null || setting.key.isEmpty()) {
                return;
            }

            // Add setting to list of dependencies
            dependencies.put(setting.key, new ArrayList<>());

            // If this setting depends on another setting, add it to the list
            if (setting.dependency != null) {
                List<PreferenceDependency> list = dependencies.get(setting.dependency);
                if (list != null) {
                    PreferenceDependency dependency = new PreferenceDependency(setting.key, setting.dependencyValue);
                    if (setting.seekbarIsString) {
                        dependency.key = setting.key + "_seekbar";
                    }

                    list.add(dependency);

                    // Initial check
                    updatePreferenceDependent(setting.dependency, dependency);
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
                    checkBoxPreference.setChecked(preferences.getBoolean(setting.key, false));
                    checkBoxPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });
                    return checkBoxPreference;
                case SWITCH:
                    SwitchPreference switchPreference = new SwitchPreference(context);
                    switchPreference.setKey(setting.key);
                    switchPreference.setChecked(preferences.getBoolean(setting.key, false));
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
                    switchPreferenceScreen.setChecked(preferences.getBoolean(setting.key, false));
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
                    dropDownPreference.setValue(preferences.getString(setting.key, ""));
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
                    listPreference.setValue(preferences.getString(setting.key, ""));
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
                    noiseControlPreference.setValue(preferences.getString(setting.key, ""));
                    noiseControlPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        onPreferenceChanged(setting.key);
                        return true;
                    });

                    if (setting.entryValues != 0) {
                        noiseControlPreference.setEntryValues(setting.entryValues);
                    } else {
                        Log.e("PreferenceListFragment", "No entry values found for ANC preference");
                    }

                    return noiseControlPreference;
                case SEEKBAR_PRO:
                    SeekBarPreferencePro seekBarPreferencePro = new SeekBarPreferencePro(context, null);
                    seekBarPreferencePro.setKey(setting.seekbarIsString ? setting.key + "_seekbar" : setting.key);
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

            boolean enabled = getValueOfPreference(key).equals(dependency.value);
            pref.setVisible(enabled);
        }

        private String getValueOfPreference(String key) {
            Preference pref = findPreference(key);
            if (pref == null) return "";

            if (pref instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) pref;
                return String.valueOf(checkBoxPreference.isChecked());
            } else if (pref instanceof SwitchPreference) {
                SwitchPreference switchPreference = (SwitchPreference) pref;
                return String.valueOf(switchPreference.isChecked());
            } else if (pref instanceof SeslSwitchPreferenceScreen) {
                SeslSwitchPreferenceScreen switchPreferenceScreen = (SeslSwitchPreferenceScreen) pref;
                return String.valueOf(switchPreferenceScreen.isChecked());
            } else if (pref instanceof DropDownPreference) {
                DropDownPreference dropDownPreference = (DropDownPreference) pref;
                return dropDownPreference.getValue();
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                return listPreference.getValue();
            } else if (pref instanceof NoiseControlPreference) {
                NoiseControlPreference noiseControlPreference = (NoiseControlPreference) pref;
                return noiseControlPreference.getValue();
            } else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                return String.valueOf(seekBarPreferencePro.getValue());
            }

            return "";
        }

        private String getValueFromSharedPreference(String key) {
            Preference pref = findPreference(key);
            if (pref == null) return "";

            if (pref instanceof CheckBoxPreference) {
                return String.valueOf(preferences.getBoolean(key, false));
            } else if (pref instanceof SwitchPreference) {
                return String.valueOf(preferences.getBoolean(key, false));
            } else if (pref instanceof SeslSwitchPreferenceScreen) {
                return String.valueOf(preferences.getBoolean(key, false));
            } else if (pref instanceof DropDownPreference) {
                return preferences.getString(key, "");
            } else if (pref instanceof ListPreference) {
                return preferences.getString(key, "");
            } else if (pref instanceof NoiseControlPreference) {
                return preferences.getString(key, "");
            } else if (pref instanceof SeekBarPreferencePro) {
                return String.valueOf(preferences.getInt(key, 0));
            }

            return "";
        }

        private void updatePreference(String key) {
            Preference pref = findPreference(key);
            if (pref instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) pref;
                boolean enabled = preferences.getBoolean(key, false);
                checkBoxPreference.setChecked(enabled);
            } else if (pref instanceof SwitchPreference) {
                SwitchPreference switchPreference = (SwitchPreference) pref;
                boolean enabled = preferences.getBoolean(key, false);
                switchPreference.setChecked(enabled);
            } else if (pref instanceof SeslSwitchPreferenceScreen) {
                SeslSwitchPreferenceScreen switchPreferenceScreen = (SeslSwitchPreferenceScreen) pref;
                boolean enabled = preferences.getBoolean(key, false);
                switchPreferenceScreen.setChecked(enabled);
            } else if (pref instanceof DropDownPreference) {
                DropDownPreference dropDownPreference = (DropDownPreference) pref;
                String value = preferences.getString(key, "");
                dropDownPreference.setValue(value);
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                String value = preferences.getString(key, "");
                listPreference.setValue(value);
            } else if (pref instanceof NoiseControlPreference) {
                NoiseControlPreference noiseControlPreference = (NoiseControlPreference) pref;
                String value = preferences.getString(key, "");
                noiseControlPreference.setValue(value);
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
