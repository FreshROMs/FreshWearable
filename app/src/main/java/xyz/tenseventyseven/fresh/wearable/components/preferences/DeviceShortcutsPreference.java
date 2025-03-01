package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.components.DashboardShortcut;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceShortcut;

public class DeviceShortcutsPreference extends Preference {
    public interface OnShortcutClickListener {
        void onShortcutClicked(String key);
    }

    private OnShortcutClickListener onShortcutClickListener;

    private final List<DeviceShortcut> shortcuts = new ArrayList<>();
    private GridLayout layout;

    public DeviceShortcutsPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public DeviceShortcutsPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DeviceShortcutsPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DeviceShortcutsPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.wear_activity_dashboard_shortcuts);
    }

    @Override
    protected void onClick() {
        // Do nothing
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.seslSetViewHolderRecoilEffectEnabled(false);

        layout = (GridLayout) holder.findViewById(R.id.dashboard_shortcuts);
        if (layout == null) {
            Log.e("DeviceShortcutsPreference", "GridLayout not found");
            return;
        }

        layout.removeAllViews();
        for (DeviceShortcut shortcut : shortcuts) {
            DashboardShortcut view = new DashboardShortcut(getContext());
            view.setIcon(shortcut.icon);
            view.setTitle(shortcut.title);
            view.setOnShortcutClickListener(() -> {
                if (onShortcutClickListener != null) {
                    onShortcutClickListener.onShortcutClicked(shortcut.key);
                }

                return true;
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            view.setLayoutParams(params);

            layout.addView(view);
        }
    }

    public void setShorcuts(List<DeviceShortcut> shortcuts) {
        this.shortcuts.clear();
        this.shortcuts.addAll(shortcuts);
        notifyChanged();
    }

    public void setOnShortcutClickListener(OnShortcutClickListener listener) {
        this.onShortcutClickListener = listener;
        notifyChanged();
    }
}
