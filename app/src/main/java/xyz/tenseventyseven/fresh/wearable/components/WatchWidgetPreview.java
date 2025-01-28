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
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import xyz.tenseventyseven.fresh.R;

public class WatchWidgetPreview extends ConstraintLayout {

    public enum WidgetType {
        TYPE_2X2_LEFT_TOP,
        TYPE_2X2_RIGHT_TOP,
        TYPE_2X2_LEFT_BOTTOM,
        TYPE_2X2_RIGHT_BOTTOM,
        TYPE_2X4_TOP,
        TYPE_2X4_BOTTOM,
    }

    public enum PreviewMode {
        TOP_2_BOTTOM_2,
        TOP_2_BOTTOM_1,
        TOP_1_BOTTOM_2,
        SINGLE,
        TWO,
        CENTER_TOP_BOTTOM,
    }

    private PreviewMode mCurrentMode = PreviewMode.TOP_2_BOTTOM_2;

    public interface OnWidgetItemClickListener {
        void onChangeState(WidgetScreen screen, WidgetPart part, int index, boolean selected);
    }
    private OnWidgetItemClickListener mOnWidgetItemClickListener;

    private WidgetScreen mWidgetScreen;
    private Map<WidgetType, WidgetPart> mWidgetParts = new HashMap<>();
    private Map<WidgetType, Integer> mWidgetIndex = new HashMap<>();


