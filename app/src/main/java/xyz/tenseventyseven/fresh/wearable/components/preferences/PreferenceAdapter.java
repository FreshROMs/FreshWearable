package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.util.SeslRoundedCorner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceAdapter extends RecyclerView.Adapter<PreferenceAdapter.PreferenceViewHolder> {
    private final Context context;
    private final GBDevice device;
    private final List<DeviceSetting> settings;

    public PreferenceAdapter(Context context, GBDevice device, List<DeviceSetting> settings) {
        this.context = context;
        this.device = device;
        this.settings = settings;
    }

    @NonNull
    @Override
    public PreferenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.wear_preference_item, parent, false);
        return new PreferenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        DeviceSetting setting = settings.get(position);
        AbstractPreference preference;
        switch (Objects.requireNonNull(setting).type) {
            case CHECKBOX:
                preference = new CheckBoxPreference(context, device, setting);
                break;
            case SWITCH:
            case SWITCH_SCREEN:
                preference = new SwitchPreference(context, device, setting);
                break;
            case DIVIDER:
                preference = new DividerPreference(context);
                break;
            case SCREEN:
                preference = new ListPreference(context, device, setting);
                break;
            default:
                throw new IllegalArgumentException("Unknown setting type: " + setting.type);
        }

        int corners = getCorners(position);
        preference.seslSetRoundCorners(corners);
        holder.bind(preference);
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    private int getCorners(int position) {
        int corners = SeslRoundedCorner.ROUNDED_CORNER_NONE;

        boolean isFirstItem = position == 0;
        boolean isLastItem = position == settings.size() - 1;
        boolean nextIsDivider = position < settings.size() - 1 && settings.get(position + 1).type == DeviceSetting.DeviceSettingType.DIVIDER;
        boolean prevIsDivider = position > 0 && settings.get(position - 1).type == DeviceSetting.DeviceSettingType.DIVIDER;

        if (isFirstItem || prevIsDivider) {
            corners |= SeslRoundedCorner.ROUNDED_CORNER_TOP_RIGHT | SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT;
        }

        if (isLastItem || nextIsDivider) {
            corners |= SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_RIGHT | SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_LEFT;
        }

        return corners;
    }

    public static class PreferenceViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup container;

        public PreferenceViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.preference_container);
        }

        public void bind(AbstractPreference preference) {
            container.removeAllViews();
            container.addView(preference);
        }
    }
}
