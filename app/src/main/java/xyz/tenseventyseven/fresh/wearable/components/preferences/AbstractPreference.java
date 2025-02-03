package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import dev.oneuiproject.oneui.widget.RoundedLinearLayout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;

public abstract class AbstractPreference extends RoundedLinearLayout {
    GBDevice device;
    String key;
    int title;
    int summary;
    int icon;
    protected SharedPreferences sharedPreferences;
    String activity;

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

    public AbstractPreference(Context context, GBDevice device, String key, int title, int summary, int icon) {
        super(context);
        this.device = device;
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.icon = icon;
        this.sharedPreferences = Application.getDevicePrefs(device).getPreferences();
    }

    public void init(Context context) {
        // Empty by default; subclasses must override this
    }

    public String getKey() {
        return key;
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
        notifyPreferenceChanged(this.key);
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
        this.activity = activity;
    }

    public void putExtra(String key, Object value) {
        this.extras.put(key, value);
    }

    public void clearExtras() {
        this.extras.clear();
    }

    public void removeExtra(String key) {
        this.extras.remove(key);
    }
}
