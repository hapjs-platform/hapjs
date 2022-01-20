/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.graphics.PointF;
import android.view.animation.Interpolator;
import java.util.Objects;

public class CubicBezierInterpolator implements Interpolator {

    protected PointF mStart;
    protected PointF mEnd;
    protected PointF mA = new PointF();
    protected PointF mB = new PointF();
    protected PointF mC = new PointF();

    public CubicBezierInterpolator(PointF start, PointF end) throws IllegalArgumentException {
        if (start.x < 0 || start.x > 1) {
            throw new IllegalArgumentException("startX value must be in the range [0, 1]");
        }
        if (end.x < 0 || end.x > 1) {
            throw new IllegalArgumentException("endX value must be in the range [0, 1]");
        }
        this.mStart = start;
        this.mEnd = end;
    }

    public CubicBezierInterpolator(float startX, float startY, float endX, float endY) {
        this(new PointF(startX, startY), new PointF(endX, endY));
    }

    @Override
    public float getInterpolation(float t) {
        return getBezierCoordinateY(getXForTime(t));
    }

    protected float getBezierCoordinateY(float time) {
        mC.y = 3 * mStart.y;
        mB.y = 3 * (mEnd.y - mStart.y) - mC.y;
        mA.y = 1 - mC.y - mB.y;
        return time * (mC.y + time * (mB.y + time * mA.y));
    }

    protected float getXForTime(float t) {
        float x = t;
        float z;
        for (int i = 1; i < 14; i++) {
            z = getBezierCoordinateX(x) - t;
            if (Math.abs(z) < 1e-3) {
                break;
            }
            x -= z / getXDerivate(x);
        }
        return x;
    }

    private float getXDerivate(float t) {
        return mC.x + t * (2 * mB.x + 3 * mA.x * t);
    }

    private float getBezierCoordinateX(float t) {
        mC.x = 3 * mStart.x;
        mB.x = 3 * (mEnd.x - mStart.x) - mC.x;
        mA.x = 1 - mC.x - mB.x;
        return t * (mC.x + t * (mB.x + t * mA.x));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (!(obj instanceof CubicBezierInterpolator)) {
            return false;
        }
        CubicBezierInterpolator interpolator = (CubicBezierInterpolator) obj;
        if (!Objects.equals(mStart, interpolator.mStart)) {
            return false;
        } else {
            return Objects.equals(mEnd, interpolator.mEnd);
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        int startCode = mStart == null ? 0 : mStart.hashCode();
        int endCode = mEnd == null ? 0 : mEnd.hashCode();
        return 31 * (31 * hashCode + startCode) + endCode;
    }
}
