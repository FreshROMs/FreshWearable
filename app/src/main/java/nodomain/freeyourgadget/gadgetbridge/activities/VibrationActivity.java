/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer

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

import android.os.Bundle;
import android.widget.SeekBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;


public class VibrationActivity extends AbstractActionBarActivity {
    private static final Logger LOG = LoggerFactory.getLogger(VibrationActivity.class);
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibration);

        seekBar = (SeekBar) findViewById(R.id.vibration_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 0) { // 1-16
                    progress = progress * 16 - 1; // max 255
                }
                Application.deviceService().onSetConstantVibration(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
