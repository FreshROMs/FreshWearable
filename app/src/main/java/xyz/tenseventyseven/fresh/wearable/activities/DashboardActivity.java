package xyz.tenseventyseven.fresh.wearable.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.Objects;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityDashboardBinding;
import xyz.tenseventyseven.fresh.wearable.adapters.DeviceSettingsAdapter;
import xyz.tenseventyseven.fresh.wearable.components.DeviceHeader;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class DashboardActivity extends AbstractNoActionBarActivity {

    private WearActivityDashboardBinding binding;
    private DeviceHeader header;
    private ToolbarLayout toolbarLayout;
    private GBDevice device;
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
        header = new DeviceHeader(this);

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        setupToolbar();
        setupIntentListeners();

        // Request device info from service
        Application.deviceService().requestDeviceInfo();
        setupLastDevice();

        List<DeviceSetting> deviceSettings = device.getDeviceCoordinator().getDeviceSettings();
        if (deviceSettings != null) {
            setupDeviceSettings(deviceSettings);
        }
    }

    private void setupToolbar() {
        toolbarLayout = binding.toolbarLayout;
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbarLayout.setCustomTitleView(header);
        toolbarLayout.getAppBarLayout().seslSetCustomHeightProportion(true, 0.5f);
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
        toolbarLayout.setTitle(device.getAliasOrName());
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

        DeviceSettingsAdapter adapter = new DeviceSettingsAdapter(this, device, deviceSettings);
        ListView listView = binding.deviceSettings;
        listView.setAdapter(adapter);
    }
}