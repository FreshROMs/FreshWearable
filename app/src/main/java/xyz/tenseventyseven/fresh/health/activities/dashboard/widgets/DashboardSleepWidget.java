/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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
package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.Locale;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractGaugeWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.health.data.NightSleepData;
import xyz.tenseventyseven.fresh.health.data.models.NightSleepDataModel;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardSleepWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardSleepWidget extends AbstractGaugeWidget {
    public DashboardSleepWidget() {
        super(R.string.menuitem_sleep, "sleep", dev.oneuiproject.oneui.R.drawable.ic_oui_bed);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardSleepWidget.
     */
    public static DashboardSleepWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardSleepWidget fragment = new DashboardSleepWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsSleepMeasurement();
    }

    @Override
    protected void populateData(final HomeFragment.DashboardData dashboardData) {
        dashboardData.getSleepData();
        dashboardData.getSleepMinutesGoalFactor();
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        final long totalSleepMinutes = dashboardData.getSleepMinutesTotal();
        final String valueText = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(totalSleepMinutes / 60f),
                (int) (totalSleepMinutes % 60f)
        );
        setText(valueText);

        NightSleepData data = dashboardData.getSleepData();
        float factor = dashboardData.getSleepMinutesGoalFactor();
        final int[] colors = new int[data.getData().size()];
        final float[] segments = new float[data.getData().size()];

        // Compute start time as 0% and end time as 100%
        final int startTime = data.startTime;
        final int endTime = data.endTime;
        final int totalMinutes = endTime - startTime;

        int i = 0;
        for (NightSleepDataModel sleepData : data.getData()) {
            final int start = sleepData.start;
            final int end = sleepData.end;
            final int duration = end - start;
            final float durationPercentage = duration / (float) totalMinutes;

            segments[i] = durationPercentage * factor;
            colors[i] = sleepData.getColor();

            i++;
        }

        drawSegmentedGauge(colors, segments, -1, false, false);
    }
}
