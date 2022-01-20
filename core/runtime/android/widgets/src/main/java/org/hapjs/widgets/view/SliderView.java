/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatSeekBar;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class SliderView extends AppCompatSeekBar implements ComponentHost, GestureHost {

    private static final String TAG = "SliderView";
    private static final int DEFAULT_MAX = 100;
    private static final int MAX_HEIGHT = 3; // 单位dp
    boolean mMirrorForRtl = false;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mMaxHeight;
    private int mThumbOffset;

    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private GradientDrawable mBackgroundDrawable;
    private GradientDrawable mProgressDrawable;

    private int mMin;
    private int mMax;
    private int mStep = 1;
    private IGesture mGesture;

    private OnProgressChangeListener mOnProgressChangeListener;

    public SliderView(Context context) {
        this(context, null);
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBackgroundDrawable = new GradientDrawable();
        mBackgroundDrawable.setColor(0xfff0f0f0);
        mBackgroundDrawable.setCornerRadius(getResources().getDisplayMetrics().density * 5);

        mProgressDrawable = new GradientDrawable();
        // make progress color same with thumb color.
        mProgressDrawable.setColor(0xff009688);
        mProgressDrawable.setCornerRadius(getResources().getDisplayMetrics().density * 5);

        ClipDrawable clip =
                new ClipDrawable(mProgressDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable layer = new LayerDrawable(new Drawable[] {mBackgroundDrawable, clip});
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);

        setProgressDrawable(layer);

        setMax(DEFAULT_MAX);

        setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                        progress += mMin;
                        if (mStep <= 0) {
                            mStep = 1;
                        }
                        if (mStep > 1 && fromUser) {
                            progress = (Math.round(progress * 1f / mStep)) * mStep;
                            seekBar.setProgress(progress);
                        }

                        if (mOnProgressChangeListener != null) {
                            mOnProgressChangeListener.onChange(progress, fromUser);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    }
                });
        mMaxHeight = DisplayUtil.dip2Pixel(getContext(), MAX_HEIGHT);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
    }

    public void setMin(int min) {
        mMin = min;
        super.setMax(mMax - mMin);
    }

    public void setMax(int max) {
        mMax = max;
        super.setMax(mMax - mMin);
    }

    public void setStep(int step) {
        if (step > (getMax() - mMin)) {
            step = getMax() - mMin;
        }
        if (step <= 0) {
            step = 1;
        }
        mStep = step;
    }

    public void setColor(int color) {
        mBackgroundDrawable.setColor(color);
    }

    public void setSelectedColor(int color) {
        mProgressDrawable.setColor(color);
    }

    public void setBlockColor(int color) {
        getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void setProgress(int progress) {
        progress -= mMin;
        super.setProgress(progress);
    }

    public void setOnProgressChangeListener(OnProgressChangeListener l) {
        mOnProgressChangeListener = l;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 让track和thumb垂直居中，高度不超过MAX_HEIGHT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            updateThumbAndTrackPos(w, h);
        }
    }

    private void updateThumbAndTrackPos(int w, int h) {
        final int paddedHeight = h - mPaddingTop - mPaddingBottom;
        final Drawable track = getProgressDrawable();
        final Drawable thumb = getThumb();

        final int trackHeight = Math.min(mMaxHeight, paddedHeight);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            final int offsetHeight = (paddedHeight - thumbHeight) / 2;
            trackOffset = offsetHeight + (thumbHeight - trackHeight) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedHeight - trackHeight) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            final int trackWidth = w - mPaddingRight - mPaddingLeft;
            track.setBounds(0, trackOffset, trackWidth, trackOffset + trackHeight);
        }

        if (thumb != null) {
            setThumbPos(w, thumb, getScale(), thumbOffset);
        }
    }

    private float getScale() {
        int range = mMax - mMin;
        return range > 0 ? getProgress() / (float) range : 0;
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        mThumbOffset = thumb.getIntrinsicWidth() / 2;
    }

    private void setThumbPos(int w, Drawable thumb, float scale, int offset) {

        int available = w - mPaddingLeft - mPaddingRight;
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int top;
        final int bottom;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }

        final int left = (isLayoutRtl() && mMirrorForRtl) ? available - thumbPos : thumbPos;
        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = mPaddingLeft - mThumbOffset;
            final int offsetY = mPaddingTop;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                background.setHotspotBounds(
                        left + offsetX, top + offsetY, right + offsetX, bottom + offsetY);
            }
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);
    }

    private boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    public interface OnProgressChangeListener {
        void onChange(int progress, boolean fromUser);
    }
}
