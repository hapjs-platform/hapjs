/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;

public class GestureScrollerView extends ScrollView implements GestureHost {

    private IGesture mGesture;

    public GestureScrollerView(Context context) {
        super(context);
    }

    public GestureScrollerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureScrollerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        if (mGesture != null) {
            result |= mGesture.onTouch(ev);
        }
        return result;
    }
}
