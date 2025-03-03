/*  Copyright (C) 2019-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Damien Gaignon, Daniel Dakhno, Hasan Ammar, José Rebelo, Morten
    Rieger Hannemose, Petr Vaněk

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

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.AppException;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class QHybridCoordinator extends AbstractBLEDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(QHybridCoordinator.class);

    @NonNull
    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        for(ParcelUuid uuid : candidate.getServiceUuids()){
            if(uuid.getUuid().toString().equals("3dda0001-957f-7d4a-34a6-74696673696d")){
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.singletonList(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("3dda0001-957f-7d4a-34a6-74696673696d")).build());
    }

    @Override
    public boolean supportsActivityDataFetching() {
        List<GBDevice> devices = Application.app().getDeviceManager().getSelectedDevices();
        for(GBDevice device : devices){
            if(isFossilHybrid(device) && device.getState() == GBDevice.State.INITIALIZED){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HybridHRActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new HybridHRSpo2SampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        if (isHybridHR()) {
            FossilHRInstallHandler installHandler = new FossilHRInstallHandler(uri, context);
            if (!installHandler.isValid()) {
                LOG.warn("Not a Fossil Hybrid firmware or app!");
                return null;
            } else {
                return installHandler;
            }
        }
        FossilInstallHandler installHandler = new FossilInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }

    private boolean supportsAlarmConfiguration() {
        List<GBDevice> devices = Application.app().getDeviceManager().getSelectedDevices();
        LOG.debug("devices count: " + devices.size());
        for(GBDevice device : devices){
            if(isFossilHybrid(device) && device.getState() == GBDevice.State.INITIALIZED){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return this.supportsAlarmConfiguration() ? 5 : 0;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        if (isHybridHR(device)) {
            return 16;
        }

        return 0;
    }

    @Override
    public boolean supportsAlarmTitle(GBDevice device) {
        return isHybridHR(device);
    }

    @Override
    public boolean supportsAlarmDescription(GBDevice device) {
        return isHybridHR(device);
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return isHybridHR(device);
    }

    @Override
    public String getManufacturer() {
        return "Fossil";
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAppListFetching() {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return isHybridHR() ? AppManagerActivity.class : QHybridConfigActivity.class;
    }

    @Override
    public Class<? extends Activity> getWatchfaceDesignerActivity() {
        return isHybridHR() ? HybridHRWatchfaceDesignerActivity.class : null;
    }

    /**
     * Returns the directory containing the watch app cache.
     * @throws IOException when the external files directory cannot be accessed
     */
    public File getAppCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "qhybrid-app-cache");
    }

    /**
     * Returns a String containing the device app sort order filename.
     */
    @Override
    public String getAppCacheSortFilename() {
        return "wappcacheorder.txt";
    }

    /**
     * Returns a String containing the file extension for watch apps.
     */
    @Override
    public String getAppFileExtension() {
        return ".wapp";
    }

    @Override
    public boolean supportsWeather() {
        return isHybridHR();
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws AppException {

    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        return new BatteryConfig[]{
                new BatteryConfig(
                        0,
                        GBDevice.BATTERY_ICON_DEFAULT,
                        GBDevice.BATTERY_LABEL_DEFAULT,
                        isHybridHR(device) ? 10 : 2,
                        100
                )
        };
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        if (!isHybridHR(device)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_fossilqhybrid_legacy);
            return deviceSpecificSettings;
        }
        final List<Integer> generic = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.GENERIC);
        // Firmware version specific settings
        if (getFirmwareVersion() != null && getFirmwareVersion().smallerThan(new Version("3.0"))) {
            generic.add(R.xml.devicesettings_fossilhybridhr_pre_fw300);
        } else {
            generic.add(R.xml.devicesettings_fossilhybridhr_post_fw300);
        }
        if (getFirmwareVersion() != null && getFirmwareVersion().smallerThan(new Version("2.20"))) {
            generic.add(R.xml.devicesettings_fossilhybridhr_pre_fw220);
        }
        // Settings applicable to all firmware versions
        generic.add(R.xml.devicesettings_fossilhybridhr_calibration);
        generic.add(R.xml.devicesettings_fossilhybridhr_navigation);
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_fossilhybridhr_workout_detection);
        health.add(R.xml.devicesettings_inactivity);
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_fossilhybridhr_vibration);
        notifications.add(R.xml.devicesettings_autoremove_notifications);
        notifications.add(R.xml.devicesettings_canned_dismisscall_16);
        notifications.add(R.xml.devicesettings_reject_call_method);
        notifications.add(R.xml.devicesettings_transliteration);
        notifications.add(R.xml.devicesettings_custom_deviceicon);
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_fossilhybridhr_dev);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new QHybridSettingsCustomizer();
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return QHybridSupport.class;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey
        };
    }

    @Deprecated // we should use the isHybridHR(GBDevice) instead of iterating every single device
    private boolean isHybridHR() {
        List<GBDevice> devices = Application.app().getDeviceManager().getSelectedDevices();
        for(GBDevice device : devices){
            if(isHybridHR(device)){
                return true;
            }
        }
        return false;
    }

    private boolean isHybridHR(GBDevice device){
        if(!isFossilHybrid(device)) return false;
        return device.getName().startsWith("Hybrid HR") || device.getName().equals("Fossil Gen. 6 Hybrid");
    }

    private Version getFirmwareVersion() {
        List<GBDevice> devices = Application.app().getDeviceManager().getSelectedDevices();
        for (GBDevice device : devices) {
            if (isFossilHybrid(device)) {
                return new Version(device.getFirmwareVersion2());
            }
        }

        return null;
    }

    private boolean isFossilHybrid(GBDevice device){
        return device.getType() == DeviceType.FOSSILQHYBRID;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_qhybrid;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_zetime_disabled;
    }

    @Override
    public boolean supportsNavigation() {
        return isHybridHR();
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return device.getName().equals("Fossil Gen. 6 Hybrid");
    }
}
