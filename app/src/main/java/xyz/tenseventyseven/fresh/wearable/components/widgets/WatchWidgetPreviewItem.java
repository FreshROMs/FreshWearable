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
package xyz.tenseventyseven.fresh.wearable.components.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import xyz.tenseventyseven.fresh.R;

public class WatchWidgetPreviewItem extends LinearLayout {
    private boolean mIsSelected = false;
    private int mBackgroundColor = -1;

    public WatchWidgetPreviewItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs, defStyle, 0);
    }

    public WatchWidgetPreviewItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs, 0, 0);

    }

    public WatchWidgetPreviewItem(Context context) {
        super(context);
        initView(null, 0, 0);
    }

    private void initView(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        inflate(getContext(), R.layout.wear_watch_widget_preview_item, this);
        mBackgroundColor = getResources().getColor(dev.oneuiproject.oneui.design.R.color.oui_background_color);
    }

    public void setIcon(Drawable icon) {
        ImageView iconView = findViewById(R.id.widget_preview_item_icon);
        iconView.setImageDrawable(icon);
    }

    public void setColor(int color) {
        mBackgroundColor = color;

        // Make color darker if selected
        int newColor;
        if (mIsSelected) {
            newColor = Color.argb(255, Color.red(mBackgroundColor) / 2, Color.green(mBackgroundColor) / 2, Color.blue(mBackgroundColor) / 2);
        } else {
            newColor = mBackgroundColor;
        }

        findViewById(R.id.widget_preview_item).setBackgroundColor(newColor);

    }

    public void setIconColor(int color) {
        ImageView iconView = findViewById(R.id.widget_preview_item_icon);
        TextView textView = findViewById(R.id.widget_preview_item_title);
        iconView.setColorFilter(color);
        textView.setTextColor(color);
    }

    public void setTitle(String title) {
        TextView textView = findViewById(R.id.widget_preview_item_title);
        textView.setText(title);
    }

    @Override
    public void setSelected(boolean selected) {
        mIsSelected = selected;
        int newColor;

        // Make color darker if selected
        if (selected) {
            newColor = Color.argb(255, Color.red(mBackgroundColor) / 2, Color.green(mBackgroundColor) / 2, Color.blue(mBackgroundColor) / 2);
        } else {
            newColor = mBackgroundColor;
        }

        findViewById(R.id.widget_preview_item).setBackgroundColor(newColor);
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        findViewById(R.id.widget_preview_item_container).setOnClickListener(l);
    }
}
