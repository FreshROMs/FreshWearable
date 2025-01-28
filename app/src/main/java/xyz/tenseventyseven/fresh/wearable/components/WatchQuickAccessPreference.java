package xyz.tenseventyseven.fresh.wearable.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.activities.WidgetSettingsActivity;
import xyz.tenseventyseven.fresh.databinding.ComponentWatchQuickAccessBinding;

public class WatchQuickAccessPreference extends Preference {
    private boolean showWatchFace = false;
    private boolean showApps = false;
    private boolean showTiles = false;
    private boolean showWidgets = false;

    private ComponentWatchQuickAccessBinding binding;

    private GBDevice device;

    private Context context;

    public WatchQuickAccessPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (attrs != null) {
            init(context, attrs);
        }
    }

    public WatchQuickAccessPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.component_watch_quick_access);
        if (attrs != null) {
            init(context, attrs);
        }
    }

    public WatchQuickAccessPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            init(context, attrs);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WatchQuickAccessPreference);
        showWatchFace = a.getBoolean(R.styleable.WatchQuickAccessPreference_qpWatchfaces, true);
        showApps = a.getBoolean(R.styleable.WatchQuickAccessPreference_qpApps, true);
        showTiles = a.getBoolean(R.styleable.WatchQuickAccessPreference_qpTiles, true);
        showWidgets = a.getBoolean(R.styleable.WatchQuickAccessPreference_qpWidgets, true);
        a.recycle();
    }

    @Override
    public void onClick() {
        // Override and leave empty to prevent default click behavior
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        device = WearableApplication.getLastDevice(); // We assume that the last device is the currently-active one in the view
        binding = ComponentWatchQuickAccessBinding.bind(holder.itemView);

        binding.quickAccessWatchfaceLayout.setVisibility(showWatchFace ? View.VISIBLE : View.GONE);
        binding.quickAccessAppsLayout.setVisibility(showApps ? View.VISIBLE : View.GONE);
        binding.quickAccessWidgetsLayout.setVisibility(showTiles ? View.VISIBLE : View.GONE);
        binding.quickAccessQuickSettingsLayout.setVisibility(showWidgets ? View.VISIBLE : View.GONE);

        binding.quickAccessWatchfaceLayout.setOnClickListener(v -> onClickWatchFace());
        binding.quickAccessAppsLayout.setOnClickListener(v -> onClickApps());
        binding.quickAccessWidgetsLayout.setOnClickListener(v -> onClickTiles());
        binding.quickAccessQuickSettingsLayout.setOnClickListener(v -> onClickWidgets());
    }

    private void onClickWatchFace() {
        Intent startIntent;
        startIntent = new Intent(context, ConfigureAlarms.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        context.startActivity(startIntent);
    }

    private void onClickApps() {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        Class<? extends Activity> appsManagementActivity = coordinator.getAppsManagementActivity();
        if (appsManagementActivity != null) {
            Intent startIntent = new Intent(context, appsManagementActivity);
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            context.startActivity(startIntent);
        }
    }

    private void onClickTiles() {
        Intent startIntent;
        startIntent = new Intent(context, ActivityChartsActivity.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        context.startActivity(startIntent);
    }

    private void onClickWidgets() {
        Intent startIntent;
        startIntent = new Intent(context, WidgetSettingsActivity.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        context.startActivity(startIntent);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        // Do nothing
    }
}
