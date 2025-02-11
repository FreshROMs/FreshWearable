package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeslSwitchPreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.mobeta.android.dslv.DragSortListPreference;
import com.mobeta.android.dslv.DragSortListPreferenceFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.oneuiproject.oneui.preference.SeekBarPreferencePro;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.XDatePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XDatePreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialEditTextPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialListPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialMultiSelectListPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.databinding.WearPreferenceListBinding;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceShortcut;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableSettingCoordinator;

public class PreferenceList extends LinearLayout {
    private static final String SEEKBAR_SUFFIX = "_seekbar";
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
        init(context, false, null);
    }

    public void setSettings(Context context, GBDevice device, List<DeviceSetting> settings, boolean hasShortcuts) {
        this.device = device;
        this.settings = settings;
        init(context, hasShortcuts, null);
    }

    public void setSettings(Context context, GBDevice device, List<DeviceSetting> settings, String summary) {
        this.device = device;
        this.settings = settings;
        init(context, false, summary);
    }

    public void clear() {
        removeAllViews();
    }

    private void init(Context context, boolean hasShortcuts, String summary) {
        WearPreferenceListBinding binding = WearPreferenceListBinding.inflate(LayoutInflater.from(context), this, true);

        // Get fragment manager for preference list
        FragmentManager fragmentManager = FragmentManager.findFragmentManager(this);
        Fragment fragment = PreferenceListFragment.newInstance(device, settings, hasShortcuts, summary);
        fragmentManager.beginTransaction()
                .replace(binding.preferencesContainer.getId(), fragment)
                .commit();
    }

    public static class PreferenceDependency {
        String key;
        List<String> value;
        boolean isValueDependency = false;
        boolean isValueDisablePreference = false;

        public PreferenceDependency() {
        }

        public PreferenceDependency(String key) {
            this.key = key;
        }
    }

    public static class PreferenceListFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
        private GBDevice device;
        private WearableSettingCoordinator coordinator;
        private List<DeviceSetting> settings;
        private boolean hasShortcuts;
        private String summary;
        private final Map<String, List<PreferenceDependency>> dependencies = new HashMap<>();
        private final Map<String, Preference> preferenceMap = new HashMap<>();
        private final Map<String, String> defaultValues = new HashMap<>();
        private final Map<String, String[]> preferencesWithValues = new HashMap<>();
        private SharedPreferences preferences;
        private PreferenceScreen screen;

        // No-argument constructor
        public PreferenceListFragment() {
            // Required empty public constructor
        }

        public static PreferenceListFragment newInstance(GBDevice device, List<DeviceSetting> settings, boolean hasShortcuts, String summary) {
            PreferenceListFragment fragment = new PreferenceListFragment();
            Bundle args = new Bundle();
            args.putParcelable("device", device);
            args.putParcelableArrayList("settings", new ArrayList<>(settings));
            args.putBoolean("hasShortcuts", hasShortcuts);
            args.putString("summary", summary);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (getArguments() != null) {
                device = getArguments().getParcelable("device");
                settings = getArguments().getParcelableArrayList("settings");
                hasShortcuts = getArguments().getBoolean("hasShortcuts");
                summary = getArguments().getString("summary");
                preferences = Application.getDevicePrefs(device).getPreferences();
                coordinator = device.getDeviceCoordinator().getDeviceSettings(device);
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
            screen = getPreferenceScreen();

            String preferenceName = getSharedPreferencesName();
            if (preferenceName != null) {
                getPreferenceManager().setSharedPreferencesName(preferenceName);
                getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            }

            if (summary != null && !summary.isEmpty()) {
                try {
                    EditTextPreference summaryPreference = new EditTextPreference(context);
                    summaryPreference.setKey("summary");
                    summaryPreference.setSummary(summary);
                    summaryPreference.setPersistent(false);
                    summaryPreference.setCopyingEnabled(false);
                    summaryPreference.setSelectable(false);
                    summaryPreference.setLayoutResource(R.layout.wear_preference_summary);
                    summaryPreference.seslSetSummaryColor(context.getColor(R.color.wearable_primary_text));

                    preferenceScreen.addPreference(summaryPreference);
                } catch (Exception e) {
                    Log.e("PreferenceListFragment", "Error adding summary", e);
                }
            }

            if (hasShortcuts) {
                try {
                    List<DeviceShortcut> deviceShortcuts = coordinator.getShortcuts();
                    if (deviceShortcuts != null && !deviceShortcuts.isEmpty()) {
                        DeviceShortcutsPreference shortcuts = new DeviceShortcutsPreference(context);
                        shortcuts.setShorcuts(deviceShortcuts);
                        shortcuts.setOnShortcutClickListener(key -> coordinator.onShortcutClicked(context, device, key));
                        preferenceScreen.addPreference(shortcuts);
                    }
                } catch (Exception e) {
                    Log.e("PreferenceListFragment", "Error adding shortcuts", e);
                }
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

                    if (!setting.key.isEmpty()) {
                        category.setKey(setting.key);
                        addSettingDependency(category, setting);
                        preferenceMap.put(setting.key, category);
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
                        setPreferenceDefaultValue(preference, setting);
                    }

                    category.addPreference(preference);
                    defaultValues.put(preference.getKey(), setting.defaultValue);
                    preferenceMap.put(preference.getKey(), preference);
                    addSettingDependency(preference, setting);
                } catch (Exception e) {
                    Log.e("PreferenceListFragment", "Error adding preference: " + setting.key, e);
                }
            }

            // Update dependent preferences after all preferences have been added
            for (String key : dependencies.keySet()) {
                updatePreferenceDependents(key, null);
            }

            coordinator.onSettingsCreated(preferenceScreen);
        }

        private void addSettingDependency(Preference preference, DeviceSetting setting) {
            if (setting.key == null || setting.key.isEmpty()) {
                return;
            }

            // If this setting depends on another setting, add it to the list
            if (setting.dependency != null && !setting.dependency.isEmpty()) {
                List<PreferenceDependency> list = dependencies.get(setting.dependency);
                if (list == null) {
                    list = new ArrayList<>();
                    dependencies.put(setting.dependency, list);
                }

                PreferenceDependency dependency = new PreferenceDependency(preference.getKey());
                if (setting.dependencyValue.contains(",")) {
                    dependency.value = new ArrayList<>();
                    dependency.value.addAll(Arrays.asList(setting.dependencyValue.split(",")));
                } else {
                    dependency.value = new ArrayList<>();
                    dependency.value.add(setting.dependencyValue);
                }

                dependency.isValueDependency = setting.dependencyAsValue;
                dependency.isValueDisablePreference = setting.dependencyDisablesPref;
                list.add(dependency);
            }
        }

        @SuppressLint("ApplySharedPref")
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
                        if (allowPreferenceChange(checkBoxPreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
                    });
                    return checkBoxPreference;
                case SWITCH:
                    SwitchPreference switchPreference = new SwitchPreference(context);
                    switchPreference.setKey(setting.key);
                    switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (allowPreferenceChange(switchPreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
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
                        if (allowPreferenceChange(switchPreferenceScreen, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
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
                        if (allowPreferenceChange(dropDownPreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
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
                        if (allowPreferenceChange(noiseControlPreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
                    });
                    noiseControlPreference.setEntryValues(setting.entryValues);
                    return noiseControlPreference;
                case SEEKBAR_PRO:
                    SeekBarPreferencePro seekBarPreferencePro = new SeekBarPreferencePro(context, null);
                    seekBarPreferencePro.setKey(setting.entryValues != 0 ? setting.key + SEEKBAR_SUFFIX : setting.key);
                    seekBarPreferencePro.setMin(setting.min);
                    seekBarPreferencePro.setMax(setting.max);

                    if (setting.entryValues != 0) {
                        preferencesWithValues.put(setting.key, context.getResources().getStringArray(setting.entryValues));
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
                        if (!allowPreferenceChange(seekBarPreferencePro, newValue.toString())) {
                            return false;
                        }

                        if (setting.entryValues != 0) {
                            // This preference saves values as an integer, but setting tells us
                            // it should be saved as a string, so we need to convert it.
                            String[] values = preferencesWithValues.get(setting.key);
                            if (values != null) {
                                int index = (int) newValue;
                                preferences.edit().putString(setting.key, values[index]).commit();
                            }
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
                        if (allowPreferenceChange(buttonGroupPreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
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
                case DESCRIPTION:
                    EditTextPreference editTextPreference = new EditTextPreference(context);
                    editTextPreference.setPersistent(false);
                    editTextPreference.setSelectable(false);
                    editTextPreference.setCopyingEnabled(false);
                    editTextPreference.setKey(setting.key);
                    editTextPreference.setSummary(setting.summary);
                    editTextPreference.setLayoutResource(R.layout.wear_preference_summary);
                    editTextPreference.seslSetSummaryColor(context.getColor(R.color.wearable_primary_text));
                    return editTextPreference;
                case EDIT_TEXT:
                    return getEditTextPreference(setting, context);
                case SELECT_DIALOG:
                    ListPreference listPreferenceDialog = new ListPreference(context);
                    listPreferenceDialog.setKey(setting.key);
                    listPreferenceDialog.setEntries(setting.entries);
                    listPreferenceDialog.setEntryValues(setting.entryValues);
                    listPreferenceDialog.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (allowPreferenceChange(listPreferenceDialog, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
                    });
                    listPreferenceDialog.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                    listPreferenceDialog.seslSetSummaryColor(context.getColor(R.color.wearable_accent_primary));
                    return listPreferenceDialog;
                case TIME_PICKER:
                    XTimePreference xTimePreference = new XTimePreference(context, null);
                    xTimePreference.setKey(setting.key);
                    xTimePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (allowPreferenceChange(xTimePreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
                    });
                    return xTimePreference;
                case DATE_PICKER:
                    XDatePreference xDatePreference = new XDatePreference(context, null);
                    xDatePreference.setKey(setting.key);
                    xDatePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (allowPreferenceChange(xDatePreference, newValue.toString())) {
                            onPreferenceChanged(setting.key);
                            return true;
                        }
                        return false;
                    });
                    return xDatePreference;
                case DRAG_SORT:
                    return getDragSortListPreference(setting, context);
                default:
                    Log.w("PreferenceListFragment", "Unknown setting type: " + setting.type);
            }

            return null;
        }

        private DragSortListPreference getDragSortListPreference(DeviceSetting setting, Context context) {
            try {
                DragSortListPreference dragSortListPreference = new DragSortListPreference(context, null);
                dragSortListPreference.setKey(setting.key);
                dragSortListPreference.setDefaultValue(R.array.pref_transliteration_languages_default);
                dragSortListPreference.setEntries(setting.entries);
                dragSortListPreference.setEntryValues(setting.entryValues);
                dragSortListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (allowPreferenceChange(dragSortListPreference, newValue.toString())) {
                        onPreferenceChanged(setting.key);
                        return true;
                    }
                    return false;
                });
                dragSortListPreference.setSelectable(true);
                dragSortListPreference.setPersistent(true);
                dragSortListPreference.setDialogTitle(setting.title);
                return dragSortListPreference;
            } catch (Exception e) {
                Log.e("PreferenceListFragment", "Error creating DragSortListPreference", e);
                return null;
            }
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            DialogFragment dialogFragment;
            if (preference instanceof XTimePreference) {
                dialogFragment = new XTimePreferenceFragment();
            } else if (preference instanceof XDatePreference) {
                dialogFragment = new XDatePreferenceFragment();
            } else if (preference instanceof DragSortListPreference) {
                dialogFragment = new DragSortListPreferenceFragment();
            } else if (preference instanceof EditTextPreference) {
                dialogFragment = MaterialEditTextPreferenceDialogFragment.newInstance(preference.getKey());
            } else if (preference instanceof ListPreference) {
                dialogFragment = MaterialListPreferenceDialogFragment.newInstance(preference.getKey());
            } else if (preference instanceof MultiSelectListPreference) {
                dialogFragment = MaterialMultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
            } else {
                super.onDisplayPreferenceDialog(preference);
                return;
            }

            final Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        }

        @NonNull
        private EditTextPreference getEditTextPreference(DeviceSetting setting, Context context) {
            EditTextPreference editText = new EditTextPreference(context);
            editText.setKey(setting.key);
            if (setting.title != 0) {
                editText.setDialogTitle(setting.title);
            }
            if (setting.summary != 0) {
                editText.setSummary(setting.summary);
            }
            editText.setOnPreferenceChangeListener((preference, newValue) -> {
                if (allowPreferenceChange(editText, newValue.toString())) {
                    onPreferenceChanged(setting.key);
                    return true;
                }
                return false;
            });

            editText.setOnBindEditTextListener(input -> {
                switch (setting.valueKind) {
                    case INT:
                    case LONG:
                        input.setInputType(InputType.TYPE_CLASS_NUMBER);
                        input.addTextChangedListener(new MinMaxTextWatcher(input, setting.min, setting.max, true));
                        break;
                    case FLOAT:
                    case DOUBLE:
                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        input.addTextChangedListener(new MinMaxTextWatcher(input, setting.min, setting.max, false));
                        break;
                    case PASSWORD:
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        break;
                    case NUMBER_PASSWORD:
                        final List<InputFilter> inputFilters = new ArrayList<>();
                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                        if (setting.length != 0) {
                            inputFilters.add(new InputFilter.LengthFilter(setting.length));
                        }

                        input.setFilters(inputFilters.toArray(new InputFilter[0]));
                        input.addTextChangedListener(new PasswordCapabilityImpl.ExpectedLengthTextWatcher(input, setting.length));
                        break;
                    case STRING:
                    default:
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        break;
                }
            });

            if (setting.valueAsSummary) {
                editText.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
            return editText;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            if (key == null) {
                return;
            }

            if (preferenceMap.containsKey(key) &&
                    !Objects.equals(getValueOfPreference(key), getValueFromSharedPreference(key))) {
                updatePreference(key);
            }

            updatePreferenceDependents(key, null);
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public <T extends Preference> T findPreference(@NonNull CharSequence key) {
            return (T) preferenceMap.get(key.toString());
        }

        private void updatePreferenceDependents(String key, String value) {
            List<PreferenceDependency> list = dependencies.get(key);
            if (list == null || list.isEmpty()) {
                return;
            }

            if (value == null) {
                if (preferenceMap.containsKey(key)) {
                    value = getValueOfPreference(key);
                } else {
                    value = getValueFromSharedPreference(key);
                }
            }

            for (PreferenceDependency dependency : list) {
                updatePreferenceDependent(dependency, value);
            }
        }

        private void updatePreferenceDependent(PreferenceDependency dependency, String value) {
            Preference pref = findPreference(dependency.key);
            if (pref == null) return;

            if (dependency.isValueDependency) {
                setPreferenceValue(pref, value);
            } else if (dependency.isValueDisablePreference) {
                boolean enabled = dependency.value.contains(value);
                pref.setEnabled(enabled);
                updatePreferenceDependents(dependency.key, enabled ? "true" : "false");
            } else {
                boolean enabled = dependency.value.contains(value);
                pref.setVisible(enabled);
                updatePreferenceDependents(dependency.key, enabled ? "true" : "false");
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
                if (pref.getKey().endsWith(SEEKBAR_SUFFIX)) {
                    seekBarPreferencePro.setValue(findIndexFromEntries(preferencesWithValues.get(pref.getKey().replace(SEEKBAR_SUFFIX, "")), value));
                } else {
                    seekBarPreferencePro.setValue(Integer.parseInt(value));
                }
            }
        }

        private void setPreferenceDefaultValue(Preference pref, DeviceSetting setting) {
            if (pref instanceof TwoStatePreference) {
                TwoStatePreference preference = (TwoStatePreference) pref;
                preference.setDefaultValue(Boolean.parseBoolean(setting.defaultValue));
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) pref;
                switch (setting.valueKind) {
                    case INT:
                    case LONG:
                        editTextPreference.setDefaultValue(String.valueOf(Integer.parseInt(setting.defaultValue)));
                        break;
                    case FLOAT:
                    case DOUBLE:
                        editTextPreference.setDefaultValue(String.valueOf(Float.parseFloat(setting.defaultValue)));
                        break;
                    case STRING:
                    default:
                        editTextPreference.setDefaultValue(setting.defaultValue);
                        break;
                }
            } else if (pref instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) pref;
                listPreference.setDefaultValue(setting.defaultValue);
            }  else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                if (setting.entryValues != 0) {
                    seekBarPreferencePro.setDefaultValue(findIndexFromEntries(preferencesWithValues.get(setting.key), setting.defaultValue));
                } else {
                    seekBarPreferencePro.setDefaultValue(Integer.parseInt(setting.defaultValue));
                }
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
                if (pref.getKey().endsWith(SEEKBAR_SUFFIX) && preferencesWithValues.containsKey(pref.getKey().replace(SEEKBAR_SUFFIX, ""))) {
                    return preferencesWithValues.get(pref.getKey().replace(SEEKBAR_SUFFIX, ""))[seekBarPreferencePro.getValue()];
                } else {
                    return String.valueOf(seekBarPreferencePro.getValue());
                }
            }

            return "";
        }

        private String getValueFromSharedPreference(String key) {
            // Check if the key exists in the map first
            if (preferences.contains(key)) {
                Map<String, ?> all = preferences.getAll();
                return String.valueOf(all.get(key));
            }

            // Fallback to the original logic if the key is not found in the map
            Preference pref = findPreference(key);
            if (pref == null) return "";

            if (pref instanceof TwoStatePreference) {
                return String.valueOf(preferences.getBoolean(key, false));
            } else if (pref instanceof ListPreference) {
                return preferences.getString(key, defaultValues.get(key));
            } else if (pref instanceof SeekBarPreferencePro) {
                if (pref.getKey().endsWith(SEEKBAR_SUFFIX)) {
                    return preferences.getString(key.replace(SEEKBAR_SUFFIX, ""), defaultValues.get(key));
                } else {
                    String value = defaultValues.get(key);
                    return String.valueOf(preferences.getInt(key, Integer.parseInt(value != null ? value : "0")));
                }
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
                String value = preferences.getString(key, defaultValues.get(key));
                listPreference.setValue(value);
            } else if (pref instanceof SeekBarPreferencePro) {
                SeekBarPreferencePro seekBarPreferencePro = (SeekBarPreferencePro) pref;
                if (pref.getKey().endsWith(SEEKBAR_SUFFIX)) {
                    String value = preferences.getString(key.replace(SEEKBAR_SUFFIX, ""), defaultValues.get(key));
                    seekBarPreferencePro.setValue(findIndexFromEntries(preferencesWithValues.get(key.replace(SEEKBAR_SUFFIX, "")), value));
                } else {
                    String value = defaultValues.get(key);
                    seekBarPreferencePro.setValue(preferences.getInt(key, Integer.parseInt(value != null ? value : "0")));
                }
            }

        }

        private int findIndexFromEntries(String[] entries, String value) {
            if (entries == null || entries.length == 0) {
                return 0;
            }

            for (int i = 0; i < entries.length; i++) {
                if (entries[i].equals(value)) {
                    return i;
                }
            }

            return 0;
        }

        private boolean allowPreferenceChange(Preference preference, String newValue) {
            return coordinator.allowPreferenceChange(screen, preference, newValue);
        }

        private void onPreferenceChanged(String key) {
            if (device != null) {
                new Thread(() -> {
                    coordinator.onSettingChanged(device, screen, findPreference(key), key);
                    invokeLater(() -> Application.deviceService(device).onSendConfiguration(key));
                }).start();
            }
        }

        private void invokeLater(Runnable runnable) {
            if (getListView() != null) {
                getListView().post(runnable);
            }
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
                intent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, setting.screenHasSwitchBar);
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
