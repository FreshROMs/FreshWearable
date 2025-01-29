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

import java.util.Locale;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractGaugeWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardActiveTimeWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardActiveTimeWidget extends AbstractGaugeWidget {
    public DashboardActiveTimeWidget() {
        super(R.string.activity_list_summary_active_time, "activity", dev.oneuiproject.oneui.R.drawable.ic_oui_time);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardActiveTimeWidget.
     */
    public static DashboardActiveTimeWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardActiveTimeWidget fragment = new DashboardActiveTimeWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void populateData(final HomeFragment.DashboardData dashboardData) {
        dashboardData.getActiveMinutesTotal();
        dashboardData.getActiveMinutesGoalFactor();
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        setText(getString(R.string.dashboard_widget_goals_active_time, dashboardData.getActiveMinutesTotal()));
        drawSimpleGauge(
                requireContext().getColor(R.color.health_active_time_color),
                dashboardData.getActiveMinutesGoalFactor()
        );
    }
}
