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
package xyz.tenseventyseven.fresh.wearable.components.header;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class DeviceHeaderBatteryCommon extends LinearLayout {
    protected GBDevice mDevice;
    protected Context mContext;

    public DeviceHeaderBatteryCommon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public DeviceHeaderBatteryCommon(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public DeviceHeaderBatteryCommon(Context context) {
        super(context);
        mContext = context;
    }

    public void setDevice(GBDevice device) {
        mDevice = device;
    }

    public void refresh() {
    }
}
