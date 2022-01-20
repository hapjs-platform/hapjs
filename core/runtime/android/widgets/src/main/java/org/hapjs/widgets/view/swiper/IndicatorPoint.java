/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class IndicatorPoint extends View {

    private Paint mPaint = new Paint();
    private Paint mSelectedPaint = new Paint();
    private float mSize;

    public IndicatorPoint(Context context) {
        super(context);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mSelectedPaint.setAntiAlias(true);
        mSelectedPaint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float x = getWidth() / 2.0f;
        float y = getHeight() / 2.0f;
        final Paint paint;
        if (isSelected()) {
            paint = mSelectedPaint;
        } else {
            paint = mPaint;
        }
        canvas.drawCircle(x, y, mSize / 2, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Math.round(mSize * 2), Math.round(mSize * 2));
    }

    public void setSize(float size) {
        mSize = size;
        requestLayout();
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setSelectedColor(int selectedColor) {
        mSelectedPaint.setColor(selectedColor);
        invalidate();
    }
}
