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
package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractGaugeWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import xyz.tenseventyseven.fresh.health.activities.dashboard.data.DashboardStressData;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class DashboardStressSegmentedWidget extends AbstractGaugeWidget {
    public DashboardStressSegmentedWidget() {
        super(R.string.menuitem_stress, "stress");
    }

    public static DashboardStressSegmentedWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardStressSegmentedWidget fragment = new DashboardStressSegmentedWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsStressMeasurement();
    }

    @Override
    protected void populateData(final HomeFragment.DashboardData dashboardData) {
        dashboardData.computeIfAbsent("stress", () -> DashboardStressData.compute(dashboardData));
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        final int[] colors = new int[]{
                ContextCompat.getColor(WearableApplication.getContext(), R.color.chart_stress_relaxed),
                ContextCompat.getColor(WearableApplication.getContext(), R.color.chart_stress_mild),
                ContextCompat.getColor(WearableApplication.getContext(), R.color.chart_stress_moderate),
                ContextCompat.getColor(WearableApplication.getContext(), R.color.chart_stress_high),
        };

        final float[] segments;
        final float value;
        final String valueText;

        final DashboardStressData stressData = (DashboardStressData) dashboardData.get("stress");

        if (stressData != null) {
            segments = new float[]{
                    (stressData.ranges[1] - stressData.ranges[0]) / 100f,
                    (stressData.ranges[2] - stressData.ranges[1]) / 100f,
                    (stressData.ranges[3] - stressData.ranges[2]) / 100f,
                    1 - stressData.ranges[2] / 100f,
            };
            value = stressData.value / 100f;
            valueText = String.valueOf(stressData.value);
        } else {
            segments = new float[]{
                    40 / 100f,
                    20 / 100f,
                    20 / 100f,
                    20 / 100f,
            };
            value = -1;
            valueText = WearableApplication.getContext().getString(R.string.stats_empty_value);
        }

        setText(valueText);
        drawSegmentedGauge(
                colors,
                segments,
                value,
                false,
                true
        );
    }
}
