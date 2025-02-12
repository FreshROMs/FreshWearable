/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniele Gobbetti, Felix
    Konstantin Maurer, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;

public class AutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = AutoStartReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Application.getPrefs().getAutoStart() &&
                (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                        Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())
                )) {
            Log.i(TAG, "Boot or reinstall completed, starting Gadgetbridge");
            if (Application.getPrefs().getBoolean("general_autoconnectonbluetooth", false)) {
                Log.i(TAG, "Autoconnect is enabled, attempting to connect");
                Application.deviceService().connect();
            }
            Log.i(TAG, "Going to enable periodic exporter");
            PeriodicExporter.enablePeriodicExport(context);
        }
    }
}
