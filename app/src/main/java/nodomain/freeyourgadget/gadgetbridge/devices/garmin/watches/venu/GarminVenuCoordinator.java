package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVenuCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Venu$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_venu;
    }
}
