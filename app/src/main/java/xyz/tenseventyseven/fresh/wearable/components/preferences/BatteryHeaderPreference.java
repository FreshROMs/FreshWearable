package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.components.CircularProgressView;
import xyz.tenseventyseven.fresh.components.HorizontalProgressView;

public class BatteryHeaderPreference extends Preference {
    private GBDevice device;
    private final TextView[] batteryPercentage = new TextView[3];
    private HorizontalProgressView batteryProgressHorizontal = null;
    private final CircularProgressView[] batteryProgressCircular = new CircularProgressView[3];
    private boolean isMultipleBatteries = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                case Application.ACTION_NEW_DATA:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev == null || device == null) {
                        break;
                    }

                    if (dev.getAddress().equals(device.getAddress())) {
                        refreshBattery();
                    }
                    break;
            }
        }
    };

    public BatteryHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatteryHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BatteryHeaderPreference(Context context) {
        super(context);
    }

    public BatteryHeaderPreference(Context context, GBDevice device) {
        super(context);
        this.device = device;

        // Inflate the layout
        if (device.getDeviceCoordinator().getBatteryCount(device) > 1) {
            isMultipleBatteries = true;
            setLayoutResource(R.layout.wear_preference_battery_header_multiple);
        } else {
            setLayoutResource(R.layout.wear_preference_battery_header);
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // Register the broadcast receiver
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(Application.ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filterLocal);
    }

    @Override
    public void onDetached() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        super.onDetached();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.seslSetViewHolderRecoilEffectEnabled(false);
        if (isMultipleBatteries) {
            onBindViewHolderMultiple(holder);
        } else {
            onBindViewHolderSingle(holder);
        }
    }

    private void onBindViewHolderMultiple(@NonNull PreferenceViewHolder holder) {
        // Find the views
        LinearLayout[] batteryFrames = new LinearLayout[3];
        ImageView[] batteryIcons = new ImageView[3];

        batteryFrames[0] = (LinearLayout) holder.findViewById(R.id.battery_percentage_1);
        batteryFrames[1] = (LinearLayout) holder.findViewById(R.id.battery_percentage_2);
        batteryFrames[2] = (LinearLayout) holder.findViewById(R.id.battery_percentage_3);

        if (batteryFrames[0] == null || batteryFrames[1] == null || batteryFrames[2] == null) {
            return;
        }

        batteryIcons[0] = (ImageView) holder.findViewById(R.id.battery_percentage_1_icon);
        batteryIcons[1] = (ImageView) holder.findViewById(R.id.battery_percentage_2_icon);
        batteryIcons[2] = (ImageView) holder.findViewById(R.id.battery_percentage_3_icon);

        if (batteryIcons[0] == null || batteryIcons[1] == null || batteryIcons[2] == null) {
            return;
        }

        batteryPercentage[0] = (TextView) holder.findViewById(R.id.battery_percentage_1_text);
        batteryPercentage[1] = (TextView) holder.findViewById(R.id.battery_percentage_2_text);
        batteryPercentage[2] = (TextView) holder.findViewById(R.id.battery_percentage_3_text);

        if (batteryPercentage[0] == null || batteryPercentage[1] == null || batteryPercentage[2] == null) {
            return;
        }

        batteryProgressCircular[0] = (CircularProgressView) holder.findViewById(R.id.battery_percentage_1_progress);
        batteryProgressCircular[1] = (CircularProgressView) holder.findViewById(R.id.battery_percentage_2_progress);
        batteryProgressCircular[2] = (CircularProgressView) holder.findViewById(R.id.battery_percentage_3_progress);

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        switch (coordinator.getBatteryCount(device)) {
            case 3:
                batteryFrames[2].setVisibility(View.VISIBLE);
                batteryIcons[2].setImageResource(coordinator.getBatteryIconResource(2));
            case 2:
                batteryFrames[1].setVisibility(View.VISIBLE);
                batteryIcons[1].setImageResource(coordinator.getBatteryIconResource(1));
            case 1:
            default:
                batteryFrames[0].setVisibility(View.VISIBLE);
                batteryIcons[0].setImageResource(coordinator.getBatteryIconResource(0));
        }

        // Set the battery progress
        refreshBattery();
    }

    private void refreshBattery() {
        if (isMultipleBatteries) {
            for (int i = 0; i < device.getDeviceCoordinator().getBatteryCount(device); i++) {
                if (batteryProgressCircular[i] == null || batteryPercentage[i] == null) {
                    continue;
                }

                batteryProgressCircular[i].setProgress(device.getBatteryLevel(i));
                batteryProgressCircular[i].setProgressBackgroundColor(R.color.wearable_battery_progress_background, true);

                int color = device.getBatteryLevel(i) < 20 ? R.color.wearable_battery_progress_low : R.color.wearable_battery_progress_normal;
                batteryProgressCircular[i].setProgressColor(color, true);

                batteryPercentage[i].setText(getContext().getString(R.string.wear_device_battery_status_single, device.getBatteryLevel(i)));
            }
        } else {
            if (batteryProgressHorizontal == null || batteryPercentage[0] == null) {
                return;
            }

            batteryProgressHorizontal.setProgress(device.getBatteryLevel() / 100f);
            batteryPercentage[0].setText(getContext().getString(R.string.wear_device_battery_status_single, device.getBatteryLevel()));

            int color = device.getBatteryLevel() < 20 ? R.color.wearable_battery_progress_low : R.color.wearable_battery_progress_normal;
            batteryProgressHorizontal.setProgressColor(color, true);
        }
    }

    private void onBindViewHolderSingle(@NonNull PreferenceViewHolder holder) {
        // Find the views
        batteryPercentage[0] = (TextView) holder.findViewById(R.id.battery_title);
        batteryProgressHorizontal = (HorizontalProgressView) holder.findViewById(R.id.battery_progress);

        // Set the battery percentage
        batteryPercentage[0].setText(getContext().getString(R.string.wear_device_battery_status_single, device.getBatteryLevel()));

        // Set the battery progress
        refreshBattery();
    }

    // This is the method that is called when the preference is clicked
    @Override
    public void onClick() {
        // Do nothing
    }
}
