/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import org.hapjs.common.utils.DisplayUtil;

public class TitleLinearLayout extends LinearLayout {

    public static final int DEFAULT_MENUBAR_HEIGHT_SIZE = 26;
    public static final int DEFAULT_MENUBAR_WIDTH_SIZE = 78;
    private static final double MINIMUM_DISTANCE = 0.1;
    private static final String TAG = "TitleLinearLayout";
    private double mTouchSlop;
    private double mTouchMoveSlop;
    private boolean mIsIntercept = false;
    private int mlastX;
    private int mlastY;
    private float mdownX;
    private float mdownY;
    private boolean mIsInit = false;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mTitlebarHeight = FrameLayout.LayoutParams.WRAP_CONTENT;
    private int mTitlebarWidth = FrameLayout.LayoutParams.WRAP_CONTENT;
    private int mLastMoveLeft = -1;
    private int mLastMoveTop = -1;
    private boolean mIsNeedMove = true;
    private MenubarMoveListener mMenubarMoveListener = null;

    public TitleLinearLayout(Context context) {
        super(context);
        initData();
    }

    public TitleLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public TitleLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    public void setIsNeedMove(boolean isNeedMove) {
        this.mIsNeedMove = isNeedMove;
    }

    public void setMenubarMoveListener(MenubarMoveListener menubarMoveListener) {
        this.mMenubarMoveListener = menubarMoveListener;
    }

    private void initData() {
        mTitlebarHeight =
                (int) (DEFAULT_MENUBAR_HEIGHT_SIZE * getResources().getDisplayMetrics().density);
        mTitlebarWidth =
                (int) (DEFAULT_MENUBAR_WIDTH_SIZE * getResources().getDisplayMetrics().density);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mTouchMoveSlop = MINIMUM_DISTANCE;
        mScreenWidth = DisplayUtil.getScreenWidth(getContext());
        mScreenHeight = DisplayUtil.getScreenHeight(getContext());
    }

    public void refreshScreenWidthHeight(int orientation, int statusBarHeight) {
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                && (mScreenHeight > mScreenWidth)) {
            int tmpWidth = mScreenHeight;
            mScreenHeight = mScreenWidth - statusBarHeight;
            mScreenWidth = tmpWidth;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mIsNeedMove) {
            return super.onInterceptTouchEvent(event);
        }
        int ea = event.getAction();
        switch (ea) {
            case MotionEvent.ACTION_DOWN:
                mIsIntercept = false;
                mlastX = (int) event.getX();
                mlastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getX() - mlastX;
                int dy = (int) event.getY() - mlastY;
                if (!mIsIntercept) {
                    if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                        mIsIntercept = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsIntercept = false;
                mlastX = 0;
                mlastY = 0;
                break;
            default:
                break;
        }
        return mIsIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsNeedMove) {
            return super.onTouchEvent(event);
        }
        int ea = event.getAction();
        switch (ea) {
            case MotionEvent.ACTION_DOWN:
                mdownX = event.getX();
                mdownY = event.getY();
                mIsInit = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mIsInit) {
                    mdownX = event.getX();
                    mdownY = event.getY();
                    mIsInit = true;
                }
                final float xDistance = event.getX() - mdownX;
                final float yDistance = event.getY() - mdownY;
                if (Math.abs(xDistance) > mTouchMoveSlop || Math.abs(yDistance) > mTouchMoveSlop) {
                    int l = (int) (getLeft() + xDistance);
                    int r = (int) (getRight() + xDistance);
                    int t = (int) (getTop() + yDistance);
                    int b = (int) (getBottom() + yDistance);
                    boolean isMove = false;
                    if (l > 0 && r < mScreenWidth && t > 0 && b < mScreenHeight) {
                        mLastMoveLeft = l;
                        mLastMoveTop = t;
                        isMove = true;
                        layout(mLastMoveLeft, mLastMoveTop, r, b);
                    } else if (l > 0 && r < mScreenWidth) {
                        mLastMoveLeft = l;
                        mLastMoveTop = getTop();
                        isMove = true;
                        layout(mLastMoveLeft, mLastMoveTop, r, getBottom());
                    } else if (t > 0 && b < mScreenHeight) {
                        mLastMoveLeft = getLeft();
                        mLastMoveTop = t;
                        isMove = true;
                        layout(mLastMoveLeft, mLastMoveTop, getRight(), b);
                    }
                    if (null != mMenubarMoveListener && isMove) {
                        mMenubarMoveListener.onMenubarMove();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsInit = false;
                if (mLastMoveTop != -1 && mLastMoveLeft != -1) {
                    FrameLayout.LayoutParams menuViewParams =
                            new FrameLayout.LayoutParams(mTitlebarWidth, mTitlebarHeight);
                    menuViewParams.leftMargin = mLastMoveLeft;
                    menuViewParams.topMargin = mLastMoveTop;
                    setLayoutParams(menuViewParams);
                }
                break;
            default:
                break;
        }
        return true;
    }

    public interface MenubarMoveListener {
        void onMenubarMove();
    }
}
