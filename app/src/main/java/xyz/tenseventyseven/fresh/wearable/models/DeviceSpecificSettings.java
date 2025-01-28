/*  Copyright (C) 2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package xyz.tenseventyseven.fresh.wearable.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;

/**
 * A class representing device-specific settings with nested sub-screens.
 */
public class DeviceSpecificSettings implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSpecificSettings.class);
    private Screen root;

    public DeviceSpecificSettings() {
        root = new Screen("root");
    }

    public DeviceSpecificSettings(final int[] screens) {
        root = new Screen("root");
        for (final int screen : screens) {
            root.addSetting(screen);
        }
    }

    public DeviceSpecificSettings(final Screen root) {
        this.root = root;
    }

    public void addRootScreen(@XmlRes final int screen) {
        root.addSetting(screen);
    }

    public List<Integer> addRootScreen(final DeviceSpecificSettingsScreen screen) {
        if (!root.hasSetting(screen.getKey())) {
            root.addSetting(screen.getXml());
        }

        return addSubScreen(screen.getKey(), screen.getXml());
    }

    public Screen addRootScreen(final DeviceSpecificSettingsScreen screen, boolean returnScreen) {
        if (!root.hasSetting(screen.getKey())) {
            root.addSetting(screen.getXml());
        }

        return addSubScreen(screen.getKey(), returnScreen, screen.getXml());
    }

    public List<Integer> addRootScreen(final DeviceSpecificSettingsScreen screen, final int... subScreens) {
        addRootScreen(screen);
        return addSubScreen(screen, subScreens);
    }

    public Screen getParent(final String key, boolean createIfNotExists) {
        Screen parent = root.getScreen(key);
        if (parent == null && createIfNotExists) {
            parent = root.addScreen(key);
        }

        return parent;
    }

    public Screen getChild(Screen parent, final String key) {
        Screen child = parent.getScreen(key);
        if (child == null) {
            child = parent.addScreen(key);
        }

        return child;
    }

    public List<Integer> addSubScreen(final String parentKey, final int... screens) {
        Screen parent = getParent(parentKey, true);
        for (final int screen : screens) {
            parent.addSetting(screen);
        }

        return parent.getSettings();
    }

    public Screen addSubScreen(final String parentKey, boolean r, final int... screens) {
        Screen parent = getParent(parentKey, true);
        for (final int screen : screens) {
            parent.addSetting(screen);
        }

        return parent;
    }

    public List<Integer> addSubScreen(final DeviceSpecificSettingsScreen deviceSpecificSettingsScreen, final int... screens) {
        Screen parent = getParent(deviceSpecificSettingsScreen.getKey(), true);
        for (final int screen : screens) {
            parent.addSetting(screen);
        }

        return parent.getSettings();
    }

    public List<Integer> addSubScreen(final String key, final Screen parent, final int... screens) {
        Screen child = getChild(parent, key);
        for (final int screen : screens) {
            child.addSetting(screen);
        }

        return child.getSettings();
    }

    public List<Integer> addSubRootScreen(final Screen parent, final DeviceSpecificSettingsScreen screen) {
        if (!parent.hasSetting(screen.getKey())) {
            parent.addSetting(screen.getXml());
        }

        return addSubScreen(parent, screen.getKey(), screen.getXml());
    }

    public List<Integer> addSubScreen(final Screen parent, final String childKey, final int... screens) {
        Screen child = getChild(parent, childKey);

        for (final int screen : screens) {
            child.addSetting(screen);
        }

        return child.getSettings();
    }

    public List<Integer> addSubScreen(final String parentKey, final DeviceSpecificSettingsScreen screen, final int... screens) {
        Screen parent = getParent(parentKey, true);
        addSubRootScreen(parent, screen);
        return addSubScreen(parent, screen.getKey(), screens);
    }

    public void mergeFrom(final DeviceSpecificSettings other) {
        root.merge(other.root);
    }

    public List<Integer> getRootScreens() {
        return root.getSettings();
    }

    @Nullable
    public List<Integer> getScreen(@NonNull final String key) {
        Screen screen = root.getScreen(key);
        return screen != null ? screen.getSettings() : null;
    }

    public List<Integer> getAllScreens() {
        List<Integer> allScreens = new ArrayList<>(root.getSettings());
        for (Screen screen : root.getScreens().values()) {
            allScreens.addAll(screen.getSettings());
        }
        return allScreens;
    }

    public Screen getRoot() {
        return root;
    }

    public static final Creator<DeviceSpecificSettings> CREATOR = new Creator<DeviceSpecificSettings>() {
        @Override
        public DeviceSpecificSettings createFromParcel(Parcel in) {
            return new DeviceSpecificSettings(Objects.requireNonNull(Screen.createFromParcel(in)));
        }

        @Override
        public DeviceSpecificSettings[] newArray(int size) {
            return new DeviceSpecificSettings[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        root.writeToParcel(dest, flags);
    }
}
