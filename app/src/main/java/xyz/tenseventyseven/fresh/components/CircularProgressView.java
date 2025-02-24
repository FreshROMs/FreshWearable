package xyz.tenseventyseven.fresh.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import xyz.tenseventyseven.fresh.R;

public class CircularProgressView extends View {

    private Paint progressPaint;
    private Paint backgroundPaint;
    private float max = MAX_PROGRESS;
    private final RectF rect = new RectF();
    private float diameter = 0F;
    private float angle = 0F;

    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            try (TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressView, 0, 0)) {
                float stroke = typedArray.getDimension(R.styleable.CircularProgressView_cStroke, context.getResources().getDimension(R.dimen.wear_1dp));
                int backgroundColor = typedArray.getColor(R.styleable.CircularProgressView_cBackgroundColor, ContextCompat.getColor(context, R.color.secondarytext));
                int progressColor = typedArray.getColor(R.styleable.CircularProgressView_cProgressColor, ContextCompat.getColor(context, R.color.tertiarytext_black));
                progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeWidth(stroke);
                progressPaint.setColor(progressColor);
                progressPaint.setStrokeCap(Paint.Cap.ROUND);
                backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                backgroundPaint.setStyle(Paint.Style.STROKE);
                backgroundPaint.setStrokeWidth(stroke);
                backgroundPaint.setColor(backgroundColor);
                max = typedArray.getFloat(R.styleable.CircularProgressView_cMax, MAX_PROGRESS);
                setProgress(typedArray.getFloat(R.styleable.CircularProgressView_cProgress, START_PROGRESS));
            }
        }
    }

    public void setProgress(float value) {
        angle = calculateAngle(value);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawCircle(MAX_ANGLE, canvas, backgroundPaint);
        drawCircle(angle, canvas, progressPaint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        diameter = Math.min(width, height);
        updateRect();
    }

    public void setProgressColor(@ColorRes int color, boolean context) {
        progressPaint.setColor(ContextCompat.getColor(getContext(), color));
        invalidate();
    }

    public void setProgressColor(@ColorInt int color) {
        progressPaint.setColor(color);
        invalidate();
    }

    public void setProgressBackgroundColor(@ColorInt int color) {
        backgroundPaint.setColor(color);
        invalidate();
    }

    public void setProgressBackgroundColor(@ColorRes int color, boolean context) {
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), color));
        invalidate();
    }

    private void updateRect() {
        float strokeWidth = backgroundPaint.getStrokeWidth();
        rect.set(strokeWidth, strokeWidth, diameter - strokeWidth, diameter - strokeWidth);
    }

    private void drawCircle(float angle, Canvas canvas, Paint paint) {
        canvas.drawArc(rect, START_ANGLE, angle, false, paint);
    }

    private float calculateAngle(float progress) {
        return MAX_ANGLE / max * progress;
    }

    private static final float START_ANGLE = -90F;
    private static final float MAX_ANGLE = 360F;
    private static final float MAX_PROGRESS = 100F;
    private static final float START_PROGRESS = 0F;
}
