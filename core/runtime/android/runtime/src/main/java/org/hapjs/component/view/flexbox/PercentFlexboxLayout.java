/*
 * Copyright (c) 2021-present,  the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.flexbox;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.Floating;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.ScrollView;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.system.utils.TalkBackUtils;

public class PercentFlexboxLayout extends YogaLayout implements ComponentHost, GestureHost {

    private Component mComponent;
    private IGesture mGesture;
    private KeyEventDelegate mKeyEventDelegate;

    private List<Integer> mPositionArray;
    private boolean mDisallowIntercept = false;
    private boolean mIsEnableTalkBack;
    private MotionEvent mLastMotionEvent = null;

    public PercentFlexboxLayout(Context context) {
        super(context);
        mIsEnableTalkBack = TalkBackUtils.isEnableTalkBack(context, false);
        getYogaNode().setFlexDirection(YogaFlexDirection.ROW);
        getYogaNode().setFlexShrink(1f);
    }

    @Override
    public boolean performClick() {
        boolean isConsume = super.performClick();
        if (mIsEnableTalkBack) {
            if (null != mComponent) {
                mComponent.performComponentClick(mLastMotionEvent);
            }
        }
        return isConsume;
    }


    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        mLastMotionEvent = event;
        return super.dispatchHoverEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLastMotionEvent = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(getParent() instanceof YogaLayout)) {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            YogaNode node = getYogaNode();

            if ((widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST)
                    && !mComponent.isWidthDefined()) {
                node.setWidth(YogaConstants.UNDEFINED);
            }

            if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
                if (!mComponent.isHeightDefined()) {
                    node.setHeight(YogaConstants.UNDEFINED);
                } else if (getParent() instanceof ScrollView) {
                    // 针对ScrollView下使用div，设置100%无意义
                    // div初次计算的高度如果小于ScrollView的高度，div的最终高度将被限制在scrollView的高度内
                    // 因此在ScrollView下的div，设置的高度都应该为minHeight
                    float percentHeight = mComponent.getPercentHeight();
                    if (!FloatUtil.isUndefined(percentHeight) && percentHeight >= 0) {
                        node.setMinHeightPercent(percentHeight);
                    }
                }
            }

            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            if (getId() == getComponent().getRootComponent().getViewId(DocComponent.ROOT_VIEW_ID)) {
                // ScrollView measure yoga root with [size, AT_MOST],so yoga root may won't
                // expand horizontally.
                if (widthSize > 0 && widthMode == MeasureSpec.AT_MOST) {
                    node.setWidth(widthSize);
                }
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getId() == getComponent().getRootComponent().getViewId(DocComponent.ROOT_VIEW_ID)) {
            createLayout(
                    MeasureSpec.makeMeasureSpec(r - l, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(b - t, MeasureSpec.UNSPECIFIED));
            applyLayoutRecursive(getYogaNode(), 0, 0);
        } else {
            super.onLayout(changed, l, t, r, b);
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
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new YogaLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void requestLayout() {
        // remeasure yoga.
        YogaNode yogaNode = getYogaNode();
        if (mComponent != null && yogaNode != null && getVisibility() != GONE) {
            if (!mComponent.isWidthDefined()) {
                yogaNode.setWidth(YogaConstants.UNDEFINED);
            }
            if (!mComponent.isHeightDefined()) {
                yogaNode.setHeight(YogaConstants.UNDEFINED);
            }
        }

        super.requestLayout();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        YogaNode yogaNode = getYogaNode();
        YogaNode childNode = getYogaNodeForView(child);

        // fix yoga bug.
        if (index > -1 && index < yogaNode.getChildCount() - 1) {
            yogaNode.removeChildAt(yogaNode.indexOf(childNode));
            yogaNode.addChildAt(childNode, index);
        }

        if (!(child instanceof PercentFlexboxLayout)) {
            // default values.
            childNode.setFlexDirection(YogaFlexDirection.ROW);
            childNode.setFlexShrink(1f);
        }
    }

    @Override
    public void addView(View child, YogaNode node, int index) {
        super.addView(child, node, index);
        if (!(child instanceof PercentFlexboxLayout)) {
            // default values.
            node.setFlexDirection(YogaFlexDirection.ROW);
            node.setFlexShrink(1f);
        }
    }

    public void setDisallowIntercept(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mDisallowIntercept && ev.getAction() == MotionEvent.ACTION_DOWN) {
            ViewParent viewParent = getParent();
            if (viewParent != null) {
                viewParent.requestDisallowInterceptTouchEvent(mDisallowIntercept);
            }
        }
        return super.onInterceptTouchEvent(ev);
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

    @Override
    public void setChildrenDrawingOrderEnabled(boolean enabled) {
        super.setChildrenDrawingOrderEnabled(enabled);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final Container container = ((Container) mComponent);
        final int count = container.getChildCount();

        if (mPositionArray == null) {
            mPositionArray = new ArrayList<>();
        } else {
            mPositionArray.clear();
        }
        int excludeCount = 0;

        for (int index = 0; index < count; index++) {
            Component component = container.getChildAt(index);
            if (component == null) {
                continue;
            }
            if (component.isFixed() || component instanceof Floating) {
                excludeCount++;
                continue;
            }

            if (component.isRelative() || component.isAbsolute()) {
                mPositionArray.add(index - excludeCount);
            }
        }

        return mPositionArray.get(i);
    }
}
