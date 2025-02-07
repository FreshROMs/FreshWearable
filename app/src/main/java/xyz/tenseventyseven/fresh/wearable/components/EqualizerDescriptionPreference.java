package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class EqualizerDescriptionPreference extends Preference {
    private CharSequence[] entries; // Display names
    private CharSequence[] entryValues; // Corresponding values
    private String selectedValue;

    public EqualizerDescriptionPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.component_equalizer_description);
    }

    public EqualizerDescriptionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.component_equalizer_description);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerPreviewPreference);
            entries = a.getTextArray(R.styleable.EqualizerPreviewPreference_drawableEntries);
            entryValues = a.getTextArray(R.styleable.EqualizerPreviewPreference_graphEntries);
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

    public String getValue() {
        return selectedValue;
    }

    public void setValue(String value) {
        if (value.isEmpty()) {
            return;
        }
        // No value should be set in this preference component
        selectedValue = value;
        notifyChanged();
    }

    public void setEntriesAndValues(@ArrayRes int entries, @ArrayRes int entryValues) {
        this.entries = getContext().getResources().getTextArray(entries);
        this.entryValues = getContext().getResources().getTextArray(entryValues);
        if (this.entries.length != this.entryValues.length) {
            throw new IllegalArgumentException("Entries and entryValues must be provided and have the same length.");
        }
    }
}
