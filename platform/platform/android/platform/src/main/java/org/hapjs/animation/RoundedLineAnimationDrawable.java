/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.animation;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class RoundedLineAnimationDrawable extends Drawable implements Animatable {
    private static final int LINE_COUNT = 5;
    private static final int HALF_COUNT = (LINE_COUNT - 1) / 2;

    private Paint mLinePaint;
    private int mLineWidth;
    private int mLineHeight;
    private int mLineSpace;
    private int mRadius;
    private float mYCenter;
    private int mBodyHeight;
    private int mDuration;
    private float mHalfDuration;
    private long mStartTime;

    public RoundedLineAnimationDrawable(
            int lineWidth, int lineHeight, int lineSpace, int lineColor, int duration) {
        mLinePaint = new Paint();
        mLinePaint.setColor(lineColor);
        mLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLineWidth = lineWidth;
        mLineHeight = lineHeight;
        mLineSpace = lineSpace;
        mBodyHeight = mLineHeight - mLineWidth;
        mDuration = duration;
        mHalfDuration = duration / 2f;
        mRadius = mLineWidth / 2;
        mYCenter = mLineHeight / 2f;
    }

    @Override
    public void draw(Canvas canvas) {
        int elapsedTime;
        if (mStartTime > 0) {
            elapsedTime = (int) (SystemClock.elapsedRealtime() - mStartTime);
        } else {
            elapsedTime = 0;
        }

        for (int i = 0; i < LINE_COUNT; ++i) {
            float baseHeight;
            if (i > HALF_COUNT) {
                baseHeight = 2 * mBodyHeight - mBodyHeight * i / (float) HALF_COUNT;
            } else {
                baseHeight = mBodyHeight * i / (float) HALF_COUNT;
            }

            float overHeight = baseHeight + (elapsedTime / mHalfDuration) * mBodyHeight;
            float modeHeight = overHeight % (2 * mBodyHeight);
            float height;
            if (modeHeight > mBodyHeight) {
                height = 2 * mBodyHeight - modeHeight;
            } else {
                height = modeHeight;
            }

            float xOffset = i * (mLineWidth + mLineSpace);
            drawLine(canvas, height, xOffset);
        }

        if (mStartTime > 0) {
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mLineWidth * LINE_COUNT + mLineSpace * (LINE_COUNT - 1);
    }

    @Override
    public int getIntrinsicHeight() {
        return mLineHeight;
    }

    @Override
    public void start() {
        if (mStartTime > 0) {
            return;
        }
        mStartTime = SystemClock.elapsedRealtime();
        invalidateSelf();
    }

    @Override
    public void stop() {
        mStartTime = 0;
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return mStartTime > 0;
    }

    private void drawLine(Canvas canvas, float height, float offsetX) {
        float lineLeft = offsetX;
        float lineRight = offsetX + mLineWidth;

        // Draw top round
        int roundBottom1 = (int) (mYCenter - height / 2);
        int roundTop1 = roundBottom1 - mRadius;
        canvas.save();
        canvas.clipRect(lineLeft, roundTop1, lineRight, roundBottom1);
        canvas.drawCircle(lineLeft + mRadius, roundBottom1, mRadius, mLinePaint);
        canvas.restore();

        // Draw bottom round
        int roundTop2 = (int) (roundBottom1 + height);
        int roundBottom2 = roundTop2 + mRadius;
        canvas.save();
        canvas.clipRect(lineLeft, roundTop2, lineRight, roundBottom2);
        canvas.drawCircle(lineLeft + mRadius, roundTop2, mRadius, mLinePaint);
        canvas.restore();

        // Draw body
        canvas.drawRect(lineLeft, roundBottom1, lineRight, roundTop2, mLinePaint);
    }
}
