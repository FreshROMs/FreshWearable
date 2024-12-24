/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe, John Vincent Corcega (TenSeventy7)

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
package xyz.tenseventyseven.fresh.wearable.activities;

import static dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.updateAdaptiveSideMargins;
import static dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.updateStatusBarVisibility;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import dev.oneuiproject.oneui.utils.DeviceLayoutUtil;
import dev.oneuiproject.oneui.widget.ScrollAwareFloatingActionButton;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.PermissionsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import xyz.tenseventyseven.fresh.wearable.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.DeviceSettingsFragment;
import xyz.tenseventyseven.fresh.wearable.components.DeviceHeader;

//TODO: extend AbstractGBActivity, but it requires actionbar that is not available
public class DashboardActivity extends AppCompatActivity implements ActivityCommon {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardActivity.class);
    private boolean isLanguageInvalidated = false;

    private DeviceManager mDeviceManager;

    public enum MENU_ENTRY_POINTS {
        DEVICE_SETTINGS,
        AUTH_SETTINGS,
        APPLICATION_SETTINGS
    }

    GBDevice mCurrentDevice;
    private final Map<GBDevice, ActivitySample> currentHRSample = new HashMap<>();
    private List<GBDevice> mDeviceList;

    Prefs mPreferences;

    private CollapsingToolbarLayout mToolbarLayout;
    private FrameLayout mBottomContainer;

    private AppBarLayout mAppBarLayout;

    private AppBarListener mAppBarListener = new AppBarListener();

    private DeviceHeader mDeviceHeader;

    private DeviceSettingsFragment mFragment;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case WearableApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(WearableApplication.getLanguage(), true);
                    break;
                case WearableApplication.ACTION_QUIT:
                    finish();
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                case WearableApplication.ACTION_NEW_DATA:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (mCurrentDevice == null) {
                        mCurrentDevice = dev;
                        if (mDeviceHeader != null) {
                            mDeviceHeader.setDevice(mCurrentDevice);
                        }
                    }

                    if (dev != null && dev.getAddress().equals(mCurrentDevice.getAddress())) {
                        if (mDeviceHeader != null) {
                            if (!mDeviceHeader.isInitialized()) {
                                mDeviceHeader.setDevice(mCurrentDevice);
                            }

                            if (mFragment != null) {
                                mFragment.setState(mCurrentDevice.isInitialized());
                            }

                            mDeviceHeader.refresh();
                        }
                    }
                    break;
            }
        }
    };

    public ActivitySample getCurrentHRSample(final GBDevice device) {
        return currentHRSample.get(device);
    }

    private void setCurrentHRSample(final GBDevice device, ActivitySample sample) {
        if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
            currentHRSample.put(device, sample);
        }
    }

    private void handleRealtimeSample(final GBDevice device, Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            setCurrentHRSample(device, sample);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setupAppBar(Configuration config) {
        updateStatusBarVisibility(this);
        updateAdaptiveSideMargins(mBottomContainer);

        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode() || DeviceLayoutUtil.INSTANCE.isTabletLayoutOrDesktop(this)) {
            mAppBarLayout.seslSetCustomHeightProportion(true, 0.4f);
            mAppBarLayout.addOnOffsetChangedListener(mAppBarListener);
            mAppBarLayout.setExpanded(true, false);
        } else {
            mAppBarLayout.setExpanded(false, false);
            mAppBarLayout.seslSetCustomHeightProportion(true, 0);
            mAppBarLayout.removeOnOffsetChangedListener(mAppBarListener);
            mBottomContainer.setAlpha(1f);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupAppBar(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CommonActivityAbstract.init(this, CommonActivityAbstract.NO_ACTIONBAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mToolbarLayout = findViewById(R.id.activity_device_collapsing_toolbar);
        mBottomContainer = findViewById(R.id.activity_device_actions);
        mAppBarLayout = findViewById(R.id.activity_device_app_bar);
        mDeviceHeader = findViewById(R.id.activity_device_info);

        mPreferences = WearableApplication.getPrefs();

        setupAppBar(getResources().getConfiguration());
        setupIntentListeners();
        showOnboardingFlow();
        showChangelog(savedInstanceState);
        setupFAB();

        // Finally, connect to the device
        initDeviceConnection(null);
    }

    // Set up local intent listener
    private void setupIntentListeners() {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(WearableApplication.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(WearableApplication.ACTION_THEME_CHANGE);
        filterLocal.addAction(WearableApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(WearableApplication.ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    private void showChangelog(Bundle savedInstanceState) {
        GBChangeLog cl = GBChangeLog.createChangeLog(this);
        boolean showChangelog = mPreferences.getBoolean("show_changelog", true);
        if (showChangelog && cl.isFirstRun() && cl.hasChanges(cl.isFirstRunEver())) {
            try {
                cl.getMaterialLogDialog().show();
            } catch (Exception ignored) {
                GB.toast(this, getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    // Open the Welcome flow on first run, only check permissions on next runs
    private void showOnboardingFlow() {
        boolean firstRun = mPreferences.getBoolean("first_run", true);
        if (firstRun) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else {
            boolean pesterWithPermissions = mPreferences.getBoolean("permission_pestering", true);
            if (pesterWithPermissions && !PermissionsUtils.checkAllPermissions(this)) {
                Intent permissionsIntent = new Intent(this, PermissionsActivity.class);
                startActivity(permissionsIntent);
            }
        }
    }

    private void initDeviceConnection(GBDevice device) {
        // Request device info from service
        WearableApplication.deviceService().requestDeviceInfo();

        mDeviceManager = WearableApplication.app().getDeviceManager();
        mDeviceList = mDeviceManager.getDevices();

        if (!mDeviceList.isEmpty()) {
            mCurrentDevice = device;
            if (mCurrentDevice == null) { // If this is run from app start, get the last connected device
                int lastDeviceIndex = mPreferences.getInt("last_device_index", 0);
                mCurrentDevice = mDeviceList.get(lastDeviceIndex);
            }

            // Get device index and save as last selected device
            int deviceIndex = mDeviceList.indexOf(mCurrentDevice);
            WearableApplication.setLastDeviceIndex(deviceIndex);

            if (!mCurrentDevice.isConnected()) {
                // Attempt to connect to the device
                WearableApplication.deviceService(mCurrentDevice).connect();
            }

            loadDeviceSettings();
            mDeviceHeader.setDevice(mCurrentDevice);
            refreshToolbar();
        }
    }

    private void setupFAB() {
        ScrollAwareFloatingActionButton fab = findViewById(R.id.activity_device_fab);
        fab.setOnClickListener(v -> {
            // TODO: Open device picker activity
            startActivity(new Intent(this, DiscoveryActivity.class));
        });
    }

    private void refreshToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.activity_device_toolbar);
        toolbar.setTitle(mCurrentDevice.getAliasOrName());
    }

    private void loadDeviceSettings() {
        mFragment = DeviceSettingsFragment.newInstance(mCurrentDevice);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_device_actions, mFragment, DeviceSettingsFragment.FRAGMENT_TAG)
                .runOnCommit(() -> mFragment.setState(mCurrentDevice.isInitialized()))
                .commit();
    }
    /**
     * Enables/Disables all child views in a view group.
     *
     * @param viewGroup the view group
     * @param enabled <code>true</code> to enable, <code>false</code> to disable
     * the views.
     */
    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        if (viewGroup == null) {
            return;
        }

        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleShortcut(getIntent());
        if (isLanguageInvalidated) {
            isLanguageInvalidated = false;
            recreate();
        }

        if (mFragment != null) {
            mFragment.update();
        }

        if (mDeviceHeader != null) {
            mDeviceHeader.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void handleShortcut(Intent intent) {
        if(ACTION_CONNECT.equals(intent.getAction())) {
            String btDeviceAddress = intent.getStringExtra("device");
            if (btDeviceAddress != null) {
                GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                if (candidate != null) {
                    initDeviceConnection(candidate);
                }
            }
        }
    }

    public void setLanguage(Locale language, boolean invalidateLanguage) {
        if (invalidateLanguage) {
            isLanguageInvalidated = true;
        }
        AndroidUtils.setLanguage(this, language);
    }

    private class AppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

            final int totalScrollRange = appBarLayout.getTotalScrollRange();
            final int abs = Math.abs(verticalOffset);

            if (abs >= totalScrollRange / 2) {
                mDeviceHeader.setAlpha(0f);
            } else if (abs == 0) {
                mDeviceHeader.setAlpha(1f);
            } else {
                float offsetAlpha = (appBarLayout.getY() / totalScrollRange);
                float arrowAlpha = 1 - (offsetAlpha * -3);
                if (arrowAlpha < 0) {
                    arrowAlpha = 0;
                } else if (arrowAlpha > 1) {
                    arrowAlpha = 1;
                }
                mDeviceHeader.setAlpha(arrowAlpha);
            }
        }
    }
}
