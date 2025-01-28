package xyz.tenseventyseven.fresh.wearable.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.adapters.DeviceListItemAdapter;
import xyz.tenseventyseven.fresh.databinding.ActivityDeviceListBinding;

public class DeviceListActivity extends CommonActivityAbstract implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {
    private ActivityDeviceListBinding binding;

    private DeviceManager mDeviceManager;
    private List<GBDevice> mConnectedDevices = new ArrayList<>();
    private List<GBDevice> mDisconnectedDevices = new ArrayList<>();

    private DeviceListItemAdapter mConnectedDevicesAdapter;
    private DeviceListItemAdapter mDisconnectedDevicesAdapter;

    private boolean mIsRemovingDevices = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDeviceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.deviceListToolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbarMenu();
        setupDeviceList();
        setupIntentListeners();

        updateDeviceList();
        tryConnectAllDevices();
    }

    private void tryConnectAllDevices() {
        // Try to connect to at most 3 disconnected devices
        // start from the last of the list, assuming that the user is more likely to use
        // the last paired device(s)
        int count = 0;
        for (int i = mDisconnectedDevices.size() - 1; i >= 0; i--) {
            GBDevice device = mDisconnectedDevices.get(i);
            if (count >= 3) {
                break;
            }

            WearableApplication.deviceService(device).connect();
            count++;
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsRemovingDevices) {
            hideRemoveDevices();
            return;
        }

        super.onBackPressed();
    }

    private void setupDeviceList() {
        mDeviceManager = WearableApplication.app().getDeviceManager();
        mConnectedDevicesAdapter = new DeviceListItemAdapter(this, mConnectedDevices);
        mDisconnectedDevicesAdapter = new DeviceListItemAdapter(this, mDisconnectedDevices);

        binding.deviceListConnected.setAdapter(mConnectedDevicesAdapter);
        binding.deviceListDisconnected.setAdapter(mDisconnectedDevicesAdapter);

        binding.deviceListConnected.setOnItemClickListener(this);
        binding.deviceListDisconnected.setOnItemClickListener(this);

        binding.deviceListConnected.setOnItemLongClickListener(this);
        binding.deviceListDisconnected.setOnItemLongClickListener(this);
    }

    private void updateDeviceList() {
        List<GBDevice> devices = mDeviceManager.getDevices();
        mConnectedDevices.clear();
        mDisconnectedDevices.clear();

        mConnectedDevicesAdapter.notifyDataSetChanged();
        mDisconnectedDevicesAdapter.notifyDataSetChanged();

        for (GBDevice device : devices) {
            if (device.isConnected()) {
                mConnectedDevices.add(device);
            } else {
                mDisconnectedDevices.add(device);
            }
        }

        mConnectedDevicesAdapter.notifyDataSetChanged();
        mDisconnectedDevicesAdapter.notifyDataSetChanged();
    }

    private void setupIntentListeners() {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(WearableApplication.ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    private void setupToolbarMenu() {
        addMenuProvider(menuProvider);
        binding.deleteDevicesButtonContainer.setVisibility(View.GONE);
        binding.deleteDevicesButton.setOnClickListener(v -> removeSelectedDevices());
    }

    private void showAppSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showDeviceDiscovery() {
        startActivity(new Intent(this, DiscoveryActivity.class));
    }

    private void showRemoveDevices() {
        mIsRemovingDevices = true;
        binding.deleteDevicesButtonContainer.setVisibility(View.VISIBLE);

        // Show checkboxes for device removal
        mConnectedDevicesAdapter.setSelectMode(true);
        mDisconnectedDevicesAdapter.setSelectMode(true);

        mConnectedDevicesAdapter.notifyDataSetChanged();
        mDisconnectedDevicesAdapter.notifyDataSetChanged();
    }

    private void hideRemoveDevices() {
        mIsRemovingDevices = false;

        mConnectedDevicesAdapter.setSelectMode(false);
        mDisconnectedDevicesAdapter.setSelectMode(false);

        mConnectedDevicesAdapter.notifyDataSetChanged();
        mDisconnectedDevicesAdapter.notifyDataSetChanged();

        binding.deleteDevicesButtonContainer.setVisibility(View.GONE);
    }

    private void showFailedToast(Exception ex) {
        GB.toast(this, getString(R.string.error_deleting_device, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
    }

    private void removeSelectedDevices() {
        List<GBDevice> selectedDevices = new ArrayList<>();
        selectedDevices.addAll(mConnectedDevicesAdapter.getSelectedDevices());
        selectedDevices.addAll(mDisconnectedDevicesAdapter.getSelectedDevices());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove devices");
        builder.setMessage(R.string.controlcenter_delete_device_dialogmessage);
        builder.setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    for (GBDevice device : selectedDevices) {
                        DeviceCoordinator coordinator = device.getDeviceCoordinator();
                        coordinator.deleteDevice(device);
                        DeviceHelper.getInstance().removeBond(device);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            removeDynamicShortcut(device);
                        }
                    }
                } catch (Exception ex) {
                    showFailedToast(ex);
                } finally {
                    hideRemoveDevices();
                    updateDeviceList();
                }
            }
        });
        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
            // do nothing
        });

        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void removeDynamicShortcut(GBDevice device) {
        final ShortcutManager shortcutManager = (ShortcutManager) getApplicationContext().getSystemService(Context.SHORTCUT_SERVICE);

        shortcutManager.removeDynamicShortcuts(Collections.singletonList(device.getAddress()));
    }

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fresh_wearable_device_list_menu, menu);
            menu.findItem(R.id.device_list_add_device).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.device_list_settings) {
                showAppSettings();
                return true;
            } else if (item.getItemId() == R.id.device_list_add_device) {
                showDeviceDiscovery();
                return true;
            } else if (item.getItemId() == R.id.device_list_remove_device) {
                showRemoveDevices();
                return true;
            }

            return true;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                case WearableApplication.ACTION_NEW_DATA:
                    updateDeviceList();
                    break;
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GBDevice device = (GBDevice) parent.getItemAtPosition(position);
        if (device == null) {
            return;
        }

        if (mIsRemovingDevices) {
            if (parent == binding.deviceListConnected) {
                mConnectedDevicesAdapter.toggleSelectedDevice(device, view);
            } else if (parent == binding.deviceListDisconnected) {
                mDisconnectedDevicesAdapter.toggleSelectedDevice(device, view);
            }

            return;
        }

        WearableApplication.setLastDevice(device);
        finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mIsRemovingDevices) { // Already in remove mode
            return false;
        }

        GBDevice device = (GBDevice) parent.getItemAtPosition(position);
        if (device == null) {
            return false;
        }

        showRemoveDevices();
        if (parent == binding.deviceListConnected) {
            mConnectedDevicesAdapter.toggleSelectedDevice(device, view);
        } else if (parent == binding.deviceListDisconnected) {
            mDisconnectedDevicesAdapter.toggleSelectedDevice(device, view);
        }

        return true;
    }
}