package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DataSync;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync.HuaweiDataSyncCommon;

public class SendDataSyncEventCommand extends Request {

    private final String srcPackage;
    private final String dstPackage;
    private final HuaweiDataSyncCommon.EventCommandData data;

    public SendDataSyncEventCommand(HuaweiSupportProvider support,
                                     String srcPackage,
                                     String dstPackage,
                                     HuaweiDataSyncCommon.EventCommandData data) {
        super(support);
        this.serviceId = DataSync.id;
        this.commandId = DataSync.EventCommand.id;

        this.srcPackage = srcPackage;
        this.dstPackage = dstPackage;
        this.data = data;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DataSync.EventCommand.Request(paramsProvider, this.srcPackage, this.dstPackage, this.data).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
