package xyz.tenseventyseven.fresh.wearable.interfaces;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceSetting implements Parcelable {
    public static String EXTRA_SETTING = "setting";
    public static String EXTRA_IS_SWITCH_BAR = "isSwitchBar";

    public enum DeviceSettingType {
        DIVIDER,
        SCREEN,
        CHECKBOX,
        SWITCH,
        SWITCH_SCREEN,
        DROPDOWN,
    }

    public DeviceSettingType type = DeviceSettingType.DIVIDER;
    public String key = "";
    public int title = 0;
    public int summary = 0;
    public int icon = 0;
    public String dependency = "";
    public String dependencyValue = "";
    public String defaultValue = "";
    public String activity = "";
    public boolean valueAsSummary = false;
    private Map<String, Object> extras = new HashMap<>();

    // For SpinnerPreference, add labels and corresponding values
    public int entries = 0;
    public int entryValues = 0;

    // Main constructor
    public DeviceSetting(DeviceSettingType type, String key, int title, int summary, int icon, String defaultValue) {
        this.type = type;
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.icon = icon;
        this.defaultValue = defaultValue;
    }

    public DeviceSetting(DeviceSettingType type) {
        this.type = type;
    }

    protected DeviceSetting(Parcel in) {
//        dest.writeInt(type.ordinal());
//        dest.writeString(key);
//        dest.writeInt(title);
//        dest.writeInt(summary);
//        dest.writeInt(icon);
//        dest.writeString(defaultValue);
//        dest.writeString(activity);
//        dest.writeInt(screenSummary);
//        dest.writeString(dependency);
//        dest.writeString(dependencyValue);
//        dest.writeInt(valueAsSummary ? 1 : 0);
//        dest.writeInt(entries);
//        dest.writeInt(entryValues);
//        dest.writeTypedList(settings);

        type = DeviceSettingType.values()[in.readInt()];
        key = in.readString();
        title = in.readInt();
        summary = in.readInt();
        icon = in.readInt();
        defaultValue = in.readString();
        activity = in.readString();
        screenSummary = in.readInt();
        dependency = in.readString();
        dependencyValue = in.readString();
        valueAsSummary = in.readInt() == 1;
        entries = in.readInt();
        entryValues = in.readInt();
        settings = in.createTypedArrayList(DeviceSetting.CREATOR);

//        int size = in.readInt();
//        if (size == 0) {
//            return;
//        }
//
//        for (int i = 0; i < size; i++) {
//            String key = in.readString();
//            Object value = in.readValue(getClass().getClassLoader());
//            extras.put(key, value);
//        }
    }

    public static final Creator<DeviceSetting> CREATOR = new Creator<DeviceSetting>() {
        @Override
        public DeviceSetting createFromParcel(Parcel in) {
            return new DeviceSetting(in);
        }

        @Override
        public DeviceSetting[] newArray(int size) {
            return new DeviceSetting[size];
        }
    };

    /*
     * Factory methods
     */
    public static DeviceSetting divider() {
        return new DeviceSetting(DeviceSettingType.DIVIDER);
    }

    // Divider with title
    public static DeviceSetting divider(int title) {
        return new DeviceSetting(DeviceSettingType.DIVIDER, "", title, 0, 0, null);
    }

    public static DeviceSetting screen(String key, int title, int summary, int icon) {
        return new DeviceSetting(DeviceSettingType.SCREEN, key, title, summary, icon, null);
    }

    public static DeviceSetting screen(String key, int title, int summary, int icon, String activity) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SCREEN, key, title, summary, icon, null);
        setting.activity = activity;
        return setting;
    }

    public static DeviceSetting checkbox(String key, int title, int summary, int icon, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.CHECKBOX, key, title, summary, icon, defaultValue);
    }

    public static DeviceSetting switchSetting(String key, int title, int summary, int icon, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH, key, title, summary, icon, defaultValue);
    }

    public static DeviceSetting switchSetting(String key, int title, int summary, int icon, String defaultValue, boolean valueAsSummary) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH, key, title, summary, icon, defaultValue);
        setting.valueAsSummary = valueAsSummary;
        return setting;
    }

    public static DeviceSetting switchScreen(String key, int title, int summary, int icon, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
    }

    public static DeviceSetting switchScreen(String key, int title, int summary, int icon, String defaultValue, boolean valueAsSummary) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
        setting.valueAsSummary = valueAsSummary;
        return setting;
    }

    public static DeviceSetting switchScreen(String key, int title, int summary, int icon, String defaultValue, String activity) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
        setting.activity = activity;
        return setting;
    }

    public static DeviceSetting switchScreen(String key, int title, int summary, int icon, String defaultValue, String activity, boolean valueAsSummary) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
        setting.activity = activity;
        setting.valueAsSummary = valueAsSummary;
        return setting;
    }

    public static DeviceSetting dropdown(String key, int title, int icon, String defaultValue, int entries, int entryValues) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.DROPDOWN, key, title, 0, icon, defaultValue);
        setting.entries = entries;
        setting.entryValues = entryValues;
        return setting;
    }

    /*
     * Helper methods
     */

    public boolean isDivider() {
        return type == DeviceSettingType.DIVIDER;
    }

    public boolean screenHasIntent() {
        return (type == DeviceSettingType.SCREEN || type == DeviceSettingType.SWITCH_SCREEN) && activity != null;
    }

    public boolean hasIcon() {
        return icon != 0;
    }

    /* For use when type == SCREEN or SWITCH_SCREEN and 'screen' is null */
    public int screenSummary = 0;
    public List<DeviceSetting> settings = new ArrayList<>();

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void putExtra(String key, Object value) {
        extras.put(key, value);
    }

    public void clearExtras() {
        extras.clear();
    }

    public void removeExtra(String key) {
        extras.remove(key);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(type.ordinal());
        dest.writeString(key);
        dest.writeInt(title);
        dest.writeInt(summary);
        dest.writeInt(icon);
        dest.writeString(defaultValue);
        dest.writeString(activity);
        dest.writeInt(screenSummary);
        dest.writeString(dependency);
        dest.writeString(dependencyValue);
        dest.writeInt(valueAsSummary ? 1 : 0);
        dest.writeInt(entries);
        dest.writeInt(entryValues);
        dest.writeTypedList(settings);
//        dest.writeInt(extras.size());
//        dest.writeMap(extras);
    }
}
