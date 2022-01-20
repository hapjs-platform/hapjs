/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.animation.Interpolator;

public class ViscousInterpolator implements Interpolator {

    private static final float VISCOUS_FLUID_SCALE = 4f;

    private final float mViscousFluidNormalize;
    private final float mViscousFluidOffset;
    private float mCurrentViscousScale;

    public ViscousInterpolator() {
        this(VISCOUS_FLUID_SCALE);
    }

    public ViscousInterpolator(float viscousScale) {
        mCurrentViscousScale = viscousScale;
        // must be set to 1.0 (used in viscousFluid())
        mViscousFluidNormalize = 1.0f / viscousFluid(mCurrentViscousScale, 1.0f);
        // account for very small floating-point error
        mViscousFluidOffset =
                1.0f - mViscousFluidNormalize * viscousFluid(mCurrentViscousScale, 1.0f);
    }

    private float viscousFluid(float viscousScale, float x) {
        x *= viscousScale;
        if (x < 1.0f) {
            x -= (1.0f - (float) Math.exp(-x));
        } else {
            float start = 0.36787944117f; // 1/e == exp(-1)
            x = 1.0f - (float) Math.exp(1.0f - x);
            x = start + x * (1.0f - start);
        }
        return x;
    }

    @Override
    public float getInterpolation(float input) {
        final float interpolated =
                mViscousFluidNormalize * viscousFluid(mCurrentViscousScale, input);
        if (interpolated > 0) {
            return interpolated + mViscousFluidOffset;
        }
        return interpolated;
    }
}
