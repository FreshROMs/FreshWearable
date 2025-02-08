package xyz.tenseventyseven.fresh.wearable.interfaces;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class WearableSettingCoordinator {
    public List<DeviceSetting> getSettings() {
        return null;
    }

    public List<DeviceShortcut> getShortcuts() {
        return null;
    }

    public void onSettingChanged(PreferenceScreen preferenceScreen, Preference preference) {
    }

    public void onShortcutClicked(Context context, GBDevice device, String key) {
    }
}
