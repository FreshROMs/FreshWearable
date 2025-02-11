package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import xyz.tenseventyseven.fresh.Application;

public class DoNotDisturbModeReceiver extends BroadcastReceiver {
    int lastFilter;

    public DoNotDisturbModeReceiver(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        lastFilter = notificationManager.getCurrentInterruptionFilter();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(intent.getAction())) {
            Log.w("DoNotDisturbModeReceiver", "Unexpected action " + intent.getAction());
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int currentFilter = notificationManager.getCurrentInterruptionFilter();
        if (currentFilter == lastFilter) {
            return;
        }

        lastFilter = currentFilter;
        Application.deviceService().onSetDNDMode(currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL);
    }
}
