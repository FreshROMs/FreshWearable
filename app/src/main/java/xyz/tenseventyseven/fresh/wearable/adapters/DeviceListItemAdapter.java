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
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.utils.BatteryUtils;

/**
 * Adapter for displaying GBDeviceCandate instances.
 */
public class DeviceListItemAdapter extends ArrayAdapter<GBDevice> {

    private final Context context;
    private boolean selectMode = false;
    private final List<GBDevice> selectedDevices = new ArrayList<>();


    public DeviceListItemAdapter(Context context, List<GBDevice> devices) {
        super(context, 0, devices);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        GBDevice device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.device_list_item, parent, false);
        }

        ImageView deviceImageView = view.findViewById(R.id.item_image);
        TextView deviceNameLabel = view.findViewById(R.id.item_name);
        TextView deviceStatus = view.findViewById(R.id.item_status);
        CheckBox checkBox = view.findViewById(R.id.item_checkbox);

        if (device == null) {
            return view;
        }

        DeviceCoordinator coordinator = device.getDeviceCoordinator();

        // Set device name and image
        String name = device.getAliasOrName();
        deviceNameLabel.setText(name);
        deviceImageView.setImageResource(coordinator.getDefaultIconResource());

        if (selectMode) {
            checkBox.setVisibility(View.VISIBLE);
        } else {
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
        }

        // Set device status
        final List<String> statusLines = new ArrayList<>();
        if (device.isConnected()) {
            statusLines.addAll(BatteryUtils.formatDeviceBattery(context, device));
        }

        if (!statusLines.isEmpty()) {
            deviceStatus.setVisibility(View.VISIBLE);
            deviceStatus.setText(TextUtils.join("|", statusLines));
        } else {
            // Explicitly reset deviceStatus
            deviceStatus.setVisibility(View.GONE);
            deviceStatus.setText(null);
        }

        // Set device image tint and alpha based on connection status
        Drawable drawable = DrawableCompat.wrap(deviceImageView.getDrawable());
        if (device.isConnected()) {
            DrawableCompat.setTint(
                    drawable,
                    ContextCompat.getColor(context, dev.oneuiproject.oneui.design.R.color.oui_btn_colored_background)
            );
            view.setAlpha(1.0f);
        } else {
            DrawableCompat.setTint(
                    drawable,
                    ContextCompat.getColor(context, R.color.secondarytext)
            );
            view.setAlpha(0.5f);
        }

        // Handle checkbox visibility and state
        checkBox.setChecked(selectedDevices.contains(device));
        View finalView = view;
        checkBox.setOnClickListener(v -> {
            toggleSelectedDevice(device, finalView);
        });

        return view;
    }

    public void addSelectedDevice(GBDevice device) {
        selectedDevices.add(device);
    }

    public void removeSelectedDevice(GBDevice device) {
        selectedDevices.remove(device);
    }

    public void toggleSelectedDevice(GBDevice device, View view) {
        if (selectedDevices.contains(device)) {
            removeSelectedDevice(device);
            ((CheckBox) view.findViewById(R.id.item_checkbox)).setChecked(false);
        } else {
            addSelectedDevice(device);
            ((CheckBox) view.findViewById(R.id.item_checkbox)).setChecked(true);
        }
    }

    public List<GBDevice> getSelectedDevices() {
        return selectedDevices;
    }

    public void setSelectMode(boolean selectMode) {
        selectedDevices.clear();
        this.selectMode = selectMode;
    }

}
