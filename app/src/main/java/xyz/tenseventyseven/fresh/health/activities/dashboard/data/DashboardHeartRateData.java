/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package xyz.tenseventyseven.fresh.health.activities.dashboard.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.StressChartFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;

public class DashboardHeartRateData implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardHeartRateData.class);

    public int min;
    public int max;

    public static DashboardHeartRateData compute(final HomeFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = WearableApplication.app().getDeviceManager().getDevices();
        GBDevice device = null;

        try (DBHandler dbHandler = WearableApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsHeartRateMeasurement(dev)) {
                    final List<? extends HeartRateSample> samples = dev.getDeviceCoordinator()
                            .getHeartRateManualSampleProvider(dev, dbHandler.getDaoSession())
                            .getAllSamples(dashboardData.timeFrom * 1000L, dashboardData.timeTo * 1000L);

                    if (!samples.isEmpty()) {
                        device = dev;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Could not compute stress", e);
        }

        if (device != null) {
            final DashboardHeartRateData data = new DashboardHeartRateData();

            return data;
        }

        return null;
    }
}
