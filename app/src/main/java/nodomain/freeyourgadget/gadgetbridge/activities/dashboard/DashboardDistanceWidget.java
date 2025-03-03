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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardDistanceWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardDistanceWidget extends AbstractGaugeWidget {
    public DashboardDistanceWidget() {
        super(R.string.distance, "stepsweek");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardDistanceWidget.
     */
    public static DashboardDistanceWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardDistanceWidget fragment = new DashboardDistanceWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        dashboardData.getDistanceTotal();
        dashboardData.getDistanceGoalFactor();
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        setText(FormatUtils.getFormattedDistanceLabel(dashboardData.getDistanceTotal()));
        drawSimpleGauge(
                color_distance,
                dashboardData.getDistanceGoalFactor()
        );
    }
}
