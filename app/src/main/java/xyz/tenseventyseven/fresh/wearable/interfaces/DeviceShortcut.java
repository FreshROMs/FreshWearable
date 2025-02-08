package xyz.tenseventyseven.fresh.wearable.interfaces;

public class DeviceShortcut {
    public String key;
    public int title;
    public int icon;

    public DeviceShortcut() {
    }

    public DeviceShortcut(String key, int title, int icon) {
        this.key = key;
        this.title = title;
        this.icon = icon;
    }
}
