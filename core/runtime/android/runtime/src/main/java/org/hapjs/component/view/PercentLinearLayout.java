/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.PercentLayoutHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class PercentLinearLayout extends LinearLayout implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private IGesture mGesture;

    public PercentLinearLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PercentLayoutHelper
                .adjustChildren(widthMeasureSpec, heightMeasureSpec, (Container) mComponent);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    /**
     * tabs组件中ViewPager会消费掉事件，这里需要在dispatchTouchEvent中，tabs才能响应事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean result = super.dispatchTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }
}
