package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.coordinators;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.RedmiBudsCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds.RedmiBudsCoordinator;
import xyz.tenseventyseven.fresh.R;

public class RedmiBuds6ActiveCoordinator extends RedmiBudsCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_6_active;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Redmi Buds 6 Active");
    }

    @Override
    public List<RedmiBudsCapabilities> getCapabilities() {
        return Arrays.asList(
                RedmiBudsCapabilities.LowLatencyMode,
                RedmiBudsCapabilities.ReportsBattery,
                RedmiBudsCapabilities.ReportsCaseBattery,
                RedmiBudsCapabilities.ReportsLeftEarbudBattery,
                RedmiBudsCapabilities.ReportsRightEarbudBattery,
                RedmiBudsCapabilities.GestureControl,
                RedmiBudsCapabilities.EqualizerV1
        );
    }

}
