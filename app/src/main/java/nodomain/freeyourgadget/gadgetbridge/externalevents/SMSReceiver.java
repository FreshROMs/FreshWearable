/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, mvn23, Normano64, Zhong Jianxin

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

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = Application.getPrefs();
        if ("never".equals(prefs.getString("notification_mode_sms", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(prefs.getString("notification_mode_sms", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager != null && powermanager.isScreenOn()) {
                return;
            }
        }

        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.type = NotificationType.GENERIC_SMS;

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                int pduSize = pdus.length;
                Map<String, StringBuilder> messageMap = new LinkedHashMap<>();
                SmsMessage[] messages = new SmsMessage[pduSize];
                for (int i = 0; i < pduSize; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String originatingAddress = messages[i].getOriginatingAddress();
                    if (!messageMap.containsKey(originatingAddress)) {
                        messageMap.put(originatingAddress, new StringBuilder());
                    }
                    messageMap.get(originatingAddress).append(messages[i].getMessageBody());
                }
                for (Map.Entry<String, StringBuilder> entry : messageMap.entrySet()) {
                    String originatingAddress = entry.getKey();
                    if (originatingAddress != null) {
                        notificationSpec.body = entry.getValue().toString();
                        notificationSpec.phoneNumber = originatingAddress;
                        notificationSpec.attachedActions = new ArrayList<>();

                        // REPLY action
                        NotificationSpec.Action replyAction = new NotificationSpec.Action();
                        replyAction.title = context.getString(R.string._pebble_watch_reply);
                        replyAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR;
                        notificationSpec.attachedActions.add(replyAction);

                        // DISMISS ALL action
                        NotificationSpec.Action dismissAllAction = new NotificationSpec.Action();
                        dismissAllAction.title = context.getString(R.string.notifications_dismiss_all);
                        dismissAllAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS_ALL;
                        notificationSpec.attachedActions.add(dismissAllAction);

                        int dndSuppressed = 0;
                        switch (Application.getGrantedInterruptionFilter()) {
                            case NotificationManager.INTERRUPTION_FILTER_ALL:
                                break;
                            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                            case NotificationManager.INTERRUPTION_FILTER_NONE:
                                dndSuppressed = 1;
                                break;
                            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                                if (Application.isPriorityNumber(Policy.PRIORITY_CATEGORY_MESSAGES, notificationSpec.phoneNumber)) {
                                    break;
                                }
                                dndSuppressed = 1;
                        }
                        if (prefs.getBoolean("notification_filter", false) && dndSuppressed == 1) {
                            return;
                        }
                        notificationSpec.dndSuppressed = dndSuppressed;
                        Application.deviceService().onNotification(notificationSpec);
                    }
                }
            }
        }
    }
}
