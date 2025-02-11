/*  Copyright (C) 2023-2025 José Rebelo, John Vincent Corcega

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
package nodomain.freeyourgadget.gadgetbridge.util;

import static xyz.tenseventyseven.fresh.Application.getContext;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import xyz.tenseventyseven.fresh.Application;

public class DoNotDisturbMode {
    private static final Logger LOG = LoggerFactory.getLogger(DoNotDisturbMode.class);

    public enum DNDMode {
        ALL(NotificationManager.INTERRUPTION_FILTER_ALL),
        PRIORITY(NotificationManager.INTERRUPTION_FILTER_PRIORITY),
        NONE(NotificationManager.INTERRUPTION_FILTER_NONE),
        ALARMS(NotificationManager.INTERRUPTION_FILTER_ALARMS),
        UNKNOWN(NotificationManager.INTERRUPTION_FILTER_UNKNOWN);

        private final int code;

        DNDMode(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static DNDMode fromCode(final int code) {
            for (final DNDMode DNDMode : values()) {
                if (DNDMode.code == code) {
                    return DNDMode;
                }
            }

            return DNDMode.UNKNOWN;
        }
    }

    public static void setPhoneMode(final String deviceAddress, final boolean enabled) {
        final Prefs prefs = new Prefs(Application.getDeviceSpecificSharedPrefs(deviceAddress));
        if (!prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_DND_SYNC, false)) {
            return; // Preference is disabled
        }

        DNDMode mode = enabled ? getPhoneDndMode(deviceAddress) : DNDMode.ALL;
        setMode(mode);
    }

    public static DNDMode getPhoneDndMode(final String deviceAddress) {
        final Prefs prefs = new Prefs(Application.getDeviceSpecificSharedPrefs(deviceAddress));
        final String mode = prefs.getString(DeviceSettingsPreferenceConst.PREF_DND_SYNC_WITH_WATCH_MODE, DNDMode.ALL.name());
        return DNDMode.valueOf(mode.toUpperCase(Locale.US));
    }

    @SuppressLint("WrongConstant")
    public static void setMode(final DNDMode mode) {
        if (mode == DNDMode.UNKNOWN) {
            LOG.warn("Unable to set unknown do not disturb mode");
            return;
        }

        final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            LOG.warn("Do not disturb permissions not granted");
            return;
        }

        notificationManager.setInterruptionFilter(mode.getCode());

    }
}
