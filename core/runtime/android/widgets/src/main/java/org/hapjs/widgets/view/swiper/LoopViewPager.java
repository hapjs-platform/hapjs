/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.component.view.gesture.GestureDispatcher;
import org.hapjs.runtime.HapEngine;

public class LoopViewPager extends ViewPager implements Handler.Callback {

    private static final String TAG = "LoopViewPager";

    private static final int POSITION_NONE = -1;
    private static final int MSG_AUTO_SCROLL = 1;
    private static final int MSG_NEXT = 2;
    private static final int MSG_PREVIOU = 3;
    private Handler mHandler;
    private boolean mAutoScroll;
    private long mInterval = 3 * 1000; // 3 sec;
    private boolean mEnableSwipe = true;
    private int mLastMotionX;
    private int mLastMotionY;
    private HapEngine mHapEngine;

    private int mPendingPosition = -1;
    private List<OnPageChangeCallback> mCallbacks = new ArrayList<>();

    public LoopViewPager(Context context, HapEngine hapEngine) {
        super(context);
        mHapEngine = hapEngine;
        mHandler = new Handler(Looper.getMainLooper(), this);
        setOverScrollMode(View.OVER_SCROLL_NEVER);

        super.addOnPageChangeListener(
                new OnPageChangeListener() {

                    private int mCurrentPosition = POSITION_NONE;
                    private int mPreviousPosition = POSITION_NONE;
                    private boolean isPageChanging;

                    @Override
                    public void onPageScrolled(int position, float positionOffset,
                                               int positionOffsetPixels) {
                        int i = convertToRealPosition(position);
                        for (OnPageChangeCallback callback : mCallbacks) {
                            callback.onPageScrolled(i, positionOffset, positionOffsetPixels);
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        if (isLoop() && isPageChanging) {
                            mCurrentPosition = position;
                        }

                        int i = convertToRealPosition(position);
                        if (mPreviousPosition == i) {
                            return;
                        }
                        mPreviousPosition = i;
                        for (OnPageChangeCallback callback : mCallbacks) {
                            callback.onPageSelected(i);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        if (isLoop()) {
                            if (state == ViewPager.SCROLL_STATE_DRAGGING
                                    || (state == ViewPager.SCROLL_STATE_SETTLING && mAutoScroll)) {
                                isPageChanging = true;
                            } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                                isPageChanging = false;
                                int position = fixLoopPosition(mCurrentPosition);
                                if (position != POSITION_NONE && position != mCurrentPosition) {
                                    setCurrentItem(convertToRealPosition(position), false);
                                }
                            }
                        }

                        for (OnPageChangeCallback callback : mCallbacks) {
                            callback.onPageScrollStateChanged(state);
                        }
                    }

                    // fix position when it is edge in loop mode.
                    private int fixLoopPosition(int position) {
                        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
                        if (adapter == null || !adapter.isLoop()) {
                            return position;
                        }

                        if (position == POSITION_NONE) {
                            return POSITION_NONE;
                        }

                        int itemCount = adapter.getCount();
                        if (itemCount <= 0) {
                            return 0;
                        }

                        int endPosition = itemCount - 1;
                        int fixPosition = position;
                        if (position == 0) {
                            fixPosition = endPosition - 1;
                        } else if (position == endPosition) {
                            fixPosition = 1;
                        }
                        return fixPosition;
                    }
                });
    }

    /**
     * @deprecated please use{@link LoopViewPager#registerOnPageChangeCallback(OnPageChangeCallback)}
     */
    @Deprecated
    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        // unsupport
    }

