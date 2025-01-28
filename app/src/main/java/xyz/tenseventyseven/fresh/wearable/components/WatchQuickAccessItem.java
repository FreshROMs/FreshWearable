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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import xyz.tenseventyseven.fresh.R;

public class WatchQuickAccessItem extends LinearLayout {
    public WatchQuickAccessItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs, defStyle, 0);
    }

    public WatchQuickAccessItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs, 0, 0);

    }

    public WatchQuickAccessItem(Context context) {
        super(context);
        initView(null, 0, 0);
    }

    private void initView(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        inflate(getContext(), R.layout.component_watch_quick_access_item, this);
        ImageView icon = findViewById(R.id.customization_item_icon);
        TextView title = findViewById(R.id.customization_item_title);

        // Get the attributes from the XML
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.WatchQuickAccessItem, defStyleAttr, defStyleRes);
            try {
                String titleText = a.getString(R.styleable.WatchQuickAccessItem_qpText);
                int iconResource = a.getResourceId(R.styleable.WatchQuickAccessItem_qpImage, 0);

                if (titleText != null) {
                    title.setText(titleText);
                }

                if (iconResource != 0) {
                    icon.setImageResource(iconResource);
                }
            } finally {
                a.recycle();
            }
        }
    }
}
