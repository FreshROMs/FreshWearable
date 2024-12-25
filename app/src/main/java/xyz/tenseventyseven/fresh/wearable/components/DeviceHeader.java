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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SeslProgressBar;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.wearable.R;
import xyz.tenseventyseven.fresh.wearable.WearableApplication;

public class DeviceHeader extends LinearLayout {
    private GBDevice mDevice;
    private final Context mContext;
    private DeviceHeaderBatteryCommon mDeviceHeaderBattery;

    public DeviceHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        inflate(getContext(), R.layout.component_device_header, this);
    }

    public DeviceHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        inflate(getContext(), R.layout.component_device_header, this);
    }

    public DeviceHeader(Context context) {
        super(context);
        mContext = context;
        inflate(getContext(), R.layout.component_device_header, this);
    }

    public void setDevice(GBDevice device) {
        mDevice = device;
        initView();
        refresh();
    }

    private void initView() {
        findViewById(R.id.header_device_battery_buds).setVisibility(View.GONE);
        findViewById(R.id.header_device_battery_watch).setVisibility(View.GONE);

        if (mDevice != null) {
            switch (mDevice.getDeviceCoordinator().getGeneralDeviceType()) {
                case EARBUDS:
                case HEADPHONES:
                    mDeviceHeaderBattery = findViewById(R.id.header_device_battery_buds);
                    break;
                case WATCH:
                case FITNESS_TRACKER:
                default:
                    mDeviceHeaderBattery = findViewById(R.id.header_device_battery_watch);
                    break;
            }

            mDeviceHeaderBattery.setDevice(mDevice);
        }
    }

    public void refresh() {
        Button btnConnect = findViewById(R.id.header_device_connect);
        SeslProgressBar progressBar = findViewById(R.id.header_device_connecting);
        btnConnect.setOnClickListener(v -> {
            btnConnect.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            WearableApplication.deviceService(mDevice).connect();
        });

        TextView deviceName = findViewById(R.id.header_device_name);
        deviceName.setText(mDevice.getAliasOrName());

        ImageView deviceImage = findViewById(R.id.header_device_image);
        deviceImage.setImageDrawable(
                mContext.getDrawable(mDevice.getDeviceCoordinator().getDeviceImageResource(mDevice))
        );

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
