/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class CustomScroller extends Scroller {

    private int mPageScrollDuration;

    public CustomScroller(Context context) {
        super(context);
    }

    public CustomScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public CustomScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    public void setDuration(int duration) {
        mPageScrollDuration = duration;
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        if (checkValidDuration()) {
            super.startScroll(startX, startY, dx, dy, mPageScrollDuration);
        } else {
            super.startScroll(startX, startY, dx, dy, duration);
        }
    }

    private boolean checkValidDuration() {
        return mPageScrollDuration > 0;
    }
}
