package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.graphics.Color;
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
 * Use the {@link DashboardCaloriesTotalSegmentedWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardCaloriesTotalSegmentedWidget extends AbstractGaugeWidget {
    public DashboardCaloriesTotalSegmentedWidget() {
        super(R.string.calories, "calories", CaloriesDailyFragment.GaugeViewMode.TOTAL_CALORIES_SEGMENT.toString(), ir.alirezaivaz.tablericons.R.drawable.ic_flame);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardCaloriesTotalSegmentedWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardCaloriesTotalSegmentedWidget fragment = new DashboardCaloriesTotalSegmentedWidget();
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
        dashboardData.getRestingCaloriesTotal();
    }

    @Override
    protected void draw(final HomeFragment.DashboardData dashboardData) {
        int activeCalories = dashboardData.getActiveCaloriesTotal();
        int restingCalories = dashboardData.getRestingCaloriesTotal();
        int totalCalories = activeCalories + restingCalories;
        setText(String.valueOf(totalCalories));
        final int[] colors;
        final float[] segments;
        if (totalCalories != 0) {
            colors = new int[] {
                    ContextCompat.getColor(WearableApplication.getContext(), R.color.calories_resting_color),
                    ContextCompat.getColor(WearableApplication.getContext(), R.color.calories_color)
            };
            segments = new float[] {
                    restingCalories > 0 ? (float) restingCalories / totalCalories : 0,
                    activeCalories > 0 ? (float) activeCalories / totalCalories : 0
            };
        } else {
            colors = new int[]{
                    Color.argb(25, 128, 128, 128)
            };
            segments = new float[] {
                    1f
            };
        }
        drawSegmentedGauge(
                colors,
                segments,
                -1,
                false,
                false
        );
    }
}
