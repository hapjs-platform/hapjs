/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import org.hapjs.common.utils.FloatUtil;

public class RadialGradient extends CanvasGradient {

    private float mX0;
    private float mY0;
    private float mR0;
    private float mX1;
    private float mY1;
    private float mR1;
    private Bitmap mBitmap;

    public RadialGradient(float x0, float y0, float r0, float x1, float y1, float r1) {
        super();
        mX0 = x0;
        mY0 = y0;
        mR0 = r0;
        mX1 = x1;
        mY1 = y1;
        mR1 = r1;
    }

    @Override
    public Shader createShader() {
        if (!isValid()) {
            return null;
        }

        // offsets和colors按照pos的升序排列
        int[] colors = colors();
        float[] offsets = offsets();

        if (colors == null
                || colors.length <= 0
                || offsets == null
                || offsets.length <= 0
                || offsets.length != colors.length) {
            return null;
        }

        float x0 = mX0 * mDesignRatio;
        float y0 = mY0 * mDesignRatio;
        float r0 = mR0 * mDesignRatio;
        float x1 = mX1 * mDesignRatio;
        float y1 = mY1 * mDesignRatio;
        float r1 = mR1 * mDesignRatio;

        android.graphics.RadialGradient gradient1;
        android.graphics.RadialGradient gradient2;

        Circle circleA = new Circle(x0, y0, r0);
        Circle circleB = new Circle(x1, y1, r1);

        // 同心圆，使用RadialGradient
        if (circleA.isConcentric(circleB)) {

            if (FloatUtil.floatsEqual(r0, r1) && FloatUtil.floatsEqual(r0, 0)) {
                return null;
            }
            if (r0 > r1) {
                colors = reverseColors();
            }

            float limit = Math.min(r0, r1) / Math.max(r0, r1);

            int[] newColors = new int[colors.length + 1];
            float[] newOffsets = new float[offsets.length + 1];

            System.arraycopy(colors, 0, newColors, 1, colors.length);
            System.arraycopy(offsets, 0, newOffsets, 1, offsets.length);

            // 增加圆心处的渐变颜色，为colors里面的第一个颜色
            newOffsets[0] = 0;
            newColors[0] = newColors[1];

            for (int i = 1, size = newOffsets.length; i < size; i++) {
                newOffsets[i] = Math.min((1 - limit) * newOffsets[i] + limit, 1);
            }

            float r = Math.max(r0, r1);
            if (r <= 0) {
                r = 0.1f;
            }
            return new android.graphics.RadialGradient(
                    x0, y0, r, newColors, newOffsets, Shader.TileMode.CLAMP);
        } else if (circleA.isInclusion(circleB)) {
            // 圆A与圆B包含关系

            if (offsets.length == 1) {
                return new android.graphics.RadialGradient(
                        x0, y0, Math.max(r0, r1), colors[0], colors[0], Shader.TileMode.CLAMP);
            }

            mBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            float largeCircleCenterX = mBitmap.getWidth() / 2f;
            float largeCircleCenterY = mBitmap.getHeight() / 2f;
            float largeCircleR = mBitmap.getWidth() / 2f;

            float smallCircleCenterX =
                    Math.min(x0, x1) * (mBitmap.getWidth() / 2f) / Math.max(x0, x1);
            float smallCircleCenterY =
                    Math.min(y0, y1) * (mBitmap.getHeight() / 2f) / Math.max(y0, y1);
            float smallCircleR = Math.min(r0, r1) * (mBitmap.getHeight() / 2f) / Math.max(r0, r1);

            // 先画大圆
            paint.setColor(colors[colors.length - 1]);
            canvas.drawCircle(largeCircleCenterX, largeCircleCenterY, largeCircleR, paint);

            // 再画中间的渐变圆
            // 从倒数第二个遍历到第1个，最后一个和第0个是大圆和小圆
            for (int size = offsets.length, i = size - 2; i > 0; i--) {
                paint.setColor(colors[i]);
                canvas.drawCircle(
                        largeCircleCenterX,
                        largeCircleCenterX,
                        smallCircleR + (largeCircleR - smallCircleR) * offsets[i],
                        paint);
                canvas.drawCircle(
                        smallCircleCenterX + (largeCircleCenterX - smallCircleCenterX) * offsets[i],
                        smallCircleCenterY + (largeCircleCenterY - smallCircleCenterY) * offsets[i],
                        smallCircleR + (largeCircleR - smallCircleR) * offsets[i],
                        paint);
            }

            // 最后画小圆
            paint.setColor(colors[0]);
            canvas.drawCircle(smallCircleCenterX, smallCircleCenterY, smallCircleR, paint);

            return new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            gradient1 =
                    new android.graphics.RadialGradient(x0, y0, r0, colors, offsets,
                            Shader.TileMode.CLAMP);
            gradient2 =
                    new android.graphics.RadialGradient(x1, y1, r1, colors, offsets,
                            Shader.TileMode.CLAMP);
        }

        return new ComposeShader(gradient1, gradient2, PorterDuff.Mode.ADD);
    }

    @Override
    public void destroy() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
