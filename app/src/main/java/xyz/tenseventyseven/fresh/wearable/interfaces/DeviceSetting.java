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
    public String activity;
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
        type = DeviceSettingType.values()[in.readInt()];
        key = in.readString();
        title = in.readInt();
        summary = in.readInt();
        icon = in.readInt();
        defaultValue = in.readString();
        activity = in.readString();
        screenSummary = in.readInt();
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

    // Factory methods
    public static DeviceSetting divider() {
        return new DeviceSetting(DeviceSettingType.DIVIDER, null, 0, 0, 0, null);
    }

    public static DeviceSetting screen(String key, int title, int summary, int icon, String screen) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SCREEN, key, title, summary, icon, null);
        setting.activity = screen;
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

    // Switcher with screen with title, summary, and icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, int icon, String screen, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
        setting.activity = screen;
        return setting;
    }

    // Switcher with screen without icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, String screen, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, 0, defaultValue);
        setting.activity = screen;
        return setting;
    }

    // Switcher without icon and summary
    public static DeviceSetting switcherScreen(String key, int title, String screen, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, 0, 0, defaultValue);
        setting.activity = screen;
        return setting;
    }

    // Switcher without screen with title, summary, and icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, int icon, boolean noScreenSummary, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, defaultValue);
    }

    // Switcher without screen without icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, boolean noScreenSummary, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, 0, defaultValue);
    }

    // Switcher without screen without icon and summary
    public static DeviceSetting switcherScreen(String key, int title, String defaultValue) {
        return new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, 0, 0, defaultValue);
    }

    // Switcher without screen with screen summary with title, summary, and icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, int icon, int screenSummary, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, icon, null);
        setting.screenSummary = screenSummary;
        return setting;
    }

    // Switcher without screen with screen summary without icon
    public static DeviceSetting switcherScreen(String key, int title, int summary, int screenSummary, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, summary, 0, defaultValue);
        setting.screenSummary = screenSummary;
        return setting;
    }

    // Switcher without screen with screen summary without icon and summary
    public static DeviceSetting switcherScreen(String key, int title, int screenSummary, String defaultValue) {
        DeviceSetting setting = new DeviceSetting(DeviceSettingType.SWITCH_SCREEN, key, title, 0, 0, defaultValue);
        setting.screenSummary = screenSummary;
        return setting;
    }

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
    public int screenSummary;
    public List<DeviceSetting> settings;

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
        dest.writeTypedList(settings);
//        dest.writeInt(extras.size());
//        dest.writeMap(extras);
    }
}
