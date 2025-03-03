/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class PebbleReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        Prefs prefs = Application.getPrefs();
        if ("never".equals(prefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(prefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        String messageType = intent.getStringExtra("messageType");
        if (!messageType.equals("PEBBLE_ALERT")) {
            LOG.info("non PEBBLE_ALERT message type not supported");
            return;
        }

        if (!intent.hasExtra("notificationData")) {
            LOG.info("missing notificationData extra");
            return;
        }

        NotificationSpec notificationSpec = new NotificationSpec();

        String notificationData = intent.getStringExtra("notificationData");
        try {
            JSONArray notificationJSON = new JSONArray(notificationData);
            notificationSpec.title = notificationJSON.getJSONObject(0).getString("title");
            notificationSpec.body = notificationJSON.getJSONObject(0).getString("body");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (notificationSpec.title != null) {
            notificationSpec.type = NotificationType.UNKNOWN;
            String sender = intent.getStringExtra("sender");
            if (Application.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
                if (Application.appIsPebbleBlacklisted(sender)) {
                    LOG.info("Ignoring Pebble message, application " + sender + " is blacklisted");
                    return;
                }
            } else {
                if (!Application.appIsPebbleBlacklisted(sender)) {
                    LOG.info("Ignoring Pebble message, application " + sender + " is not whitelisted");
                    return;
                }
            }

            if ("Conversations".equals(sender)) {
                notificationSpec.type = NotificationType.CONVERSATIONS;
            }
            else if ("OsmAnd".equals(sender)) {
                notificationSpec.type = NotificationType.GENERIC_NAVIGATION;
            }
            Application.deviceService().onNotification(notificationSpec);
        }
    }
}
