/*  Copyright (C) 2024 John Vincent Corcega (TenSeventy7)

    This file is part of Fresh Wearable.

    Fresh Wearable is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SeslProgressBar;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.databinding.WearDeviceHeaderBinding;
import xyz.tenseventyseven.fresh.wearable.components.header.DeviceHeaderBatteryCommon;
import xyz.tenseventyseven.fresh.wearable.interfaces.WearableDeviceCoordinator;

public class DeviceHeader extends LinearLayout {
    private WearDeviceHeaderBinding binding;
    private GBDevice mDevice;
    private final Context mContext;
    private DeviceHeaderBatteryCommon mDeviceHeaderBattery;

    public DeviceHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context);
    }

    public DeviceHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    public DeviceHeader(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        binding = WearDeviceHeaderBinding.inflate(LayoutInflater.from(context), this, true);
        binding.headerDeviceConnecting.setVisibility(View.GONE);
    }

    public void setDevice(GBDevice device) {
        mDevice = device;
        initView();
        refresh();
    }

    private void initView() {
        binding.headerDeviceBatteryBuds.setVisibility(View.GONE);
        binding.headerDeviceBatteryWatch.setVisibility(View.GONE);

        if (mDevice != null) {
            switch (mDevice.getDeviceCoordinator().getDeviceKind()) {
                case EARBUDS:
                case HEADPHONES:
                    mDeviceHeaderBattery = binding.headerDeviceBatteryBuds;
                    break;
                case WATCH:
                case FITNESS_TRACKER:
                default:
                    mDeviceHeaderBattery = binding.headerDeviceBatteryWatch;
                    break;
            }

            mDeviceHeaderBattery.setDevice(mDevice);
        }
    }

    public void refresh() {
        if (mDevice == null || binding == null) {
            Log.d("DeviceHeader", "Device or binding is null");
            return;
        }

        Button btnConnect = binding.headerDeviceConnect;
        SeslProgressBar progressBar = binding.headerDeviceConnecting;
        btnConnect.setOnClickListener(v -> {
            btnConnect.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            Application.deviceService(mDevice).connect();
        });

        TextView deviceName = findViewById(R.id.header_device_name);
        deviceName.setText(mDevice.getAliasOrName());

        ImageView deviceImage = findViewById(R.id.header_device_image);
        deviceImage.setImageDrawable(
                mContext.getDrawable(mDevice.getDeviceCoordinator().getDeviceImageResource(mDevice))
        );

        // If watch set maxheight to 260dip, else 130dip
        if (mDevice.getDeviceCoordinator().getDeviceKind() == WearableDeviceCoordinator.DeviceKind.WATCH) {
            deviceImage.setMaxHeight((int) (260 * mContext.getResources().getDisplayMetrics().density));
        } else {
            deviceImage.setMaxHeight((int) (140 * mContext.getResources().getDisplayMetrics().density));
        }

        if (!mDevice.isConnected()) {
            mDeviceHeaderBattery.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            btnConnect.setVisibility(View.VISIBLE);
            return;
        } else if (mDevice.isConnecting() || mDevice.isInitializing()) {
            mDeviceHeaderBattery.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            btnConnect.setVisibility(View.GONE);
            return;
        }

        btnConnect.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        mDeviceHeaderBattery.setVisibility(View.VISIBLE);
        mDeviceHeaderBattery.refresh();
    }

    public boolean isInitialized() {
        return mDevice != null;
    }
}
