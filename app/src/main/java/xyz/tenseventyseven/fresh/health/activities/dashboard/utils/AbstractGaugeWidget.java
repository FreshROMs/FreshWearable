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

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.GaugeDrawer;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;

public abstract class AbstractGaugeWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGaugeWidget.class);

    private TextView gaugeValue;
    private ImageView gaugeBar;
    protected GaugeDrawer gaugeDrawer;

    private ProgressBar gaugeBarNew;

    private final int label;
    private final String targetActivityTab;
    private String mode = "";

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab) {
        this.label = label;
        this.targetActivityTab = targetActivityTab;
    }

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab, final String mode) {
        this(label, targetActivityTab);
        this.mode = mode;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.dashboard_widget_generic_gauge, container, false);
        setupView(fragmentView);

        return fragmentView;
    }

    public void setupView(View fragmentView) {
        if (targetActivityTab != null) {
            onClickOpenChart(fragmentView, targetActivityTab, label, mode);
        }

        gaugeValue = fragmentView.findViewById(R.id.gauge_value);
        gaugeBar = fragmentView.findViewById(R.id.gauge_bar);
        gaugeBarNew = fragmentView.findViewById(R.id.gauge_progress);
        gaugeDrawer = new GaugeDrawer();
        final TextView gaugeLabel = fragmentView.findViewById(R.id.gauge_label);
        gaugeLabel.setText(label);

        fillData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gaugeValue != null && gaugeBarNew != null) fillData();
    }

    @Override
    protected void fillData() {
        if (gaugeBarNew == null) return;
        gaugeBarNew.post(() -> {
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

    /**
     * Draw a simple gauge.
     *
     * @param color     the gauge color
     * @param value     the gauge value. Range: [0, 1]
     */
    protected void drawSimpleGauge(final int color,
                                   final float value) {
        // gaugeDrawer.drawSimpleGauge(gaugeBar, color, value);
        gaugeBarNew.setProgress((int) (value * 100));
        gaugeBarNew.setProgressTintList(ColorStateList.valueOf(color));
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
        gaugeDrawer.drawSegmentedGauge(gaugeBar, colors, segments, value, fadeOutsideDot, gapBetweenSegments);
    }
}
