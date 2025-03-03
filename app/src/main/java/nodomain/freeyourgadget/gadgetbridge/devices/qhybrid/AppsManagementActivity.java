/*  Copyright (C) 2021-2024 Daniel Dakhno, Hasan Ammar

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AppsManagementActivity extends AbstractActionBarActivity {
    ListView appsListView;
    String[] appNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_apps_management);

        initViews();
        refreshInstalledApps();
    }

    private void toast(String data) {
        GB.toast(data, Toast.LENGTH_LONG, GB.INFO);
    }

    private void refreshInstalledApps() {
        try {
            List<GBDevice> devices = Application.app().getDeviceManager().getSelectedDevices();
            boolean deviceFound = false;
            for(GBDevice device : devices){
                if (
                        device.getType() == DeviceType.FOSSILQHYBRID &&
                                device.isConnected() &&
                                (device.getModel().startsWith("DN") || device.getModel().startsWith("IV")) &&
                                device.getState() == GBDevice.State.INITIALIZED
                ) {
                    String installedAppsJson = device.getDeviceInfo("INSTALLED_APPS").getDetails();
                    if (installedAppsJson == null || installedAppsJson.isEmpty()) {
                        throw new RuntimeException("can't get installed apps");
                    }
                    JSONArray apps = new JSONArray(installedAppsJson);
                    appNames = new String[apps.length()];
                    for (int i = 0; i < apps.length(); i++) {
                        appNames[i] = apps.getString(i);
                    }
                    appsListView.setAdapter(new AppsListAdapter(this, appNames));
                }
                return;
            }
        } catch (JSONException e) {
            toast(e.getMessage());
            finish();
            return;
        }
        throw new RuntimeException("Device not connected");
    }

    class AppsListAdapter extends ArrayAdapter<String> {
        public AppsListAdapter(@NonNull Context context, @NonNull String[] objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater.from(getContext()));
                convertView = inflater.inflate(R.layout.fossil_hr_row_installed_app, null);
            }
            TextView nameView = convertView.findViewById(R.id.fossil_hr_row_app_name);
            nameView.setText(getItem(position));
            return nameView;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceUpdateReceiver);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceUpdateReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
    }

    BroadcastReceiver deviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshInstalledApps();
        }
    };

    private void initViews() {
        appsListView = findViewById(R.id.qhybrid_apps_list);
        appsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                PopupMenu menu = new PopupMenu(AppsManagementActivity.this, view);
                menu.getMenu()
                        .add("uninstall")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_UNINSTALL_APP);
                                intent.putExtra("EXTRA_APP_NAME", appNames[position]);
                                LocalBroadcastManager.getInstance(AppsManagementActivity.this).sendBroadcast(intent);
                                return true;
                            }
                        });
                menu.show();
            }
        });
    }
}
