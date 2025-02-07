package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class EqualizerPreviewPreference extends Preference {
    private int[] entries; // Display names
    private CharSequence[] entryValues; // Corresponding values
    private String selectedValue;

    public EqualizerPreviewPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.wear_preference_equalizer_preview);
    }

    public EqualizerPreviewPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.wear_preference_equalizer_preview);

        CharSequence[] entriesChar = null;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerPreviewPreference);
            entriesChar = a.getTextArray(R.styleable.EqualizerPreviewPreference_drawableEntries);
            entryValues = a.getTextArray(R.styleable.EqualizerPreviewPreference_graphEntries);
            a.recycle();
        }

        // Convert the entries (which are resource references) to drawable resource IDs
        if (entriesChar != null) {
            entries = new int[entriesChar.length];
            for (int i = 0; i < entriesChar.length; i++) {
                // Remove path and trailing extension
                String entry = entriesChar[i].toString();
                int lastSlash = entry.lastIndexOf('/');
                int lastDot = entry.lastIndexOf('.');
                if (lastSlash >= 0 && lastDot >= 0) {
                    entriesChar[i] = entry.substring(lastSlash + 1, lastDot);
                }

                entries[i] = context.getResources().getIdentifier(entriesChar[i].toString(), "drawable", context.getPackageName());
                // Log.d("EqualizerPreviewPreference", "Drawable resource ID: " + entriesChar[i]);
            }
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

        ImageView graphImage = (ImageView) holder.findViewById(R.id.equalizer_preview_graph_image);
        if (graphImage != null) {
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
                graphImage.setImageResource(entries[index]);
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
        // Get the drawable resource IDs from
        CharSequence[] entriesChar = getContext().getResources().getStringArray(entries);
        this.entryValues = getContext().getResources().getTextArray(entryValues);

        // Convert the entries (which are resource references) to drawable resource IDs
        this.entries = new int[entriesChar.length];
        for (int i = 0; i < entriesChar.length; i++) {
            // Remove path and trailing extension
            String entry = entriesChar[i].toString();
            int lastSlash = entry.lastIndexOf('/');
            int lastDot = entry.lastIndexOf('.');
            if (lastSlash >= 0 && lastDot >= 0) {
                entriesChar[i] = entry.substring(lastSlash + 1, lastDot);
            }

            this.entries[i] = getContext().getResources().getIdentifier(entriesChar[i].toString(), "drawable", getContext().getPackageName());
            // Log.d("EqualizerPreviewPreference", "Drawable resource ID: " + entriesChar[i]);
        }
    }
}
