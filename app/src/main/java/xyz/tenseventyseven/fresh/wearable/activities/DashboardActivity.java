package xyz.tenseventyseven.fresh.wearable.activities;

import static dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ADP_DEFAULT;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.AppBarLayout;

import java.util.List;
import java.util.Objects;

import dev.oneuiproject.oneui.utils.DeviceLayoutUtil;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityDashboardBinding;
import xyz.tenseventyseven.fresh.wearable.components.DeviceHeader;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableSettingCoordinator;

public class DashboardActivity extends AbstractNoActionBarActivity {

    private WearActivityDashboardBinding binding;
    private DeviceHeader header;
    private GBDevice device;

    private AppBarLayout appBar;

    private AppBarListener appBarListener = new AppBarListener();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                case Application.ACTION_NEW_DATA:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev == null || device == null || header == null) {
                        break;
                    }

                    if (dev.getAddress().equals(device.getAddress())) {
                        if (!header.isInitialized()) {
                            header.setDevice(device);
                        }

                        header.refresh();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = WearActivityDashboardBinding.inflate(getLayoutInflater());
        binding.coordinator.configureAdaptiveMargin(MARGIN_PROVIDER_ADP_DEFAULT, binding.dashboardContent);
        header = binding.deviceHeader;
        appBar = binding.appBar;

        setContentView(binding.getRoot());

        setupAppBar(getResources().getConfiguration());
        setupIntentListeners();

        // Request device info from service
        Application.deviceService().requestDeviceInfo();
        setupLastDevice();

        WearableSettingCoordinator deviceSettings = device.getDeviceCoordinator().getDeviceSettings();
        if (deviceSettings != null) {
            setupDeviceSettings(deviceSettings.getSettings());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupAppBar(newConfig);
    }

    @SuppressLint("RestrictedApi")
    private void setupAppBar(Configuration config) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (config.orientation != Configuration.ORIENTATION_LANDSCAPE
                    && !isInMultiWindowMode() || DeviceLayoutUtil.INSTANCE.isTabletLayoutOrDesktop(this)) {
                appBar.seslSetCustomHeightProportion(true, 0.4f);
                appBar.addOnOffsetChangedListener(appBarListener);
                appBar.setExpanded(true, false);
            } else {
                appBar.setExpanded(false, false);
                appBar.seslSetCustomHeightProportion(true, 0);
                appBar.removeOnOffsetChangedListener(appBarListener);
            }
        }
    }

    // Set up local intent listeners
    private void setupIntentListeners() {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(Application.ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    private void updateDevice(GBDevice device) {
        if (device == null) return;

        if (!device.isConnected()) {
            Application.deviceService(device).connect();
        }

        header.setDevice(device);
        binding.toolbar.setTitle(device.getAliasOrName());
        header.refresh();
    }

    private void setupLastDevice() {
        DeviceManager deviceManager = Application.app().getDeviceManager();
        List<GBDevice> devices = deviceManager.getDevices();
        String lastDeviceAddress = Application.app().getLastDeviceAddress();

        if (lastDeviceAddress != null) {
            for (GBDevice device : devices) {
                if (device.getAddress().equals(lastDeviceAddress)) {
                    this.device = device;
                    updateDevice(device);
                    break;
                }
            }
        }

        if (device == null && !devices.isEmpty()) {
            this.device = devices.get(0);
            Application.app().setLastDeviceAddress(this.device.getAddress());
            updateDevice(this.device);
        }
    }

    private void setupDeviceSettings(List<DeviceSetting> deviceSettings) {
        if (device == null) return;

        binding.preferenceList.setSettings(this, device, deviceSettings);
    }

    private class AppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

            final int totalScrollRange = appBarLayout.getTotalScrollRange();
            final int abs = Math.abs(verticalOffset);

            if (abs >= totalScrollRange / 2) {
                header.setAlpha(0f);
            } else if (abs == 0) {
                header.setAlpha(1f);
            } else {
                float offsetAlpha = (appBarLayout.getY() / totalScrollRange);
                float arrowAlpha = 1 - (offsetAlpha * -3);
                if (arrowAlpha < 0) {
                    arrowAlpha = 0;
                } else if (arrowAlpha > 1) {
                    arrowAlpha = 1;
                }
                header.setAlpha(arrowAlpha);
            }
        }
    }
}