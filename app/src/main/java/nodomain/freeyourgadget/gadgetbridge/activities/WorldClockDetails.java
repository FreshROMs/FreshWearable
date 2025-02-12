/*  Copyright (C) 2022-2024 Arjan Schrijver, Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.TimeZone;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;

public class WorldClockDetails extends AbstractActionBarActivity {
    private WorldClock worldClock;
    private GBDevice device;

    ArrayAdapter<String> timezoneAdapter;

    TextView worldClockTimezone;
    EditText worldClockLabel;
    EditText worldClockCode;
    View worldClockEnabledCard;
    CheckBox worldClockEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_clock_details);

        worldClock = (WorldClock) getIntent().getSerializableExtra(WorldClock.EXTRA_WORLD_CLOCK);

        if (worldClock == null) {
            GB.toast("No worldClock provided to WorldClockDetails Activity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        worldClockEnabledCard = findViewById(R.id.card_enabled);
        worldClockEnabled = findViewById(R.id.world_clock_enabled);
        worldClockTimezone = findViewById(R.id.world_clock_timezone);
        worldClockLabel = findViewById(R.id.world_clock_label);
        worldClockCode = findViewById(R.id.world_clock_code);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            GB.toast("No device provided to WorldClockDetails Activity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final String[] timezoneIDs = TimeZone.getAvailableIDs();
        timezoneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, timezoneIDs);

        final View cardTimezone = findViewById(R.id.card_timezone);
        cardTimezone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(WorldClockDetails.this).setAdapter(timezoneAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        worldClock.setTimeZoneId(timezoneIDs[i]);
                        updateUiFromWorldClock();
                    }
                }).create().show();
            }
        });

        if (coordinator.supportsDisabledWorldClocks()) {
            worldClockEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                worldClock.setEnabled(isChecked);
            });
        } else {
            worldClockEnabledCard.setVisibility(View.GONE);
        }

        worldClockLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(coordinator.getWorldClocksLabelLength())});
        worldClockLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                worldClock.setLabel(s.toString());
            }
        });

        worldClockCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        worldClockCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                worldClock.setCode(s.toString());
            }
        });

        final FloatingActionButton fab = findViewById(R.id.fab_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateWorldClock();
                WorldClockDetails.this.setResult(1);
                finish();
            }
        });

        updateUiFromWorldClock();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            // TODO confirm when exiting without saving
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWorldClock() {
        DBHelper.store(worldClock);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("worldClock", worldClock);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        worldClock = (WorldClock) savedInstanceState.getSerializable("worldClock");
        updateUiFromWorldClock();
    }

    public void updateUiFromWorldClock() {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final int maxLabelLength = coordinator.getWorldClocksLabelLength();

        worldClockEnabled.setChecked(worldClock.getEnabled() == null || worldClock.getEnabled());

        final String oldTimezone = worldClockTimezone.getText().toString();

        worldClockTimezone.setText(worldClock.getTimeZoneId());

        // Check if the label was still the default (the timezone city name)
        // If so, and if the user changed the timezone, update the label to match the new city name
        if (!StringUtils.isNullOrEmpty(oldTimezone) && !oldTimezone.equals(worldClock.getTimeZoneId())) {
            final String[] oldTimezoneParts = oldTimezone.split("/");
            final String[] newTimezoneParts = worldClock.getTimeZoneId().split("/");
            final String newLabel = StringUtils.truncate(newTimezoneParts[newTimezoneParts.length - 1], maxLabelLength);
            final String oldLabel = StringUtils.truncate(oldTimezoneParts[oldTimezoneParts.length - 1], maxLabelLength);
            final String userLabel = worldClockLabel.getText().toString();
            if (StringUtils.isNullOrEmpty(userLabel) || userLabel.equals(oldLabel)) {
                // The label was still the original, so let's override it with the new city
                worldClock.setLabel(newLabel);
            }
            final String newCode = StringUtils.truncate(newLabel, 3).toUpperCase();
            final String oldCode = StringUtils.truncate(oldLabel, 3).toUpperCase();
            final String userCode = worldClockCode.getText().toString();
            if (StringUtils.isNullOrEmpty(userCode) || userCode.equals(oldCode)) {
                // The code was still the original, so let's override it with the new one
                worldClock.setCode(newCode);
            }
        }

        worldClockLabel.setText(worldClock.getLabel());
        worldClockCode.setText(worldClock.getCode());
    }
}
