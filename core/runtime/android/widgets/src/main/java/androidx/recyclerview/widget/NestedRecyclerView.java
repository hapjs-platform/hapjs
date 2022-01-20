/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.recyclerview.widget;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

public class NestedRecyclerView extends RecyclerView implements NestedScrollingParent3 {
    public static final int ORIENTATION_NONE = -1;
    public static final int ORIENTATION_HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int ORIENTATION_VERTICAL = RecyclerView.VERTICAL;
    private NestedScrollingParentHelper mScrollingParentHelper;
    private int[] mConsumedPair = new int[2];

    private int mLastMotionX;
    private int mLastMotionY;
    private int mTouchSlop;

    public NestedRecyclerView(@NonNull Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = (int) ev.getY();
                mLastMotionX = (int) ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                final int xDiff = Math.abs(x - mLastMotionX);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (canScrollHorizontally(1) || canScrollHorizontally(-1)) {
                    if (xDiff >= yDiff && xDiff > mTouchSlop) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                if (canScrollVertically(1) || canScrollVertically(-1)) {
                    if (yDiff >= xDiff && yDiff > mTouchSlop) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                mLastMotionX = x;
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes, int type) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return false;
        }

        int orientation = getOrientation();
        return (orientation == ORIENTATION_HORIZONTAL && axes == ViewCompat.SCROLL_AXIS_HORIZONTAL)
                || (orientation == ORIENTATION_VERTICAL && axes == ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    private int getOrientation() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return ORIENTATION_NONE;
        }

        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).getOrientation();

        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            return ((StaggeredGridLayoutManager) layoutManager).getOrientation();
        }
        return ORIENTATION_NONE;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        getScrollingParentHelper().onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onNestedScrollAccepted(
            @NonNull View child, @NonNull View target, int axes, int type) {
        getScrollingParentHelper().onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onStopNestedScroll(View child) {
        getScrollingParentHelper().onStopNestedScroll(child);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        getScrollingParentHelper().onStopNestedScroll(target, type);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        super.dispatchNestedPreScroll(dx, dy, consumed, null, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(
            @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        super.dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    @Override
    public void onNestedScroll(
            View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public void onNestedScroll(
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type) {
        nestedScrollSelf(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, null);
    }

    @Override
    public void onNestedScroll(
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type,
            @NonNull int[] consumed) {
        nestedScrollSelf(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
    }

    private void callSuperDispatchNestedScroll(
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type,
            int[] consumed) {
        if (consumed == null) {
            super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null,
                    type);
        } else {
            super.dispatchNestedScroll(
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type, consumed);
        }
    }

    private void nestedScrollSelf(
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type,
            int[] consumed) {
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // 只处理触摸时的嵌套滚动
            callSuperDispatchNestedScroll(
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
            return;
        }

        int orientation = getOrientation();
        if (orientation == ORIENTATION_HORIZONTAL && dxUnconsumed != 0) {
            int used = nestedScrollHorizontal(dxUnconsumed);
            int oldConsumedX = consumed != null ? consumed[0] : 0;
            dxConsumed += used;
            dxUnconsumed -= used;
            callSuperDispatchNestedScroll(
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
            if (consumed != null) {
                consumed[0] += oldConsumedX;
            }

        } else if (orientation == ORIENTATION_VERTICAL && dyUnconsumed != 0) {
            int used = nestedScrollVertical(dyUnconsumed);
            int oldConsumedY = consumed != null ? consumed[1] : 0;
            dyConsumed += used;
            dyUnconsumed -= used;
            callSuperDispatchNestedScroll(
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
            if (consumed != null) {
                consumed[1] += oldConsumedY;
            }
        } else {
            callSuperDispatchNestedScroll(
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        }
    }

    private int nestedScrollHorizontal(int dx) {
        mConsumedPair[0] = 0;
        mConsumedPair[1] = 0;
        scrollStep(dx, 0, mConsumedPair);
        return mConsumedPair[0];
    }

    private int nestedScrollVertical(int dy) {
        mConsumedPair[0] = 0;
        mConsumedPair[1] = 0;
        scrollStep(0, dy, mConsumedPair);
        return mConsumedPair[1];
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return super.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (consumed || !fling((int) velocityX, (int) velocityY)) {
            return super.dispatchNestedFling(velocityX, velocityY, consumed);
        }

        return true;
    }

    private NestedScrollingParentHelper getScrollingParentHelper() {
        if (mScrollingParentHelper == null) {
            mScrollingParentHelper = new NestedScrollingParentHelper(this);
        }
        return mScrollingParentHelper;
    }
}
