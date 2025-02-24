/*  Copyright (C) 2022-2024 Petr Vaněk

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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;

public class OpenFwAppInstallerActivity extends AbstractActionBarActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AppManagerActivity.class);
    private int READ_REQUEST_CODE = 42;
    private GBDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        setContentView(R.layout.activity_open_fw_app_installer);
        Button pickFileButton = findViewById(R.id.open_fw_installer_pick_button);
        TextView label = findViewById(R.id.open_fw_installer_no_device);
        label.setText(String.format(getString(R.string.open_fw_installer_select_file), device.getAliasOrName()));

        final List<GBDevice> devices = ((Application) getApplicationContext()).getDeviceManager().getSelectedDevices();
        switch (devices.size()) {
            case 0:
                label.setText(R.string.open_fw_installer_connect_minimum_one_device);
                pickFileButton.setEnabled(false);
                break;
            case 1:
                label.setText(String.format(getString(R.string.open_fw_installer_select_file), device.getAliasOrName()));
                pickFileButton.setEnabled(true);
                break;
            default:
                label.setText(R.string.open_fw_installer_connect_maximum_one_device);
                pickFileButton.setEnabled(false);
        }

        pickFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent startIntent = new Intent(OpenFwAppInstallerActivity.this, FwAppInstallerActivity.class);
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            startIntent.setAction(Intent.ACTION_VIEW);
            startIntent.setDataAndType(resultData.getData(), null);
            startActivity(startIntent);
        }
    }
}
