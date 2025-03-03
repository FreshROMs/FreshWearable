package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import xyz.tenseventyseven.fresh.Application;

public class VolumeChangeReceiver {
    private final Logger LOG = LoggerFactory.getLogger(VolumeChangeReceiver.class);

    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private AudioManager audioManager;
    private Context context;
    private boolean registered = false;
    private VolumeChangeReceiver.VolumeBroadcastReceiver volumeBroadcastReceiver;

    public VolumeChangeReceiver() {}

    public int getCurrentMusicVolume() {
        AudioManager audioManager = this.audioManager;
        if (audioManager != null) {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return -1;
    }

    public int getMaxMusicVolume() {
        AudioManager audioManager = this.audioManager;
        if (audioManager != null) {
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 15;
    }

    public void registerReceiver(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.volumeBroadcastReceiver = new VolumeBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VOLUME_CHANGED_ACTION);
        ContextCompat.registerReceiver(this.context, this.volumeBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        this.registered = true;
    }

    public void unregisterReceiver() {
        if (this.registered) {
            try {
                this.context.unregisterReceiver(this.volumeBroadcastReceiver);
                this.registered = false;
            } catch (Exception e) {
                LOG.error("Error unregister volume receiver", e);
            }
        }
    }

    private static class VolumeBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<VolumeChangeReceiver> observerWeakReference;

        public VolumeBroadcastReceiver(VolumeChangeReceiver volumeChangeObserver) {
            this.observerWeakReference = new WeakReference<>(volumeChangeObserver);
        }

        public void onReceive(Context context, Intent intent) {
            VolumeChangeReceiver volumeChangeObserver;
            int currentMusicVolume;
            if (VolumeChangeReceiver.VOLUME_CHANGED_ACTION.equals(intent.getAction()) && intent.getIntExtra(VolumeChangeReceiver.EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC && (volumeChangeObserver = this.observerWeakReference.get()) != null && (currentMusicVolume = volumeChangeObserver.getCurrentMusicVolume()) >= 0) {
                final int volumePercentage = (byte) Math.round(100 * (currentMusicVolume / (float) volumeChangeObserver.getMaxMusicVolume()));
                Application.deviceService().onSetPhoneVolume(volumePercentage);
            }
        }
    }
}
