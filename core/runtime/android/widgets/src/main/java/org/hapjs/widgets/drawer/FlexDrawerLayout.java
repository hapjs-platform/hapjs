/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.drawer;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;

public class FlexDrawerLayout extends DrawerLayout implements ComponentHost, GestureHost {
    private Component mComponent;
    private IGesture mGesture;
    private boolean mHasMovedInMask = false;
    private int mGravity = Gravity.START;
    private int mLastMotionX;
    private int mLastMotionY;
    private int mTouchSlop;

    public FlexDrawerLayout(@NonNull Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec =
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),
                        MeasureSpec.EXACTLY);
        heightMeasureSpec =
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),
                        MeasureSpec.EXACTLY);
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
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mComponent != null && !((Drawer) mComponent).isEnableSwipe()) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (isInMask(ev)) {
                        mLastMotionX = (int) ev.getX();
                        mLastMotionY = (int) ev.getY();
                        if (mGesture != null) {
                            return mGesture.onTouch(ev);
                        }
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();
                    if (Math.abs(x - mLastMotionX) > mTouchSlop
                            || Math.abs(y - mLastMotionY) > mTouchSlop) {
                        mHasMovedInMask = true;
                    }
                    mLastMotionX = x;
                    mLastMotionY = y;
                    break;
                case MotionEvent.ACTION_UP:
                    if (isInMask(ev) && !mHasMovedInMask) {
                        ((Drawer) mComponent).closeDrawer(mGravity);
                        if (mGesture != null) {
                            return mGesture.onTouch(ev);
                        }
                        return true;
                    }
                    mHasMovedInMask = false;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mHasMovedInMask = false;
                    break;
                default:
                    break;
            }
        }

        boolean result = super.dispatchTouchEvent(ev);
        if (mGesture != null) {
            result |= mGesture.onTouch(ev);
        }
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mComponent != null && !((Drawer) mComponent).isEnableSwipe()) {
            if (isInMask(ev)) {
                return super.onInterceptTouchEvent(ev);
            } else {
                // 1040以上本版本，在执行滑出抽屉的手势时，Drawer对move事件会拦截，在isEnableSwipe为false时，抽屉仍然被打开。
                // 所以此处return false，让抽屉不拦截move事件，将move及后续的事件分发给子view处理。
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 设置EnableSwipe为false, 子view如果不消费事件，此处也不消费事件，让禁止滑动操作生效。
        if (mComponent != null && !((Drawer) mComponent).isEnableSwipe()) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private boolean isInMask(MotionEvent ev) {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = this.getChildAt(i);
            if (childView instanceof DrawerPercentFlexLayout) {
                int childWidth = childView.getWidth();
                int gravity = ((LayoutParams) ((childView).getLayoutParams())).gravity;
                if (mComponent != null) {
                    if (((Drawer) mComponent).isLeftOpen() && gravity == Gravity.START) {
                        if (ev.getX() - childWidth > 0) {
                            mGravity = gravity;
                            return true;
                        }
                    }

                    if (((Drawer) mComponent).isRightOpen() && gravity == Gravity.END) {
                        if (ev.getX() < this.getWidth() - childWidth) {
                            mGravity = gravity;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
