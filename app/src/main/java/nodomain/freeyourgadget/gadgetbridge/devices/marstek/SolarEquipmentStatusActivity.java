/*  Copyright (C) 2024 Andreas Shimokawa

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.marstek;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.cardview.widget.CardView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SolarEquipmentStatusActivity extends AbstractActionBarActivity {
    public static String ACTION_SEND_SOLAR_EQUIPMENT_STATUS = "send_solar_equipment_status";
    public static String EXTRA_BATTERY_PCT = "battery_pct";
    public static String EXTRA_BATTERY_WH = "battery_wh";
    public static String EXTRA_PANEL1_WATT = "panel1_watt";
    public static String EXTRA_PANEL2_WATT = "panel2_watt";
    public static String EXTRA_OUTPUT1_WATT = "output1_watt";
    public static String EXTRA_OUTPUT2_WATT = "output2_watt";
    public static String EXTRA_TEMP1 = "temp1";
    public static String EXTRA_TEMP2 = "temp2";
    public static String EXTRA_DEBUG = "debug";

    private final Map<String, View> widgetMap = new HashMap<>();
    private GridLayout gridLayout;
    private SwipeRefreshLayout swipeLayout;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Objects.requireNonNull(action).equals(ACTION_SEND_SOLAR_EQUIPMENT_STATUS)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    int battery_pct = extras.getInt(EXTRA_BATTERY_PCT);
                    int battery_wh = extras.getInt(EXTRA_BATTERY_WH);
                    int panel1_watt = extras.getInt(EXTRA_PANEL1_WATT);
                    int panel2_watt = extras.getInt(EXTRA_PANEL2_WATT);
                    int temp1 = extras.getInt(EXTRA_TEMP1);
                    int temp2 = extras.getInt(EXTRA_TEMP2);
                    int output1_watt = extras.getInt(EXTRA_OUTPUT1_WATT);
                    int output2_watt = extras.getInt(EXTRA_OUTPUT2_WATT);
                    String debug = extras.getString(EXTRA_DEBUG);
                    updateGaugeWidget("battery", battery_pct + "%\n" + battery_wh + "Wh", (float) (battery_pct / 100.0));
                    updateGaugeWidget("panel1", panel1_watt + "W", (float) (panel1_watt / 380.0));
                    updateGaugeWidget("panel2", panel2_watt + "W", (float) (panel2_watt / 380.0));
                    updateGaugeWidget("temp1", temp1 + "°C", (float) ((temp1 + 20) / 100.0));
                    updateGaugeWidget("temp2", temp2 + "°C", (float) ((temp2 + 20) / 100.0));
                    updateGaugeWidget("output1", output1_watt + "W", (float) (output1_watt / 400.0));
                    updateGaugeWidget("output2", output2_watt + "W", (float) (output2_watt / 400.0));
                    updateTextWidget("debug",debug);
                    swipeLayout.setRefreshing(false);
                }
            }
        }
    };
    private GBDevice gBDevice = null;

    public static int[] getColors() {
        return new int[]{
                ContextCompat.getColor(Application.getContext(), R.color.chart_stress_unknown),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_poor_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(Application.getContext(), R.color.body_energy_level_color),
        };
    }

    public static int[] getColorsTemp() {
        return new int[]{
                ContextCompat.getColor(Application.getContext(), R.color.calories_resting_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_excellent_color),
                ContextCompat.getColor(Application.getContext(), R.color.body_energy_level_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_poor_color),
        };
    }

    public static int[] getColorsOutput() {
        return new int[]{
                ContextCompat.getColor(Application.getContext(), R.color.chart_stress_unknown),
                ContextCompat.getColor(Application.getContext(), R.color.body_energy_level_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(Application.getContext(), R.color.vo2max_value_poor_color),
        };
    }

    public static float[] getSegments() {
        return new float[]{
                0.01f,
                0.09f,
                0.2f,
                0.7f,
        };
    }

    public static float[] getSegmentsTemp() {
        return new float[]{
                0.2f,
                0.1f,
                0.2f,
                0.3f,
                0.2f,
        };
    }

    public static float[] getSegmentsOutput() {
        return new float[]{
                0.01f,
                0.33f,
                0.33f,
                0.33f,
        };
    }

    private void updateTextWidget(String name, String value) {
        View view = widgetMap.get(name);
        if (view != null) {
            TextView debugValue = view.findViewById(R.id.text_value);
            debugValue.setText(value);
        }
    }

    private void updateGaugeWidget(String name, String value, float gaugeFill) {
        View view = widgetMap.get(name);
        if (view != null) {
            TextView gaugeValue = view.findViewById(R.id.gauge_value);
            gaugeValue.setText(value);
            GaugeDrawer gaugeDrawer = new GaugeDrawer();
            ImageView gaugeBar = view.findViewById(R.id.gauge_bar);
            if (name.startsWith("output")) {
                gaugeDrawer.drawSegmentedGauge(gaugeBar, getColorsOutput(), getSegmentsOutput(), gaugeFill, true, false);
            } else if (name.startsWith("temp")) {
                gaugeDrawer.drawSegmentedGauge(gaugeBar, getColorsTemp(), getSegmentsTemp(), gaugeFill, true, false);
            } else {
                gaugeDrawer.drawSegmentedGauge(gaugeBar, getColors(), getSegments(), gaugeFill, true, false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            gBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_equipment_status);
        gridLayout = findViewById(R.id.solarequipmentview_gridlayout);
        createWidget("panel1", "Panel 1", 1);
        createWidget("panel2", "Panel 2", 1);
        createWidget("battery", "Battery", 2);
        createWidget("temp1", "Temp 1", 1);
        createWidget("temp2", "Temp 2", 1);
        createWidget("output1", "Output 1", 1);
        createWidget("output2", "Output 2", 1);
        createWidget("debug", "Debug", 2);

        // Set pull-down-to-refresh action
        swipeLayout = findViewById(R.id.solarequipmentview_swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            if (gBDevice == null || !gBDevice.isInitialized()) {
                swipeLayout.setRefreshing(false);
                GB.toast(getString(R.string.info_no_devices_connected), Toast.LENGTH_LONG, GB.WARN);
                return;
            }
            Application.deviceService(gBDevice).onFetchRecordedData(0);
        });

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_SEND_SOLAR_EQUIPMENT_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    private void createWidget(String name, String label, int columnSpan) {
        final float scale = getResources().getDisplayMetrics().density;

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL, 1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        CardView card = new CardView(this);
        int pixels_4dp = (int) (4 * scale + 0.5f);
        card.setRadius(pixels_4dp);
        card.setCardElevation(pixels_4dp);
        card.setContentPadding(pixels_4dp, pixels_4dp, pixels_4dp, pixels_4dp);
        card.setLayoutParams(layoutParams);
        LayoutInflater inflater = getLayoutInflater();

        View view;
        if (name.equals("debug")) {
            view = inflater.inflate(R.layout.dashboard_widget_text, card, false);
        } else {
            view = inflater.inflate(R.layout.dashboard_widget_generic_gauge, card, false);
            final TextView gaugeLabel = view.findViewById(R.id.gauge_label);
            gaugeLabel.setText(label);
        }
        card.addView(view);
        widgetMap.put(name, view);
        gridLayout.addView(card);
    }

}
