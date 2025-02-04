package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceItemDecoration extends RecyclerView.ItemDecoration {
    private final Drawable divider;
    private final List<DeviceSetting> settings;
    private final int insetMargin;

    public PreferenceItemDecoration(Context context, List<DeviceSetting> settings) {
        this.divider = ContextCompat.getDrawable(context, R.drawable.wear_preference_list_divider);
        this.settings = settings;
        this.insetMargin = context.getResources().getDimensionPixelSize(R.dimen.wear_preference_divider_inset_margin);
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < parent.getChildCount() - 1; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);

            if (shouldDrawDivider(position)) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + divider.getIntrinsicHeight();
                int left = parent.getPaddingLeft();

                if (settings.get(position).hasIcon()) {
                    left += insetMargin;
                }

                divider.setBounds(left, top, right, bottom);
                divider.draw(canvas);
            }
        }
    }

    private boolean shouldDrawDivider(int position) {
        if (position == 0 || position == settings.size() - 1) {
            return true; // Let RecyclerView handle the first and last dividers
        }

        DeviceSetting current = settings.get(position);
        DeviceSetting next = settings.get(position + 1);
        DeviceSetting prev = settings.get(position - 1);

        return current.type != DeviceSetting.DeviceSettingType.DIVIDER &&
                next.type != DeviceSetting.DeviceSettingType.DIVIDER &&
                prev.type != DeviceSetting.DeviceSettingType.DIVIDER;
    }
}
