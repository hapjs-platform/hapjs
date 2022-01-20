/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatCheckBox;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class FlexCheckBox extends AppCompatCheckBox implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private IGesture mGesture;

    public FlexCheckBox(Context context) {
        super(context);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
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
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
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
}
