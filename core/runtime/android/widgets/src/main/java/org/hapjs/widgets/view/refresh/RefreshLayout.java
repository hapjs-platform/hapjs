/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hapjs.component.view.NestedScrollingListener;
import org.hapjs.component.view.NestedScrollingView;
import org.hapjs.widgets.BuildConfig;
import org.hapjs.widgets.view.utils.ScrollableViewUtil;

public class RefreshLayout extends ViewGroup
        implements NestedScrollingParent2, NestedScrollingChild2, NestedScrollingView {

    public static final int DEFAULT_ANIMATION_DURATION = 300;
    /**
     * 越界回弹默认手指拖动的速度
     */
    public static final float DEFAULT_REBOUND_DRAG_RATE = 0.5f;
    /**
     * 越界回弹默认的高度系数。
     */
    public static final float DEFAULT_REBOUND_DISPLAY_RATIO = 0.2f;
    private static final String TAG = "RefreshLayout";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final int INVALID_POINTER = -1;
    private final int[] mNestedOffsets = new int[2];
    private final int[] mScrollOffset = new int[2];
    private final int[] mConsumedPair = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mGestureEnable = true;
    private boolean mPullDownEnable = true;
    private boolean mPullUpEnable = false;
    private boolean mReboundEnable = false;
    private boolean mReboundTopEnable = false;
    private boolean mReboundBottomEnable = false;
    /**
     * 越界回弹手势拖动速度
     */
    private float mReboundDragRate = DEFAULT_REBOUND_DRAG_RATE;
    /**
     * 越界回弹拖动高度系数。拖动高度=ratio * refreshlayout.height.
     */
    private float mReboundDisplayRatio = DEFAULT_REBOUND_DISPLAY_RATIO;
    /**
     * 越界回弹拖动的高度，如果小于等于0，mReboundDisplayRatio将生效
     */
    private int mReboundDisplayHeight = 0;
    private int mAnimationDuration = DEFAULT_ANIMATION_DURATION;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private Scroller mScroller;
    private Header mHeader;
    private Footer mFooter;
    private RefreshContent mContent;
    private RefreshState mState = new RefreshState();
    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    private NestedScrollingParentHelper mNestedScrollingParentHelper;
    private NestedScrollingChildHelper mNestedScrollingChildHelper;
    private VelocityTracker mVelocityTracker;
    /**
     * header或者footer的偏移，正数表示header，负数表示footer，spinner源自SwipeRefreshLayout
     */
    private int mSpinner;

    /**
     * 手势移动距离
     */
    private int mMoveDistance;

    private ValueAnimator mScrollAnimation;

    private List<OnPullDownRefreshListener> mPullDownRefreshListeners =
            new CopyOnWriteArrayList<>();
    private List<OnPullUpListener> mPullUpListeners = new CopyOnWriteArrayList<>();
    private Interpolator mInterpolator = new ViscousInterpolator();
    private ViewGroup mNestedChildView;
    private NestedScrollingListener mNestedScrollingListener;

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mVelocityTracker = VelocityTracker.obtain();

        mScroller = new Scroller(context);
    }

    /**
     * 删除header
     */
    public void removeHeader() {
        if (mHeader != null) {
            removeMovement(mHeader);
            mHeader.setStyleChangedListener(null);
            mHeader = null;
        }
    }

    public Header getHeader() {
        return mHeader;
    }

    /**
     * 设置Header头部
     *
     * @param header
     */
    public void setHeader(Header header) {
        // 移除旧header
        removeHeader();
        mHeader = header;
        addMovement(mHeader);
        mHeader.setStyleChangedListener(
                (extension, oldStyle, newStyle) -> {
                    Header h = mHeader;
                    setHeader(h);
                });
    }

    public Footer getFooter() {
        return mFooter;
    }

    /**
     * 设置footer
     *
     * @param footer
     */
    public void setFooter(Footer footer) {
        // 移除旧footer
        removeFooter();
        mFooter = footer;
        addMovement(mFooter);
        mFooter.setStyleChangedListener(
                (extension, oldStyle, newStyle) -> {
                    Footer f = mFooter;
                    setFooter(f);
                });
    }

    /**
     * 删除footer
     */
    public void removeFooter() {
        if (mFooter != null) {
            removeMovement(mFooter);
            mFooter.setStyleChangedListener(null);
            mFooter = null;
        }
    }

    /**
     * 设置content区
     *
     * @param content
     */
    public void setContent(RefreshContent content) {
        // 移除旧的content
        removeContent();
        mContent = content;
        addMovement(mContent);
        if (mHeader != null && mHeader.getStyle() == RefreshExtension.STYLE_FIXED_FRONT) {
            bringChildToFront(mHeader.getView());
        }

        if (mFooter != null && mFooter.getStyle() == RefreshExtension.STYLE_FIXED_FRONT) {
            bringChildToFront(mFooter.getView());
        }
    }

    public void removeContent() {
        if (mContent != null) {
            removeMovement(mContent);
            mContent = null;
        }
    }

    /**
     * header、footer、content都被定义为了RefreshMovement
     *
     * @param movement
     */
    private void addMovement(RefreshMovement movement) {
        if (movement != null) {
            View view = movement.getView();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams == null) {
                layoutParams =
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            }
            if (!(layoutParams instanceof LayoutParams)) {
                layoutParams = new LayoutParams(layoutParams);
            }

            if (movement instanceof RefreshExtension
                    && ((RefreshExtension) movement).getStyle()
                    == RefreshExtension.STYLE_FIXED_FRONT) {
                addView(view, getChildCount(), layoutParams);
            } else {
                addView(view, 0, layoutParams);
            }
        }
    }

    private void removeMovement(RefreshMovement movement) {
        if (movement != null) {
            View view = movement.getView();
            removeView(view);
        }
    }

    /**
     * 设置是否支持手势刷新
     *
     * @param enable
     */
    public void enableGesture(boolean enable) {
        mGestureEnable = enable;
    }

    public boolean isGestureEnable() {
        return mGestureEnable;
    }

    /**
     * 设置是否开启越界回弹
     *
     * @param enable
     */
    public void enableRebound(boolean enable) {
        mReboundEnable = enable;
        mReboundTopEnable = enable;
        mReboundBottomEnable = enable;
    }

    public boolean isReboundEnable() {
        return mReboundEnable;
    }

    public boolean isReboundTopEnable() {
        return mReboundTopEnable;
    }

    public boolean isReboundBottomEnable() {
        return mReboundBottomEnable;
    }

    /**
     * 设置是否开启下拉刷新
     *
     * @param enable
     */
    public void enablePullDown(boolean enable) {
        mPullDownEnable = enable;
    }

    public boolean isPullDownEnable() {
        return mPullDownEnable;
    }

    /**
     * 设置是否开启上拉刷新
     *
     * @param enable
     */
    public void enablePullUp(boolean enable) {
        mPullUpEnable = enable;
    }

    public boolean isPullUpEnable() {
        return mPullUpEnable;
    }

    /**
     * 是否正在下拉刷新
     *
     * @return
     */
    public boolean isPullDownRefreshing() {
        return mState.isPullDownRefreshing();
    }

    /**
     * 是否正在上拉刷新
     *
     * @return
     */
    public boolean isPullUpRefreshing() {
        return mState.isPullUpRefreshing();
    }

    /**
     * 触发下拉刷新
     */
    public void setPullDownRefresh() {
        setPullDownRefresh(true);
    }

    /**
     * 触发下拉刷新
     *
     * @param animation 是否执行动画
     */
    public void setPullDownRefresh(boolean animation) {
        if (!mState.isPullDownRefreshing() && mHeader != null && isPullDownEnable()) {
            if (!animation) {
                setState(RefreshState.PULL_DOWN_REFRESHING);
                moveSpinner(mHeader.getMeasuredDisplaySize(), false);
                return;
            }
            int measuredDragMaxSize = mHeader.getMeasuredDragMaxSize();
            int measuredDisplaySize = mHeader.getMeasuredDisplaySize();

            // 先动画过渡到max，在退回到final位置
            animationSpinner(
                    measuredDragMaxSize,
                    mAnimationDuration,
                    mInterpolator,
                    0,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            animationSpinner(measuredDisplaySize);
                        }
                    });
        }
    }

    /**
     * 关闭下拉刷新
     */
    public void finishPullDownRefresh() {
        finishPullDownRefresh(true);
    }

    public void finishPullDownRefresh(boolean animation) {
        if (mState.isPullDownRefreshing()) {
            if (!animation) {
                setState(RefreshState.PULL_DOWN_IDLE);
                moveSpinner(0, false);
                return;
            }
            animationSpinner(
                    0,
                    mAnimationDuration,
                    mInterpolator,
                    0,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            setState(RefreshState.PULL_DOWN_IDLE);
                        }
                    });
        }
    }

    /**
     * 触发上拉刷新
     */
    public void setPullUpRefresh() {
        setPullUpRefresh(true);
    }

    /**
     * 触发上拉刷新
     *
     * @param animation 是否执行动画
     */
    public void setPullUpRefresh(boolean animation) {
        if (!mState.isPullUpRefreshing() && mFooter != null && isPullUpEnable()) {
            if (!animation) {
                setState(RefreshState.PULL_UP_REFRESHING);
                moveSpinner(mFooter.getMeasuredDisplaySize(), false);
                return;
            }
            int measuredDragMaxSize = mFooter.getMeasuredDragMaxSize();
            int measuredDisplaySize = mFooter.getMeasuredDisplaySize();

            // 先动画过渡到max，在退回到final位置
            animationSpinner(
                    -measuredDragMaxSize,
                    mAnimationDuration,
                    mInterpolator,
                    0,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            animationSpinner(-measuredDisplaySize);
                        }
                    });
        }
    }

    /**
     * 关闭上拉刷新
     */
    public void finishPullUpRefresh() {
        finishPullUpRefresh(true);
    }

    /**
     * 关闭上拉刷新
     *
     * @param animation 是否执行动画
     */
    public void finishPullUpRefresh(boolean animation) {
        if (mState.isPullUpRefreshing()) {
            if (!animation) {
                setState(RefreshState.PULL_UP_IDLE);
                moveSpinner(0, false);
                return;
            }
            animationSpinner(
                    0,
                    mAnimationDuration,
                    mInterpolator,
                    0,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            setState(RefreshState.PULL_UP_IDLE);
                        }
                    });
        }
    }

    /**
     * 关闭下拉刷新和上拉刷新
     */
    public void finishRefresh() {
        finishRefresh(true);
    }

    /**
     * 关闭下拉刷新和上拉刷新
     *
     * @param animation 是否执行动画
     */
    public void finishRefresh(boolean animation) {
        finishPullDownRefresh(animation);
        finishPullUpRefresh(animation);
    }

    public void addPullDownRefreshListener(OnPullDownRefreshListener pullDownRefreshListener) {
        mPullDownRefreshListeners.add(pullDownRefreshListener);
    }

    public void removePullDownRefreshListener(OnPullDownRefreshListener listener) {
        mPullDownRefreshListeners.remove(listener);
    }

    public void removeAllPullDownRefreshListener() {
        mPullDownRefreshListeners.clear();
    }

    public void addPullUpListener(OnPullUpListener pullUpListener) {
        mPullUpListeners.add(pullUpListener);
    }

    public void removePullUpRefreshListener(OnPullUpListener pullUpListener) {
        mPullUpListeners.remove(pullUpListener);
    }

    public void removeAllPullUpRefreshListener() {
        mPullUpListeners.clear();
    }

    /**
     * 设置回弹的动画时间
     *
     * @param duration 回弹的动画时间，单位:ms
     */
    public void setAnimationDuration(int duration) {
        if (duration <= 0) {
            return;
        }
        mAnimationDuration = duration;
    }

    public void setReboundDragRate(float reboundDragRate) {
        mReboundDragRate = reboundDragRate;
    }

    public void setReboundDisplayRatio(float reboundDisplayRatio) {
        mReboundDisplayRatio = reboundDisplayRatio;
    }

    public void setReboundDisplayHeight(int reboundDisplayHeight) {
        mReboundDisplayHeight = reboundDisplayHeight;
    }

    public int getMeasureReboundDisplaySize() {
        if (mReboundDisplayHeight > 0) {
            return mReboundDisplayHeight;
        }
        return (int) (getMeasuredHeight() * mReboundDisplayRatio);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(params instanceof LayoutParams)) {
            params = new LayoutParams(params);
        }
        super.addView(child, index, params);
    }

    private RefreshMovement findMovement(View childView) {
        if (mHeader != null && Objects.equals(childView, mHeader.getView())) {
            return mHeader;
        }

        if (mFooter != null && Objects.equals(childView, mFooter.getView())) {
            return mFooter;
        }

        if (mContent != null && Objects.equals(childView, mContent.getView())) {
            return mContent;
        }

        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int width = 0;
        int height = 0;

        int childCount = getChildCount();
        int childMaxWidth = 0;
        int contentHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            RefreshMovement movement = findMovement(child);
            if (movement != null) {
                movement.measure(
                        paddingLeft + paddingRight,
                        paddingTop + paddingBottom,
                        widthMeasureSpec,
                        heightMeasureSpec);

                childMaxWidth = Math.max(childMaxWidth, movement.getMeasureWidth());
                if (movement instanceof RefreshContent) {
                    contentHeight = movement.getMeasureHeight();
                }
            }
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSize, childMaxWidth);
        } else {
            width = childMaxWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(heightSize, contentHeight);
        } else {
            height = contentHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            LayoutParams params = (LayoutParams) child.getLayoutParams();
            RefreshMovement movement = findMovement(child);
            if (movement instanceof Header) {
                int l = paddingLeft + params.leftMargin;
                int t;
                if (((Header) movement).getStyle() == RefreshExtension.STYLE_FIXED_BEHIND) {
                    t = paddingTop + params.topMargin;
                } else {
                    t = paddingTop + params.topMargin - child.getMeasuredHeight();
                }
                int r = l + child.getMeasuredWidth();
                int b = t + child.getMeasuredHeight();
                child.layout(l, t, r, b);
            } else if (movement instanceof Footer) {
                int l = paddingLeft + params.leftMargin;
                int t;
                if (((Footer) movement).getStyle() == RefreshExtension.STYLE_FIXED_BEHIND) {
                    t = paddingTop + getMeasuredHeight() + params.topMargin
                            - child.getMeasuredHeight();
                } else {
                    t = paddingTop + getMeasuredHeight() + params.topMargin;
                }
                int r = l + child.getMeasuredWidth();
                int b = t + child.getMeasuredHeight();
                child.layout(l, t, r, b);
            } else if (movement instanceof RefreshContent) {
                // contentView
                int l = paddingLeft + params.leftMargin;
                int t = paddingTop + params.topMargin;
                int r = l + child.getMeasuredWidth();
                int b = t + child.getMeasuredHeight();
                child.layout(l, t, r, b);
            }
        }
    }

    /**
     * 手势下拉刷新是否启用
     *
     * @return
     */
    private boolean isGesturePullDownEnable() {
        return mHeader != null && isGestureEnable() && isPullDownEnable();
    }

    /**
     * 手势上拉刷新是否启用
     *
     * @return
     */
    private boolean isGesturePullUpEnable() {
        return mFooter != null && isGestureEnable() && isPullUpEnable();
    }

    private boolean isGestureReboundEnable() {
        return isGestureEnable() && isReboundEnable();
    }

    private boolean isTouchInExtensionView(MotionEvent event) {
        return (mHeader != null && mHeader.isTouchInExtensionView(event))
                || (mFooter != null && mFooter.isTouchInExtensionView(event));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            // 在按下的时候以当前的spinner位置为起点
            // 因为nested的原因，moveSpinner的操作可能是通过nested完成的，这里在dispatchTouchEvent
            // 前记下当前spinner的位置
            mMoveDistance = mSpinner;
        }
        boolean ret = super.dispatchTouchEvent(ev);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // up或者cancel的时候，回退spinner
            finishSpinner();
        }
        return ret;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isEnabled()
                || (!isGesturePullDownEnable() && !isGesturePullUpEnable()
                && !isGestureReboundEnable())) {
            return false;
        }

        if (isPullDownRefreshing() || isPullUpRefreshing()) {
            // 如果在刷新时，不拦截事件
            // header/footer需要移动，通过nested处理
            return false;
        }

        // 触摸在header/footer区域的事件不拦截
        if (isTouchInExtensionView(event)) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        switch (action) {
            // onTouchEvent里面不一定能够收到down事件，可能会被child处理
            // 因此这里的down需要和ontouchEvent里面的down的处理方式一样
            case MotionEvent.ACTION_DOWN:
                // 按下时，如果有动画，将动画取消
                cancelScrollAnimation();
                mIsBeingDragged = false;
                mActivePointerId = event.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY() + 0.5f);
                mNestedOffsets[0] = mNestedOffsets[1] = 0;
                mMoveDistance = mSpinner;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                if (mContent != null) {
                    mContent.actionDown(event);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mActivePointerId = event.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (event.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY(actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                int x = (int) (event.getX(pointerIndex) + 0.5f);
                int y = (int) (event.getY(pointerIndex) + 0.5f);
                if (!mIsBeingDragged) {
                    int dx = x - mInitialTouchX;
                    int dy = y - mInitialTouchY;
                    mLastTouchX = x;
                    mLastTouchY = y;
                    startDragging(dx, dy);
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_UP:
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelScroll();
                break;
            default:
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        debugInfo("onTouchEvent:", getEventType(event));
        // 禁用了手势
        if (!isEnabled()
                || (!isGesturePullDownEnable() && !isGesturePullUpEnable() && !isReboundEnable())) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final int action = event.getActionMasked();
        final int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        MotionEvent e = MotionEvent.obtain(event);
        e.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY() + 0.5f);
                // down时需要从上一次spinner的位置开始计算，而不是从0计算
                mMoveDistance = mSpinner;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                if (mContent != null) {
                    mContent.actionDown(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mActivePointerId = e.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final int x = (int) (event.getX(pointerIndex) + 0.5f);
                final int y = (int) (event.getY(pointerIndex) + 0.5f);
                int dx = x - mLastTouchX;
                int dy = y - mLastTouchY;
                if (!mIsBeingDragged) {
                    startDragging(dx, dy);

                    if (mIsBeingDragged) {
                        // 重新调整dy
                        if (dy > 0) {
                            dy = dy - mTouchSlop;
                        } else {
                            dy = dy + mTouchSlop;
                        }

                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }

                        if (dy > 0 && mSpinner >= 0 && !mState.isPullDownRefreshing()) {
                            // 下拉
                            setState(RefreshState.PULL_DOWN_TO_REFRESH);
                        } else if (dy < 0 && mSpinner <= 0 && !mState.isPullUpRefreshing()) {
                            // 上拉
                            setState(RefreshState.PULL_UP_TO_REFRESH);
                        }
                    }
                }

                if (mIsBeingDragged) {
                    mConsumedPair[0] = 0;
                    mConsumedPair[1] = 0;
                    int offsetY = -dy;
                    // 先将dx和dy传给可滚动的child消费
                    // dispatchNestedPreScroll/dispatchNestedScroll传的不是直接的dx和dy(手指滑动的距离)，
                    // 而是在dx和dy作用下后，最终的view相对于content的偏移，因此这里需要将dx、dy取反。
                    // header/footer没有移动，或者滑动方向与header/footer相反，child才能嵌套消费
                    if (shouldDispatchNestedChild(dy)
                            && dispatchNestedPreScroll(
                            -dx, offsetY, mConsumedPair, mScrollOffset, ViewCompat.TYPE_TOUCH)) {
                        offsetY -= mConsumedPair[1];
                        mNestedOffsets[0] += mScrollOffset[0];
                        mNestedOffsets[1] += mScrollOffset[1];
                    }
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];

                    // 累加剩余的dy，也就是offsetY。
                    mMoveDistance -= offsetY;
                    beginMoveSpinner(mMoveDistance, true);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(e);
                eventAddedToVelocityTracker = true;
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                    float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                    if (xVelocity != 0 || yVelocity != 0) {
                        fling(xVelocity, yVelocity);
                    }
                    mVelocityTracker.clear();
                    mIsBeingDragged = false;
                    finishSpinner();
                    if (mContent != null) {
                        mContent.actionUp(e);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }

        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(e);
        }
        e.recycle();
        return true;
    }

    private boolean shouldDispatchNestedChild(int dy) {
        // 在初始位置时，不管header/footer是否在刷新，都应该将先nested给child处理
        if (mSpinner == 0) {
            return true;
        }

        if (mSpinner > 0 && isPullDownRefreshing() && !canHeaderTranslation()) {
            return true;
        }

        if (mSpinner < 0 && isPullUpRefreshing() && !canFooterTranslation()) {
            return true;
        }
        boolean canPullDown =
                isGesturePullDownEnable() && mHeader != null && mHeader.getMeasureHeight() > 0;
        boolean canPullUp =
                isGesturePullUpEnable() && mFooter != null && mFooter.getMeasureHeight() > 0;
        if (dy > 0 && !canPullDown && mSpinner >= 0) {
            return true;
        }

        if (dy < 0 && !canPullUp && mSpinner <= 0) {
            return true;
        }

        debugInfo(
                "shouldDispatchNestedChild,mSpinner:",
                mSpinner,
                " dy:",
                dy,
                " canPullDown:",
                canPullDown,
                " canPullUp:",
                canPullUp);
        return false;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
            mInitialTouchX = mLastTouchX = (int) (ev.getX(newPointerIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (ev.getY(newPointerIndex) + 0.5f);
        }
    }

    private void cancelScroll() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
        mIsBeingDragged = false;
        mActivePointerId = INVALID_POINTER;
    }

    /**
     * 判断当前手势移动是否处于refresh刷新的drag状态
     *
     * @param dx 手指水平移动相对于down位置的偏移
     * @param dy 手指垂直移动相对于down位置的偏移
     */
    private void startDragging(int dx, int dy) {
        if (!mIsBeingDragged && mContent != null) {
            if (Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx)) {
                if (canPullDown(dy) || canPullUp(dy)) {
                    mIsBeingDragged = true;
                }
                if ((isPullDownRefreshing()
                        && mHeader != null
                        && mHeader.canTranslationWithContentWhenRefreshing()
                        && dy < 0)
                        || (isPullUpRefreshing()
                        && mFooter != null
                        && mFooter.canTranslationWithContentWhenRefreshing()
                        && dy > 0)) {
                    // 刷新时，移动方向与header/footer方向相反，将header/footer反向移动
                    mIsBeingDragged = true;
                }
            }
        }
    }

    private boolean canPullDown(int dy) {

        // 上拉
        if (dy <= 0) {
            return false;
        }

        // 禁止下拉
        if (!isGestureReboundEnable() && !isGesturePullDownEnable()) {
            return false;
        }

        // 在无法越界回弹的情况下，header无高度
        if (!isGestureReboundEnable()
                && (mHeader == null
                || mHeader.getMeasureHeight() <= 0
                || mHeader.getMeasuredTriggerRefreshSize() <= 0)) {
            return false;
        }

        if (mNestedChildView != null) {
            boolean canChildNestedScroll = false;
            ViewGroup nestedChildView = mNestedChildView;
            while (!canChildNestedScroll) {
                canChildNestedScroll = ScrollableViewUtil.canPullDown(nestedChildView);
                if (!(nestedChildView instanceof NestedScrollingView)) {
                    break;
                }

                nestedChildView =
                        ((NestedScrollingView) nestedChildView).getChildNestedScrollingView();
                if (nestedChildView == null) {
                    break;
                }
            }
            if (canChildNestedScroll) {
                return false;
            }
        }

        // 如果header已经拉出来了，那么可以下拉
        return mSpinner > 0 || (mContent != null && mContent.canPullDown());
    }

    private boolean canPullUp(int dy) {
        // 下拉
        if (dy >= 0) {
            return false;
        }

        // 禁止下拉
        if (!isGestureReboundEnable() && !isGesturePullUpEnable()) {
            return false;
        }

        // 在无法越界回弹的情况下，header无高度
        if (!isGestureReboundEnable()
                && (mFooter == null
                || mFooter.getMeasureHeight() <= 0
                || mFooter.getMeasuredTriggerRefreshSize() <= 0)) {
            return false;
        }

        if (mNestedChildView != null) {
            boolean canChildNestedScroll = false;
            ViewGroup nestedChildView = mNestedChildView;
            while (!canChildNestedScroll) {
                canChildNestedScroll = ScrollableViewUtil.canPullUp(nestedChildView);
                if (!(nestedChildView instanceof NestedScrollingView)) {
                    break;
                }

                nestedChildView =
                        ((NestedScrollingView) nestedChildView).getChildNestedScrollingView();
                if (nestedChildView == null) {
                    break;
                }
            }
            if (canChildNestedScroll) {
                return false;
            }
        }

        // 如果footer已经拉出来了，那么可以上拉
        return mSpinner < 0 || (mContent != null && mContent.canPullUp());
    }

    /**
     * 开始移动spinner
     *
     * @param offset 手势移动的距离
     * @param isDrag 是否在拖动
     */
    private void beginMoveSpinner(float offset, boolean isDrag) {

        debugInfo("beginMoveSpinner.offset:", offset, "  isDrag:", isDrag);

        boolean pullDownEnable =
                mHeader != null && isGesturePullDownEnable() && mHeader.getMeasureHeight() > 0;
        boolean pullUpEnable =
                mFooter != null && isGesturePullUpEnable() && mFooter.getMeasureHeight() > 0;
        boolean reboundEnable = isGestureReboundEnable();

        int spinner = (int) offset;
        if (pullDownEnable && offset > 0 && isPullDownRefreshing()) {
            // 在下拉刷新的时候，header的移动距离与手势移动的距离一样
            spinner = (int) Math.min(mHeader.getMeasuredDisplaySize(), offset);
        } else if (pullUpEnable && offset < 0 && isPullUpRefreshing()) {
            // 上拉刷新的时候，footer的移动距离与手势移动距离一样
            spinner = (int) Math.max(-mFooter.getMeasuredDisplaySize(), offset);
        } else if (offset >= 0 && pullDownEnable) {
            spinner = (int) mHeader.calculateStickyMoving(offset);
        } else if (offset <= 0 && pullUpEnable) {
            spinner = (int) mFooter.calculateStickyMoving(offset);
        } else if (reboundEnable) {
            // 越界回弹
            final float M = getMeasureReboundDisplaySize();
            final float H = getMeasuredHeight();
            if (offset > 0) {
                final float x = Math.max(0, offset * mReboundDragRate);
                spinner =
                        (int)
                                Math.min(
                                        M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))),
                                        x); // 公式 y = M(1-100^(-x/H))
            } else {
                final float x = -Math.min(0, offset * mReboundDragRate);
                spinner =
                        (int)
                                -Math.min(
                                        M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))),
                                        x); // 公式 y = M(1-100^(-x/H))
            }
        }

        if (!pullDownEnable && !pullUpEnable && !reboundEnable) {
            spinner = 0;
        }

        if (spinner > 0) {
            if (pullDownEnable && spinner > mHeader.getMeasuredDragMaxSize()) {
                spinner = mHeader.getMeasuredDragMaxSize();
            } else if (reboundEnable) {
                spinner = Math.min(spinner, getMeasureReboundDisplaySize());
            } else if (!pullDownEnable) {
                spinner = 0;
            }
        } else {
            if (pullUpEnable && Math.abs(spinner) > mFooter.getMeasuredDragMaxSize()) {
                spinner = -mFooter.getMeasuredDragMaxSize();
            } else if (reboundEnable) {
                spinner = Math.max(spinner, -getMeasureReboundDisplaySize());
            } else if (!pullUpEnable) {
                spinner = 0;
            }
        }

        moveSpinner(spinner, isDrag);
    }

    private void moveSpinner(int spinner, boolean isDrag) {
        debugInfo("moveSpinner.spinner = ", spinner, "  isDrag:", isDrag);

        mSpinner = spinner;
        boolean pullDownEnable = mHeader != null && isGesturePullDownEnable();
        boolean pullUpEnable = mFooter != null && isGesturePullUpEnable();
        boolean reboundEnable = isGestureReboundEnable();

        float percent = 0;
        if (spinner > 0 && pullDownEnable) {
            int triggerSize = mHeader.getMeasuredTriggerRefreshSize();
            percent = triggerSize == 0 ? 1 : spinner * 1f / triggerSize;
        } else if (spinner < 0 && pullUpEnable) {
            int triggerSize = mFooter.getMeasuredTriggerRefreshSize();
            percent = triggerSize == 0 ? 1 : -spinner * 1f / triggerSize;
        }

        if (mContent != null && (pullDownEnable || pullUpEnable || reboundEnable)) {
            float contentSpinner = 0;
            if (spinner >= 0 && mHeader != null) {
                contentSpinner = canContentTranslation(spinner, mHeader) ? spinner : 0;
            } else if (spinner <= 0 && mFooter != null) {
                contentSpinner = canContentTranslation(spinner, mFooter) ? spinner : 0;
            }
            mContent.move(
                    contentSpinner,
                    percent,
                    isDrag,
                    spinner > 0 ? mState.isPullDownRefreshing() : mState.isPullUpRefreshing());
        }

        if (pullDownEnable && canHeaderTranslation()) {
            mHeader.move(spinner, percent, isDrag, mState.isPullDownRefreshing());
        }

        if (pullUpEnable && canFooterTranslation()) {
            mFooter.move(spinner, percent, isDrag, mState.isPullUpRefreshing());
        }

        // 没有刷新时，根据拖动时的spinner修改state
        if (isDrag && pullDownEnable && spinner >= 0 && !isPullDownRefreshing()) {
            if (spinner > 0) {
                setState(RefreshState.PULL_DOWN_TO_REFRESH);
            } else {
                setState(RefreshState.PULL_DOWN_IDLE);
            }
        } else if (isDrag && pullUpEnable && spinner <= 0 && !isPullUpRefreshing()) {
            if (spinner < 0) {
                setState(RefreshState.PULL_UP_TO_REFRESH);
            } else {
                setState(RefreshState.PULL_UP_IDLE);
            }
        }
    }

    /**
     * 是否可以移动header
     *
     * @return
     */
    private boolean canHeaderTranslation() {
        if (mHeader == null || mHeader.getMeasureHeight() <= 0) {
            return false;
        }

        // 如果header为behind，header固定显示
        if (mHeader.getStyle() == RefreshExtension.STYLE_FIXED_BEHIND) {
            return false;
        }
        // 没有刷新时可以移动，或者刷新时运行随着content移动
        return !mState.isPullDownRefreshing() || mHeader.canTranslationWithContentWhenRefreshing();
    }

    private boolean canFooterTranslation() {
        if (mFooter == null || mFooter.getMeasureHeight() <= 0) {
            return false;
        }

        // 如果footer为behind，header固定显示
        if (mFooter.getStyle() == RefreshExtension.STYLE_FIXED_BEHIND) {
            return false;
        }
        // 没有刷新时可以移动，或者刷新时运行随着content移动
        return !mState.isPullUpRefreshing() || mFooter.canTranslationWithContentWhenRefreshing();
    }

    /**
     * 是否可以移动内容视图
     *
     * @return
     */
    private boolean canContentTranslation(int dy, RefreshExtension extension) {
        if (extension.getStyle() == RefreshExtension.STYLE_TRANSLATION
                || extension.getStyle() == RefreshExtension.STYLE_FIXED_BEHIND) {
            return true;
        }

        if (dy > 0 && !isPullDownEnable() && isReboundTopEnable()) {
            return true;
        } else if (dy < 0 && !isPullUpEnable() && isReboundBottomEnable()) {
            return true;
        }

        return dy == 0;
    }

    private void finishSpinner() {
        debugInfo("-----------finishSpinner-------------");
        boolean pullDownEnable = mHeader != null && isGesturePullDownEnable();
        boolean pullUpEnable = mFooter != null && isGesturePullUpEnable();

        int end = mSpinner;
        Interpolator interpolator = mInterpolator;
        int duration = mAnimationDuration;
        if (mSpinner > 0 && pullDownEnable) {
            // 如果当前的spinner小于triggerSize，不管是否在刷新状态，都回到位置0。
            end =
                    mSpinner >= mHeader.getMeasuredTriggerRefreshSize()
                            ? mHeader.getMeasuredDisplaySize()
                            : 0;
        } else if (mSpinner < 0 && pullUpEnable) {

            end =
                    Math.abs(mSpinner) >= mFooter.getMeasuredTriggerRefreshSize()
                            ? -mFooter.getMeasuredDisplaySize()
                            : 0;
        } else if (mReboundEnable) {
            end = 0;
        }

        animationSpinner(end, duration, interpolator);
    }

    public boolean fling(float velocityX, float velocityY) {
        if (dispatchNestedPreFling(-velocityX, -velocityY)) {
            return true;
        }

        if (flingSelf(velocityX, velocityY)) {
            return true;
        }
        return dispatchNestedFling(-velocityX, -velocityY, true);
    }

    public boolean flingSelf(float velocityX, float velocityY) {
        if (Math.abs(velocityY) < mMinimumVelocity) {
            return false;
        }
        if (mSpinner == 0) {
            return false;
        }
        // TODO
        return false;
    }

    private void setState(int state) {
        int oldHeaderState = mState.getHeaderState();
        int oldFooterState = mState.getFooterState();
        mState.setState(state);
        if (oldHeaderState != mState.getHeaderState()) {
            notifyStateChanged(oldHeaderState, mState.getHeaderState());
        }

        if (oldFooterState != mState.getFooterState()) {
            notifyStateChanged(oldFooterState, mState.getFooterState());
        }
    }

    private void notifyStateChanged(int oldState, int currentState) {
        if (mHeader != null && RefreshState.isHeaderState(currentState)) {
            mHeader.onStateChanged(mState, oldState, currentState);
        }

        if (mFooter != null && RefreshState.isFooterState(currentState)) {
            mFooter.onStateChanged(mState, oldState, currentState);
        }
        switch (currentState) {
            case RefreshState.PULL_DOWN_IDLE:
                break;
            case RefreshState.PULL_DOWN_TO_REFRESH:
                break;
            case RefreshState.PULL_DOWN_REFRESHING:
                dispatchNotifyPullDownRefresh();
                break;
            case RefreshState.PULL_DOWN_TO_RELEASE:
                break;
            case RefreshState.PULL_UP_IDLE:
                break;
            case RefreshState.PULL_UP_TO_REFRESH:
                break;
            case RefreshState.PULL_UP_REFRESHING:
                dispatchNotifyPullUpRefresh();
                break;
            case RefreshState.PULL_UP_TO_RELEASE:
                break;
            default:
                break;
        }
    }

    private void dispatchNotifyPullDownRefresh() {
        for (OnPullDownRefreshListener pullDownRefreshListener : mPullDownRefreshListeners) {
            pullDownRefreshListener.onPullDown();
        }
    }

    private void dispatchNotifyPullUpRefresh() {
        for (OnPullUpListener pullUpListener : mPullUpListeners) {
            pullUpListener.onPullUp();
        }
    }

    private void cancelScrollAnimation() {
        if (mScrollAnimation != null) {
            debugInfo("-----cancelAnimation");
            mScrollAnimation.setDuration(0);
            mScrollAnimation.cancel();
            mScrollAnimation = null;
        }
    }

    private void animationSpinner(int spinner) {
        animationSpinner(spinner, mAnimationDuration, mInterpolator);
    }

    private void animationSpinner(int spinner, int duration, Interpolator interpolator) {
        animationSpinner(spinner, duration, interpolator, 0);
    }

    private void animationSpinner(int spinner, int duration, Interpolator interpolator, int delay) {
        animationSpinner(spinner, duration, interpolator, delay, null);
    }

    private void animationSpinner(
            int spinner,
            int duration,
            Interpolator interpolator,
            int delay,
            Animator.AnimatorListener listener) {
        if (spinner != mSpinner) {
            debugInfo("------animationSpinner.spinner:", spinner);
            cancelScrollAnimation();
            mScrollAnimation = ValueAnimator.ofInt(mSpinner, spinner);
            mScrollAnimation.setInterpolator(interpolator);
            mScrollAnimation.setDuration(duration);
            mScrollAnimation.setStartDelay(delay);
            mScrollAnimation.addListener(
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (listener != null) {
                                listener.onAnimationStart(animation);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mScrollAnimation = null;
                            updateRefreshState();
                            if (listener != null) {
                                listener.onAnimationEnd(animation);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            if (listener != null) {
                                listener.onAnimationCancel(animation);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                            if (listener != null) {
                                listener.onAnimationRepeat(animation);
                            }
                        }
                    });

            mScrollAnimation.addUpdateListener(
                    animation -> moveSpinner((int) animation.getAnimatedValue(), false));
            mScrollAnimation.start();
        } else {
            updateRefreshState();
        }
    }

    private void updateRefreshState() {
        if (mSpinner == 0) {
            if (!isPullDownRefreshing()) {
                setState(RefreshState.PULL_DOWN_IDLE);
            }

            if (!mState.isPullUpRefreshing()) {
                setState(RefreshState.PULL_UP_IDLE);
            }
        } else if (!isPullDownRefreshing()
                && mHeader != null
                && mSpinner == mHeader.getMeasuredDisplaySize()) {
            setState(RefreshState.PULL_DOWN_REFRESHING);
        } else if (!isPullUpRefreshing()
                && mFooter != null
                && mSpinner == -mFooter.getMeasuredDisplaySize()) {
            setState(RefreshState.PULL_UP_REFRESHING);
        }
    }

    /**
     * 在DEBUG下打印调试信息
     *
     * @param info
     */
    private void debugInfo(Object... info) {
        if (!DEBUG) {
            return;
        }
        if (info == null || info.length == 0) {
            return;
        }

        if (info.length == 1 && info[0] != null) {
            Log.i(TAG, info[0].toString());
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (Object msg : info) {
            if (msg != null) {
                builder.append(msg);
            }
        }
        Log.i(TAG, builder.toString());
    }

    private String getEventType(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return "down";
            case MotionEvent.ACTION_MOVE:
                return "move";
            case MotionEvent.ACTION_UP:
                return "up";
            case MotionEvent.ACTION_CANCEL:
                return "cancel";
            default:
                break;
        }
        return "unknow";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mContent != null) {
            super.bringChildToFront(mContent.getView());
        }
        if (mHeader != null && mHeader.getStyle() == RefreshExtension.STYLE_FIXED_FRONT) {
            super.bringChildToFront(mHeader.getView());
        }
        if (mFooter != null && mFooter.getStyle() == RefreshExtension.STYLE_FIXED_FRONT) {
            super.bringChildToFront(mFooter.getView());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelScrollAnimation();
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mNestedScrollingChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public void stopNestedScroll(int type) {
        mNestedScrollingChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mNestedScrollingChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes, int type) {
        // 如果当前在垂直方向上运行pulldown或者loadmore或者越界回弹，则可以处理child的嵌套滚动
        if (target instanceof ViewGroup) {
            mNestedChildView = (ViewGroup) target;
        }
        boolean accepted =
                isEnabled() && isNestedScrollingEnabled()
                        && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        accepted =
                accepted
                        && (isGesturePullDownEnable() || isGesturePullUpEnable()
                        || isGestureReboundEnable());
        return accepted;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(
            @NonNull View child, @NonNull View target, int axes, int type) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mMoveDistance = mSpinner;
        cancelScrollAnimation();
    }

    @Override
    public void onStopNestedScroll(View child) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        //        finishSpinner();
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    private boolean isPartOfContent(View target) {
        if (mContent == null) {
            return false;
        }

        View contentView = mContent.getView();
        ViewParent parent = target.getParent();
        while (parent != null
                && (!Objects.equals(parent, contentView) || Objects.equals(parent, this))) {
            parent = parent.getParent();
        }
        return Objects.equals(parent, contentView);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(
            @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        debugInfo("onNestedPreScroll,dy:", dy);

        if (target instanceof NestedScrollingView) {
            if (((NestedScrollingView) target).shouldScrollFirst(dy, 0)) {
                return;
            }
        }
        // 如果header/footer已经拉出来了，需要先让header/footer推回去后，child才让滚动
        int consumedY = 0;
        int spinner = mSpinner;
        // dy > 0：上滑，dy < 0：下滑
        // spinner > 0：header已经拉出来了，spinner < 0：footer已经拉出来了
        if (type == ViewCompat.TYPE_TOUCH
                && // 在拖动的时候处理nested，fling下的scroll不处理
                dy * spinner > 0
                && isPartOfContent(target)) {
            // 将header/footer往回退
            if (Math.abs(dy) > Math.abs(spinner)) {
                consumedY = spinner;
                spinner = 0;
            } else {
                consumedY = dy;
                spinner -= dy;
            }
            beginMoveSpinner(spinner, type == ViewCompat.TYPE_TOUCH);
        }
        dispatchNestedPreScroll(dx, dy - consumedY, consumed, null);
        consumed[1] += consumedY;
    }

    @Override
    public void onNestedScroll(
            @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed) {
        onNestedScroll(
                target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type) {
        debugInfo("onNestedScroll,dyConsumed:", dyConsumed, " dyUnconsumed:", dyUnconsumed);
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if ((dy < 0 && canPullDown(-dy)) || (dy > 0 && canPullUp(-dy))) {
            if (type == ViewCompat.TYPE_TOUCH) {
                mMoveDistance -= dy;
                beginMoveSpinner(mMoveDistance, true);
            }
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (target instanceof NestedScrollingView) {
            if (((NestedScrollingView) target).shouldScrollFirst(0, (int) velocityY)) {
                return false;
            }
        }
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper
                .dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(
            int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedScroll(
            int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
            int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedScroll(
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            @Nullable int[] offsetInWindow,
            int type) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean shouldScrollFirst(int dy, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).shouldScrollFirst(dy, velocityY)) {
                return true;
            }
        }
        boolean toBottom = dy > 0;
        boolean toTop = dy < 0;
        if (toBottom && !isPullDownRefreshing() && mHeader != null && isGesturePullDownEnable()) {
            return true;
        }

        if (toTop && !isPullUpRefreshing() && mFooter != null && isGesturePullUpEnable()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean nestedFling(int velocityX, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).nestedFling(velocityX, velocityY)) {
                return true;
            }
        }
        return false;
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
    public ViewGroup getChildNestedScrollingView() {
        return mNestedChildView;
    }

    public interface OnPullDownRefreshListener {
        void onPullDown();
    }

    public interface OnPullUpListener {
        void onPullUp();
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);
        }
    }
}
