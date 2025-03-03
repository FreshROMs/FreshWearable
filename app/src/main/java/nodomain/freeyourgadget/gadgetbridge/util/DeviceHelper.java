/*  Copyright (C) 2015-2024 115ek, akasaka / Genjitsu Labs, Andreas Böhler,
    Andreas Shimokawa, Andrew Watkins, angelpup, Arjan Schrijver, Carsten Pfeiffer,
    Cre3per, DanialHanif, Daniel Dakhno, Daniele Gobbetti, Daniel Thompson, Da Pa,
    Dmytro Bielik, Frank Ertl, GeekosaurusR3x, Gordon Williams, Jean-François
    Greffier, jfgreffier, jhey, João Paulo Barraca, Jochen S, Johannes Krude,
    José Rebelo, ksiwczynski, ladbsoft, Lesur Frederic, Maciej Kuśnierz, mamucho,
    Manuel Ruß, maxirnilian, mkusnierz, narektor, Noodlez, odavo32nof, opavlov,
    pangwalla, Pavel Elagin, Petr Kadlec, Petr Vaněk, protomors, Quallenauge,
    Quang Ngô, Raghd Hamzeh, Sami Alaoui, Sebastian Kranz, sedy89, Sophanimus,
    Stefan Bora, Taavi Eomäe, thermatk, tiparega, Vadim Kaushan, x29a, xaos,
    Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.AppException;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class DeviceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceHelper.class);

    private static final DeviceHelper instance = new DeviceHelper();

    private DeviceType[] orderedDeviceTypes = null;

    public static DeviceHelper getInstance() {
        return instance;
    }

    private final HashMap<String, DeviceType> deviceTypeCache = new HashMap<>();

    public GBDevice findAvailableDevice(String deviceAddress, Context context) {
        Set<GBDevice> availableDevices = getAvailableDevices(context);
        for (GBDevice availableDevice : availableDevices) {
            if (deviceAddress.equals(availableDevice.getAddress())) {
                return availableDevice;
            }
        }
        return null;
    }

    /**
     * Returns the list of all available devices that are supported by Gadgetbridge.
     * Note that no state is known about the returned devices. Even if one of those
     * devices is connected, it will report the default not-connected state.
     *
     * Clients interested in the "live" devices being managed should use the class
     * DeviceManager.
     * @param context
     * @return
     */
    public Set<GBDevice> getAvailableDevices(Context context) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            GB.toast(context, context.getString(R.string.bluetooth_is_not_supported_), Toast.LENGTH_SHORT, GB.WARN);
        } else if (!btAdapter.isEnabled()) {
            GB.toast(context, context.getString(R.string.bluetooth_is_disabled_), Toast.LENGTH_SHORT, GB.WARN);
        }

        Set<GBDevice> availableDevices = new LinkedHashSet<>(getDatabaseDevices());
        Prefs prefs = Application.getPrefs();
        return availableDevices;
    }

    public GBDevice toSupportedDevice(BluetoothDevice device) {
        GBDeviceCandidate candidate = new GBDeviceCandidate(device, GBDevice.RSSI_UNKNOWN, device.getUuids());
        candidate.refreshNameIfUnknown();
        return toSupportedDevice(candidate);
    }

    public GBDevice toSupportedDevice(GBDeviceCandidate candidate) {
        DeviceType resolvedType = resolveDeviceType(candidate);
        return resolvedType.getDeviceCoordinator().createDevice(candidate, resolvedType);
    }

    private DeviceType[] getOrderedDeviceTypes(){
        if(orderedDeviceTypes == null){
            ArrayList<DeviceType> orderedDevices = new ArrayList<>(Arrays.asList(DeviceType.values()));
            Collections.sort(orderedDevices, Comparator.comparingInt(dc -> dc.getDeviceCoordinator().getOrderPriority()));
            orderedDeviceTypes = orderedDevices.toArray(new DeviceType[0]);
        }

        return orderedDeviceTypes;
    }
    public DeviceType resolveDeviceType(GBDeviceCandidate deviceCandidate) {
        return resolveDeviceType(deviceCandidate, true);
    }

    public DeviceType resolveDeviceType(GBDeviceCandidate deviceCandidate, boolean useCache){
        synchronized (this) {
            if(useCache) {
                DeviceType cachedType =
                        deviceTypeCache.get(deviceCandidate.getMacAddress().toLowerCase());
                if (cachedType != null) {
                    return cachedType;
                }
            }

            for (DeviceType type : getOrderedDeviceTypes()) {
                if (type.getDeviceCoordinator().supports(deviceCandidate)) {
                    deviceTypeCache.put(deviceCandidate.getMacAddress().toLowerCase(), type);
                    return type;
                }
            }
            deviceTypeCache.put(deviceCandidate.getMacAddress().toLowerCase(), DeviceType.UNKNOWN);
        }
        return DeviceType.UNKNOWN;
    }

    public DeviceCoordinator resolveCoordinator(GBDeviceCandidate device) {
        return resolveDeviceType(device).getDeviceCoordinator();
    }

    private List<GBDevice> getDatabaseDevices() {
        List<GBDevice> result = new ArrayList<>();
        try (DBHandler lockHandler = Application.acquireDB()) {
            List<Device> activeDevices = DBHelper.getActiveDevices(lockHandler.getDaoSession());
            for (Device dbDevice : activeDevices) {
                GBDevice gbDevice = toGBDevice(dbDevice);
                if (gbDevice != null && gbDevice.getType().isSupported()) {
                    result.add(gbDevice);
                }
            }
            return result;

        } catch (Exception e) {
            GB.toast(Application.getContext().getString(R.string.error_retrieving_devices_database), Toast.LENGTH_SHORT, GB.ERROR, e);
            return Collections.emptyList();
        }
    }

    /**
     * Converts a known device from the database to a GBDevice.
     * Note: The device might not be supported anymore, so callers should verify that.
     * @param dbDevice
     * @return
     */
    public GBDevice toGBDevice(Device dbDevice) {
        DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
        GBDevice gbDevice = new GBDevice(dbDevice.getIdentifier(), dbDevice.getName(), dbDevice.getAlias(), dbDevice.getParentFolder(), deviceType);
        DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
        for (BatteryConfig batteryConfig : coordinator.getBatteryConfig(gbDevice)) {
            gbDevice.setBatteryIcon(batteryConfig.getBatteryIcon(), batteryConfig.getBatteryIndex());
            gbDevice.setBatteryLabel(batteryConfig.getBatteryLabel(), batteryConfig.getBatteryIndex());
        }

        List<DeviceAttributes> deviceAttributesList = dbDevice.getDeviceAttributesList();
        if (deviceAttributesList.size() > 0) {
            gbDevice.setModel(dbDevice.getModel());
            DeviceAttributes attrs = deviceAttributesList.get(0);
            gbDevice.setFirmwareVersion(attrs.getFirmwareVersion1());
            gbDevice.setFirmwareVersion2(attrs.getFirmwareVersion2());
            gbDevice.setVolatileAddress(attrs.getVolatileIdentifier());
        }

        return gbDevice;
    }

    /**
     * Attempts to removing the bonding with the given device. Returns true
     * if bonding was supposedly successful and false if anything went wrong
     * @param device
     * @return
     */
    public boolean removeBond(GBDevice device) throws AppException {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            BluetoothDevice remoteDevice = defaultAdapter.getRemoteDevice(device.getAddress());
            if (remoteDevice != null) {
                try {
                    Method method = BluetoothDevice.class.getMethod("removeBond", (Class[]) null);
                    Object result = method.invoke(remoteDevice, (Object[]) null);
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    throw new AppException("Error removing bond to device: " + device, e);
                }
            }
        }
        return false;
    }

}
