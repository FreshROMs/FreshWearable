/*  Copyright (C) 2018-2024 Andreas Shimokawa, DerFetzer, José Rebelo,
    Matthieu Baerts, NekoBox, Oleg Vasilev

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;

public class AmazfitBipSupport extends HuamiSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipSupport.class);

    public AmazfitBipSupport() {
        super(LOG);
    }

    @Override
    public NotificationStrategy getNotificationStrategy() {
        return new AmazfitBipTextNotificationStrategy(this);
    }

    @Override
    protected AmazfitBipSupport setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyPosMap = new LinkedHashMap<>();
        keyPosMap.put("status", 1);
        keyPosMap.put("activity", 2);
        keyPosMap.put("weather", 3);
        keyPosMap.put("alarm", 4);
        keyPosMap.put("timer", 5);
        keyPosMap.put("compass", 6);
        keyPosMap.put("settings", 7);
        keyPosMap.put("alipay", 8);

        setDisplayItemsOld(builder, false, R.array.pref_bip_display_items_default, keyPosMap);
        return this;
    }

    @Override
    protected AmazfitBipSupport setShortcuts(TransactionBuilder builder) {
        Map<String, Integer> keyPosMap = new LinkedHashMap<>();
        keyPosMap.put("alipay", 1);
        keyPosMap.put("weather", 2);

        setDisplayItemsOld(builder, true, R.array.pref_bip_shortcuts_default, keyPosMap);
        return this;
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");

        if (HuamiCoordinator.getOverwriteSettingsOnConnection(getDevice().getAddress())) {
            setLanguage(builder);
        }

        requestGPSVersion(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitBipFWHelper(uri, context);
    }
}
