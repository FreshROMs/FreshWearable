package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import dev.oneuiproject.oneui.widget.RoundedLinearLayout;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.databinding.WearDashboardShortcutItemBinding;

public class DashboardShortcut extends RoundedLinearLayout {
    private WearDashboardShortcutItemBinding binding;
    private ImageView iconView;
    private TextView titleView;

    private Drawable icon;
    private String title;

    private String activity;

    private Map<String, Object> extras = new HashMap<>();

    public DashboardShortcut(Context context) {
        super(context);
        init(context, null);
    }

    public DashboardShortcut(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    public DashboardShortcut(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet);
    }

    public DashboardShortcut(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attrs) {
        this.binding = WearDashboardShortcutItemBinding.inflate(LayoutInflater.from(context), this, true);
        this.iconView = this.binding.shortcutIcon;
        this.titleView = this.binding.shortcutTitle;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearDashboardShortcutItem);
            this.icon = a.getDrawable(R.styleable.WearDashboardShortcutItem_wShortcutIcon);
            this.title = a.getString(R.styleable.WearDashboardShortcutItem_wShortcutName);
            this.activity = a.getString(R.styleable.WearDashboardShortcutItem_wShortcutActivity);
            a.recycle();

            this.iconView.setImageDrawable(this.icon);
            this.titleView.setText(this.title);

            this.binding.shortcutItem.setOnClickListener(v -> {
                launchActivity();
            });
        }
    }

    private void launchActivity() {
        if (this.activity != null) {
            Intent intent = new Intent();
            intent.setClassName(getContext(), this.activity);

            for (Map.Entry<String, Object> entry : this.extras.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (int) value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (boolean) value);
                } else if (value instanceof Float) {
                    intent.putExtra(key, (float) value);
                } else if (value instanceof Double) {
                    intent.putExtra(key, (double) value);
                } else if (value instanceof Long) {
                    intent.putExtra(key, (long) value);
                } else if (value instanceof Short) {
                    intent.putExtra(key, (short) value);
                } else if (value instanceof Byte) {
                    intent.putExtra(key, (byte) value);
                } else if (value instanceof Character) {
                    intent.putExtra(key, (char) value);
                } else if (value instanceof Parcelable) {
                    intent.putExtra(key, (Parcelable) value);
                } else {
                    intent.putExtra(key, value.toString());
                }
            }

            getContext().startActivity(intent);
        }
    }

    public void setIcon(Drawable drawable) {
        this.icon = drawable;
        this.iconView.setImageDrawable(drawable);
    }

    public void setTitle(String str) {
        this.title = str;
        this.titleView.setText(str);
    }

    public void setActivity(String str) {
        this.activity = str;
    }

    public void clearExtras() {
        this.extras.clear();
    }

    public void addExtra(String key, Object value) {
        this.extras.put(key, value);
    }

    public void removeExtra(String key) {
        this.extras.remove(key);
    }
}
