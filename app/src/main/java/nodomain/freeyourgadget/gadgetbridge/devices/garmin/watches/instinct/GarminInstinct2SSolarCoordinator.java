package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminInstinct2SSolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct 2S Solar$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2s_solar;
    }
}
