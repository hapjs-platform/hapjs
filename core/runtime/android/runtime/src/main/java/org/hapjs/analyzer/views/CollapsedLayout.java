/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollapsedLayout extends CoordinatorLayout {

    private int[] mLocationTemp = new int[2];
    private int[] mParentLocationTemp = new int[2];
    private List<View> mDragShieldViews = new CopyOnWriteArrayList<>();

    public CollapsedLayout(@NonNull Context context) {
        super(context);
    }

    public CollapsedLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CollapsedLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addDragShieldView(View view) {
        if (view != null) {
            mDragShieldViews.add(view);
        }
    }

    public void addDragShieldViews(List<View> views) {
        if (views == null || views.isEmpty()) {
            return;
        }
        for (View view : views) {
            if (view != null) {
                mDragShieldViews.add(view);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mDragShieldViews == null || mDragShieldViews.isEmpty()) {
            return super.onInterceptTouchEvent(ev);
        }
        // When the touch event is within the range of any view in mDragViews, it is not intercepted
        for (View view : mDragShieldViews) {
            if (view.getVisibility() == View.VISIBLE && isViewUnder(view, (int) ev.getX(), (int) ev.getY())) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mDragShieldViews == null || mDragShieldViews.isEmpty()) {
            return super.onTouchEvent(ev);
        }
        int action = ev.getAction();
        if (MotionEvent.ACTION_DOWN == action) {
            for (View view : mDragShieldViews) {
                if (view.getVisibility() == View.VISIBLE && isViewUnder(view, (int) ev.getX(), (int) ev.getY())) {
                    return false;
                }
            }
            return super.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Determine whether the touch point is within the view range
     * @param view
     * @param x
     * @param y
     * @return
     */
    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        view.getLocationOnScreen(mLocationTemp);
        getLocationOnScreen(mParentLocationTemp);
        int screenX = mParentLocationTemp[0] + x;
        int screenY = mParentLocationTemp[1] + y;
        return screenX >= mLocationTemp[0] && screenX < mLocationTemp[0] + view.getWidth() &&
                screenY >= mLocationTemp[1] && screenY < mLocationTemp[1] + view.getHeight();
    }


    @Override
    public boolean onStartNestedScroll(View child, View target, int axes, int type) {
        return false;
    }
}
