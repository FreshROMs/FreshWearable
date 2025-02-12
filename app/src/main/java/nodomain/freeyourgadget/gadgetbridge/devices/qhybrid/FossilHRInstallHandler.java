/*  Copyright (C) 2020-2024 Andreas Shimokawa, Arjan Schrijver, Daniel Dakhno,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FossilHRInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FossilHRInstallHandler.class);

    private final Uri mUri;
    private final Context mContext;
    private FossilFileReader fossilFile;

    FossilHRInstallHandler(Uri uri, Context context) {
        mUri = uri;
        mContext = context;
        try {
            fossilFile = new FossilFileReader(uri, mContext);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }
        if (device.getType() != DeviceType.FOSSILQHYBRID || !device.isConnected() || !fossilFile.isValid()) {
            installActivity.setInfoText("Element cannot be installed 1, type: " + device.getType() + " device: "+ device  + " is connected" + device.isConnected() + " is valid file:" + fossilFile.isValid());
            installActivity.setInstallEnabled(false);
            return;
        }
        GenericItem installItem = new GenericItem();
        installItem.setName(fossilFile.getName());
        installItem.setDetails(fossilFile.getVersion());
        Bitmap preview = fossilFile.getPreview();
        if (preview != null) {
            preview = Bitmap.createScaledBitmap(preview, preview.getWidth() * 3, preview.getHeight() * 3, false);
        }
        installItem.setPreview(preview);
        if (fossilFile.isFirmware()) {
            installItem.setIcon(R.drawable.ic_firmware);
            installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, "(unknown)"));
        } else if (fossilFile.isApp()) {
            installItem.setIcon(R.drawable.ic_watchapp);
            installActivity.setInfoText(mContext.getString(R.string.app_install_info, installItem.getName(), fossilFile.getVersion(), "(unknown)"));
        } else if (fossilFile.isWatchface()) {
            installItem.setIcon(R.drawable.ic_watchface);
            installActivity.setInfoText(mContext.getString(R.string.watchface_install_info, installItem.getName(), fossilFile.getVersion(), "(unknown)"));
        } else {
            installActivity.setInfoText("Element cannot be installed 2");
            installActivity.setInstallEnabled(false);
            return;
        }
        installActivity.setInstallEnabled(true);
        installActivity.setInstallItem(installItem);
    }

    @Override
    public void onStartInstall(GBDevice device) {
        DeviceCoordinator mCoordinator = device.getDeviceCoordinator();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(mContext);
        manager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_INDETERMINATE, true));
        if (fossilFile.isFirmware()) {
            return;
        }
        saveAppInCache(fossilFile, fossilFile.getBackground(), fossilFile.getPreview(), mCoordinator, mContext);
        // refresh list
        manager.sendBroadcast(new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST));
    }

    public static boolean saveAppInCache(FossilFileReader fossilFile, Bitmap backgroundImg, Bitmap previewImg, DeviceCoordinator mCoordinator, Context mContext) {
        GBDeviceApp app;
        File destDir;
        // write app file
        try {
            app = fossilFile.getGBDeviceApp();
            destDir = mCoordinator.getAppCacheDir();
            destDir.mkdirs();
            FileUtils.copyURItoFile(mContext, fossilFile.getUri(), new File(destDir, app.getUUID().toString() + mCoordinator.getAppFileExtension()));
        } catch (IOException e) {
            LOG.error("Saving app in cache failed: " + e.getMessage(), e);
            return false;
        }
        // write app metadata
        File outputFile = new File(destDir, app.getUUID().toString() + ".json");
        Writer writer;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            LOG.error("Failed to open output file: " + e.getMessage(), e);
            return false;
        }
        try {
            LOG.info(app.getJSON().toString());
            JSONObject appJSON = app.getJSON();
            JSONObject appKeysJSON = fossilFile.getAppKeysJSON();
            if (appKeysJSON != null) {
                appJSON.put("appKeys", appKeysJSON);
            }
            writer.write(appJSON.toString());

            writer.close();
        } catch (IOException e) {
            LOG.error("Failed to write to output file: " + e.getMessage(), e);
            return false;
        } catch (JSONException e) {
            LOG.error("Failed to load or write appKeys JSON: " + e.getMessage(), e);
            return false;
        }
        // write watchface background image
        if (backgroundImg != null) {
            outputFile = new File(destDir, app.getUUID().toString() + "_bg.png");
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                backgroundImg.compress(Bitmap.CompressFormat.PNG, 9, fos);
                fos.close();
            } catch (IOException e) {
                LOG.error("Failed to write to output file: " + e.getMessage(), e);
                return false;
            }
        }
        // write watchface preview image
        if (previewImg != null) {
            outputFile = new File(destDir, app.getUUID().toString() + "_preview.png");
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                previewImg.compress(Bitmap.CompressFormat.PNG, 9, fos);
                fos.close();
            } catch (IOException e) {
                LOG.error("Failed to write to output file: " + e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValid() {
        return fossilFile.isValid();
    }
}
