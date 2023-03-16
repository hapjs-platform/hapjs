/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.graphics.drawable.DrawableWrapper;

import org.hapjs.debugger.app.impl.R;

public class BlurShadowDrawable extends DrawableWrapper {
    private float mXOffset;
    private float mYOffset;
    private float mBlurRadius;
    private float mSpreadRadius;
    private int mColor;
    private float mCornerRadius;
    private View mView;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BlurShadowDrawable(View view, Context context, AttributeSet attributeSet) {
        super(view.getBackground());

        mView = view;

        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.BlurShadow);
        mXOffset = typedArray.getDimension(R.styleable.BlurShadow_x_offset, 0);
        mYOffset = typedArray.getDimension(R.styleable.BlurShadow_y_offset, 0);
        mBlurRadius = typedArray.getDimension(R.styleable.BlurShadow_blur_radius, 0);
        mSpreadRadius = typedArray.getDimension(R.styleable.BlurShadow_spread_radius, 0);
        mColor = typedArray.getColor(R.styleable.BlurShadow_blur_color, 0);
        mCornerRadius = typedArray.getDimension(R.styleable.BlurShadow_corner_radius, 0);
        typedArray.recycle();

        mPaint.setColor(mColor);
        mPaint.setMaskFilter(new BlurMaskFilter(mBlurRadius, BlurMaskFilter.Blur.NORMAL));
    }

    public BlurShadowDrawable(View view, Drawable bg, int xOffset, int yOffset,
                              int blurRadius, int spreadRadius, int color, int cornerRadius) {
        super(bg);

        mView = view;
        mXOffset = xOffset;
        mYOffset = yOffset;
        mBlurRadius = blurRadius;
        mSpreadRadius = spreadRadius;
        mColor = color;
        mCornerRadius = cornerRadius;

        mPaint.setColor(mColor);
        mPaint.setMaskFilter(new BlurMaskFilter(mBlurRadius, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    public void draw(Canvas canvas) {
        if (mView.isEnabled()) {
            RectF spreadedBox = new RectF(0 - mSpreadRadius + mXOffset,
                    0 - mSpreadRadius + mYOffset,
                    mView.getWidth() + mSpreadRadius + mXOffset,
                    mView.getHeight() + mSpreadRadius + mYOffset);
            canvas.drawRoundRect(spreadedBox, mCornerRadius, mCornerRadius, mPaint);
        }

        super.draw(canvas);
    }
}
