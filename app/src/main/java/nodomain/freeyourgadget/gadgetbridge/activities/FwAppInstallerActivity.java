/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo, Lem Dulfo, Petr Vaněk, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ItemWithDetailsAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;


public class FwAppInstallerActivity extends AbstractActionBarActivity implements InstallActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);
    private static final String ITEM_DETAILS = "details";

    private TextView fwAppInstallTextView;
    private ImageView previewImage;
    private Button installButton;
    private Button closeButton;
    private Uri uri;
    private GBDevice device;
    private InstallHandler installHandler;
    private boolean mayConnect;

    private ProgressBar progressBar;
    private TextView progressText;
    private ListView itemListView;
    private final List<ItemWithDetails> items = new ArrayList<>();
    private ItemWithDetailsAdapter itemAdapter;

    private ListView detailsListView;
    private ItemWithDetailsAdapter detailsAdapter;
    private ArrayList<ItemWithDetails> details = new ArrayList<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                final GBDevice changedDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (changedDevice != null && changedDevice.equals(device)) {
                    refreshBusyState(device);
                    if (!device.isInitialized()) {
                        setInstallEnabled(false);
                        if (mayConnect) {
                            GB.toast(FwAppInstallerActivity.this, getString(R.string.connecting), Toast.LENGTH_SHORT, GB.INFO);
                            connect();
                        } else {
                            setInfoText(getString(R.string.fwappinstaller_connection_state, device.getStateString(context)));
                        }
                    } else {
                        validateInstallation();
                    }
                }
            } else if (GB.ACTION_SET_PROGRESS_BAR.equals(action)) {
                if (intent.hasExtra(GB.PROGRESS_BAR_INDETERMINATE)) {
                    setProgressIndeterminate(intent.getBooleanExtra(GB.PROGRESS_BAR_INDETERMINATE, false));
                }

                if (intent.hasExtra(GB.PROGRESS_BAR_PROGRESS)) {
                    setProgressIndeterminate(false);
                    setProgressBar(intent.getIntExtra(GB.PROGRESS_BAR_PROGRESS, 0));
                }
            } else if (GB.ACTION_SET_PROGRESS_TEXT.equals(action)) {
                if (intent.hasExtra(GB.DISPLAY_MESSAGE_MESSAGE)) {
                    setProgressText(intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE));
                }
            } else if (GB.ACTION_SET_INFO_TEXT.equals(action)) {
                if (intent.hasExtra(GB.DISPLAY_MESSAGE_MESSAGE)) {
                    setInfoText(intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE));
                }
            } else if (GB.ACTION_DISPLAY_MESSAGE.equals(action)) {
                String message = intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE);
                int severity = intent.getIntExtra(GB.DISPLAY_MESSAGE_SEVERITY, GB.INFO);
                addMessage(message, severity);
            } else if (GB.ACTION_SET_FINISHED.equals(action)) {
                setProgressBarVisibility(false);
                setInstallEnabled(false);
                setCloseEnabled(true);
            }
        }
    };

    private void refreshBusyState(GBDevice dev) {
        if (dev.isConnecting() || dev.isBusy()) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            boolean wasBusy = progressBar.getVisibility() != View.GONE;
            if (wasBusy) {
                progressBar.setVisibility(View.GONE);
                // done!
            }
        }
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(indeterminate);
    }

    public void setProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    public void setProgressText(String text) {
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(text);
    }

    private void connect() {
        mayConnect = false; // only do that once per #onCreate
        Application.deviceService(device).connect();
    }

    private void validateInstallation() {
        if (installHandler != null) {
            installHandler.validateInstallation(this, device);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appinstaller);

        GBDevice dev = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (dev != null) {
            device = dev;
        }
        if (savedInstanceState != null) {
            details = savedInstanceState.getParcelableArrayList(ITEM_DETAILS);
            if (details == null) {
                details = new ArrayList<>();
            }
        }

        mayConnect = true;
        itemListView = findViewById(R.id.itemListView);
        itemAdapter = new ItemWithDetailsAdapter(this, items);
        itemListView.setAdapter(itemAdapter);
        fwAppInstallTextView = findViewById(R.id.infoTextView);
        previewImage = findViewById(R.id.previewImage);
        installButton = findViewById(R.id.installButton);
        closeButton = findViewById(R.id.closeButton);
        progressBar = findViewById(R.id.installProgressBar);
        progressText = findViewById(R.id.installProgressText);
        detailsListView = findViewById(R.id.detailsListView);
        detailsAdapter = new ItemWithDetailsAdapter(this, details);
        detailsAdapter.setSize(ItemWithDetailsAdapter.SIZE_SMALL);
        detailsListView.setAdapter(detailsAdapter);

        setInstallEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filter.addAction(GB.ACTION_DISPLAY_MESSAGE);
        filter.addAction(GB.ACTION_SET_PROGRESS_BAR);
        filter.addAction(GB.ACTION_SET_PROGRESS_TEXT);
        filter.addAction(GB.ACTION_SET_INFO_TEXT);
        filter.addAction(GB.ACTION_SET_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInstallEnabled(false);
                installHandler.onStartInstall(device);
                Application.deviceService(device).onInstallApp(uri);
            }
        });

        closeButton.setOnClickListener(v -> finish());

        uri = getIntent().getData();
        if (uri == null) { // For "share" intent
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }
        installHandler = findInstallHandlerFor(uri);
        if (installHandler == null) {
            setInfoText(getString(R.string.installer_activity_unable_to_find_handler));
        } else {
            setInfoText(getString(R.string.installer_activity_wait_while_determining_status));

            List<GBDevice> selectedDevices = Application.app().getDeviceManager().getSelectedDevices();
            if(selectedDevices.size() == 0){
                GB.toast(getString(R.string.open_fw_installer_connect_minimum_one_device), Toast.LENGTH_LONG, GB.ERROR);
                finish();
                return;
            }
            if(selectedDevices.size() != 1){
                GB.toast(getString(R.string.open_fw_installer_connect_maximum_one_device), Toast.LENGTH_LONG, GB.ERROR);
                finish();
                return;
            }
            device = selectedDevices.get(0);

            // needed to get the device
            if (device == null || !device.isConnected()) {
                connect();
            } else {
                Application.deviceService(device).requestDeviceInfo();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ITEM_DETAILS, details);
    }

    private InstallHandler findInstallHandlerFor(Uri uri) {
        for (DeviceCoordinator coordinator : getAllCoordinatorsConnectedFirst()) {
            InstallHandler handler = coordinator.findInstallHandler(uri, this);
            if (handler != null) {
                LOG.info("Found install handler {} from {}", handler.getClass(), coordinator.getClass());
                return handler;
            }
        }
        return null;
    }

    private List<DeviceCoordinator> getAllCoordinatorsConnectedFirst() {
        DeviceManager deviceManager = ((Application) getApplicationContext()).getDeviceManager();
        List<DeviceCoordinator> connectedCoordinators = new ArrayList<>();
        List<DeviceCoordinator> allCoordinators = new ArrayList<>(DeviceType.values().length);
        for(DeviceType type : DeviceType.values()){
            allCoordinators.add(type.getDeviceCoordinator());
        }
        List<DeviceCoordinator> sortedCoordinators = new ArrayList<>(allCoordinators.size());

        List<GBDevice> devices = deviceManager.getSelectedDevices();
        for(GBDevice connectedDevice : devices){
            if (connectedDevice.isConnected()) {
                DeviceCoordinator coordinator = connectedDevice.getDeviceCoordinator();
                if (coordinator != null) {
                    connectedCoordinators.add(coordinator);
                }
            }
        }


        sortedCoordinators.addAll(connectedCoordinators);
        for (DeviceCoordinator coordinator : allCoordinators) {
            if (!connectedCoordinators.contains(coordinator)) {
                sortedCoordinators.add(coordinator);
            }
        }
        return sortedCoordinators;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void setInfoText(String text) {
        fwAppInstallTextView.setText(text);
    }

    @Override
    public void setPreview(@Nullable final Bitmap bitmap) {
        previewImage.setImageBitmap(bitmap);
        if (previewImage == null) {
            previewImage.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public CharSequence getInfoText() {
        return fwAppInstallTextView.getText();
    }

    @Override
    public void setInstallEnabled(boolean enable) {
        boolean enabled = device != null && device.isConnected() && enable;
        installButton.setEnabled(enabled);
        installButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            setCloseEnabled(false);
        }
    }

    @Override
    public void setCloseEnabled(boolean enable) {
        closeButton.setEnabled(enable);
        closeButton.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void clearInstallItems() {
        items.clear();
        itemAdapter.notifyDataSetChanged();
    }

    @Override
    public void setInstallItem(ItemWithDetails item) {
        items.clear();
        items.add(item);
        itemAdapter.notifyDataSetChanged();
    }

    private void addMessage(String message, int severity) {
        details.add(new GenericItem(message));
        detailsAdapter.notifyDataSetChanged();
    }
}
