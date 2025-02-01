/*  Copyright (C) 2025 John Vincent Corcega

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
package xyz.tenseventyseven.fresh.health.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.health.data.models.NightSleepDataModel;

public class NightSleepData implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(NightSleepData.class);
    private List<NightSleepDataModel> data = new ArrayList<>();
    public int totalMinutes = 0;
    public int startTime = 0;
    public int endTime = 0;
    public static long MAX_AWAKE_MINUTES = 30; // Max minutes of awake time to consider ending a sleep session

    public List<NightSleepDataModel> getData() {
        return data;
    }

    private static List<ActivityKind> kinds = List.of(ActivityKind.DEEP_SLEEP, ActivityKind.LIGHT_SLEEP, ActivityKind.REM_SLEEP, ActivityKind.AWAKE_SLEEP);

    public static NightSleepData compute(final GBDevice device, int timeTo) {
        if (device == null) {
            LOG.error("Device is null");
            return null;
        }

        Calendar day = GregorianCalendar.getInstance();
        day.setTimeInMillis(timeTo * 1000L);

        try (DBHandler handler = WearableApplication.acquireDB()) {
            NightSleepData nightSleepData = new NightSleepData();

            // TODO: Have a setting in the app to set the time range for night sleep
            //       so we can infer that any other sleep data we get is a day nap
            List<? extends ActivitySample> samples = DataCommon.getSamplesOfDay(handler, day, -12, device);
            NightSleepDataModel.SleepState last = null;
            NightSleepDataModel current = null;
            boolean isSleeping = false;
            int lastAwakeTs = 0;
            for (ActivitySample sample : samples) {
                ActivityKind kind = sample.getKind();
                boolean isSleepState = kinds.contains(kind);
                if (!isSleeping && !isSleepState) {
                    // Only consider the kinds we are interested in
                    continue;
                }

                isSleeping = true;
                NightSleepDataModel.SleepState state = NightSleepDataModel.SleepState.fromActivityKind(kind);

                if (state == last) {
                    current.end = sample.getTimestamp();
                    continue;
                }

                if (current != null) {
                    current.end = sample.getTimestamp();
                }

                // If we are awake and the last time we were awake was less than 30 minutes ago, we are still awake
                // cut this session off
                if (state == NightSleepDataModel.SleepState.AWAKE) {
                    if (lastAwakeTs > 0 && sample.getTimestamp() - lastAwakeTs >= MAX_AWAKE_MINUTES * 60) {
                        // Remove current from the list
                        nightSleepData.data.remove(current);
                        break;
                    }
                    lastAwakeTs = sample.getTimestamp();
                }

                last = state;
                current = new NightSleepDataModel();
                current.state = state;
                current.start = sample.getTimestamp();
                current.end = sample.getTimestamp();
                nightSleepData.data.add(current);
            }

            // Compute the total sleep time
            ActivityAnalysis analysis = new ActivityAnalysis();
            ActivityAmounts amounts = analysis.calculateActivityAmounts(DataCommon.getSamplesOfDay(handler, day, -12, device));
            for (ActivityAmount amount : amounts.getAmounts()) {
                if (kinds.contains(amount.getActivityKind())) {
                    nightSleepData.totalMinutes += (int) (amount.getTotalSeconds() / 60);
                }
            }

            nightSleepData.startTime = nightSleepData.data.get(0).start;
            nightSleepData.endTime = nightSleepData.data.get(nightSleepData.data.size() - 1).end;
            return nightSleepData;
        } catch (final Exception e) {
            LOG.error("Could not acquire database", e);
        }

        return null;
    }

}
