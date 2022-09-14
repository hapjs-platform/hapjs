/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.render.DecorLayout;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.ResourceConfig;

public class FullscreenHelper implements ConfigurationManager.ConfigurationListener {

    private static final String TAG = "FullscreenHelper";
    private Component mComponent;
    private PlaceholderView mPlaceHolderView;
    private int mOriginScreenOrientation;

    private DecorLayout mDecorLayout;
    private boolean mFullScreenContainerMode;
    private int mOriginWidth;
    private int mOriginHeight;
    private int mRootDescendantFocus;

    public FullscreenHelper(DecorLayout decorLayout) {
        mDecorLayout = decorLayout;
    }

    public boolean enterFullscreen(
            Context context,
            Component component,
            int screenOrientation,
            boolean showStatusBar,
            boolean fullScreenContainer) {
        if (mComponent != null || component == null || component.getHostView() == null) {
            return false;
        }
        mComponent = component;
        int rootDescendantFocus = mDecorLayout.getDescendantFocusability();
        mDecorLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        Container parent = (Container) mComponent.getParent();
        if (parent == null) {
            return false;
        }

        if (!fullScreenContainer) {
            mFullScreenContainerMode = false;
            View hostView = component.getHostView();
            if (mPlaceHolderView == null) {
                mPlaceHolderView = new PlaceholderView(context);
            }
            int index = parent.getChildren().indexOf(mComponent);
            if (index >= 0) {
                int offsetIndex = parent.offsetIndex(index);
                parent.removeView(hostView);
                parent.addView(mPlaceHolderView, offsetIndex);
            } else {
                Log.e(TAG, "enterFullScreen: index of component smaller than 0");
                return false;
            }
            // video 是 fixed的情况, hostView 已经被加入到 DecorLayout 中, 需要先移除
            if (hostView.getParent() != null) {
                ViewGroup parentView = (ViewGroup) hostView.getParent();
                parentView.removeView(hostView);
            }

            mOriginScreenOrientation = ((Activity) context).getRequestedOrientation();
            ((Activity) context).setRequestedOrientation(screenOrientation);
            View fullScreenView = mComponent.getFullScreenView();
            DecorLayout.LayoutParams lp = new DecorLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, fullScreenView.getLayoutParams());
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            mDecorLayout.addView(fullScreenView, lp);

            // restore descendant focus
            mDecorLayout.setDescendantFocusability(rootDescendantFocus);
            setFullscreenVisibility(mDecorLayout, !showStatusBar);

            if (hostView instanceof ComponentHost) {
                ((ComponentHost) hostView).getComponent().onFullscreenChange(true);
            }
        } else {
            // 弹幕模式
            mFullScreenContainerMode = true;
            mOriginScreenOrientation = ((Activity) context).getRequestedOrientation();
            ((Activity) context).setRequestedOrientation(screenOrientation);

            if (mPlaceHolderView == null) {
                mPlaceHolderView = new PlaceholderView(context);
            }

            Container grandParent = parent.getParent();
            int index = grandParent.getChildren().indexOf(parent);
            // 在祖父中移除视频的父view
            if (index >= 0) {
                int offsetIndex = grandParent.offsetIndex(index);
                grandParent.removeView(parent.getHostView());
                grandParent.addView(mPlaceHolderView, offsetIndex);
                mDecorLayout.addView(parent.getHostView(), mDecorLayout.getLayoutParams());
            } else {
                return false;
            }
            mOriginWidth = mComponent.getWidth();
            mOriginHeight = mComponent.getHeight();
            mComponent.setWidth(String.valueOf(parent.getWidth()));
            mComponent.setHeight(String.valueOf(parent.getHeight()));
            mDecorLayout.setDescendantFocusability(rootDescendantFocus);

            setFullscreenVisibility(mDecorLayout, !showStatusBar);

            if (mComponent.getHostView() instanceof ComponentHost) {
                ((ComponentHost) mComponent.getHostView()).getComponent().onFullscreenChange(true);
            }
        }

