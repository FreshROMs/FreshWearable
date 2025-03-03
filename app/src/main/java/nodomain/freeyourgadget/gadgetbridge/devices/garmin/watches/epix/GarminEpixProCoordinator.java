package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.epix;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminEpixProCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^EPIX PRO - \\d+mm$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_epix_pro;
    }
}
