/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingParent;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import org.hapjs.widgets.view.swiper.ViewPager;
import org.hapjs.widgets.view.utils.ScrollableViewUtil;

public class RefreshContent extends RefreshMovement {

    private static final int SCROLL_UP = -1;
    private static final int SCROLL_DOWN = 1;

    private View mScrollableView;
    private Point mDownPoint;

    public RefreshContent(View contentView) {
        super(contentView);
    }

    public void actionDown(MotionEvent event) {
        mDownPoint = new Point((int) event.getX(), (int) event.getY());
        View contentView = getView();
        // event是相对于refreshlayout，这里需要减去contentView与refreshlayout间的偏移
        mDownPoint.offset(-contentView.getLeft(), -contentView.getTop());
        mScrollableView = findScrollableView(mDownPoint);
    }

    public void actionUp(MotionEvent event) {
        mDownPoint = null;
    }

    @Override
    protected void onMove(float moveY, float percent, boolean isDrag, boolean refresh) {
        View contentView = getView();
        contentView.setTranslationY(moveY);
    }

    public boolean canPullDown() {
        View view = getView();
        if (view.canScrollVertically(SCROLL_DOWN)) {
            // 如果content可以向下滑动，则不能下拉刷新
            return false;
        }
        if (mScrollableView == null) {
            return true;
        }

        if (mScrollableView instanceof AbsListView) {
            return ScrollableViewUtil.isAbsListViewToTop((AbsListView) mScrollableView);
        }

        if (mScrollableView instanceof RecyclerView) {
            return ScrollableViewUtil.isRecyclerViewToTop((RecyclerView) mScrollableView);
        }

        if (mScrollableView instanceof NestedScrollView) {
            return ScrollableViewUtil.isNestedScrollViewToTop((NestedScrollView) mScrollableView);
        }

        if (mScrollableView instanceof ScrollView) {
            return ScrollableViewUtil.isScrollViewToTop((ScrollView) mScrollableView);
        }

        if (mScrollableView instanceof ViewPager) {
            return ScrollableViewUtil.isViewPagerToTop((ViewPager) mScrollableView);
        }

        if (mScrollableView instanceof WebView) {
            return ScrollableViewUtil.isWebViewToTop((WebView) mScrollableView);
        }

        return !mScrollableView.canScrollVertically(SCROLL_DOWN);
    }

    public boolean canPullUp() {
        View view = getView();
        if (view.canScrollVertically(SCROLL_UP)) {
            // 如果content可以向上滑动，则不能上拉刷新
            return false;
        }

        if (mScrollableView == null) {
            return true;
        }

        if (mScrollableView instanceof AbsListView) {
            return ScrollableViewUtil.isAbsListViewToBottom((AbsListView) mScrollableView);
        }

        if (mScrollableView instanceof RecyclerView) {
            return ScrollableViewUtil.isRecyclerViewToBottom((RecyclerView) mScrollableView);
        }

        if (mScrollableView instanceof NestedScrollView) {
            return ScrollableViewUtil
                    .isNestedScrollViewToBottom((NestedScrollView) mScrollableView);
        }

        if (mScrollableView instanceof ScrollView) {
            return ScrollableViewUtil.isScrollViewToBottom((ScrollView) mScrollableView);
        }

        if (mScrollableView instanceof ViewPager) {
            return ScrollableViewUtil.isViewPagerToBottom((ViewPager) mScrollableView);
        }

        if (mScrollableView instanceof WebView) {
            return ScrollableViewUtil.isWebViewToBottom((WebView) mScrollableView);
        }

        return !mScrollableView.canScrollVertically(SCROLL_UP);
    }

    private View findScrollableView(Point downPoint) {
        View view = getView();
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        return findScrollableViewRecursive((ViewGroup) view, downPoint);
    }

    private View findScrollableViewRecursive(ViewGroup viewGroup, Point point) {
        if (viewGroup == null) {
            return null;
        }

        int childCount = viewGroup.getChildCount();
        int[] offsets = new int[2];
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (isPointInView(viewGroup, child, point, offsets)) {
                if (!isScrollableView(child) && child instanceof ViewGroup) {
                    point.offset(offsets[0], offsets[1]);
                    child = findScrollableViewRecursive((ViewGroup) child, point);
                }
                return child;
            }
        }
        return null;
    }

    private boolean isScrollableView(View view) {
        return view instanceof AbsListView
                || view instanceof ScrollView
                || view instanceof WebView
                || view instanceof NestedScrollingChild
                || view instanceof NestedScrollingParent
                || view instanceof ViewPager;
    }

    /**
     * 判断触摸点是否在child上
     *
     * @param parent      parentView
     * @param child       childView
     * @param parentPoint 触摸点相对于parent的位置
     * @param offset      child相对于parent的偏移
     * @return
     */
    private boolean isPointInView(ViewGroup parent, View child, Point parentPoint, int[] offset) {
        if (child.getVisibility() != View.VISIBLE) {
            // child不可见，忽略
            return false;
        }

        offset[0] = parent.getScrollX() - child.getLeft();
        offset[1] = parent.getScrollY() - child.getTop();
        int x = offset[0] + parentPoint.x;
        int y = offset[1] + parentPoint.y;
        return x >= 0 && y >= 0 && x <= child.getWidth() && y <= child.getHeight();
    }
}
