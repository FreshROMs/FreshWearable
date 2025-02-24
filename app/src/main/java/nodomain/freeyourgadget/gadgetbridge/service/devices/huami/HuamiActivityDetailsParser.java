/*  Copyright (C) 2019-2024 Andreas Shimokawa, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import xyz.tenseventyseven.fresh.AppException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuamiActivityDetailsParser extends AbstractHuamiActivityDetailsParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiActivityDetailsParser.class);

    private static final byte TYPE_GPS = 0;
    private static final byte TYPE_HR = 1;
    private static final byte TYPE_PAUSE = 2;
    private static final byte TYPE_RESUME = 3;
    private static final byte TYPE_SPEED4 = 4;
    private static final byte TYPE_SPEED5 = 5;
    private static final byte TYPE_SPEED6 = 6;
    private static final byte TYPE_SWIMMING = 8;

    private final ActivityTrack activityTrack;
    private final Date baseDate;
    private long baseLongitude;
    private long baseLatitude;
    private int baseAltitude;
    private ActivityPoint lastActivityPoint;

    public void setSkipCounterByte(boolean skipCounterByte) {
        this.skipCounterByte = skipCounterByte;
    }

    private boolean skipCounterByte;

    public HuamiActivityDetailsParser(BaseActivitySummary summary) {
        this.baseLongitude = summary.getBaseLongitude();
        this.baseLatitude = summary.getBaseLatitude();
        this.baseAltitude = summary.getBaseAltitude();
        this.baseDate = summary.getStartTime();

        this.activityTrack = new ActivityTrack();
        activityTrack.setUser(summary.getUser());
        activityTrack.setDevice(summary.getDevice());
        activityTrack.setName(createActivityName(summary));
    }

    public ActivityTrack parse(byte[] bytes) throws AppException {
        int i = 0;
        try {
            long totalTimeOffset = 0;
            int lastTimeOffset = 0;
            while (i < bytes.length) {
                if (skipCounterByte && (i % 17) == 0) {
                    i++;
                }

                byte type = bytes[i++]; // lgtm [java/index-out-of-bounds]]
                int timeOffset = BLETypeConversions.toUnsigned(bytes[i++]); // lgtm [java/index-out-of-bounds]
                // handle timeOffset overflows (1 byte, always increasing, relative to base)
                if (lastTimeOffset <= timeOffset) {
                    timeOffset = timeOffset - lastTimeOffset;
                    lastTimeOffset += timeOffset;
                } else {
                    lastTimeOffset = timeOffset;
                }
                totalTimeOffset += timeOffset;

                switch (type) {
                    case TYPE_GPS:
                        i += consumeGPSAndUpdateBaseLocation(bytes, i, totalTimeOffset);
                        break;
                    case TYPE_HR:
                        i += consumeHeartRate(bytes, i, totalTimeOffset);
                        break;
                    case TYPE_PAUSE:
                        i += consumePause(bytes, i);
                        break;
                    case TYPE_RESUME:
                        i += consumeResume(bytes, i);
                        break;
                    case TYPE_SPEED4:
                        i += consumeSpeed4(bytes, i);
                        break;
                    case TYPE_SPEED5:
                        i += consumeSpeed5(bytes, i);
                        break;
                    case TYPE_SPEED6:
                        i += consumeSpeed6(bytes, i);
                        break;
                    case TYPE_SWIMMING:
                        i += consumeSwimming(bytes, i);
                        break;
                    default:
                        LOG.warn("unknown packet type" + type);
                        i+=6;
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new AppException("Error parsing activity details: " + ex.getMessage(), ex);
        }

        fixupMissingTimestamps(activityTrack);

        return activityTrack;
    }

    private void fixupMissingTimestamps(ActivityTrack activityTrack) {
        try {
            int pointer = 0;
            List<ActivityPoint> activityPointList = new ArrayList<>();
            for (List<ActivityPoint> segment : activityTrack.getSegments()) {
                activityPointList.addAll(segment);
            }

            Date gpsStartTime = null;
            List<ActivityPoint> entriesToFixUp = new ArrayList<>();
            while (pointer < activityPointList.size() - 1) {
                ActivityPoint activityPoint = activityPointList.get(pointer);
                if (activityPoint.getLocation() == null) {
                    pointer++;
                    continue;
                }
                if (activityPoint.getTime().equals(activityPointList.get(pointer + 1).getTime())) {
                    entriesToFixUp.add(activityPoint);
                } else {
                    // found the first activity point with a proper timestamp
                    entriesToFixUp.add(activityPoint);
                    gpsStartTime = activityPointList.get(pointer + 1).getTime();
                    break;
                }
                pointer++;
            }
            if (gpsStartTime != null) {
                // now adjust those entries without a timestamp
                long differenceInSec = TimeUnit.SECONDS.convert(Math.abs(gpsStartTime.getTime() - baseDate.getTime()), TimeUnit.MILLISECONDS);

                double multiplier = (double) differenceInSec / (double) (entriesToFixUp.size());

                for (int j = 0; j < entriesToFixUp.size(); j++) {
                    long timeOffsetSeconds = Math.round(j * multiplier);
                    entriesToFixUp.get(j).setTime(makeAbsolute(timeOffsetSeconds));
                }
            }
        } catch (Exception ex) {
            LOG.warn("Error cleaning activity details", ex);
        }
    }

    private int consumeGPSAndUpdateBaseLocation(byte[] bytes, int offset, long timeOffset) {
        int i = 0;
        int longitudeDelta = BLETypeConversions.toInt16(bytes[offset + i++], bytes[offset + i++]);
        int latitudeDelta = BLETypeConversions.toInt16(bytes[offset + i++], bytes[offset + i++]);
        int altitudeDelta = BLETypeConversions.toInt16(bytes[offset + i++], bytes[offset + i++]);

        baseLongitude += longitudeDelta;
        baseLatitude += latitudeDelta;
        if (baseAltitude != -20000) {
            baseAltitude += altitudeDelta;
        }

        GPSCoordinate coordinate = new GPSCoordinate(
                convertHuamiValueToDecimalDegrees(baseLongitude),
                convertHuamiValueToDecimalDegrees(baseLatitude),
                baseAltitude);

        ActivityPoint ap = getActivityPointFor(timeOffset, coordinate);
        ap.setLocation(coordinate);
        add(ap);

        return i;
    }

    private int consumeHeartRate(byte[] bytes, int offset, long timeOffsetSeconds) {
        int v1 = BLETypeConversions.toUint16(bytes[offset]);
        int v2 = BLETypeConversions.toUint16(bytes[offset + 1]);
        int v3 = BLETypeConversions.toUint16(bytes[offset + 2]);
        int v4 = BLETypeConversions.toUint16(bytes[offset + 3]);
        int v5 = BLETypeConversions.toUint16(bytes[offset + 4]);
        int v6 = BLETypeConversions.toUint16(bytes[offset + 5]);

        if (v2 == 0 && v3 == 0 && v4 == 0 && v5 == 0 && v6 == 0) {
            // new version
            LOG.info("detected heart rate in 'new' version format");
            ActivityPoint ap = getActivityPointFor(timeOffsetSeconds);
            ap.setHeartRate(v1);
            add(ap);
        } else {
            ActivityPoint ap = getActivityPointFor(v1);
            ap.setHeartRate(v2);
            add(ap);

            ap = getActivityPointFor(v3);
            ap.setHeartRate(v4);
            add(ap);

            ap = getActivityPointFor(v5);
            ap.setHeartRate(v6);
            add(ap);
        }
        return 6;
    }

    private ActivityPoint getActivityPointFor(long timeOffsetSeconds) {
        Date time = makeAbsolute(timeOffsetSeconds);
        if (lastActivityPoint != null) {
            if (lastActivityPoint.getTime().equals(time)) {
                return lastActivityPoint;
            }
        }
        return new ActivityPoint(time);
    }

    private ActivityPoint getActivityPointFor(long timeOffsetSeconds, GPSCoordinate gpsCoordinate) {
        Date time = makeAbsolute(timeOffsetSeconds);
        if (lastActivityPoint != null) {
            if (lastActivityPoint.getTime().equals(time)) {
                if (lastActivityPoint.getLocation() != null && !lastActivityPoint.getLocation().equals(gpsCoordinate)) {
                    return new ActivityPoint(time);
                }
                return lastActivityPoint;
            }
        }
        return new ActivityPoint(time);
    }

    private Date makeAbsolute(long timeOffsetSeconds) {
        return new Date(baseDate.getTime() + timeOffsetSeconds * 1000);
    }

    private void add(ActivityPoint ap) {
        if (ap != lastActivityPoint) {
            lastActivityPoint = ap;
            activityTrack.addTrackPoint(ap);
        } else {
            LOG.info("skipping point!");
        }
    }

    private int consumePause(byte[] bytes, int offset) {
        LOG.debug("got pause packet: " + GB.hexdump(bytes, offset, 6));
        return 6;
    }

    private int consumeResume(byte[] bytes, int offset) {
        LOG.debug("got resume package: " + GB.hexdump(bytes, offset, 6));
        return 6;
    }

    private int consumeSpeed4(byte[] bytes, int offset) {
        LOG.debug("got packet type 4 (speed): " + GB.hexdump(bytes, offset, 6));
        return 6;
    }

    private int consumeSpeed5(byte[] bytes, int offset) {
        LOG.debug("got packet type 5 (speed): " + GB.hexdump(bytes, offset, 6));
        return 6;
    }

    private int consumeSpeed6(byte[] bytes, int offset) {
        LOG.debug("got packet type 6 (speed): " + GB.hexdump(bytes, offset, 6));
        return 6;
    }

    private int consumeSwimming(byte[] bytes, int offset) {
        LOG.debug("got packet type 8 (swimming?): " + GB.hexdump(bytes, offset, 6));
        return 6;
    }
}
