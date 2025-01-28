package xyz.tenseventyseven.fresh.wearable.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import xyz.tenseventyseven.fresh.R;

public class ImagePreference extends Preference {
    private int mDrawable = -1;

    public ImagePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.component_image_preference);

        CharSequence drawable = null;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImagePreference);
            drawable = a.getText(R.styleable.ImagePreference_preferenceImage);
            a.recycle();
        }

        // Convert the entries (which are resource references) to drawable resource IDs
        if (drawable != null) {
            // Remove path and trailing extension
            String entry = drawable.toString();
            int lastSlash = entry.lastIndexOf('/');
            int lastDot = entry.lastIndexOf('.');
            if (lastSlash >= 0 && lastDot >= 0) {
                drawable = entry.substring(lastSlash + 1, lastDot);
            }

            mDrawable = context.getResources().getIdentifier(drawable.toString(), "drawable", context.getPackageName());
        }
    }

    @Override
    public void onClick() {
        // Override and leave empty to prevent default click behavior
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ImageView imageView = (ImageView) holder.findViewById(R.id.preference_image_view);
        if (imageView != null && mDrawable != -1) {
            imageView.setImageResource(mDrawable);
        }
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        // Do nothing
    }
}
