package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import xyz.tenseventyseven.fresh.health.components.CircularProgressView;
import xyz.tenseventyseven.fresh.health.components.HorizontalProgressView;

public class StepsDailyFragment extends StepsFragment<StepsDailyFragment.StepsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;

    private HorizontalProgressView stepsProgress;
    private TextView stepsTotal;
    private TextView stepsGoal;
    private TextView steps;
    private TextView distance;
    ImageView stepsStreaksButton;
    private LineChart stepsChart;

    protected int STEPS_GOAL;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.health_fragment_steps, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.steps_date_view);
        steps = rootView.findViewById(R.id.steps_count);
        distance = rootView.findViewById(R.id.steps_distance);
        stepsChart = rootView.findViewById(R.id.steps_daily_chart);

        stepsProgress = rootView.findViewById(R.id.health_steps_progress);
        stepsTotal = rootView.findViewById(R.id.health_steps_total);
        stepsGoal = rootView.findViewById(R.id.health_steps_goal);

        setupStepsChart();

        STEPS_GOAL = WearableApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        refresh();

        stepsStreaksButton = rootView.findViewById(R.id.steps_streaks_button);
        stepsStreaksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                StepStreaksDashboard stepStreaksDashboard = StepStreaksDashboard.newInstance(STEPS_GOAL, getChartsHost().getDevice());
                stepStreaksDashboard.show(fm, "steps_streaks_dashboard");
            }
        });

        return rootView;
    }

        @Override
    public String getTitle() {
        return getString(R.string.steps);
    }

    @Override
    protected StepsDailyFragment.StepsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        List<StepsDay> stepsDayList = getMyStepsDaysData(db, day, device);
        final StepsDay stepsDay;
        if (stepsDayList.isEmpty()) {
            LOG.error("Failed to get StepsDay for {}", day);
            stepsDay = new StepsDay(day, 0, 0);
        } else {
            stepsDay = stepsDayList.get(0);
        }
        List<? extends ActivitySample> samplesOfDay = getSamplesOfDay(db, day, 0, device);
        return new StepsDailyFragment.StepsData(stepsDay, samplesOfDay);
    }

    @Override
    protected void updateChartsnUIThread(StepsDailyFragment.StepsData stepsData) {
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        mDateView.setText(formattedDate);

        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                WearableApplication.getContext().getResources().getDisplayMetrics()
        );

        float progress = (float) stepsData.todayStepsDay.steps / STEPS_GOAL;
        stepsProgress.setProgressColor(getProgressColor(R.color.health_steps_color, progress));
        stepsProgress.setProgressBackgroundColor(getBackgroundColor(R.color.health_steps_color, progress));

        // Show the excess steps if the user has exceeded the goal
        if (progress > 1.0f) {
            progress = progress - 1.0f;
        }

        stepsProgress.setProgress(progress);

        stepsTotal.setText(String.valueOf(stepsData.todayStepsDay.steps));
        stepsGoal.setText(String.valueOf(STEPS_GOAL));

        steps.setText(String.format(String.valueOf(stepsData.todayStepsDay.steps)));

        final WorkoutValueFormatter valueFormatter = new WorkoutValueFormatter();
        distance.setText(valueFormatter.formatValue(stepsData.todayStepsDay.distance, "km"));

        // Chart
        stepsChart.getLegend().setEnabled(false);

        final List<Entry> lineEntries = new ArrayList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        int sum = 0;
        for (final ActivitySample sample : stepsData.samples) {
            if (sample.getSteps() > 0) {
                sum += sample.getSteps();
            }
            lineEntries.add(new Entry(tsTranslation.shorten(sample.getTimestamp()), sum));
        }

        stepsChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));

        if (sum < STEPS_GOAL) {
            stepsChart.getAxisLeft().setAxisMaximum(STEPS_GOAL);
        } else {
            stepsChart.getAxisLeft().resetAxisMaximum();
        }

        final LineDataSet lineDataSet = new LineDataSet(lineEntries, getString(R.string.steps));
        lineDataSet.setColor(getResources().getColor(R.color.health_steps_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.health_steps_color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillAlpha(60);
        lineDataSet.setFillColor(getResources().getColor(R.color.health_steps_color ));

        final LimitLine goalLine = new LimitLine(STEPS_GOAL);
        goalLine.setLineColor(getResources().getColor(R.color.health_steps_color));
        goalLine.setLineWidth(1.5f);
        goalLine.enableDashedLine(15f, 10f, 0f);
        stepsChart.getAxisLeft().removeAllLimitLines();
        stepsChart.getAxisLeft().addLimitLine(goalLine);
        stepsChart.getAxisLeft().setAxisMaximum(Math.max(lineDataSet.getYMax(), STEPS_GOAL) + 2000);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        lineDataSets.add(lineDataSet);
        final LineData lineData = new LineData(lineDataSets);
        stepsChart.setData(lineData);
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

    @Override
    protected void renderCharts() {
        stepsChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    private void setupStepsChart() {
        stepsChart.getDescription().setEnabled(false);
        stepsChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = stepsChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        //xAxisBottom.setLabelCount(7, true);

        final YAxis yAxisLeft = stepsChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = stepsChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    protected static class StepsData extends ChartsData {
        StepsDay todayStepsDay;
        List<? extends ActivitySample> samples;

        public StepsData(final StepsDay todayStepsDay, final List<? extends ActivitySample> samplesOfDay) {
            this.todayStepsDay = todayStepsDay;
            this.samples = samplesOfDay;
        }
    }
}
