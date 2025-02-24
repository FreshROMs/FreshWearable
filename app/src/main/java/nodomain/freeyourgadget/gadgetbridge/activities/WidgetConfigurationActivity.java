/*  Copyright (C) 2020-2024 Arjan Schrijver, Daniel Dakhno, Petr Vaněk

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
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.common.AppActivity;

public class WidgetConfigurationActivity extends Activity implements AppActivity {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetConfigurationActivity.class);
    int mAppWidgetId;

    LinkedHashMap<String, Pair<String, Integer>> allDevices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AbstractNoActionBarActivity.init(this);

        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // make the result intent and set the result to canceled
        Intent resultValue; resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WidgetConfigurationActivity.this);
        builder.setTitle(R.string.widget_settings_select_device_title);

        allDevices = getAllDevices(getApplicationContext());

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Pair<String, Integer>> item : allDevices.entrySet()) {
            list.add(item.getKey());
        }
        String[] allDevicesString = list.toArray(new String[0]);

        builder.setSingleChoiceItems(allDevicesString, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView lw = ((AlertDialog) dialog).getListView();
                int selectedItemPosition = lw.getCheckedItemPosition();

                if (selectedItemPosition > -1) {
                    Map.Entry<String, Pair<String, Integer>> selectedItem =
                            (Map.Entry<String, Pair<String, Integer>>) allDevices.entrySet().toArray()[selectedItemPosition];
                    WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
                    widgetPreferenceStorage.saveWidgetPrefs(getApplicationContext(), String.valueOf(mAppWidgetId), selectedItem.getValue().first);
                }
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public LinkedHashMap getAllDevices(Context appContext) {
        DaoSession daoSession;
        Application gbApp = (Application) appContext;
        LinkedHashMap<String, Pair<String, Integer>> newMap = new LinkedHashMap<>(1);
        List<? extends GBDevice> devices = gbApp.getDeviceManager().getDevices();

        try (DBHandler handler = Application.acquireDB()) {
            daoSession = handler.getDaoSession();
            for (GBDevice device : devices) {
                DeviceCoordinator coordinator = device.getDeviceCoordinator();
                Device dbDevice = DBHelper.findDevice(device, daoSession);
                int icon = device.getEnabledDisabledIconResource();
                if (dbDevice != null && coordinator != null
                        && (coordinator.supportsActivityDataFetching() || coordinator.supportsActivityTracking())
                        && !newMap.containsKey(device.getAliasOrName())) {
                    newMap.put(device.getAliasOrName(), new Pair(device.getAddress(), icon));
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting list of all devices: " + e);
        }
        return newMap;
    }

    @Override
    public void setLanguage(Locale language, boolean invalidateLanguage) {
        AndroidUtils.setLanguage(this, language);
    }
}
