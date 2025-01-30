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
package xyz.tenseventyseven.fresh.health.activities.dashboard.utils;

import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.GaugeDrawer;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import xyz.tenseventyseven.fresh.health.components.HorizontalProgressView;

public abstract class AbstractGaugeWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGaugeWidget.class);

    protected TextView gaugeValue;
    protected GaugeDrawer gaugeDrawer;

    private HorizontalProgressView gaugeBar;

    private ImageView gaugeIcon;

    private final int label;

    private final int iconId;
    private final String targetActivityTab;
    private String mode = "";

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab) {
        this.label = label;
        this.iconId = -1;
        this.targetActivityTab = targetActivityTab;
    }

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab, final int iconId) {
        this.label = label;
        this.iconId = iconId;
        this.targetActivityTab = targetActivityTab;
    }

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab, final String mode) {
        this(label, targetActivityTab);
        this.mode = mode;
    }

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab, final String mode, final int iconId) {
        this(label, targetActivityTab, iconId);
        this.mode = mode;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.health_dashboard_widget_generic, container, false);
        setupView(fragmentView);

        return fragmentView;
    }

    public void setupView(View fragmentView) {
        if (targetActivityTab != null) {
            onClickOpenChart(fragmentView, targetActivityTab, label, mode);
        }

        gaugeValue = fragmentView.findViewById(R.id.gauge_value);
        gaugeBar = fragmentView.findViewById(R.id.gauge_progress);
        gaugeDrawer = new GaugeDrawer();
        gaugeIcon = fragmentView.findViewById(R.id.gauge_icon);

        final TextView gaugeLabel = fragmentView.findViewById(R.id.gauge_label);
        if (gaugeLabel != null) {
            gaugeLabel.setText(label);
        }

        if (gaugeIcon != null && iconId != -1) {
            gaugeIcon.setImageResource(iconId);
        }

        fillData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gaugeValue != null && gaugeBar != null) fillData();
    }

    @Override
    protected void fillData() {
        if (gaugeBar == null) return;
        gaugeBar.post(() -> {
            final FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
            myAsyncTask.execute();
        });
    }

    /**
     * This is called from the async task, outside of the UI thread. It's expected that
     * {@link HomeFragment.DashboardData} be
     * populated with the necessary data for display.
     *
     * @param dashboardData the DashboardData to populate
     */
    protected abstract void populateData(HomeFragment.DashboardData dashboardData);

    /**
     * This is called from the UI thread.
     *
     * @param dashboardData populated DashboardData
     */
    protected abstract void draw(HomeFragment.DashboardData dashboardData);

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(final Void... params) {
            final long nanoStart = System.nanoTime();
            try {
                populateData(dashboardData);
            } catch (final Exception e) {
                LOG.error("fillData for {} failed", AbstractGaugeWidget.this.getClass().getSimpleName(), e);
            }
            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", AbstractGaugeWidget.this.getClass().getSimpleName(), executionTime);
            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {
            super.onPostExecute(unused);
            try {
                draw(dashboardData);
            } catch (final Exception e) {
                LOG.error("draw for {} failed", AbstractGaugeWidget.this.getClass().getSimpleName(), e);
            }
        }
    }

    protected void setText(final CharSequence text) {
        gaugeValue.setText(text);
    }

    protected void setIcon(final int iconId) {
        gaugeIcon.setImageResource(iconId);
    }

    /**
     * Draw a simple gauge.
     *
     * @param color     the gauge color
     * @param value     the gauge value. Range: [0, 1]
     */
    protected void drawSimpleGauge(final int color,
                                   final float value) {
        if (gaugeBar == null) {
            return;
        }

        gaugeBar.setProgress(value);
        gaugeBar.setProgressColor(color);
    }

    /**
     * Draws a segmented gauge.
     *
     * @param colors             the colors of each segment
     * @param segments           the size of each segment. The sum of all segments should be 1
     * @param value              the gauge value, in range [0, 1], or -1 for no value and only segments
     * @param fadeOutsideDot     whether to fade out colors outside the dot value
     * @param gapBetweenSegments whether to introduce a small gap between the segments
     */
    protected void drawSegmentedGauge(final int[] colors,
                                      final float[] segments,
                                      final float value,
                                      final boolean fadeOutsideDot,
                                      final boolean gapBetweenSegments) {
        if (gaugeBar == null) {
            return;
        }

        // Restructure segments to fit the gauge's new format
        boolean dotMode = value != -1;
        gaugeBar.setDotMode(dotMode);
        gaugeBar.clearBackgroundSegments();
        gaugeBar.clearProgressSegments();

        float last = 0;
        for (int i = 0; i < segments.length; i++) {
            final float high = last + segments[i];
            final int color = colors[i];

            if (dotMode) {
                gaugeBar.addBackgroundSegment(last, high, color);
            } else {
                gaugeBar.addProgressSegment(last, high, color);
            }

            last = high;
        }

        if (dotMode) {
            gaugeBar.setProgress(value);
        }
    }
}
