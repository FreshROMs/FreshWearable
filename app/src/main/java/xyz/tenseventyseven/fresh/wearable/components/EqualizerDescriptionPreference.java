package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.wearable.R;

public class EqualizerDescriptionPreference extends Preference {
    private CharSequence[] entries; // Display names
    private CharSequence[] entryValues; // Corresponding values
    private String selectedValue;

    private String watchKey;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener =
            (sharedPreferences, key) -> {
                if (key != null && key.equals(watchKey)) {
                    selectedValue = sharedPreferences.getString(key, selectedValue); // Default to offValue
                    notifyChanged(); // Refresh the UI
                }
            };

    public EqualizerDescriptionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.component_equalizer_description);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerPreviewPreference);
            entries = a.getTextArray(R.styleable.EqualizerPreviewPreference_drawableEntries);
            entryValues = a.getTextArray(R.styleable.EqualizerPreviewPreference_graphEntries);
            watchKey = a.getString(R.styleable.EqualizerPreviewPreference_watchKey);
            a.recycle();
        }

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalArgumentException("Entries and entryValues must be provided and have the same length.");
        }
    }

    @Override
    public void onClick() {
        // Override and leave empty to prevent default click behavior
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView description = (TextView) holder.findViewById(R.id.equalizer_preview_description);
        if (description != null) {
            // Set the graph image based on the selected value
            int index = -1;
            for (int i = 0; i < entryValues.length; i++) {
                if (entryValues[i].toString().equals(selectedValue)) {
                    index = i;
                    break;
                }
            }

            // Entries hold the drawable resource IDs
            if (index >= 0 && index < entries.length) {
                description.setText(entries[index]);
            }
        }
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        selectedValue = getPersistedString((String) defaultValue);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        // Register the preference change listener
        getSharedPreferences().registerOnSharedPreferenceChangeListener(prefChangeListener);
        selectedValue = getSharedPreferences().getString(watchKey, selectedValue);
        notifyChanged();
    }

    @Override
    public void onDetached() {
        super.onDetached();
        // Unregister the preference change listener
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefChangeListener);
    }

    public String getValue() {
        return selectedValue;
    }

    public void setValue(String value) {
        // No value should be set in this preference component
        selectedValue = value;
    }
}