    public WatchWidgetPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs, defStyle, 0);
    }

    public WatchWidgetPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs, 0, 0);

    }

    public WatchWidgetPreview(Context context) {
        super(context);
        initView(null, 0, 0);
    }

    private void initView(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        inflate(getContext(), R.layout.component_watch_widget_preview, this);
        updateView();
        addOnClickListeners();
    }

    public void setOnWidgetItemClickListener(OnWidgetItemClickListener listener) {
        mOnWidgetItemClickListener = listener;
    }

    public void removeOnWidgetItemClickListener() {
        mOnWidgetItemClickListener = null;
    }

    public void setWidgetScreen(WidgetScreen screen) {
        mWidgetScreen = screen;
    }

    public void setTypeMap(WidgetType type, WidgetPart part) {
        mWidgetParts.put(type, part);
    }

    public WidgetPart getTypeMap(WidgetType type) {
        if (mWidgetParts.containsKey(type)) {
            return mWidgetParts.get(type);
        }

        return null;
    }

    public void setIndexMap(WidgetType type, int index) {
        mWidgetIndex.put(type, index);
    }

    public int getIndexMap(WidgetType type) {
        if (mWidgetIndex.containsKey(type)) {
            Integer index = mWidgetIndex.get(type);
            return index != null ? index : -1;
        }

        return -1;
    }

    private void unselectAllWidgets() {
        WatchWidgetPreviewItem top_left = findViewById(R.id.widget_preview_2x2_t_left);
        WatchWidgetPreviewItem top_right = findViewById(R.id.widget_preview_2x2_t_right);
        WatchWidgetPreviewItem bottom_left = findViewById(R.id.widget_preview_2x2_b_left);
        WatchWidgetPreviewItem bottom_right = findViewById(R.id.widget_preview_2x2_b_right);
        WatchWidgetPreviewItem center_top = findViewById(R.id.widget_preview_2x4_center_top);
        WatchWidgetPreviewItem center_bottom = findViewById(R.id.widget_preview_2x4_center_bottom);

        top_left.setSelected(false);
        top_right.setSelected(false);
        bottom_left.setSelected(false);
        bottom_right.setSelected(false);
        center_top.setSelected(false);
        center_bottom.setSelected(false);
    }

    private void addOnClickListeners() {
        WatchWidgetPreviewItem top_left = findViewById(R.id.widget_preview_2x2_t_left);
        WatchWidgetPreviewItem top_right = findViewById(R.id.widget_preview_2x2_t_right);
        WatchWidgetPreviewItem bottom_left = findViewById(R.id.widget_preview_2x2_b_left);
        WatchWidgetPreviewItem bottom_right = findViewById(R.id.widget_preview_2x2_b_right);
        WatchWidgetPreviewItem center_top = findViewById(R.id.widget_preview_2x4_center_top);
        WatchWidgetPreviewItem center_bottom = findViewById(R.id.widget_preview_2x4_center_bottom);

        top_left.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X2_LEFT_TOP), getIndexMap(WidgetType.TYPE_2X2_LEFT_TOP), !top_left.isSelected());
            }
            top_left.setSelected(!top_left.isSelected());
        });

        top_right.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X2_RIGHT_TOP), getIndexMap(WidgetType.TYPE_2X2_RIGHT_TOP), !top_right.isSelected());
            }
            top_right.setSelected(!top_right.isSelected());
        });

        bottom_left.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X2_LEFT_BOTTOM), getIndexMap(WidgetType.TYPE_2X2_LEFT_BOTTOM), !bottom_left.isSelected());
            }
            bottom_left.setSelected(!bottom_left.isSelected());
        });

        bottom_right.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X2_RIGHT_BOTTOM), getIndexMap(WidgetType.TYPE_2X2_RIGHT_BOTTOM), !bottom_right.isSelected());
            }
            bottom_right.setSelected(!bottom_right.isSelected());
        });

        center_top.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X4_TOP), getIndexMap(WidgetType.TYPE_2X4_TOP), !center_top.isSelected());
            }
            center_top.setSelected(!center_top.isSelected());
        });

        center_bottom.setOnClickListener(view -> {
            unselectAllWidgets();
            if (mOnWidgetItemClickListener != null) {
                mOnWidgetItemClickListener.onChangeState(mWidgetScreen, getTypeMap(WidgetType.TYPE_2X4_BOTTOM), getIndexMap(WidgetType.TYPE_2X4_BOTTOM), !center_bottom.isSelected());
            }
            center_bottom.setSelected(!center_bottom.isSelected());
        });
    }

    private void updateView() {
        LinearLayout top = findViewById(R.id.widget_preview_2x2_top);
        LinearLayout center_top = findViewById(R.id.widget_preview_2x4_center_top);
        LinearLayout center_bottom = findViewById(R.id.widget_preview_2x4_center_bottom);
        LinearLayout bottom = findViewById(R.id.widget_preview_2x2_bottom);

        switch (mCurrentMode) {
            case TOP_2_BOTTOM_2:
                top.setVisibility(VISIBLE);
                center_top.setVisibility(GONE);
                center_bottom.setVisibility(GONE);
                bottom.setVisibility(VISIBLE);
                break;
            case TOP_2_BOTTOM_1:
                top.setVisibility(VISIBLE);
                center_top.setVisibility(VISIBLE);
                center_bottom.setVisibility(GONE);
                bottom.setVisibility(GONE);
                break;
            case TOP_1_BOTTOM_2:
                top.setVisibility(GONE);
                center_top.setVisibility(VISIBLE);
                center_bottom.setVisibility(GONE);
                bottom.setVisibility(VISIBLE);
                break;
            case SINGLE:
                top.setVisibility(GONE);
                center_top.setVisibility(VISIBLE);
                center_bottom.setVisibility(GONE);
                bottom.setVisibility(GONE);
                break;
            case TWO:
                top.setVisibility(GONE);
                center_top.setVisibility(GONE);
                center_bottom.setVisibility(GONE);
                bottom.setVisibility(GONE);
                break;
            case CENTER_TOP_BOTTOM:
                top.setVisibility(GONE);
                center_top.setVisibility(VISIBLE);
                center_bottom.setVisibility(VISIBLE);
                bottom.setVisibility(GONE);
                break;
        }
    }

    public void setMode(PreviewMode mode) {
        mCurrentMode = mode;
        updateView();
    }

    public PreviewMode getMode() {
        return mCurrentMode;
    }

    public void setWidget(WidgetType type, WidgetPart part) {
        WatchWidgetPreviewItem item = null;
        switch (type) {
            case TYPE_2X2_LEFT_TOP:
                item = findViewById(R.id.widget_preview_2x2_t_left);
                break;
            case TYPE_2X2_RIGHT_TOP:
                item = findViewById(R.id.widget_preview_2x2_t_right);
                break;
            case TYPE_2X2_LEFT_BOTTOM:
                item = findViewById(R.id.widget_preview_2x2_b_left);
                break;
            case TYPE_2X2_RIGHT_BOTTOM:
                item = findViewById(R.id.widget_preview_2x2_b_right);
                break;
            case TYPE_2X4_TOP:
                item = findViewById(R.id.widget_preview_2x4_center_top);
                break;
            case TYPE_2X4_BOTTOM:
                item = findViewById(R.id.widget_preview_2x4_center_bottom);
                break;
        }

        if (item != null) {
            int icon = part.getIcon();
            if (icon != -1) {
                item.setIcon(getResources().getDrawable(icon));
            }

            int color = part.getColor();
            if (color != -1) {
                if (part.isAlternate()) {
                    item.setColor(color);
                    item.setIconColor(Color.parseColor("#FFFFFF"));
                } else {
                    item.setColor(Color.parseColor("#17171A"));
                    item.setIconColor(color);
                }
            }

            item.setTitle(part.getName());
        }
    }
}
