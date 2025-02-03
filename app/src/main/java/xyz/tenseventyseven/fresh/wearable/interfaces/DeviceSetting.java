package xyz.tenseventyseven.fresh.wearable.interfaces;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

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
    }

    public DeviceSettingType type;
    public String key;
    public int title;
    public int summary;
    public int icon;
    public String defaultValue;
    public String screen;
    private Map<String, Object> extras = new HashMap<>();

    // Main constructor
    public DeviceSetting(DeviceSettingType type, String key, int title, int summary, int icon, String defaultValue) {
        this.type = type;
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.icon = icon;
        this.defaultValue = defaultValue;
    }

    protected DeviceSetting(Parcel in) {
        key = in.readString();
        title = in.readInt();
        summary = in.readInt();
        icon = in.readInt();
        defaultValue = in.readString();
        screen = in.readString();
        screenSummary = in.readInt();
        settings = in.createTypedArrayList(DeviceSetting.CREATOR);

        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            Object value = in.readValue(getClass().getClassLoader());
            extras.put(key, value);
        }
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

    // Factory methods
    public static DeviceSetting divider() {
        return new DeviceSetting(DeviceSettingType.DIVIDER, null, 0, 0, 0, null);
    }

    public static DeviceSetting screen(String key, int title, int summary, int icon, String screen) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SCREEN, key, title, summary, icon, null);
        setting.screen = screen;
        return setting;
    }

    public static DeviceSetting checkbox(String key, int title, int summary, int icon, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.CHECKBOX, key, title, summary, icon, defaultValue);
    }

    // Checkbox without icon
    public static DeviceSetting checkbox(String key, int title, int summary, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.CHECKBOX, key, title, summary, 0, defaultValue);
    }

    // Checkbox without icon and summary
    public static DeviceSetting checkbox(String key, int title, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.CHECKBOX, key, title, 0, 0, defaultValue);
    }

    public static DeviceSetting switcher(String key, int title, int summary, int icon, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH, key, title, summary, icon, defaultValue);
    }

    // Switcher without icon
    public static DeviceSetting switcher(String key, int title, int summary, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH, key, title, summary, 0, defaultValue);
    }

    // Switcher without icon and summary
    public static DeviceSetting switcher(String key, int title, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH, key, title, 0, 0, defaultValue);
    }

    public boolean isDivider() {
        return type == DeviceSettingType.DIVIDER;
    }

    public boolean screenHasIntent() {
        return (type == DeviceSettingType.SCREEN || type == DeviceSettingType.SWITCH_SCREEN) && screen != null;
    }

    /* For use when type == SCREEN or SWITCH_SCREEN and 'screen' is null */
    public int screenSummary;
    public List<DeviceSetting> settings;

    public void addExtra(String key, Object value) {
        extras.put(key, value);
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
        dest.writeString(screen);
        dest.writeInt(screenSummary);
        dest.writeTypedList(settings);
        dest.writeInt(extras.size());
        dest.writeMap(extras);
    }
}
