/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.recyclerview.widget;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.HapRefreshLayout;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.NestedScrollingListener;
import org.hapjs.component.view.NestedScrollingView;
import org.hapjs.component.view.ScrollView;
import org.hapjs.component.view.SwipeDelegate;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.hapjs.widgets.view.utils.ScrollableViewUtil;
import org.hapjs.widgets.view.list.RecyclerViewAdapter;

public class FlexRecyclerView extends RecyclerView
        implements ComponentHost, NestedScrollingView, GestureHost, RecyclerViewAdapter {

    private final Map<Integer, Integer> mChildrenHeightMap = new HashMap<>();
    SwipeDelegate mCurrentSwipeDelegate = null;
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private ViewGroup mMoveableView;
    private HapRefreshLayout mRefreshLayout;
    private boolean mScrollPage;
    private boolean mDirty;
    private int mLastMotionX;
    private int mLastMotionY;
    private int mTouchSlop;
    private NestedScrollingListener mNestedScrollingListener;
    private IGesture mGesture;
    private ViewGroup mNestedChildView;

    public FlexRecyclerView(Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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
    public View getMoveableView() {
        return mMoveableView;
    }

    public State getState() {
        return mState;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mMoveableView = null;
        mRefreshLayout = null;
        ViewParent parentView = getParent();
        while (parentView instanceof ViewGroup) {
            if (mMoveableView == null && parentView instanceof ScrollView) {
                mMoveableView = (ViewGroup) parentView;
            }
            if (mRefreshLayout == null && parentView instanceof HapRefreshLayout) {
                mRefreshLayout = (HapRefreshLayout) parentView;
            }
            if (mMoveableView != null && mRefreshLayout != null) {
                break;
            }
            parentView = parentView.getParent();
        }

        setScrollPage(mScrollPage);
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
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        if (result) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    if (mComponent instanceof ComponentHost) {
                        Component component = ((ComponentHost) mComponent).getComponent();
                        mCurrentSwipeDelegate = component.getSwipeDelegate();
                    }
                    break;
                }
                default:
                    break;
            }
            if (mCurrentSwipeDelegate != null) {
                mCurrentSwipeDelegate.onTouchEvent(ev);
            }
        }

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
    protected void onMeasure(int widthSpec, int heightSpec) {
        mChildrenHeightMap.clear();
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mDirty) {
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            resumeRequestLayout();
                            requestLayout();
                        }
                    });
            mDirty = false;
        }
    }

    @Override
    void absorbGlows(int velocityX, int velocityY) {
        if (mNestedScrollingListener != null) {
            if (velocityY < 0) {
                mNestedScrollingListener.onFling(0, velocityY);
                return;
            } else if (velocityY == 0) {
                if (getLayoutManager() instanceof FlexLayoutManager) {
                    int overScrolledY = ((FlexLayoutManager) getLayoutManager()).getOverScrolledY();
                    if (overScrolledY < 0) {
                        mNestedScrollingListener.onOverScrolled(0, overScrolledY);
                        return;
                    }
                }
            }
        }
        super.absorbGlows(velocityX, velocityX);
    }

    @Override
    public RecyclerView getActualRecyclerView() {
        return this;
    }

    @Override
    public void setScrollPage(boolean scrollPage) {
        if (mMoveableView == null) {
            return;
        }

        mScrollPage = scrollPage;

        if (mRefreshLayout != null) {
            mRefreshLayout.setOnChildScrollUpCallback(
                    new HapRefreshLayout.OnChildScrollUpCallback() {
                        @Override
                        public boolean canChildScrollUp(HapRefreshLayout parent,
                                                        @Nullable View child) {
                            return mMoveableView.getScrollY() != 0
                                    || mMoveableView.canScrollVertically(-1);
                        }
                    });

            ViewGroup.LayoutParams lp = mRefreshLayout.getLayoutParams();
            if (lp == null) {
                lp =
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
            }
            final ViewGroup.LayoutParams finalLp = lp;
            OnLayoutChangeListener listLayoutChangeListener =
                    new OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(
                                View v,
                                int left,
                                int top,
                                int right,
                                int bottom,
                                int oldLeft,
                                int oldTop,
                                int oldRight,
                                int oldBottom) {
                            finalLp.height = bottom - top;
                            mRefreshLayout.setLayoutParams(finalLp);
                        }
                    };
            if (scrollPage) {
                addOnLayoutChangeListener(listLayoutChangeListener);
            } else {
                removeOnLayoutChangeListener(listLayoutChangeListener);
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mRefreshLayout.setLayoutParams(lp);
            }
        }
    }

    @Override
    public void resumeRequestLayout() {
        stopInterceptRequestLayout(false);
    }

    @Override
    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    @Override
    public boolean shouldScrollFirst(int dy, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).shouldScrollFirst(dy, velocityY)) {
                return true;
            }
        }
        if (mScrollPage) {
            return false;
        }

        boolean toBottom = dy > 0 || velocityY > 0;
        boolean toTop = dy < 0 || velocityY < 0;
        if (getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
            if (toTop && layoutManager.findFirstVisibleItemPosition() == 0) {
                View firstView = getChildAt(0);
                if (firstView != null && firstView.getTop() == getPaddingTop()) {
                    return false;
                }
            } else if (toBottom
                    && layoutManager.findLastVisibleItemPosition()
                    == layoutManager.getItemCount() - 1) {
                View lastView = getChildAt(getChildCount() - 1);
                if (lastView != null && lastView.getBottom() == getHeight() - getPaddingBottom()) {
                    return false;
                }
            }
        } else if (getLayoutManager() instanceof HapStaggeredGridLayoutManager) {
            if (toTop) {
                int[] aa =
                        ((HapStaggeredGridLayoutManager) getLayoutManager())
                                .findFirstVisibleItemPositions(null);
                View firstView = getChildAt(0);
                if (firstView != null && firstView.getTop() == getPaddingTop() && aa[0] == 0) {
                    return false;
                }
            } else if (toBottom) {
                HapStaggeredGridLayoutManager layoutManager =
                        (HapStaggeredGridLayoutManager) getLayoutManager();
                int[] firstVisibleItems =
                        ((HapStaggeredGridLayoutManager) getLayoutManager())
                                .findFirstVisibleItemPositions(null);
                // 真实Position就是position - firstVisibleItems[0]
                int[] lastPositions = new int[layoutManager.getSpanCount()];
                layoutManager.findLastVisibleItemPositions(lastPositions);
                if (isReachBottom(lastPositions, firstVisibleItems[0],
                        layoutManager.getItemCount())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isReachBottom(int[] lastPositions, int firstVisibleItem, int itemCount) {
        if (lastPositions == null || lastPositions.length == 0) {
            return false;
        }
        int lastPosition = lastPositions[0];
        for (int value : lastPositions) {
            if (value > lastPosition) {
                lastPosition = value;
            }
        }
        int maxBottom = 0;
        int length = lastPositions.length;
        for (int i = 0; i < length; i++) {
            View lastChild = getChildAt(i - firstVisibleItem);
            if (lastChild == null) {
                continue;
            }
            if (lastChild.getBottom() > maxBottom) {
                maxBottom = lastChild.getBottom();
            }
        }
        return (lastPosition == itemCount - 1) && (maxBottom == getHeight() - getPaddingBottom());
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        super.onNestedScrollAccepted(child, target, axes);
        if (target instanceof ViewGroup) {
            mNestedChildView = (ViewGroup) target;
        }
    }

    @Override
    public boolean nestedFling(int velocityX, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).nestedFling(velocityX, velocityY)) {
                return true;
            }
        }
        boolean toBottom = velocityY > 0;
        boolean toTop = velocityY < 0;
        if (toBottom && ScrollableViewUtil.isRecyclerViewToBottom(this)) {
            return false;
        }

        if (toTop && ScrollableViewUtil.isRecyclerViewToTop(this)) {
            return false;
        }
        fling(0, velocityY);
        return true;
    }

    @Override
    public ViewGroup getChildNestedScrollingView() {
        return mNestedChildView;
    }

    @Override
    public NestedScrollingListener getNestedScrollingListener() {
        return mNestedScrollingListener;
    }

    @Override
    public void setNestedScrollingListener(NestedScrollingListener listener) {
        mNestedScrollingListener = listener;
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
