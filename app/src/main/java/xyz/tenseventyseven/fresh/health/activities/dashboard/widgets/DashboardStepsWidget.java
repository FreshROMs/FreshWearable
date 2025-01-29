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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.GaugeDrawer;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractGaugeWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardStepsWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardStepsWidget extends AbstractGaugeWidget {
    private TextView gaugeTotal;

    public DashboardStepsWidget() {
        super(R.string.steps, "stepsweek", ir.alirezaivaz.tablericons.R.drawable.ic_mood_check);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.health_dashboard_widget_steps, container, false);
        gaugeTotal = fragmentView.findViewById(R.id.gauge_label);
        setupView(fragmentView);
        return fragmentView;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardStepsWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardStepsWidget fragment = new DashboardStepsWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void populateData(final HomeFragment.DashboardData dashboardData) {
        dashboardData.getStepsTotal();
        dashboardData.getStepsGoalFactor();
        dashboardData.getStepsGoal();
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        setText(String.valueOf(dashboardData.getStepsTotal()));
        if (gaugeTotal != null) {
            String str = "/" +
                    getString(R.string.dashboard_widget_goals_steps, dashboardData.getStepsGoal());
            gaugeTotal.setText(str);
        }

        drawSimpleGauge(
                requireContext().getColor(R.color.health_steps_color),
                dashboardData.getStepsGoalFactor()
        );
    }
}
