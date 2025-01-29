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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.health.components.CircularProgressView;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardGoalsWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardGoalsWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardGoalsWidget.class);
    private View goalsView;
    private TextView statsSteps;
    private TextView statsDistance;
    private TextView statsActiveTime;

    private CircularProgressView progessBarSteps;
    private CircularProgressView progessBarDistance;
    private CircularProgressView progessBarActiveTime;

    public DashboardGoalsWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of HomeFragment.DashboardData.
     * @return A new instance of fragment DashboardGoalsWidget.
     */
    public static DashboardGoalsWidget newInstance(HomeFragment.DashboardData dashboardData) {
        DashboardGoalsWidget fragment = new DashboardGoalsWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        goalsView = inflater.inflate(R.layout.health_dashboard_widget_goals, container, false);
        statsSteps = goalsView.findViewById(R.id.dashboard_goals_steps);
        statsDistance = goalsView.findViewById(R.id.dashboard_goals_distance);
        statsActiveTime = goalsView.findViewById(R.id.dashboard_goals_active_time);
        progessBarSteps = goalsView.findViewById(R.id.dashboard_goals_steps_progress);
        progessBarDistance = goalsView.findViewById(R.id.dashboard_goals_distance_progress);
        progessBarActiveTime = goalsView.findViewById(R.id.dashboard_goals_active_time_progress);
        return goalsView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (goalsView != null) fillData();
    }

    @Override
    protected void fillData() {
        if (goalsView == null) return;
        if (dashboardData == null) return;
        if (getContext() == null) return;

        float stepsGoalFactor = dashboardData.getStepsGoalFactor();
        float distanceGoalFactor = dashboardData.getDistanceGoalFactor();
        float activeMinutesGoalFactor = dashboardData.getActiveMinutesGoalFactor();

        LOG.debug("Steps goal factor: {}", stepsGoalFactor);
        LOG.debug("Distance goal factor: {}", distanceGoalFactor);
        LOG.debug("Active minutes goal factor: {}", activeMinutesGoalFactor);

        // Set Progress color for all goals
        progessBarSteps.setProgressColor(getProgressColor(R.color.health_steps_color, stepsGoalFactor));
        progessBarDistance.setProgressColor(getProgressColor(R.color.health_distance_color, distanceGoalFactor));
        progessBarActiveTime.setProgressColor(getProgressColor(R.color.health_active_time_color, activeMinutesGoalFactor));

        // Set Progress background color for all goals
        progessBarSteps.setProgressBackgroundColor(getBackgroundColor(R.color.health_steps_color, stepsGoalFactor));
        progessBarDistance.setProgressBackgroundColor(getBackgroundColor(R.color.health_distance_color, distanceGoalFactor));
        progessBarActiveTime.setProgressBackgroundColor(getBackgroundColor(R.color.health_active_time_color, activeMinutesGoalFactor));

        // Set progress for all goals
        progessBarSteps.setProgress(getProgress(stepsGoalFactor));
        progessBarDistance.setProgress(getProgress(distanceGoalFactor));
        progessBarActiveTime.setProgress(getProgress(activeMinutesGoalFactor));

        statsSteps.setText(getString(R.string.dashboard_widget_goals_steps, dashboardData.getStepsTotal()));
        statsDistance.setText(FormatUtils.getFormattedDistanceLabel(dashboardData.getDistanceTotal()));
        statsActiveTime.setText(getString(R.string.dashboard_widget_goals_active_time, dashboardData.getActiveMinutesTotal()));
    }

    private int getProgressColor(int resId, float factor) {
        // Return regular color if below goal
        int color = requireContext().getColor(resId);
        if (factor < 1) return color;

        // Get color beyond goal - make color brighter (15%)
        return Color.argb(255, Math.min(255, (int) (Color.red(color) * 1.2)),
                Math.min(255, (int) (Color.green(color) * 1.25)),
                Math.min(255, (int) (Color.blue(color) * 1.25)));
    }

    private int getBackgroundColor(int resId, float factor) {
        // Return progress normal color if below goal
        int color = requireContext().getColor(resId);

        // Set the color to be more transparent - 25%
        if (factor < 1)
            return Color.argb(64, Color.red(color), Color.green(color), Color.blue(color));

        // Return regular color if above goal
        return color;
    }

    private int getProgress(float factor) {
        // Return regular progress if below goal
        if (factor < 1) return (int) (factor * 100);

        // Get progress beyond goal, get excess and return it
        float excess = (factor - 1);
        LOG.debug("Excess: {}", excess);
        return (int) (excess * 100);
    }
}