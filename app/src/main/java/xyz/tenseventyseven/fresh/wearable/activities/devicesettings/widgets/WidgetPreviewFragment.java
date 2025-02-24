package xyz.tenseventyseven.fresh.wearable.activities.devicesettings.widgets;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.components.widgets.WatchWidgetPreview;

public class WidgetPreviewFragment extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetPreviewFragment.class);
    private WatchWidgetPreview mWidgetPreview;

    private FloatingActionButton mDeleteButton;

    private boolean mDeleteVisible = false;

    private WidgetScreen mWidgetScreen;

    private WatchWidgetPreview.OnWidgetItemClickListener mListener;

    public interface OnDeleteItemClickListener {
        void onDeleteItem(WidgetScreen screen);
    }

    private OnDeleteItemClickListener mDeleteListener;

    public WidgetPreviewFragment() {
        // Required empty public constructor
    }

    public WidgetPreviewFragment(WidgetScreen widgetScreen) {
        mWidgetScreen = widgetScreen;
    }

    public WidgetPreviewFragment(WidgetScreen widgetScreen, WatchWidgetPreview.OnWidgetItemClickListener listener, OnDeleteItemClickListener deleteListener) {
        mWidgetScreen = widgetScreen;
        if (listener != null) {
            mListener = listener;
        }

        if (deleteListener != null) {
            mDeleteListener = deleteListener;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.wear_watch_widget_preview_fragment, container, false);
        mWidgetPreview = view.findViewById(R.id.fragment_widget_preview_screen);
        mDeleteButton = view.findViewById(R.id.fragment_widget_preview_delete_button);
        mWidgetPreview.setOnWidgetItemClickListener(mListener);
        mDeleteButton.setOnClickListener(v -> {
            if (mDeleteListener != null) {
                mDeleteListener.onDeleteItem(mWidgetScreen);
            }
        });

        update();
        return view;
    }

    public void setDeleteVisibility(boolean visible) {
        if (mDeleteButton != null) {
            mDeleteButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        mDeleteVisible = visible;
    }

    public void update() {
        if (mWidgetPreview == null) {
            Log.e("WidgetPreviewFragment", "mWidgetPreview is null in update");
            return;
        }

        mDeleteButton.setVisibility(mDeleteVisible ? View.VISIBLE : View.GONE);

        Log.d("WidgetPreviewFragment", "layout: " + mWidgetScreen.getLayout());
        switch (mWidgetScreen.getLayout()) {
            case TOP_2X2_BOT_2:
            case TOP_1_BOT_2:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.TOP_1_BOTTOM_2);
                break;
            case TOP_2_BOT_2X2:
            case TOP_2_BOT_1:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.TOP_2_BOTTOM_1);
                break;
            case TOP_2_BOT_2:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.TOP_2_BOTTOM_2);
                break;
            case TWO_BY_TWO_SINGLE:
            case ONE_BY_TWO_SINGLE:
            case TWO_BY_THREE_SINGLE:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.SINGLE);
                break;
            case TWO:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.TWO);
                break;
            case TOP_1_BOT_2X2:
            case TOP_2X2_BOT_1:
                mWidgetPreview.setMode(WatchWidgetPreview.PreviewMode.CENTER_TOP_BOTTOM);
                break;
        }
        mWidgetPreview.setWidgetScreen(mWidgetScreen);
        updateWidgetParts();
    }

    public void updateWidgetParts() {
        List<WidgetPart> parts = mWidgetScreen.getParts();
        switch (mWidgetPreview.getMode()) {
            case TOP_1_BOTTOM_2:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, parts.get(1));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, 1);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, parts.get(1));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, parts.get(2));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, 2);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, parts.get(2));
                break;
            case TOP_2_BOTTOM_1:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, 1);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(2));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, 2);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(2));
                break;
            case TOP_2_BOTTOM_2:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, 1);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, parts.get(2));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, 2);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_BOTTOM, parts.get(2));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, parts.get(3));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, 3);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_BOTTOM, parts.get(3));
                break;
            case SINGLE:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));
                break;
            case TWO:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_LEFT_TOP, parts.get(0));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, 1);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X2_RIGHT_TOP, parts.get(1));
                break;
            case CENTER_TOP_BOTTOM:
                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, 0);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X4_TOP, parts.get(0));

                mWidgetPreview.setTypeMap(WatchWidgetPreview.WidgetType.TYPE_2X4_BOTTOM, parts.get(1));
                mWidgetPreview.setIndexMap(WatchWidgetPreview.WidgetType.TYPE_2X4_BOTTOM, 1);
                mWidgetPreview.setWidget(WatchWidgetPreview.WidgetType.TYPE_2X4_BOTTOM, parts.get(1));
                break;
        }
    }

    public WatchWidgetPreview getWidgetPreview() {
        return mWidgetPreview;
    }
}
