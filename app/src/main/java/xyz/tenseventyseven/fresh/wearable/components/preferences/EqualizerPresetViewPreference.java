package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class EqualizerPresetViewPreference extends ListPreference {
    public enum DisplayType {
        GRAPH_PREVIEW,
        DESCRIPTION
    }

    private int[] drawableEntries;
    private CharSequence[] descriptionEntries;
    private final DisplayType displayType;

    @NonNull
    public static EqualizerPresetViewPreference preview(@NonNull Context context) {
        return new EqualizerPresetViewPreference(context, DisplayType.GRAPH_PREVIEW);
    }

    @NonNull
    public static EqualizerPresetViewPreference preview(@NonNull Context context, @Nullable AttributeSet attrs) {
        return new EqualizerPresetViewPreference(context, attrs, DisplayType.GRAPH_PREVIEW);
    }

    @NonNull
    public static EqualizerPresetViewPreference description(@NonNull Context context) {
        return new EqualizerPresetViewPreference(context, DisplayType.DESCRIPTION);
    }

    @NonNull
    public static EqualizerPresetViewPreference description(@NonNull Context context, @Nullable AttributeSet attrs) {
        return new EqualizerPresetViewPreference(context, attrs, DisplayType.DESCRIPTION);
    }

    public EqualizerPresetViewPreference(@NonNull Context context, DisplayType displayType) {
        super(context);
        this.displayType = displayType;
        init(context, null);
    }

    public EqualizerPresetViewPreference(@NonNull Context context, @Nullable AttributeSet attrs, DisplayType displayType) {
        super(context, attrs);
        this.displayType = displayType;
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(getLayoutForDisplayType());

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerPreviewPreference);

            switch (displayType) {
                case GRAPH_PREVIEW:
                    initializeGraphPreview(context, a);
                    break;
                case DESCRIPTION:
                    initializeDescription(a);
                    break;
            }
            a.recycle();
            validateEntries();
        }
    }

    private int getLayoutForDisplayType() {
        switch (displayType) {
            case GRAPH_PREVIEW:
                return R.layout.wear_preference_equalizer_preview;
            case DESCRIPTION:
                return R.layout.wear_preference_equalizer_description;
            default:
                throw new IllegalStateException("Unsupported display type: " + displayType);
        }
    }

    private void initializeGraphPreview(Context context, TypedArray a) {
        CharSequence[] drawableEntriesChar = a.getTextArray(R.styleable.EqualizerPreviewPreference_drawableEntries);
        if (drawableEntriesChar != null) {
            drawableEntries = new int[drawableEntriesChar.length];
            for (int i = 0; i < drawableEntriesChar.length; i++) {
                String entry = drawableEntriesChar[i].toString();
                String resourceName = extractResourceName(entry);
                drawableEntries[i] = context.getResources().getIdentifier(
                        resourceName,
                        "drawable",
                        context.getPackageName()
                );
            }
        }
    }

    private void initializeDescription(TypedArray a) {
        descriptionEntries = a.getTextArray(R.styleable.EqualizerPreviewPreference_graphEntries);
    }

    private String extractResourceName(String entry) {
        int lastSlash = entry.lastIndexOf('/');
        int lastDot = entry.lastIndexOf('.');
        if (lastSlash >= 0 && lastDot >= 0) {
            return entry.substring(lastSlash + 1, lastDot);
        }
        return entry;
    }

    private void validateEntries() {
        CharSequence[] values = getEntryValues();
        switch (displayType) {
            case GRAPH_PREVIEW:
                if (drawableEntries == null || values == null || drawableEntries.length != values.length) {
                    throw new IllegalArgumentException("Graph preview entries and values must match in length");
                }
                break;
            case DESCRIPTION:
                if (descriptionEntries == null || values == null || descriptionEntries.length != values.length) {
                    throw new IllegalArgumentException("Description entries and values must match in length");
                }
                break;
        }
    }

    @Override
    public void onClick() {
        // Prevent default click behavior
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        String value = getValue();
        int index = findIndexOfValue(value);

        switch (displayType) {
            case GRAPH_PREVIEW:
                bindGraphPreview(holder, index);
                break;
            case DESCRIPTION:
                bindDescription(holder, index);
                break;
        }
    }

    private void bindGraphPreview(PreferenceViewHolder holder, int index) {
        ImageView graphImage = (ImageView) holder.findViewById(R.id.equalizer_preview_graph_image);
        if (graphImage != null && index >= 0 && index < drawableEntries.length) {
            graphImage.setImageResource(drawableEntries[index]);
        }
    }

    private void bindDescription(PreferenceViewHolder holder, int index) {
        TextView description = (TextView) holder.findViewById(R.id.equalizer_preview_description);
        if (description != null && index >= 0 && index < descriptionEntries.length) {
            description.setText(descriptionEntries[index]);
        }
    }

    public void setEntriesAndValues(@ArrayRes int entries, @ArrayRes int entryValues) {
        Context context = getContext();
        switch (displayType) {
            case GRAPH_PREVIEW:
                initializeGraphEntriesFromResource(context, entries);
                break;
            case DESCRIPTION:
                descriptionEntries = context.getResources().getStringArray(entries);
                break;
        }
        setEntryValues(context.getResources().getTextArray(entryValues));
        validateEntries();
    }

    private void initializeGraphEntriesFromResource(Context context, @ArrayRes int entries) {
        CharSequence[] entriesChar = context.getResources().getStringArray(entries);
        drawableEntries = new int[entriesChar.length];
        for (int i = 0; i < entriesChar.length; i++) {
            String resourceName = extractResourceName(entriesChar[i].toString());
            drawableEntries[i] = context.getResources().getIdentifier(
                    resourceName,
                    "drawable",
                    context.getPackageName()
            );
        }
    }
}
