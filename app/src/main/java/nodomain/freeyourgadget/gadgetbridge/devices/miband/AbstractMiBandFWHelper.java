/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

/**
 * Also see Mi1SFirmwareInfo.
 */
public abstract class AbstractMiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMiBandFWHelper.class);

    private byte[] fw;

    public AbstractMiBandFWHelper(Uri uri, Context context) throws IOException {
        UriHelper uriHelper = UriHelper.get(uri, context);
        String pebblePattern = ".*\\.(pbw|pbz|pbl)";
        if (uriHelper.getFileName().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        if (uriHelper.getFileSize() > getMaxExpectedFileSize()) {
            throw new IOException("Firmware size is larger than the maximum expected file size of " + getMaxExpectedFileSize());
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            this.fw = FileUtils.readAll(in, getMaxExpectedFileSize());
            determineFirmwareInfo(fw);
        } catch (IOException ex) {
            throw ex; // pass through
        } catch (IllegalArgumentException ex) {
            throw new IOException("This doesn't seem to be a Mi Band firmware: " + ex.getLocalizedMessage(), ex);
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    /**
     * Returns a localized, user-visible representation of the kind of firmware to be installed.
     */
    @NonNull
    public abstract String getFirmwareKind();

    public abstract int getFirmwareVersion();

    public abstract int getFirmware2Version();

    public static String formatFirmwareVersion(int version) {
        if (version == -1) {
            return Application.getContext().getString(R.string._unknown_);
        }

        return String.format(Locale.UK,
                "%d.%d.%d.%d",
                version >> 24 & 255,
                version >> 16 & 255,
                version >> 8 & 255,
                version & 255);
    }

    public String getHumanFirmwareVersion() {
        return format(getFirmwareVersion());
    }

    public abstract String getHumanFirmwareVersion2();

    public String format(int version) {
        return formatFirmwareVersion(version);
    }

    @NonNull
    public byte[] getFw() {
        if (fw == null) {
            throw new IllegalStateException("fw is null");
        }

        return fw;
    }

    public void unsetFwBytes() {
        this.fw = null;
    }

    public boolean isFirmwareWhitelisted() {
        for (int wlf : getWhitelistedFirmwareVersions()) {
            if (wlf == getFirmwareVersion()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The maximum expected file size, in bytes. Files larger than this are assumed to be invalid.
     */
    public long getMaxExpectedFileSize() {
        return 1024 * 1024 * 16; // 16.0MB
    }

    protected abstract int[] getWhitelistedFirmwareVersions();

    public abstract boolean isFirmwareGenerallyCompatibleWith(GBDevice device);

    public abstract boolean isSingleFirmware();

    /**
     * @param wholeFirmwareBytes
     * @return
     * @throws IllegalArgumentException when the data is not recognized as firmware data
     */
    @NonNull
    protected abstract void determineFirmwareInfo(byte[] wholeFirmwareBytes);

    public abstract void checkValid() throws IllegalArgumentException;

    public abstract HuamiFirmwareType getFirmwareType();

    public Bitmap getPreview() {
        return null;
    }
}
