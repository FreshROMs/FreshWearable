package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds;

import java.util.regex.Pattern;

import xyz.tenseventyseven.fresh.R;

public class RedmiBuds6ActiveCoordinator extends AbstractRedmiBudsCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_6_active;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Redmi Buds 6 Active");
    }

}
