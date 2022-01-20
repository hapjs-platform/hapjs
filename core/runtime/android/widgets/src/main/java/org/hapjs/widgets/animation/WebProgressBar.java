/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.animation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import org.hapjs.widgets.R;

public class WebProgressBar extends View {
    public static final int NOT_FORCE_SET_PROGRESS = 0;
    public static final int FORCE_SET_PROGRESS = 1;
    public static final int SET_PROGRESS_WITH_ANIMATE = 2;
    private static final float PROGRESS_VALUE_010 = 0.100F;
    private static final float PROGRESS_VALUE_040 = 0.500F;
    private static final float PROGRESS_VALUE_0958 = 0.958F;
    private static final float PROGRESS_VALUE_098 = 0.99F;
    private static final float PROGRESS_VALUE_002 = 0.02F;

    private static final float PROGRESS_INCREASE_TO_010_FACTOR = 0.35F;
    private static final float PROGRESS_INCREASE_RECEIVED_DATA_FACTOR = 0.30F;
    private static final float PROGRESS_INCREASE_TO_040_FACTOR = 0.15F;
    private static final float PROGRESS_INCREASE_TO_0958_FACTOR = 0.05F;
    private static final float PROGRESS_INCREASE_TO_098_FACTOR = 0.005F;

    private float mProgressValue; // 0~1
    // private float mLastProgressValue;
    private float mExpectProgressValue;

    private boolean mEndAnimating;
    private float mEndAnimateXDelta;

    private float mDrawingTimeDelta;
    private long mLastDrawingTime;
    private int mHighLightOffsetX;

    private Rect mRect = new Rect();
    private Drawable mDrawableHead;
    private Drawable mDrawableTail;
    private Drawable mDrawableHighLight;
    private int mDrawableHighLightAlpha = 255;

    private Context mContext;

    public WebProgressBar(Context context) {

        this(context, null);
    }

    public WebProgressBar(Context context, AttributeSet attributeSet) {

        this(context, attributeSet, 0);
    }

    public WebProgressBar(Context context, AttributeSet attributeSet, int defStyle) {

        super(context, attributeSet, defStyle);
        mContext = context;
        init();
    }

    private void init() {

        setWillNotDraw(false);
        setBackgroundColor(0);
        setBackgroundDrawable(null);
        stopProgress(true);
        mDrawableHead = getDrawable(R.drawable.webprogress_head);
        mDrawableHead.setBounds(
                0, 0, mDrawableHead.getIntrinsicWidth(), mDrawableHead.getIntrinsicHeight());
        mDrawableTail = getDrawable(R.drawable.webprogress_tail);
        mDrawableTail.setBounds(
                0, 0, mDrawableTail.getIntrinsicWidth(), mDrawableTail.getIntrinsicHeight());

        mDrawableHighLight = getDrawable(R.drawable.webprogress_highlight);
        mDrawableHighLight.setBounds(
                0, 0, mDrawableHighLight.getIntrinsicWidth(),
                mDrawableHighLight.getIntrinsicHeight());
    }

    /**
     * setProgressMode: SET_PROGRESS_WITH_ANIMATE, FORCE_SET_PROGRESS, NOT_FORCE_SET_PROGRESS
     */
    public void startProgress(int progress, int setProgressMode) {

        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }

        float p = 0.01F * progress;

        if (mEndAnimating && p != 1) {
            stopProgress(true);
        }

