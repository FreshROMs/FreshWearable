package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class ButtonGroupPreference extends Preference {
    private CharSequence[] entries; // Display names
    private CharSequence[] entryValues; // Corresponding values
    private String selectedValue;

    public ButtonGroupPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.wear_preference_button_group);
    }

    public ButtonGroupPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.wear_preference_button_group);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ButtonGroupPreference);
            entries = a.getTextArray(R.styleable.ButtonGroupPreference_entries);
            entryValues = a.getTextArray(R.styleable.ButtonGroupPreference_entryValues);
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

        if (entries.length != entryValues.length) {
            Log.e("ButtonGroupPreference", "Entries and entryValues must be provided and have the same length.");
            return;
        }

        // Add buttons dynamically
        View container = holder.findViewById(R.id.button_group_container);
        if (container instanceof GridLayout) {
            GridLayout buttonContainer = (GridLayout) container;
            buttonContainer.removeAllViews(); // Clear existing buttons

            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0; i < entries.length; i++) {
                String entry = entries[i].toString();
                String value = entryValues[i].toString();

                Button button = (Button) inflater.inflate(R.layout.wear_preference_button_group_item, buttonContainer, false);
                button.setText(entry);
                button.setOnClickListener(v -> {
                    setValue(value);
                });
                buttonContainer.addView(button);
            }

            updateButtonStyles(buttonContainer);
        }
    }

    private void updateButtonStyles(GridLayout buttonContainer) {
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            View child = buttonContainer.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                String value = entryValues[i].toString();
                if (value.equals(selectedValue)) {
                    button.setBackgroundTintList(getContext().getColorStateList(R.color.wearable_accent_primary));
                    button.setTextColor(getContext().getColor(android.R.color.white));
                } else {
                    button.setBackgroundTintList(getContext().getColorStateList(R.color.wearable_btn_color_default));
                    button.setTextColor(getContext().getColor(R.color.wearable_primary_text));
                }
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
        final boolean changed = !value.equals(this.selectedValue);
        if (changed && callChangeListener(value)) {
            selectedValue = value;
            persistString(value);
            notifyChanged();
        }
    }

    public void setEntriesAndValues(@ArrayRes int entriesRes, @ArrayRes int entryValuesRes) {
        entries = getContext().getResources().getTextArray(entriesRes);
        entryValues = getContext().getResources().getTextArray(entryValuesRes);

        if (entries.length != entryValues.length) {
            throw new IllegalArgumentException("Entries and entryValues must be provided and have the same length.");
        }

        notifyChanged();
    }
}