    /**
     * @deprecated please use {@link LoopViewPager#registerOnPageChangeCallback(OnPageChangeCallback)}
     */
    @Deprecated
    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        // unsupport
    }

    private boolean isLoop() {
        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
        if (adapter != null) {
            return adapter.isLoop();
        }
        return false;
    }

    public void setLoop(boolean loop) {
        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
        if (adapter != null) {
            int currentItem = getCurrentItem();
            adapter.setLoop(loop);
            setCurrentItem(currentItem);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        int what = msg.what;
        if (what == MSG_AUTO_SCROLL) {
            if (mAutoScroll) {
                super.setCurrentItem(super.getCurrentItem() + 1, true);
                mHandler.sendEmptyMessageDelayed(MSG_AUTO_SCROLL, mInterval);
            }
            return true;
        } else if (what == MSG_NEXT) {
            int currentItem = super.getCurrentItem();
            if (isLoop()) {
                if (currentItem == ((LoopPagerAdapter) getAdapter()).getActualItemCount() + 1) {
                    currentItem = 1;
                    super.setCurrentItem(currentItem, false);
                }
                super.setCurrentItem(currentItem + 1, true);
            } else {
                if (currentItem < ((LoopPagerAdapter) getAdapter()).getActualItemCount()) {
                    super.setCurrentItem(currentItem + 1, true);
                }
            }

            return true;
        } else if (what == MSG_PREVIOU) {
            int currentItem = super.getCurrentItem();
            if (isLoop()) {
                if (currentItem == 0) {
                    currentItem = ((LoopPagerAdapter) getAdapter()).getActualItemCount();
                    super.setCurrentItem(currentItem, false);
                }
                super.setCurrentItem(currentItem - 1, true);
            } else {
                if (currentItem > 1) {
                    super.setCurrentItem(currentItem - 1, true);
                }
            }
            return true;
        }
        return false;
    }

    public void nextPage() {
        if (mHandler.hasMessages(MSG_NEXT)) {
            mHandler.removeMessages(MSG_NEXT);
        }
        mHandler.sendEmptyMessage(MSG_NEXT);
    }

    public void previouPage() {
        if (mHandler.hasMessages(MSG_PREVIOU)) {
            mHandler.removeMessages(MSG_PREVIOU);
        }
        mHandler.sendEmptyMessage(MSG_PREVIOU);
    }

    public boolean isAutoScroll() {
        return mAutoScroll;
    }

    public void setAutoScroll(boolean autoScroll) {
        mAutoScroll = autoScroll;
    }

    public void startAutoScroll() {
        if (mHandler.hasMessages(MSG_AUTO_SCROLL)) {
            mHandler.removeMessages(MSG_AUTO_SCROLL);
        }
        mHandler.sendEmptyMessageDelayed(MSG_AUTO_SCROLL, mInterval);
    }

    public void stopAutoScroll() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public void setInterval(long interval) {
        if (interval < 0) {
            interval = 0;
        }
        mInterval = interval;
    }

    public void setEnableSwipe(boolean enableSwipe) {
        mEnableSwipe = enableSwipe;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mEnableSwipe) {
            return false;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = (int) ev.getY();
                mLastMotionX = (int) ev.getX();
                stopAutoScroll();
                break;
            case MotionEvent.ACTION_MOVE:
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                final int xDiff = Math.abs(x - mLastMotionX);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (!mEnableSwipe
                        && mHapEngine.getMinPlatformVersion()
                        < GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION) {
                    if ((isHorizontal() && xDiff > yDiff) || (!isHorizontal() && yDiff > xDiff)) {
                        return false;
                    }
                }
                stopAutoScroll();
                mLastMotionX = x;
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isAutoScroll()) {
                    startAutoScroll();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAutoScroll()) {
            startAutoScroll();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

    public void setAdapter(LoopPagerAdapter adapter) {
        super.setAdapter(adapter);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof LoopPagerAdapter)) {
            throw new IllegalArgumentException("adapter must be CircularPagerAdapter");
        }
        super.setAdapter(adapter);
    }

    public void setCurrentItemAlways(int item) {
        if (item < 0) {
            Log.w(TAG, "invalidate position:" + item);
            return;
        }
        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
        if (adapter == null || adapter.getActualItemCount() <= 0) {
            mPendingPosition = item;
            return;
        }

        item = item % adapter.getActualItemCount();
        if (item >= adapter.getActualItemCount()) {
            mPendingPosition = item;
        } else {
            mPendingPosition = POSITION_NONE;
        }
        super.setCurrentItemInternal(adapter.convertToLoopPosition(item), false, true);
    }

    @Override
    public void setCurrentItem(int item) {
        setCurrentItem(item, BuildPlatform.isTV());
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (item < 0) {
            Log.w(TAG, "invalidate position:" + item);
            return;
        }
        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
        if (adapter == null || adapter.getActualItemCount() <= 0) {
            mPendingPosition = item;
            return;
        }

        item = item % adapter.getActualItemCount();
        if (item >= adapter.getActualItemCount()) {
            mPendingPosition = item;
        } else {
            mPendingPosition = POSITION_NONE;
        }

        super.setCurrentItem(adapter.convertToLoopPosition(item), smoothScroll);
    }

    @Override
    public int getCurrentItem() {
        int currentItem = super.getCurrentItem();
        return convertToRealPosition(currentItem);
    }

    private int convertToRealPosition(int position) {
        LoopPagerAdapter adapter = (LoopPagerAdapter) getAdapter();
        if (adapter == null || adapter.getActualItemCount() <= 0) {
            return position;
        }
        return adapter.convertToRealPosition(position);
    }

    @Override
    void dataSetChanged() {
        super.dataSetChanged();
        PagerAdapter adapter = getAdapter();
        if (adapter != null
                && mPendingPosition != POSITION_NONE
                && mPendingPosition < ((LoopPagerAdapter) adapter).getActualItemCount()) {
            setCurrentItem(mPendingPosition);
            mPendingPosition = POSITION_NONE;
        }
    }

    public void registerOnPageChangeCallback(@NonNull OnPageChangeCallback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterOnPageChangeCallback(@NonNull OnPageChangeCallback callback) {
        mCallbacks.remove(callback);
    }

    public interface OnPageChangeCallback {

        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        void onPageSelected(int position);

        void onPageScrollStateChanged(int state);
    }
}
