/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaNode;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.PercentLayoutHelper;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

public class PercentFrameLayout extends FrameLayout implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private IGesture mGesture;

    public PercentFrameLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PercentLayoutHelper
                .adjustChildren(widthMeasureSpec, heightMeasureSpec, (Container) mComponent);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        // remeasure yoga.
        if (getParent() instanceof YogaLayout && mComponent != null && getVisibility() != GONE) {
            YogaNode yogaNode = ((YogaLayout) getParent()).getYogaNodeForView(this);
            if (yogaNode != null) {
                if (!mComponent.getStyleDomData().containsKey(Attributes.Style.WIDTH)) {
                    yogaNode.setWidth(YogaConstants.UNDEFINED);
                }
                if (!mComponent.getStyleDomData().containsKey(Attributes.Style.HEIGHT)) {
                    yogaNode.setHeight(YogaConstants.UNDEFINED);
                }
            }
        }
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

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }
}
