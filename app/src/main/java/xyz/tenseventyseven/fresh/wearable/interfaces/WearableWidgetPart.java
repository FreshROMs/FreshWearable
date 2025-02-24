package xyz.tenseventyseven.fresh.wearable.interfaces;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;

public class WearableWidgetPart implements Serializable {

    // The human-readable part name
    private String name;
    private String fullName;
    private long category = 0;
    private int icon = -1;
    private int color = -1;
    private boolean isAlternate = false;

    // Null if it has no specific subtype
    @Nullable
    private WidgetPartSubtype subtype;

    public String getFullName() {
        if (subtype != null) {
            return String.format(Locale.ROOT, "%s (%s)", name, subtype.getName());
        }

        return getName();
    }

    public String getName() {
        return name;
    }

    public void setAlternateName(final String fullName) {
        this.fullName = fullName;
    }

    public String getAlternateName() {
        if (fullName == null || fullName.isEmpty()) {
            return getName();
        }

        return fullName;
    }

    public long getCategory() {
        return category;
    }

    public int getIcon() {
        return icon;
    }

    public void setCategory(long category) {
        this.category = category;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int colorId) {
        this.color = colorId;
    }

    public boolean isAlternate() {
        return isAlternate;
    }

    public void setAlternate(boolean isAlternate) {
        this.isAlternate = isAlternate;
    }
}
