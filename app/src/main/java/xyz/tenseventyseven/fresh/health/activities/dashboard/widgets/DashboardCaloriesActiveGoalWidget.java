package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractGaugeWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.CaloriesDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardCaloriesActiveGoalWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardCaloriesActiveGoalWidget extends AbstractGaugeWidget {
    public DashboardCaloriesActiveGoalWidget() {
        super(R.string.active_calories, "calories", CaloriesDailyFragment.GaugeViewMode.ACTIVE_CALORIES_GOAL.toString());
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardCaloriesActiveGoalWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardCaloriesActiveGoalWidget fragment = new DashboardCaloriesActiveGoalWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsActiveCalories();
    }

    @Override
    protected void populateData(final HomeFragment.DashboardData dashboardData) {
        dashboardData.getActiveCaloriesTotal();
        dashboardData.getActiveCaloriesGoalFactor();
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        setText(String.valueOf(dashboardData.getActiveCaloriesTotal()));
        final int colorCalories = ContextCompat.getColor(WearableApplication.getContext(), R.color.calories_color);
        drawSimpleGauge(
                colorCalories,
                dashboardData.getActiveCaloriesGoalFactor()
        );
    }
}
