/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import java.util.Map;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.component.Component;
import org.hapjs.component.view.ScrollView;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.R;

public class DecorLayout extends RelativeLayout {
    private boolean mIsAttachAnimation;
    private boolean mIsDetachAnimation;

    private Display mDisplay;
    private int mMenubarIndex = -1;
    private boolean mDarkMode;

    private boolean mInGrayMode = false;

    public DecorLayout(Context context, Page page, RootView rootView) {
        super(context);
        mDisplay = new Display(this, ((Activity) context).getWindow(), page, rootView);

        if (!BuildPlatform.isTV()) {
            setFocusableInTouchMode(true);
        }

        if (!GrayModeManager.getInstance().isPageExclude(page.getPath())
                && GrayModeManager.getInstance().shouldApplyGrayMode()) {
            applyGrayMode(true);
        }
    }

    public void applyGrayMode(boolean toggle) {
        if (toggle == mInGrayMode) {
            return;
        }
        mInGrayMode = GrayModeManager.getInstance().applyGrayMode(this, toggle);
    }

    public boolean isDarkMode() {
        return mDarkMode;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDisplay.onAttachedFromWindow();
        mDarkMode = DarkThemeUtil.isDarkMode(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDisplay.onDetachedFromWindow();
        mDarkMode = DarkThemeUtil.isDarkMode(getContext());
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (null != mDisplay && mDisplay.isShowMenubar()) {
            if (child instanceof ScrollView) {
                View titlebarView = mDisplay.addMenubarView();
                if (null != titlebarView) {
                    mMenubarIndex = indexOfChild(titlebarView);
                }
            }
        }
        ensureTopOrder();
    }

    public void bringChildToTop(View child) {
        if (child != null) {
            bringChildToFront(child);
        }
        ensureTopOrder();
    }

    private void ensureTopOrder() {
        // DecorLayout顶层视图顺序：MenuBar > 骨架屏 > ...
        View skeleton = findViewById(R.id.skeleton);
        if (skeleton != null) {
            if (indexOfChild(skeleton) < getChildCount() - 1) {
                bringChildToFront(skeleton);
            }
        }
        if (null != mDisplay && mDisplay.isShowMenubar()) {
            View menubarView = mDisplay.addMenubarView();
            if (mMenubarIndex != -1
                    && menubarView != null
                    && indexOfChild(menubarView) < getChildCount() - 1) {
                mDisplay.bringFrontTitlebarView();
            }
        }
    }

    public Display getDecorLayoutDisPlay() {
        return mDisplay;
    }

    public void setupDisplay() {
        mDisplay.setup();
    }

    public void clearDisplay() {
        mDisplay.clear();
    }

    public void updateTitleBar(Map<String, Object> titles, int pageId) {
        mDisplay.updateTitleBar(titles, pageId);
    }

    public boolean enterFullscreen(
            Component component,
            int screenOrientation,
            boolean showStatusBar,
            boolean fullScreenContainer) {
        return mDisplay.enterFullscreen(
                component, screenOrientation, showStatusBar, fullScreenContainer);
    }

    public boolean exitFullscreen() {
        return mDisplay.exitFullscreen();
    }

    public void setLightStatusBar(boolean lightStatusBar) {
        mDisplay.setLightStatusBar(lightStatusBar);
    }

    public void resetStatusBar() {
        mDisplay.setupStatusBar();
    }

    public void onActivityResume() {
        mDisplay.onActivityResume();
    }

    public void updateStatusBar(Map<String, Object> titles, int pageId) {
        mDisplay.updateStatusBar(titles, pageId);
    }

    public void scrollPage(HapEngine hapEngine, Map<String, Object> scrolls, int pageId) {
        mDisplay.scrollPage(hapEngine, scrolls, pageId);
    }

    public void showProgress() {
        mDisplay.showProgress();
    }

    public void hideProgress() {
        mDisplay.hideProgress();
    }

    public Rect getContentInsets() {
        return mDisplay.getContentInsets();
    }

    public int getStatusBarHeight() {
        return mDisplay.getStatusBarHeight();
    }

    public int getTitleHeight() {
        return mDisplay.getTitleHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthHint = MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = MeasureSpec.getSize(heightMeasureSpec);
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View view = getChildAt(i);
            if (!(view.getLayoutParams() instanceof LayoutParams)) {
                continue;
            }
            LayoutParams lp = (LayoutParams) view.getLayoutParams();

            float percentWidth = lp.percentWidth;
            if (percentWidth >= 0) {
                lp.width = Math.round(widthHint * percentWidth);
            }

            float percentHeight = lp.percentHeight;
            if (percentHeight >= 0) {
                lp.height = Math.round(heightHint * percentHeight);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isTransitionAnimation()) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    public void setIsAttachAnimation(boolean isAttachAnimation) {
        mIsAttachAnimation = isAttachAnimation;
    }

    public void setIsDetachAnimation(boolean isDetachAnimation) {
        mIsDetachAnimation = isDetachAnimation;
    }

    public boolean isDetachAnimation() {
        return mIsDetachAnimation;
    }

    private boolean isTransitionAnimation() {
        return mIsAttachAnimation || mIsDetachAnimation;
    }

    public static class LayoutParams extends RelativeLayout.LayoutParams {

        public float percentWidth = -1;
        public float percentHeight = -1;

        private ViewGroup.LayoutParams mSource;

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            mSource = source;
        }

        public LayoutParams(int w, int h, ViewGroup.LayoutParams source) {
            super(w, h);
            mSource = source;
        }

        public ViewGroup.LayoutParams getSourceLayoutParams() {
            return mSource;
        }
    }
}
