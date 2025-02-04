package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import dev.oneuiproject.oneui.widget.RoundedLinearLayout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.PreferenceScreenActivity;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public abstract class AbstractPreference extends RoundedLinearLayout {
    GBDevice device;

    DeviceSetting setting;

    SharedPreferences sharedPreferences;

    Map<String, Object> extras = new HashMap<>();

    public AbstractPreference(Context context) {
        super(context);
    }

    public AbstractPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AbstractPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AbstractPreference(Context context, GBDevice device, DeviceSetting setting) {
        super(context);
        this.device = device;
        this.setting = setting;
        this.sharedPreferences = Application.getDevicePrefs(device).getPreferences();
    }

    @Override
    public void onViewAdded(View child) {
        Log.d("AbstractPreference", "onViewAdded");
        super.onViewAdded(child);
    }

    @Override
    public void onViewRemoved(View child) {
        Log.d("AbstractPreference", "onViewRemoved");
        super.onViewRemoved(child);
    }

    public void init(Context context) {
        // Empty by default; subclasses must override this
    }

    public String getKey() {
        return setting.key;
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getRootView().post(runnable);
    }

    private void notifyPreferenceChanged(final String preferenceKey) {
        if (this.device == null) {
            return;
        }

        invokeLater(() -> Application.deviceService(device).onSendConfiguration(preferenceKey));
    }

    public void onPreferenceChanged() {
        notifyPreferenceChanged(this.setting.key);
    }

    public void onPreferenceChangedNotify() {

    }

    public void onPreferenceClicked() {
        // Empty by default; subclasses must override this
        onPreferenceChanged();
    }

    protected LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(getContext());
    }

    public void setText(String text) {
        // Empty by default; subclasses must override this
    }

    public void setSummary(String summary) {
        // Empty by default; subclasses must override this
    }

    public void setText(int resId) {
        // Empty by default; subclasses must override this
    }

    public void setSummary(int resId) {
        // Empty by default; subclasses must override this
    }

    public void setSummaryColor(int color) {
        // Empty by default; subclasses must override this
    }

    public void setSummaryColorRes(int resId) {
        // Empty by default; subclasses must override this
    }

    public void seslSetRoundCorners(int corners) {
        setRoundedCorners(corners);
    }

    public void setActivity(String activity) {
        this.setting.activity = activity;
    }

    public void putExtra(String key, Object value) {
        this.setting.putExtra(key, value);
    }

    public void clearExtras() {
        this.setting.clearExtras();
    }

    public void removeExtra(String key) {
        this.setting.removeExtra(key);
    }

    void launchActivity(boolean isSwitchbar) {
        Intent intent = new Intent();
        if (this.setting.activity == null) {
            intent = new Intent(getContext(), PreferenceScreenActivity.class);
            intent.putExtra(GBDevice.EXTRA_DEVICE, this.device);
            intent.putExtra(DeviceSetting.EXTRA_IS_SWITCH_BAR, isSwitchbar);
            intent.putExtra(DeviceSetting.EXTRA_SETTING, this.setting);
        } else {
            intent.setClassName(getContext(), this.setting.activity);
        }

        for (Map.Entry<String, Object> entry : this.extras.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                intent.putExtra(key, (String) value);
            } else if (value instanceof Integer) {
                intent.putExtra(key, (int) value);
            } else if (value instanceof Boolean) {
                intent.putExtra(key, (boolean) value);
            } else if (value instanceof Float) {
                intent.putExtra(key, (float) value);
            } else if (value instanceof Double) {
                intent.putExtra(key, (double) value);
            } else if (value instanceof Long) {
                intent.putExtra(key, (long) value);
            } else if (value instanceof Short) {
                intent.putExtra(key, (short) value);
            } else if (value instanceof Byte) {
                intent.putExtra(key, (byte) value);
            } else if (value instanceof Character) {
                intent.putExtra(key, (char) value);
            } else if (value instanceof Parcelable) {
                intent.putExtra(key, (Parcelable) value);
            } else {
                intent.putExtra(key, value.toString());
            }
        }

        getContext().startActivity(intent);
    }
}
