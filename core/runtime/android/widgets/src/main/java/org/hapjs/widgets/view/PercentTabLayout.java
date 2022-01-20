/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.google.android.material.tabs.TabLayout;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.PercentLayoutHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class PercentTabLayout extends TabLayout implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private ScrollListener mScrollListener;

    private IGesture mGesture;

    public PercentTabLayout(Context context) {
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

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    public void setScrollListener(ScrollListener listener) {
        mScrollListener = listener;
    }

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

    public interface ScrollListener {
        void onScrollChanged(PercentTabLayout percentTabLayout, int x, int y, int oldx, int oldy);
    }
}
