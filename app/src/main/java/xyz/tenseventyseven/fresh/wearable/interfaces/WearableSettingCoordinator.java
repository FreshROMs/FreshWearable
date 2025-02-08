package xyz.tenseventyseven.fresh.wearable.interfaces;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.List;

public abstract class WearableSettingCoordinator {
    public List<DeviceSetting> getSettings() {
        return null;
    }

    public void onSettingChanged(PreferenceScreen preferenceScreen, Preference preference) {
    }
}
