/*  Copyright (C) 2018-2024 Carsten Pfeiffer, Felix Konstantin Maurer,
    Ganblejs, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Created by maufl on 1/4/18.
 */

public class PeriodicExporter {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicExporter.class);

    private static final String TAG = "exporter_db";

    public static void enablePeriodicExport(final Context context) {
        Prefs prefs = Application.getPrefs();
        Application gbApp = Application.app();
        final long autoExportScheduled = gbApp.getAutoExportScheduledTimestamp();
        final boolean autoExportEnabled = prefs.getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
        final int autoExportInterval = prefs.getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
        scheduleAlarm(context, autoExportInterval, autoExportEnabled && autoExportScheduled == 0);
    }

    public static void scheduleAlarm(final Context context,
                                     final int autoExportInterval,
                                     final boolean autoExportEnabled) {
        final WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(TAG);

        if (!autoExportEnabled) {
            LOG.info("Not scheduling periodic export, either already scheduled or not enabled");
            return;
        }
        final int exportPeriodMillis = autoExportInterval * 60 * 60 * 1000;
        if (exportPeriodMillis == 0) {
            LOG.info("Not scheduling periodic export, interval set to 0");
            return;
        }
        LOG.info("Scheduling periodic export");
        Application gbApp = Application.app();
        gbApp.setAutoExportScheduledTimestamp(System.currentTimeMillis() + exportPeriodMillis);

        final PeriodicWorkRequest exportRequest =
                new PeriodicWorkRequest.Builder(DatabaseExportWorker.class, 1, TimeUnit.HOURS)
                        .addTag(TAG)
                        .build();

        workManager.enqueue(exportRequest);
    }

    public static void trigger() {
        final WorkManager workManager = WorkManager.getInstance(Application.getContext());
        final OneTimeWorkRequest exportRequest =
                new OneTimeWorkRequest.Builder(DatabaseExportWorker.class)
                        .addTag(TAG)
                        .build();
    }
}
