/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, José Rebelo, Petr Vaněk, Taavi Eomäe

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
package xyz.tenseventyseven.fresh.wearable.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Adapter for displaying GBDeviceCandate instances.
 */
public class DeviceCandidateAdapter extends ArrayAdapter<GBDeviceCandidate> {

    private final Context context;

    public DeviceCandidateAdapter(Context context, List<GBDeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDeviceCandidate device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_with_header, parent, false);
        }

        LinearLayout headerContainer = view.findViewById(R.id.header_container);
        TextView headerText = view.findViewById(R.id.header_text);
        ImageView deviceImageView = view.findViewById(R.id.item_image);
        TextView deviceNameLabel = view.findViewById(R.id.item_name);
        TextView deviceStatus = view.findViewById(R.id.item_status);

        DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(device);
        DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();

        String name = formatDeviceCandidate(device);
        deviceNameLabel.setText(name);
        deviceImageView.setImageResource(coordinator.getDefaultIconResource());

        final List<String> statusLines = new ArrayList<>();
        if (!deviceType.isSupported()) {
            statusLines.add(getContext().getString(R.string.device_unsupported));
        }

        if (coordinator.isExperimental()) {
            statusLines.add(getContext().getString(R.string.device_experimental));
        }
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            statusLines.add(getContext().getString(R.string.device_requires_key));
        }

        if (!statusLines.isEmpty()) {
            deviceStatus.setVisibility(View.VISIBLE);
            deviceStatus.setText(TextUtils.join("\n", statusLines));
        }

        // Add header based on device status
        if (position == 0 || !getItem(position - 1).isBonded() && device.isBonded()) {
            headerText.setText("Paired devices");
            headerContainer.setVisibility(View.VISIBLE);
        } else if (position == 0 || getItem(position - 1).isBonded() && !device.isBonded()) {
            headerText.setText("Available devices");
            headerContainer.setVisibility(View.VISIBLE);
        } else {
            headerContainer.setVisibility(View.GONE);
        }

        return view;
    }
    private String formatDeviceCandidate(GBDeviceCandidate device) {
        if (device.getRssi() > GBDevice.RSSI_UNKNOWN) {
            return context.getString(R.string.device_with_rssi, device.getName(), GB.formatRssi(device.getRssi()));
        }
        return device.getName();
    }
}