        if (p == 0) {
            mProgressValue = PROGRESS_VALUE_002;
            // mLastProgressValue = 0.0F;
            mExpectProgressValue = 0.0F;
            mLastDrawingTime = System.currentTimeMillis();
            mDrawingTimeDelta = 0.0F;

            if (getVisibility() != View.VISIBLE) {
                mDrawableTail.setAlpha(255);
                setVisibility(View.VISIBLE);
            }
            stopProgress(true);
        } else if (p == 1) {
            mProgressValue = 1.0F;
            // mLastProgressValue = 0.0F;
            mExpectProgressValue = 0.0F;
            mLastDrawingTime = 0;
            mDrawingTimeDelta = 0.0F;
            mHighLightOffsetX = -10000;
            if (getVisibility() != View.VISIBLE) {
                mDrawableTail.setAlpha(255);
                setVisibility(View.VISIBLE);
            }
        } else { // 0<p<1
            if (mProgressValue == 1) {
                mProgressValue = PROGRESS_VALUE_002;
            }
            if (setProgressMode == SET_PROGRESS_WITH_ANIMATE) {
                if (p > mProgressValue && p > mExpectProgressValue) {
                    // mLastProgressValue = 0.0F;
                    mExpectProgressValue = p;
                }
            } else if (setProgressMode == FORCE_SET_PROGRESS) {
                mProgressValue = p;
                // mLastProgressValue = 0.0F;
                mExpectProgressValue = 0.0F;
            } else { // NOT_FORCE_SET_PROGRESS
                if (p > mProgressValue) {
                    mProgressValue = p;
                    // mLastProgressValue = 0.0F;
                    mExpectProgressValue = 0.0F;
                }
            }
            if (getVisibility() != View.VISIBLE) {
                mLastDrawingTime = System.currentTimeMillis();
                mDrawingTimeDelta = 0.0F;
                mDrawableTail.setAlpha(255);
                setVisibility(View.VISIBLE);
            }
        }
    }

    public void stopProgress(boolean forceStop) {

        if (!mEndAnimating || forceStop) {
            mProgressValue = PROGRESS_VALUE_002;
            // mLastProgressValue = 0.0F;
            mExpectProgressValue = 0.0F;
            mEndAnimating = false;
            mEndAnimateXDelta = 0.0F;
            mHighLightOffsetX = -10000;
            if (getVisibility() != View.INVISIBLE) {
                setVisibility(View.INVISIBLE);
            }
        }
    }

    public int getProgress() {
        return (int) (mProgressValue * 100);
    }

    private int caculateHighLightOffsetX(int rectWidth, int highlightWidth, int lastOffsetX) {

        int leftEdge = -highlightWidth;
        int rightEdge = rectWidth - highlightWidth;
        int fullEdge = this.getMeasuredWidth() - highlightWidth;
        int delta = 10;

        int offset = lastOffsetX;
        if (mRect.width() > getMeasuredWidth() * 0.8) {
            offset = lastOffsetX + delta;
        }

        if (offset > rightEdge) {
            if (offset > fullEdge) {
                offset = leftEdge;
            }
            if (mDrawableHighLightAlpha != 0) {
                mDrawableHighLightAlpha = 0;
                mDrawableHighLight.setAlpha(0);
            }
        } else if (offset < leftEdge) {
            offset = leftEdge;
            if (mDrawableHighLightAlpha != 255 * 0.8F) {
                mDrawableHighLightAlpha = (int) (255 * 0.8F);
                mDrawableHighLight.setAlpha((int) (255 * 0.8F));
            }
        } else if (offset >= (rectWidth * 0.8F - highlightWidth)) {
            if (mDrawableHighLightAlpha != 255 * 1.0F) {
                mDrawableHighLightAlpha = (int) (255 * 1.0F);
                mDrawableHighLight.setAlpha((int) (255 * 1.0F));
            }
        } else {
            if (mDrawableHighLightAlpha != 255 * 0.8F) {
                mDrawableHighLightAlpha = (int) (255 * 0.8F);
                mDrawableHighLight.setAlpha((int) (255 * 0.8F));
            }
        }

        return offset;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int tailRectRight = 0;
        long currentTime = System.currentTimeMillis();
        long deltaT = currentTime - mLastDrawingTime;
        mDrawingTimeDelta = Math.abs((float) deltaT / 500.0F);
        mEndAnimateXDelta += 4000.0F * mDrawingTimeDelta;
        mLastDrawingTime = currentTime;

        long delayed = 30L - deltaT;
        if (delayed < 0L) {
            delayed = 0L;
        }
        postInvalidateDelayed(delayed);

        if (mEndAnimating) { // draw EndAnimating
            int alpha = (int) (200.0D * (1.0D - mEndAnimateXDelta / (2.0D * getMeasuredWidth())));
            if (alpha < 0) {
                alpha = 0;
            }
            if (mEndAnimateXDelta > getMeasuredWidth()) {
                stopProgress(true);
            }
            mDrawableTail.setAlpha(alpha);

            // draw tail
            int width =
                    (int)
                            (mRect.width()
                                    + (getMeasuredWidth() - mRect.width())
                                    * (mEndAnimateXDelta / getMeasuredWidth()));
            if (mEndAnimateXDelta == 0) {
                width = getMeasuredWidth();
            }

            mDrawableTail.setBounds(0, 0, width, mDrawableTail.getIntrinsicHeight());
            mDrawableTail.draw(canvas);

        } else {
            int alpha = (int) (200.0D * (1.0D - mEndAnimateXDelta / (15D * getMeasuredWidth())));
            if (alpha < 200) {
                alpha = 200;
            }
            mDrawableTail.setAlpha(alpha);
            if (mProgressValue < PROGRESS_VALUE_010) {
                mProgressValue += PROGRESS_INCREASE_TO_010_FACTOR * mDrawingTimeDelta;
                if (mProgressValue > PROGRESS_VALUE_010) {
                    mProgressValue = PROGRESS_VALUE_010;
                }
                // mLastProgressValue = mProgressValue;
            } else if (mProgressValue < mExpectProgressValue) {
                mProgressValue += PROGRESS_INCREASE_RECEIVED_DATA_FACTOR * mDrawingTimeDelta;
                // mLastProgressValue = mProgressValue;
            } else if (mProgressValue < PROGRESS_VALUE_040) {
                mProgressValue += PROGRESS_INCREASE_TO_040_FACTOR * mDrawingTimeDelta;
                if (mProgressValue > PROGRESS_VALUE_040) {
                    mProgressValue = PROGRESS_VALUE_040;
                }
                // mLastProgressValue = mProgressValue;
            } else if (mProgressValue < PROGRESS_VALUE_0958) {
                mProgressValue += PROGRESS_INCREASE_TO_0958_FACTOR * mDrawingTimeDelta;
                if (mProgressValue > PROGRESS_VALUE_0958) {
                    mProgressValue = PROGRESS_VALUE_0958;
                }
                // mLastProgressValue = mProgressValue;
            } else if (mProgressValue < PROGRESS_VALUE_098) {
                mProgressValue += PROGRESS_INCREASE_TO_098_FACTOR * mDrawingTimeDelta;
                if (mProgressValue > PROGRESS_VALUE_098) {
                    mProgressValue = PROGRESS_VALUE_098;
                }
                // mLastProgressValue = mProgressValue;
            } else if (Math.abs(mProgressValue - 1.0F) < 0.001F || mProgressValue > 1.0F) {
                mProgressValue = 1.0F;
                mEndAnimating = true;
                mEndAnimateXDelta = 0.0F;
                // mLastProgressValue = mProgressValue;
            }

            if (mProgressValue < 1.0F) {
                mRect.right = (int) (mProgressValue * getMeasuredWidth());
                mRect.bottom = getMeasuredHeight();
            }

            // draw tail
            tailRectRight = (int) (mRect.width());
            mDrawableTail.setBounds(0, 0, tailRectRight, mDrawableTail.getIntrinsicHeight());
            mDrawableTail.draw(canvas);

            // draw head
            canvas.save();
            canvas.translate(mRect.width() - mDrawableHead.getIntrinsicWidth(), 0.0F);
            mDrawableHead.draw(canvas);
            canvas.restore();

            // draw highlight
            mHighLightOffsetX =
                    caculateHighLightOffsetX(
                            mRect.width(), mDrawableHighLight.getIntrinsicWidth(),
                            mHighLightOffsetX);
            canvas.save();
            canvas.translate(mHighLightOffsetX, 0.0F);
            mDrawableHighLight.draw(canvas);
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    private Drawable getDrawable(int id) {
        return mContext.getResources().getDrawable(id);
    }
}
