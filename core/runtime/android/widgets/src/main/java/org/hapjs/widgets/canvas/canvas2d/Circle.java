/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import org.hapjs.common.utils.FloatUtil;

public class Circle {

    private float mCenterX;
    private float mCenterY;
    private float mRadius;

    public Circle(float x, float y, float r) {
        mCenterX = x;
        mCenterY = y;
        mRadius = r;
    }

    /**
     * 是否是包含关系，即圆A包含圆B或者圆B包含圆A
     *
     * @param circle
     * @return
     */
    public boolean isInclusion(Circle circle) {
        if (circle == null) {
            return false;
        }

        float dis =
                (float)
                        Math.sqrt(
                                Math.pow(mCenterX - circle.mCenterX, 2)
                                        + Math.pow(mCenterY - circle.mCenterY, 2));
        return Math.abs(mRadius - circle.mRadius) >= dis;
    }

    /**
     * 是否包含指定圆
     *
     * @param circle
     * @return
     */
    public boolean contains(Circle circle) {
        return mRadius >= circle.mRadius && isInclusion(circle);
    }

    /**
     * 是否为同心圆
     *
     * @param circle
     * @return
     */
    public boolean isConcentric(Circle circle) {
        if (circle == null) {
            return false;
        }

        return FloatUtil.floatsEqual(mCenterX, circle.mCenterX)
                && FloatUtil.floatsEqual(mCenterY, circle.mCenterY);
    }
}
