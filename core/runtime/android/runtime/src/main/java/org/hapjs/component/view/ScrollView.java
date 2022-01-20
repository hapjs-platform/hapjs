/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;
import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.component.Component;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;

public class ScrollView extends NestedScrollView implements GestureHost {

    private static final String TAG = "ScrollView";
    SwipeDelegate mChildViewSwipeDelegate = null;
    private List<ScrollViewListener> mScrollViewListeners;
    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;
    private boolean mScrollToTop;
    private NestedScrollingView mNestedScrollingView;
    private OverScroller mScroller;
    private int mScrollRange;
    private IGesture mGesture;

    public ScrollView(Context context) {
        super(context);

        setFillViewport(true);
        mScrollViewListeners = new ArrayList<>();

        try {
            Field mScrollerField = NestedScrollView.class.getDeclaredField("mScroller");
            mScrollerField.setAccessible(true);
            mScroller = (OverScroller) mScrollerField.get(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);

        int count = mScrollViewListeners.size();
        for (int i = 0; i < count; ++i) {
            mScrollViewListeners.get(i).onScrollChanged(this, x, y, oldx, oldy);
        }
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes, int type) {
        if (target instanceof NestedScrollingView) {
            mNestedScrollingView = (NestedScrollingView) target;
            if (mNestedScrollingView.getNestedScrollingListener() == null) {
                mNestedScrollingView.setNestedScrollingListener(
                        new NestedScrollingListener() {
                            @Override
                            public void onFling(int velocityX, int velocityY) {
                                fling(velocityY);
                            }

                            @Override
                            public void onOverScrolled(int dx, int dy) {
                                smoothScrollBy(dx, dy);
                            }
                        });
            }
        } else {
            mNestedScrollingView = null;
        }
        mScrollRange =
                Math.max(0,
                        child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
        return type == ViewCompat.TYPE_TOUCH;
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        super.onStopNestedScroll(target, type);
        // Fix bug: nestedChild still scroll or fling while ScrollView OverScrolled
        if (target == mNestedScrollingView) {
            mNestedScrollingView = null;
        }
    }

    @Override
    public void onNestedPreScroll(
            @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (mNestedScrollingView == target && mNestedScrollingView.shouldScrollFirst(dy, 0)) {
            return;
        }

        final int scrollY = getScrollY();
        if (dy > 0 && scrollY < mScrollRange) {
            int deltaY = Math.min(dy, mScrollRange - scrollY);
            scrollBy(0, deltaY);
            consumed[1] = deltaY;
        } else if (dy < 0 && scrollY != mScrollRange) {
            int deltaY = Math.max(dy, -scrollY);
            scrollBy(0, deltaY);
            consumed[1] = deltaY;
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (mNestedScrollingView == target
                && mNestedScrollingView.shouldScrollFirst(0, (int) velocityY)) {
            return false;
        }

        if (velocityY > 0 && getScrollY() < mScrollRange) {
            fling((int) velocityY);
            return true;
        } else if (velocityY < 0 && getScrollY() != mScrollRange) {
            fling((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        if (mNestedScrollingView != null
                && clampedY
                && mScrollRange > 0
                && scrollY == mScrollRange
                && mScroller != null) {
            float unconsumedVelocityY = mScroller.getCurrVelocity();
            mNestedScrollingView.nestedFling(0, (int) unconsumedVelocityY);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean canScroll = canScrollVertically(-1) || canScrollVertically(1);
        if (!canScroll) {
            return false;
        }
        final int action = ev.getAction();
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                final int y = (int) ev.getY();
                final int yDiff = y - mLastMotionY;
                if (yDiff >= 0 && getScrollY() == 0) {
                    mScrollToTop = true;
                } else {
                    mScrollToTop = false;
                }
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = (int) ev.getY();
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                View childView = null;
                if (getChildCount() == 1) {
                    childView = getChildAt(0);
                }
                if (childView instanceof ComponentHost) {
                    Component component = ((ComponentHost) childView).getComponent();
                    mChildViewSwipeDelegate = component.getSwipeDelegate();
                }
                break;
            }
            default:
                break;
        }
        if (mChildViewSwipeDelegate != null) {
            mChildViewSwipeDelegate.onTouchEvent(ev);
        }
        boolean result = super.onTouchEvent(ev);
        if (mGesture != null) {
            result |= mGesture.onTouch(ev);
        }
        return result;
    }

    @Override
    public int getNestedScrollAxes() {
        if (mScrollToTop) {
            // Don't intercept touch event when reached top and continue scroll up.
            return ViewCompat.SCROLL_AXIS_VERTICAL;
        }
        return super.getNestedScrollAxes();
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        return false;
    }

    /**
     * Add listener for scrollView.
     */
    public void addScrollViewListener(ScrollViewListener scrollViewListener) {
        if (!mScrollViewListeners.contains(scrollViewListener)) {
            mScrollViewListeners.add(scrollViewListener);
        }
    }

    public void removeScrollViewListener(ScrollViewListener scrollViewListener) {
        mScrollViewListeners.remove(scrollViewListener);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller != null && !mScroller.isFinished() && !awakenScrollBars()) {
            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this);
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

    public interface ScrollViewListener {
        void onScrollChanged(ScrollView scrollView, int x, int y, int oldx, int oldy);
    }
}
