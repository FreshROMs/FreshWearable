package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
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
    private BarChart mChart;

    protected int STEPS_GOAL;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.health_fragment_steps, container, false);

        // Enable swipe refresh only when at the top of the scroll view
        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mDateView = rootView.findViewById(R.id.steps_date_view);
        steps = rootView.findViewById(R.id.steps_count);
        distance = rootView.findViewById(R.id.steps_distance);
        mChart = rootView.findViewById(R.id.steps_daily_chart);

        stepsProgress = rootView.findViewById(R.id.health_steps_progress);
        stepsTotal = rootView.findViewById(R.id.health_steps_total);
        stepsGoal = rootView.findViewById(R.id.health_steps_goal);

        setupStepsChart();

        STEPS_GOAL = WearableApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        refresh();

        stepsStreaksButton = rootView.findViewById(R.id.steps_streaks_button);
        stepsStreaksButton.setOnClickListener(v -> {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            StepStreaksDashboard stepStreaksDashboard = StepStreaksDashboard.newInstance(STEPS_GOAL, getChartsHost().getDevice());
            stepStreaksDashboard.show(fm, "steps_streaks_dashboard");
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
        int[] steps = new int[48]; // 48 30-minute intervals in a day
        long start = getStartDate().getTime();

        // .getTime() returns milliseconds since epoch as a long
        // Convert this to what the DB uses (seconds since epoch)
        start /= 1000;

        for (final ActivitySample sample : samplesOfDay) {
            // Find which 30-minute interval this data belongs to
            int index = (int) ((sample.getTimestamp() - start) / 1800);

            // Add the steps to the interval
            steps[index] += sample.getSteps();
        }

        return new StepsDailyFragment.StepsData(stepsDay, steps);
    }

    @Override
    protected void updateChartsnUIThread(StepsDailyFragment.StepsData stepsData) {
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        mDateView.setText(formattedDate);

        float progress = (float) stepsData.todayStepsDay.steps / STEPS_GOAL;
        stepsProgress.setProgress(progress);

        stepsTotal.setText(String.valueOf(stepsData.todayStepsDay.steps));
        stepsGoal.setText(String.valueOf(STEPS_GOAL));

        steps.setText(String.format(String.valueOf(stepsData.todayStepsDay.steps)));

        final WorkoutValueFormatter valueFormatter = new WorkoutValueFormatter();
        distance.setText(valueFormatter.formatValue(stepsData.todayStepsDay.distance, "km"));

        // Chart
        mChart.getLegend().setEnabled(false);

        final List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < stepsData.samples.length; i++) {
            barEntries.add(new BarEntry(i, stepsData.samples[i]));
        }

        final BarDataSet barDataSet = new BarDataSet(barEntries, getString(R.string.steps));
        barDataSet.setColor(getResources().getColor(R.color.health_steps_color));
        barDataSet.setDrawValues(false);

        mChart.getAxisLeft().removeAllLimitLines();
        mChart.getAxisLeft().setAxisMaximum(Math.max(barDataSet.getYMax(), STEPS_GOAL));

        final List<IBarDataSet> barDataSets = new ArrayList<>();
        barDataSets.add(barDataSet);

        final BarData barData = new BarData(barDataSets);
        mChart.setRenderer(new RoundedBarChartRenderer(mChart, mChart.getAnimator(), mChart.getViewPortHandler()));
        mChart.setData(barData);
    }

    @Override
    protected void renderCharts() {
        mChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    private void setupStepsChart() {
        mChart.getDescription().setEnabled(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setPinchZoom(false);

        final XAxis xAxisBottom = mChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(48f);
        xAxisBottom.setLabelCount(48, true); // 30-minute intervals
        xAxisBottom.setValueFormatter(new TimeValueFormatter());

        final YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(false);
        yAxisLeft.setDrawLabels(false);
        yAxisLeft.setDrawGridLines(false);
        yAxisLeft.setDrawAxisLine(false);
        yAxisLeft.setAxisMinimum(0f);

        final YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(false);
    }

    protected static class StepsData extends ChartsData {
        StepsDay todayStepsDay;
        int[] samples;

        public StepsData(final StepsDay todayStepsDay, final int[] samplesOfDay) {
            this.todayStepsDay = todayStepsDay;
            this.samples = samplesOfDay;
        }
    }

    private class TimeValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            // Get system preference for 12/24 hour time
            final boolean is24Hour = DateFormat.is24HourFormat(WearableApplication.getContext());

            int intValue = (int) value;
            switch (intValue) {
                case 0: // 12AM bin
                    return is24Hour ? "0" : "12AM";
                case 12: // 6AM bin
                    return "6AM";
                case 24: // 12PM bin
                    return "12PM";
                case 36: // 6PM bin
                    return is24Hour ? "18" : "6AM";
                case 48: // 12AM bin
                    return "(h)";
                default:
                    return "";
            }
        }
    }
}
