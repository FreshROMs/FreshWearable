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
        this.type = DeviceSettingType.values()[in.readInt()];
        this.key = in.readString();
        this.title = in.readInt();
        this.summary = in.readInt();
        this.icon = in.readInt();
        this.dependency = in.readString();
        this.dependencyValue = in.readString();
        this.defaultValue = in.readString();
        this.activity = in.readString();
        this.valueAsSummary = in.readByte() != 0;
        this.entries = in.readInt();
        this.entryValues = in.readInt();
        this.entryDrawables = in.readInt();
        this.entryDescriptions = in.readInt();
        this.screenSummary = in.readInt();

        int settingsSize = in.readInt();
        this.settings = new ArrayList<>(settingsSize);
        for (int i = 0; i < settingsSize; i++) {
            this.settings.add(new DeviceSetting(in));
        }

        int extrasSize = in.readInt();
        this.extras = new HashMap<>(extrasSize);
        for (int i = 0; i < extrasSize; i++) {
            String key = in.readString();
            Object value = null;
            switch (in.readInt()) {
                case 0:
                    value = in.readString();
                    break;
                case 1:
                case 6:
                case 8:
                    value = in.readInt();
                    break;
                case 2:
                    value = in.readByte() != 0;
                    break;
                case 3:
                    value = in.readFloat();
                    break;
                case 4:
                    value = in.readDouble();
                    break;
                case 5:
                    value = in.readLong();
                    break;
                case 7:
                    value = in.readByte();
                    break;
                case 9:
                    value = in.readParcelable(getClass().getClassLoader());
                    break;
            }
            this.extras.put(key, value);
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
        dest.writeInt(this.type.ordinal());
        dest.writeString(this.key);
        dest.writeInt(this.title);
        dest.writeInt(this.summary);
        dest.writeInt(this.icon);
        dest.writeString(this.dependency);
        dest.writeString(this.dependencyValue);
        dest.writeString(this.defaultValue);
        dest.writeString(this.activity);
        dest.writeByte((byte) (this.valueAsSummary ? 1 : 0));
        dest.writeInt(this.entries);
        dest.writeInt(this.entryValues);
        dest.writeInt(this.entryDrawables);
        dest.writeInt(this.entryDescriptions);
        dest.writeInt(this.screenSummary);

        dest.writeInt(this.settings.size());
        for (DeviceSetting setting : this.settings) {
            setting.writeToParcel(dest, flags);
        }

        dest.writeInt(this.extras.size());
        for (Map.Entry<String, Object> entry : this.extras.entrySet()) {
            dest.writeString(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof String) {
                dest.writeInt(0);
                dest.writeString((String) value);
            } else if (value instanceof Integer) {
                dest.writeInt(1);
                dest.writeInt((Integer) value);
            } else if (value instanceof Boolean) {
                dest.writeInt(2);
                dest.writeByte((byte) ((Boolean) value ? 1 : 0));
            } else if (value instanceof Float) {
                dest.writeInt(3);
                dest.writeFloat((Float) value);
            } else if (value instanceof Double) {
                dest.writeInt(4);
                dest.writeDouble((Double) value);
            } else if (value instanceof Long) {
                dest.writeInt(5);
                dest.writeLong((Long) value);
            } else if (value instanceof Short) {
                dest.writeInt(6);
                dest.writeInt((Short) value);
            } else if (value instanceof Byte) {
                dest.writeInt(7);
                dest.writeInt((Byte) value);
            } else if (value instanceof Character) {
                dest.writeInt(8);
                dest.writeInt((Character) value);
            } else if (value instanceof Parcelable) {
                dest.writeInt(9);
                dest.writeParcelable((Parcelable) value, flags);
            }
        }
    }
}
