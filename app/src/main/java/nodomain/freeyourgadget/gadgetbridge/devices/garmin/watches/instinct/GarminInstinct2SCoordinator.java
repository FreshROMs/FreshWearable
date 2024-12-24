package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.wearable.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminInstinct2SCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct 2S$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2s;
    }
}
