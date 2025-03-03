/*  Copyright (C) 2019-2024 Andreas Shimokawa, Daniel Dakhno, Ganblejs,
    José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import xyz.tenseventyseven.fresh.BuildConfig;
import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;

public class AutoConnectIntervalReceiver extends BroadcastReceiver {

    final DeviceCommunicationService service;
    static int mDelay = 4;
    private static final Logger LOG = LoggerFactory.getLogger(AutoConnectIntervalReceiver.class);

    public AutoConnectIntervalReceiver(DeviceCommunicationService service) {
        this.service = service;
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        LocalBroadcastManager.getInstance(service).registerReceiver(this, filterLocal);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        GBDevice[] devices = service.getGBDevices();
        if (action.equals(DeviceManager.ACTION_DEVICES_CHANGED)){
            boolean scheduleAutoConnect = false;
            boolean allDevicesInitialized = true;
            for(GBDevice device : devices){
                if(!device.isInitialized()) {
                    allDevicesInitialized = false;
                    if (device.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                        scheduleAutoConnect = true;
                    }
                }
            }

            if(allDevicesInitialized){
                LOG.info("will reset connection delay, all devices are initialized!");
                mDelay = 4;
                return;
            }
            if(scheduleAutoConnect){
                scheduleReconnect();
            }
        }else if (action.equals("GB_RECONNECT")){
            for(GBDevice device : devices){
                if(device.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                    LOG.info("Will re-connect to " + device.getAddress() + "(" + device.getName() + ")");
                    Application.deviceService(device).connect();
                }
            }
        }
    }

    public void scheduleReconnect() {
        mDelay*=2;
        if (mDelay > 64) {
            mDelay = 64;
        }
        scheduleReconnect(mDelay);
    }

    public void scheduleReconnect(int delay) {
        LOG.info("scheduling reconnect in " + delay + " seconds");
        AlarmManager am = (AlarmManager) (Application.getContext().getSystemService(Context.ALARM_SERVICE));
        Intent intent = new Intent("GB_RECONNECT");
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntentUtils.getBroadcast(Application.getContext(), 0, intent, 0, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Calendar.getInstance().
                    getTimeInMillis() + delay * 1000, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().
                    getTimeInMillis() + delay * 1000, pendingIntent);
        }
    }

    public void destroy() {
        LocalBroadcastManager.getInstance(service).unregisterReceiver(this);
    }

}
