/*  Copyright (C) 2018-2024 Andreas Böhler, Daniele Gobbetti, José Rebelo,
    maxirnilian, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.watch9;

import static nodomain.freeyourgadget.gadgetbridge.util.BondingUtil.STATE_DEVICE_CANDIDATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;

public class Watch9PairingActivity extends AbstractActionBarActivity implements BondingInterface {
    private static final Logger LOG = LoggerFactory.getLogger(Watch9PairingActivity.class);

    private final BroadcastReceiver pairingReceiver = BondingUtil.getPairingReceiver(this);
    private TextView message;
    private GBDeviceCandidate deviceCandidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch9_pairing);

        message = findViewById(R.id.watch9_pair_message);

        deviceCandidate = getIntent().getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }

        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivityV2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        startConnecting(deviceCandidate);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE_CANDIDATE, deviceCandidate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
    }

    private void startConnecting(GBDeviceCandidate deviceCandidate) {
        message.setText(getString(R.string.pairing, deviceCandidate));

        registerBroadcastReceivers();
        BondingUtil.connectThenComplete(this, deviceCandidate);
    }

    @Override
    public void onBondingComplete(boolean success) {
        startActivity(new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceCandidate;
    }

    @Override
    public String getMacAddress() {
        return deviceCandidate.getDevice().getAddress();
    }

    @Override
    public boolean getAttemptToConnect() {
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), pairingReceiver);
    }

    @Override
    public void registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(pairingReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void onResume() {
        registerBroadcastReceivers();
        super.onResume();
    }

    @Override
    protected void onStart() {
        registerBroadcastReceivers();
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        super.onStop();
    }

    @Override
    protected void onPause() {
        // WARN: Do not remove listeners on pause
        // Bonding process can pause the activity and you might miss broadcasts
        super.onPause();
    }
}
