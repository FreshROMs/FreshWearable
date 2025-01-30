package xyz.tenseventyseven.fresh.health.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.R;

public class HorizontalProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint dotPaint;
    private Paint dotBorderPaint;
    private List<BackgroundSegment> backgroundSegments;
    private List<ProgressSegment> progressSegments;
    private float cornerRadius;
    private float progress = 0f;
    private float max = MAX_PROGRESS;
    private float min = START_PROGRESS;
    private boolean isDotMode = false;
    private float dotSize;
    private float dotBorderSize;
    private boolean hasBackgroundSegments = false;
    private boolean hasProgressSegments = false;

    private int margin = 0;

    private float[] leftCorners = new float[]{
            80, 80,        // Top left radius in px
            0, 0,          // Top right radius in px
            0, 0,          // Bottom right radius in px
            80, 80         // Bottom left radius in px
    };

    private float[] rightCorners = new float[]{
            0, 0,          // Top left radius in px
            80, 80,        // Top right radius in px
            80, 80,        // Bottom right radius in px
            0, 0           // Bottom left radius in px
    };

    private final Path leftPath = new Path();
    private final Path rightPath = new Path();

    public static class BackgroundSegment {
        float startPosition;
        float endPosition;
        @ColorInt int color;

        public BackgroundSegment(float startPosition, float endPosition, @ColorInt int color) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.color = color;
        }
    }

    public static class ProgressSegment {
        float startPosition;
        float endPosition;
        @ColorInt int color;

        public ProgressSegment(float startPosition, float endPosition, @ColorInt int color) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.color = color;
        }
    }

    public HorizontalProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HorizontalProgressView(Context context) {
        super(context, null);
        init(context, null);
    }

    public HorizontalProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        backgroundSegments = new ArrayList<>();
        progressSegments = new ArrayList<>();
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        dotPaint.setColor(Color.WHITE);
        dotBorderPaint.setColor(Color.BLACK);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalProgressView, 0, 0);
            int backgroundColor = typedArray.getColor(R.styleable.HorizontalProgressView_hBackgroundColor,
                    ContextCompat.getColor(context, R.color.secondarytext));
            int progressColor = typedArray.getColor(R.styleable.HorizontalProgressView_hProgressColor,
                    ContextCompat.getColor(context, R.color.tertiarytext_black));
            int dotColor = typedArray.getColor(R.styleable.HorizontalProgressView_hDotColor,
                    0xFFFFFFFF);
            int dotBorderColor = typedArray.getColor(R.styleable.HorizontalProgressView_hDotBorderColor,
                    ContextCompat.getColor(context, R.color.tertiarytext_black));
            progressPaint.setColor(progressColor);
            backgroundPaint.setColor(backgroundColor);
            dotPaint.setColor(dotColor);
            dotBorderPaint.setColor(dotBorderColor);
            max = typedArray.getFloat(R.styleable.HorizontalProgressView_hMax, MAX_PROGRESS);
            min = typedArray.getFloat(R.styleable.HorizontalProgressView_hMin, START_PROGRESS);
            setProgress(typedArray.getFloat(R.styleable.HorizontalProgressView_hProgress, START_PROGRESS));
            typedArray.recycle();
        }

        // Default dot settings
        dotSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.getResources().getDisplayMetrics()
        );
        dotBorderSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                context.getResources().getDisplayMetrics()
        );

        cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20f,
                context.getResources().getDisplayMetrics()
        ) / 2;

        leftCorners = new float[]{
                cornerRadius, cornerRadius, // Top left radius in px
                0, 0,                       // Top right radius in px
                0, 0,                       // Bottom right radius in px
                cornerRadius, cornerRadius  // Bottom left radius in px
        };

        rightCorners = new float[]{
                0, 0,                       // Top left radius in px
                cornerRadius, cornerRadius, // Top right radius in px
                cornerRadius, cornerRadius, // Bottom right radius in px
                0, 0                        // Bottom left radius in px
        };
    }

    private void drawLeftRoundedRect(Canvas canvas, float left, float top, float right, float bottom, Paint paint) {
        leftPath.reset();
        leftPath.addRoundRect(left, top, right, bottom, leftCorners, Path.Direction.CW);
        canvas.drawPath(leftPath, paint);
    }

    private void drawRightRoundedRect(Canvas canvas, float left, float top, float right, float bottom, Paint paint) {
        rightPath.reset();
        rightPath.addRoundRect(left, top, right, bottom, rightCorners, Path.Direction.CW);
        canvas.drawPath(rightPath, paint);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Set a height margin if we're in dot mode
        if (isDotMode) {
            margin = (int) dotBorderSize;
        } else {
            margin = 0;
        }

        int width = getWidth();
        int height = getHeight();

        // Draw background
        if (hasBackgroundSegments) {
            // Sort backgroundSegments by start position
            backgroundSegments.sort((a, b) -> Float.compare(a.startPosition, b.startPosition));

            if (backgroundSegments.size() == 1) {
                backgroundPaint.setColor(backgroundSegments.get(0).color);
                canvas.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius, backgroundPaint);
            } else {
                // Draw background segments
                for (int i = 0; i < backgroundSegments.size(); i++) {
                    BackgroundSegment segment = backgroundSegments.get(i);
                    backgroundPaint.setColor(segment.color);
                    float startX = width * segment.startPosition;
                    float endX = width * segment.endPosition;

                    if (i == 0) {
                        drawLeftRoundedRect(canvas, startX, 0, endX, height, backgroundPaint);
                    } else if (i == backgroundSegments.size() - 1) {
                        drawRightRoundedRect(canvas, startX, 0, endX, height, backgroundPaint);
                    } else {
                        canvas.drawRect(startX, 0, endX, height, backgroundPaint);
                    }
                }
            }
        } else {
            // Draw regular background
            canvas.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius, backgroundPaint);
        }

        // Draw progress
        if (!isDotMode) {
            if (hasProgressSegments) {
                // Sort progressSegments by start position
                progressSegments.sort((a, b) -> Float.compare(a.startPosition, b.startPosition));

                if (progressSegments.size() == 1) {
                    progressPaint.setColor(progressSegments.get(0).color);
                    canvas.drawRoundRect(
                            width * progressSegments.get(0).startPosition,
                            0,
                            width * progressSegments.get(0).endPosition,
                            height,
                            cornerRadius,
                            cornerRadius,
                            progressPaint
                    );
                } else {
                    // Draw progress segments
                    for (int i = 0; i < progressSegments.size(); i++) {
                        ProgressSegment segment = progressSegments.get(i);
                        progressPaint.setColor(segment.color);
                        float startX = width * segment.startPosition;
                        float endX = width * segment.endPosition;

                        if (startX == 0 && endX == width) {
                            canvas.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius, progressPaint);
                        } else if (i == 0 || startX == 0) {
                            drawLeftRoundedRect(canvas, startX, 0, endX, height, progressPaint);
                        } else if (i == progressSegments.size() - 1 || endX == width) {
                            drawRightRoundedRect(canvas, startX, 0, endX, height, progressPaint);
                        } else {
                            canvas.drawRect(startX, 0, endX, height, progressPaint);
                        }
                    }
                }
            } else {
                // Draw regular progress
                float progressWidth = width * progress;
                canvas.drawRoundRect(
                        width * min,
                        0,
                        width * min + progressWidth,
                        height,
                        cornerRadius,
                        cornerRadius,
                        progressPaint
                );
            }
        } else {
            // Draw dot with border
            float centerX = width * progress;
            float centerY = height / 2f;
            canvas.drawCircle(centerX, centerY, dotSize/2 + dotBorderSize, dotBorderPaint);
            canvas.drawCircle(centerX, centerY, dotSize/2, dotPaint);
        }
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

    // Progress segment methods
    public void addProgressSegment(float startPosition, float endPosition, @ColorInt int color) {
        hasProgressSegments = true;
        progressSegments.add(new ProgressSegment(Math.max(startPosition, 0f), Math.min(endPosition, 1f), color));
        invalidate();
    }

    public void clearProgressSegments() {
        hasProgressSegments = false;
        progressSegments.clear();
        invalidate();
    }

    // Background segment methods
    public void addBackgroundSegment(float startPosition, float endPosition, @ColorInt int color) {
        hasBackgroundSegments = true;
        backgroundSegments.add(new BackgroundSegment(Math.max(startPosition, 0f), Math.min(endPosition, 1f), color));
        invalidate();
    }

    public void clearBackgroundSegments() {
        hasBackgroundSegments = false;
        backgroundSegments.clear();
        invalidate();
    }

    // Dot mode methods
    public void setDotMode(boolean dotMode) {
        this.isDotMode = dotMode;
        invalidate();
    }

    public void setDotColor(@ColorInt int color) {
        dotPaint.setColor(color);
        invalidate();
    }

    public void setDotBorderColor(@ColorInt int color) {
        dotBorderPaint.setColor(color);
        invalidate();
    }

    public void setDotSize(float dpSize) {
        this.dotSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpSize,
                getContext().getResources().getDisplayMetrics()
        );
        invalidate();
    }

    public void setDotBorderSize(float dpSize) {
        this.dotBorderSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpSize,
                getContext().getResources().getDisplayMetrics()
        );
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = Math.min(Math.max(progress, 0), 1);
        invalidate();
    }

    public void setMin(float min) {
        this.min = Math.min(Math.max(min, 0), 1);
        invalidate();
    }

    private static final float MAX_PROGRESS = 100F;
    private static final float START_PROGRESS = 0F;
}