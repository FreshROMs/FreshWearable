package nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.Application;

public class SleepAsAndroidReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(SleepAsAndroidReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Application.getPrefs().getBoolean("pref_key_sleepasandroid_enable", false)) {
            Application.deviceService().onSleepAsAndroidAction(action, intent.getExtras());
        }
    }
}