        return true;
    }

    public boolean exitFullscreen(Context context) {
         if (mComponent == null || mComponent.getHostView() == null || mComponent.getParent() == null) {
            return false;
        }

        View hostView = mComponent.getHostView();
        // 避免layout过程中DecorLayout移除hostView
        if (hostView.isInLayout()) {
            hostView.post(() -> exitFullscreen(context));
            return true;
        }

        // Current is a focused view,while parent view is ScrollView,
        // it will be call requestFocus,and then scroll current view to the top,
        // it can't restore right position when exit fullscreen.
        mRootDescendantFocus = mDecorLayout.getDescendantFocusability();
        mDecorLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        if (!mFullScreenContainerMode) {
            View fullScreenView = mComponent.getFullScreenView();
            mDecorLayout.removeView(fullScreenView);
        } else {
            Container parent = mComponent.getParent();
            mDecorLayout.removeView(parent.getHostView());
        }
        int currentOrientation = ((Activity) context).getRequestedOrientation();

        if (currentOrientation == mOriginScreenOrientation) {
            exitFullScreenImp();
            return true;
        }

        /** 1.单页面和卡片原始方向未指定时，如果此时用户屏幕非处于竖屏，并不会立即旋转，此时直接还原View树结构 2.电视时直接返回 */
        if ((!ResourceConfig.getInstance().isLoadFromLocal()
                && mOriginScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                || BuildPlatform.isTV()) {
            ((Activity) context).setRequestedOrientation(mOriginScreenOrientation);
            exitFullScreenImp();
            return true;
        }

        ((Activity) context).setRequestedOrientation(mOriginScreenOrientation);
        setFullscreenVisibility(mDecorLayout, false);
        if (!ConfigurationManager.getInstance().contains(this)) {
            ConfigurationManager.getInstance().addListener(this);
        }
        return true;
    }

    public void onActivityResume() {
        if (mComponent != null && mDecorLayout != null) {
            setFullscreenVisibility(mDecorLayout, true);
        }
    }

    private void setFullscreenVisibility(View view, boolean fullscreen) {
        if (fullscreen) {
            view.setSystemUiVisibility(
                    view.getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            view.setSystemUiVisibility(
                    view.getSystemUiVisibility()
                            & (~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                            & (~View.SYSTEM_UI_FLAG_FULLSCREEN)
                            & (~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY));
        }
    }

    @Override
    public void onConfigurationChanged(HapConfiguration newConfig) {
        if (newConfig.getOrientation() != newConfig.getLastOrientation()) {
            exitFullScreenImp();
            ConfigurationManager.getInstance().removeListener(this);
        }
    }

    private void exitFullScreenImp() {
        if (mComponent == null || mComponent.getParent() == null) {
            return;
        }
        Container parent = mComponent.getParent();
        Container grandParent = parent.getParent();

        // 退出全屏时, 可能由于子组件状态和数量的动态变化, 导致 origin view 在父布局中的
        // index 产生变化, 因此此处不能使用 mOriginIndex, 而是通过组件在父组件中的 index 过滤
        // 掉被移除的, 以及 fixed 和 floating 组件后进行偏移得到的 offsetIndex
        if (!mFullScreenContainerMode) {
            parent.removeView(mPlaceHolderView);
            int index = parent.getChildren().indexOf(mComponent);
            if (index >= 0) {
                int offsetIndex = parent.offsetIndex(index);
                if (mComponent.getHostView().getParent() != null) {
                    ViewGroup viewGroup = (ViewGroup) mComponent.getHostView().getParent();
                    viewGroup.removeView(mComponent.getHostView());
                }
                parent.addView(mComponent.getHostView(), offsetIndex);
            } else {
                Log.e(TAG, "exitFullscreen: index of component smaller than 0");
            }
        } else {
            mComponent.setWidth(String.valueOf(mOriginWidth));
            mComponent.setHeight(String.valueOf(mOriginHeight));
            grandParent.removeView(mPlaceHolderView);
            int grandIndex = grandParent.getChildren().indexOf(parent);
            if (grandIndex >= 0) {
                if (parent.getHostView().getParent() != null) {
                    ViewGroup viewGroup = (ViewGroup) parent.getHostView().getParent();
                    viewGroup.removeView(parent.getHostView());
                }
                grandParent.addView(parent.getHostView(), grandParent.offsetIndex(grandIndex));
            } else {
                Log.e(TAG, "exitFullscreen: index of component smaller than 0");
            }
        }

        // restore descendant focus
        mDecorLayout.setDescendantFocusability(mRootDescendantFocus);
        setFullscreenVisibility(mDecorLayout, false);
        if (mComponent.getRootComponent() != null) {
            mComponent.getRootComponent().resetStatusBar();
        }

        if (mComponent.getHostView() instanceof ComponentHost) {
            ((ComponentHost) mComponent.getHostView()).getComponent().onFullscreenChange(false);
        }

        mComponent = null;
    }

    public class PlaceholderView extends View {
        public PlaceholderView(Context context) {
            super(context);
        }
    }
}
