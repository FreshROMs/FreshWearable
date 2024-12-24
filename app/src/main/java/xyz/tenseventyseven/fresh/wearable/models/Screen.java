package xyz.tenseventyseven.fresh.wearable.models;

import android.os.Parcel;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a screen with potential nested sub-screens.
 */
public class Screen {
    private final String key;
    private final List<Integer> settings = new ArrayList<>();
    private final Map<String, Screen> screens = new LinkedHashMap<>();

    public Screen(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public List<Integer> getSettings() {
        return settings;
    }

    public Map<String, Screen> getScreens() {
        return screens;
    }

    public boolean hasSetting(int setting) {
        return settings.contains(setting);
    }

    public boolean hasSetting(Screen parent, int setting) {
        return parent.hasSetting(setting);
    }

    public boolean hasSetting(String key) {
        return screens.containsKey(key);
    }

    public boolean hasScreen(String key) {
        return screens.containsKey(key);
    }

    public boolean hasScreen(Screen parent, String key) {
        return parent.hasScreen(key);
    }

    /**
     * Find a screen by key in this screen.
     *
     * @param key
     * @return the screen if found, null otherwise
     */
    @Nullable
    public Screen getRootScreen(String key) {
        return screens.get(key);
    }

    /**
     * Find a screen by key in this screen or any of its nested sub-screens.
     *
     * @param key
     * @return the screen if found, null otherwise
     */
    @Nullable
    public Screen getScreen(String key) {
        Screen screen = screens.get(key);
        if (screen != null) {
            return screen;
        }

        // Recursively search in nested sub-screens
        for (Screen nested : screens.values()) {
            return nested.getScreen(key);
        }

        return null;
    }

    public Screen getScreen(Screen parent, String key) {
        return parent.getScreen(key);
    }

    public int getScreenCount() {
        return screens.size();
    }

    public void addSetting(int setting) {
        if (!settings.contains(setting)) {
            settings.add(setting);
        }
    }

    public void addSetting(int setting, int index) {
        if (!settings.contains(setting)) {
            settings.add(index, setting);
        }
    }

    public void addSetting(Screen parent, int setting) {
        parent.addSetting(setting);
    }

    public void addSetting(Screen parent, int setting, int index) {
        parent.addSetting(setting, index);
    }

    public void addSetting(Screen parent, Screen child, int setting) {
        child.addSetting(setting);
    }

    public void addSetting(Screen parent, Screen child, int setting, int index) {
        child.addSetting(setting, index);
    }

    public void addSetting(String parentKey, int setting) {
        Screen parent = getScreen(parentKey);
        if (parent != null) {
            parent.addSetting(setting);
        }
    }

    public void addSetting(String parentKey, int setting, int index) {
        Screen parent = getScreen(parentKey);
        if (parent != null) {
            parent.addSetting(setting, index);
        }
    }

    public void addScreen(String key, Screen screen) {
        screens.put(key, screen);
    }

    public Screen addScreen(String key) {
        Screen screen = new Screen(key);
        screens.put(key, screen);

        return screen;
    }

    public void addScreen(String key, Screen screen, Screen parent) {
        parent.addScreen(key, screen);
    }

    public static Screen createFromParcel(Parcel in) {
        String key = in.readString();
        Screen screen = new Screen(key);
        int numSettings = in.readInt();
        for (int i = 0; i < numSettings; i++) {
            screen.addSetting(in.readInt());
        }
        int numScreens = in.readInt();
        for (int i = 0; i < numScreens; i++) {
            Screen nested = createFromParcel(in);
            screen.addScreen(nested.getKey(), nested);
        }
        return screen;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeInt(settings.size());
        for (final Integer setting : settings) {
            dest.writeInt(setting);
        }
        dest.writeInt(screens.size());
        for (final Screen nested : screens.values()) {
            nested.writeToParcel(dest, flags);
        }
    }

    public Screen merge(Screen other) {
        for (final int setting : other.settings) {
            addSetting(setting);
        }
        screens.putAll(other.screens);
        return this;
    }
}
