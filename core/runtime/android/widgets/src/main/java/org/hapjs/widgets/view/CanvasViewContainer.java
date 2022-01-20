/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class CanvasViewContainer extends FrameLayout implements ComponentHost, GestureHost {
    private KeyEventDelegate mKeyEventDelegate;

    private Component mComponent;

    private View mCanvasView;

    private IGesture mGesture;

    public CanvasViewContainer(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    public <T extends View> T getCanvasView() {
        return (T) mCanvasView;
    }

    public void setCanvasView(View view) {
        mCanvasView = view;
        removeAllViews();
        if (mCanvasView != null) {
            addView(
                    view,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
        }
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        if (mGesture != null) {
            result |= mGesture.onTouch(ev);
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
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child.getWidth() <= 0 || child.getHeight() <= 0) {
            child.requestLayout();
        }
        return super.drawChild(canvas, child, drawingTime);
    }
}
