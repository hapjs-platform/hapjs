/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.animation.TimingFactory;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.Swiper;

public class SwiperView extends FrameLayout implements ComponentHost, GestureHost {
    private static final String TAG = "SwiperView";

    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private LoopViewPager mViewPager;
    private LoopPagerAdapter mAdapter;
    private LinearLayout mIndicator;
    private int mIndicatorColor;
    private int mIndicatorSelectedColor;
    private float mIndicatorSize;
    private IGesture mGesture;
    private int mDuration;

    private int mPreviousMargin = IntegerUtil.UNDEFINED;
    private int mNextMargin = IntegerUtil.UNDEFINED;

    private Rect mIndicatorPosition = new Rect();
    private ViewTreeObserver.OnGlobalLayoutListener mViewPagerSizeWatcher =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // 如果swiper的大小变化了，需要重新设置设置previousmargin和nextmargin
                    if (!IntegerUtil.isUndefined(mPreviousMargin)) {
                        setPreviousMargin(mPreviousMargin);
                    }

                    if (!IntegerUtil.isUndefined(mNextMargin)) {
                        setNextMargin(mNextMargin);
                    }
                }
            };

    public SwiperView(HapEngine hapEngine, Context context) {
        super(context);

        mViewPager = new LoopViewPager(context, hapEngine);
        mViewPager.setClipToPadding(false);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.registerOnPageChangeCallback(
                new LoopViewPager.OnPageChangeCallback() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        setSelectedIndicator(position);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });

        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(mViewPagerSizeWatcher);
        mViewPager.addOnAttachStateChangeListener(
                new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        // 如果viewpager被remove掉了，应该移除globalLayoutListener，否则会造成内存泄漏
                        if (mViewPagerSizeWatcher != null) {
                            mViewPager.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(mViewPagerSizeWatcher);
                            mViewPagerSizeWatcher = null;
                        }
                        mViewPager.removeOnAttachStateChangeListener(this);
                    }
                });

        mAdapter = new LoopPagerAdapter(mViewPager);
        mViewPager.setAdapter(mAdapter);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mViewPager, lp);

        mIndicator = new LinearLayout(context);
        mIndicator.setGravity(Gravity.CENTER_VERTICAL);
        lp =
                new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        addView(mIndicator, lp);

        mIndicatorColor = ColorUtil.getColor(Swiper.DEFAULT_INDICATOR_COLOR);
        mIndicatorSelectedColor = ColorUtil.getColor(Swiper.DEFAULT_INDICATOR_SELECTED_COLOR);
        mIndicatorSize = Attributes.getFloat(hapEngine, Swiper.DEFAULT_INDICATOR_SIZE);

        mIndicatorPosition.left =
                mIndicatorPosition.top = mIndicatorPosition.right = mIndicatorPosition.bottom = -1;

        mIndicator.addOnLayoutChangeListener(
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

                        if (mIndicatorPosition.left != -1) {
                            setIndicatorLeft(mIndicatorPosition.left);
                        }

                        if (mIndicatorPosition.top != -1) {
                            setIndicatorTop(mIndicatorPosition.top);
                        }

                        if (mIndicatorPosition.right != -1) {
                            setIndicatorRight(mIndicatorPosition.right);
                        }

                        if (mIndicatorPosition.bottom != -1) {
                            setIndicatorBottom(mIndicatorPosition.bottom);
                        }
                    }
                });
    }

    public LoopViewPager getViewPager() {
        return mViewPager;
    }

    public LoopPagerAdapter getAdapter() {
        return mAdapter;
    }

    public void setIndicatorEnabled(boolean enabled) {
        if (enabled) {
            mIndicator.setVisibility(VISIBLE);
            while (mIndicator.getChildCount() != mAdapter.getActualItemCount()) {
                if (mIndicator.getChildCount() > mAdapter.getActualItemCount()) {
                    mIndicator.removeViewAt(0);
                    continue;
                }
                if (mIndicator.getChildCount() < mAdapter.getActualItemCount()) {
                    addIndicatorPoint();
                }
            }
        } else {
            mIndicator.setVisibility(GONE);
            mIndicator.removeAllViews();
        }
    }

    @Override
    public void removeView(View view) {
        if (view instanceof ComponentHost) {
            mViewPager.removeView(view);
        } else {
            super.removeView(view);
        }
    }

    @Override
    public void addView(View child, int index) {
        if (child instanceof ComponentHost) {
            mViewPager.addView(child, index);
        } else {
            super.addView(child, index);
        }
    }

    public void addIndicatorPoint() {
        IndicatorPoint indicatorPoint = new IndicatorPoint(getContext());
        indicatorPoint.setSize(mIndicatorSize);
        indicatorPoint.setColor(mIndicatorColor);
        indicatorPoint.setSelectedColor(mIndicatorSelectedColor);
        mIndicator.addView(
                indicatorPoint,
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT));

        int index = mIndicator.indexOfChild(indicatorPoint);
        if (index == mViewPager.getCurrentItem()) {
            indicatorPoint.setSelected(true);
        } else {
            indicatorPoint.setSelected(false);
        }
    }

    public void removeIndicatorPoint(int index) {
        mIndicator.removeView(mIndicator.getChildAt(index));
    }

    public void setIndicatorSize(float size) {
        if (size <= 0) {
            return;
        }
        mIndicatorSize = size;
    }

    public void setIndicatorColor(int color) {
        mIndicatorColor = color;
    }

    public void setIndicatorSelectedColor(int selectedColor) {
        mIndicatorSelectedColor = selectedColor;
    }

    public void setSelectedIndicator(int currentItem) {
        for (int i = 0; i < mIndicator.getChildCount(); i++) {
            IndicatorPoint point = (IndicatorPoint) mIndicator.getChildAt(i);
            if (i == currentItem) {
                point.setSelected(true);
            } else {
                point.setSelected(false);
            }
        }
    }

    public void updateIndicator() {
        setSelectedIndicator(mViewPager.getCurrentItem());

        for (int i = 0; i < mIndicator.getChildCount(); i++) {
            IndicatorPoint point = (IndicatorPoint) mIndicator.getChildAt(i);
            point.setColor(mIndicatorColor);
            point.setSelectedColor(mIndicatorSelectedColor);
            point.setSize(mIndicatorSize);
        }
    }

    public void setLoop(boolean loop) {
        // no need to recover current index here,it is recovered in getItemPosition
        mViewPager.setLoop(loop);
    }

    public void setEnableSwipe(boolean enableSwipe) {
        mViewPager.setEnableSwipe(enableSwipe);
    }

    public void setPageAnimation(ViewPager.PageTransformer transformer) {
        try {
            mViewPager.setPageTransformer(true, transformer);
        } catch (Exception e) {
            Log.e(TAG, "Please set the PageTransformer class");
        }
    }

    public void setTimingFunction(String timingFunction) {
        CustomScroller customScroller =
                new CustomScroller(mViewPager.getContext(),
                        TimingFactory.getTiming(timingFunction));
        customScroller.setDuration(mDuration);
        mViewPager.setScroller(customScroller);
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
    public void setGesture(IGesture gesture) {
        mGesture = gesture;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean result = super.dispatchTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mViewPager == null && mAdapter == null) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && mViewPager.isHorizontal()) {
            mViewPager.previouPage();
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && mViewPager.isHorizontal()) {
            mViewPager.nextPage();
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !(mViewPager.isHorizontal())) {
            mViewPager.previouPage();
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !(mViewPager.isHorizontal())) {
            mViewPager.nextPage();
            return true;
        }
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

    public void setVertical(boolean vertical) {
        mViewPager.setDirection(
                vertical ? ViewPager.Direction.VERTICAL : ViewPager.Direction.HORIZONTAL);
        setIndicatorOrientation(vertical);
    }

    private void setIndicatorOrientation(boolean vertical) {
        if (vertical) {
            mIndicator.setOrientation(LinearLayout.VERTICAL);
        } else {
            mIndicator.setOrientation(LinearLayout.HORIZONTAL);
        }
        refreshIndicatorPosition();
    }

    public void setDuration(int duration) {
        mDuration = duration;
        mViewPager.setPageScrollDuration(duration);
    }

    public void setPreviousMargin(int previousMargin) {
        mPreviousMargin = previousMargin;

        // 如果child的数量小于2，不设置previousmargin
        if (mAdapter.getActualItemCount() < 2) {
            return;
        }

        int paddingRight = mViewPager.getPaddingRight();
        int paddingBottom = mViewPager.getPaddingBottom();

        if (previousMargin < 0) {
            previousMargin = 0;
        }

        // paddingleft和paddingright的总和不能超过ViewPager的1/2，否则会显示异常。
        int limitMargin =
                mViewPager.isHorizontal()
                        ? (mViewPager.getWidth() / 2 - paddingRight)
                        : (mViewPager.getHeight() / 2 - paddingBottom);
        if (limitMargin <= 0) {
            return;
        }
        if (previousMargin > limitMargin) {
            previousMargin = limitMargin;
        }

        int oldPreviousMargin =
                mViewPager.isHorizontal() ? mViewPager.getPaddingLeft() :
                        mViewPager.getPaddingTop();
        if (oldPreviousMargin == previousMargin) {
            return;
        }

        if (!mViewPager.isHorizontal()) { // 上下滑动
            mViewPager.setPadding(0, previousMargin, 0, paddingBottom);
        } else { // 左右滑动
            mViewPager.setPadding(previousMargin, 0, paddingRight, 0);
        }
        mViewPager.setCurrentItemAlways(mViewPager.getCurrentItem());
    }

    public void setNextMargin(int nextMargin) {
        mNextMargin = nextMargin;

        // 如果child的数量小于2，不设置nextmargin
        if (mAdapter.getActualItemCount() < 2) {
            return;
        }

        int paddingLeft = mViewPager.getPaddingLeft();
        int paddingTop = mViewPager.getPaddingTop();

        if (nextMargin < 0) {
            nextMargin = 0;
        }

        // paddingleft和paddingright的总和不能超过ViewPager的1/2，否则会显示异常。
        int limitMargin =
                mViewPager.isHorizontal()
                        ? (mViewPager.getWidth() / 2 - paddingLeft)
                        : (mViewPager.getHeight() / 2 - paddingTop);

        if (limitMargin <= 0) {
            return;
        }
        if (nextMargin > limitMargin) {
            nextMargin = limitMargin;
        }

        int oldNextMargin =
                mViewPager.isHorizontal() ? mViewPager.getPaddingRight() :
                        mViewPager.getPaddingBottom();
        if (nextMargin == oldNextMargin) {
            return;
        }

        if (!mViewPager.isHorizontal()) { // 上下滑动
            mViewPager.setPadding(0, paddingTop, 0, nextMargin);
        } else { // 左右滑动
            mViewPager.setPadding(paddingLeft, 0, nextMargin, 0);
        }
        mViewPager.setCurrentItemAlways(mViewPager.getCurrentItem());
    }

    public void setPageMargin(int margin) {
        mViewPager.setPageMargin(margin);
    }

    public void setIndicatorLeft(int left) {
        mIndicatorPosition.left = left;
        int right = mIndicatorPosition.right;
        determineHorizonalIndicatorGravity(left, right, !mViewPager.isHorizontal());

        if (left < 0) {
            left = 0;
        } else if (left > getWidth() - getIndicatorWidth()) {
            left = getWidth() - getIndicatorWidth();
        }
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.leftMargin = left;

        // 如果同时设置了left和right，需要clear掉right
        if (left > 0) {
            params.rightMargin = 0;
        }
        mIndicator.setLayoutParams(params);
    }

    public void setIndicatorTop(int top) {
        mIndicatorPosition.top = top;

        int bottom = mIndicatorPosition.bottom;
        determineVerticalIndicatorGravity(top, bottom, !mViewPager.isHorizontal());

        if (top < 0) {
            top = 0;
        } else if (top > getHeight() - getIndicatorHeight()) {
            top = getHeight() - getIndicatorHeight();
        }
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.topMargin = top;

        // 如果top和bottom都设置了，需要clear掉bottom
        if (top > 0) {
            params.bottomMargin = 0;
        }
        mIndicator.setLayoutParams(params);
    }

    public void setIndicatorRight(int right) {
        mIndicatorPosition.right = right;

        if (mIndicatorPosition.left > 0) {
            // 如果设置了left，right不会生效
            return;
        }

        int left = mIndicatorPosition.left;
        determineHorizonalIndicatorGravity(left, right, !mViewPager.isHorizontal());

        if (right < 0) {
            right = 0;
        } else if (right > getWidth() - getIndicatorWidth()) {
            right = getWidth() - getIndicatorWidth();
        }
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.rightMargin = right;
        mIndicator.setLayoutParams(params);
    }

    public void setIndicatorBottom(int bottom) {
        mIndicatorPosition.bottom = bottom;

        if (mIndicatorPosition.top > 0) {
            // 如果设置了top，bottom不会生效
            return;
        }

        int top = mIndicatorPosition.top;
        determineVerticalIndicatorGravity(top, bottom, !mViewPager.isHorizontal());

        if (bottom < 0) {
            bottom = 0;
        } else if (bottom > getHeight() - getIndicatorHeight()) {
            bottom = getHeight() - getIndicatorHeight();
        }
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.bottomMargin = bottom;
        mIndicator.setLayoutParams(params);
    }

    private void determineHorizonalIndicatorGravity(int left, int right, boolean isVertical) {
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.gravity &= ~Gravity.START;
        params.gravity &= ~Gravity.END;
        params.gravity &= ~Gravity.CENTER_HORIZONTAL;
        if (left >= 0) {
            params.gravity |= Gravity.START;
        } else if (right >= 0) {
            params.gravity |= Gravity.END;
        } else {
            if (isVertical) {
                params.gravity |= Gravity.END;
            } else {
                params.gravity |= Gravity.CENTER_HORIZONTAL;
            }
        }
    }

    private void determineVerticalIndicatorGravity(int top, int bottom, boolean isVertical) {
        FrameLayout.LayoutParams params = (LayoutParams) mIndicator.getLayoutParams();
        params.gravity &= ~Gravity.TOP;
        params.gravity &= ~Gravity.BOTTOM;
        params.gravity &= ~Gravity.CENTER_VERTICAL;

        if (top >= 0) {
            params.gravity |= Gravity.TOP;
        } else if (bottom >= 0) {
            params.gravity |= Gravity.BOTTOM;
        } else {
            if (isVertical) {
                params.gravity |= Gravity.CENTER_VERTICAL;
            } else {
                params.gravity |= Gravity.BOTTOM;
            }
        }
    }

    private void refreshIndicatorPosition() {
        setIndicatorLeft(mIndicatorPosition.left);
        setIndicatorTop(mIndicatorPosition.top);
        setIndicatorRight(mIndicatorPosition.right);
        setIndicatorBottom(mIndicatorPosition.bottom);
    }

    public void destroy() {
        mViewPager.stopAutoScroll();
        if (mViewPagerSizeWatcher != null) {
            mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(mViewPagerSizeWatcher);
            mViewPagerSizeWatcher = null;
        }
    }

    public int getIndicatorCount() {
        return mIndicator.getChildCount();
    }

    /**
     * 设置指示器的方向后，无法立即获得正确的尺寸，所以实际的宽度和高度是由child的数量来计算的。
     */
    private int getIndicatorWidth() {
        if (mIndicator.getChildCount() <= 0) {
            return 0;
        }

        if (mViewPager.isHorizontal()) {
            return mIndicator.getChildAt(0).getMeasuredWidth() * mIndicator.getChildCount();
        } else {
            return mIndicator.getChildAt(0).getMeasuredWidth();
        }
    }

    /**
     * 设置指示器的方向后，无法立即获得正确的尺寸，所以实际的宽度和高度是由child的数量来计算的。
     */
    private int getIndicatorHeight() {
        if (mIndicator.getChildCount() <= 0) {
            return 0;
        }

        if (mViewPager.isHorizontal()) {
            return mIndicator.getChildAt(0).getMeasuredHeight();
        } else {
            return mIndicator.getChildAt(0).getMeasuredHeight() * mIndicator.getChildCount();
        }
    }

    public void setData(Container.RecyclerItem data) {
        mAdapter.setData((Container) getComponent(), data);

        // 当页数小于2时，需要清除上一页和下一页的显示。否则，恢复上一页和下一页的显示区域。
        if (data == null || data.getChildren().size() <= 1) {
            mViewPager.setPadding(0, 0, 0, 0);
        } else {
            if (!IntegerUtil.isUndefined(mPreviousMargin)) {
                setPreviousMargin(mPreviousMargin);
            }

            if (!IntegerUtil.isUndefined(mNextMargin)) {
                setNextMargin(mNextMargin);
            }
        }
    }
}
