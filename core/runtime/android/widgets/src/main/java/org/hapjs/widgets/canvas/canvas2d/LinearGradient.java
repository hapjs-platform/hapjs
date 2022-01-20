/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Shader;

public class LinearGradient extends CanvasGradient {

    private float mX0;
    private float mY0;
    private float mX1;
    private float mY1;

    public LinearGradient(float x0, float y0, float x1, float y1) {
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
    }

    @Override
    public Shader createShader() {
        if (!isValid()) {
            return null;
        }
        float x0 = mX0 * mDesignRatio;
        float y0 = mY0 * mDesignRatio;
        float x1 = mX1 * mDesignRatio;
        float y1 = mY1 * mDesignRatio;
        return new android.graphics.LinearGradient(
                x0, y0, x1, y1, colors(), offsets(), Shader.TileMode.CLAMP);
    }

    @Override
    public void destroy() {
    }
}
