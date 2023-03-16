/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.vdom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.common.utils.ViewUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.FloatingHelper;
import org.hapjs.component.SingleChoice;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.ScrollView;
import org.hapjs.model.AppInfo;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.MultiWindowManager;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.HapEngine;

public class DocComponent extends Container {
    public static final String ROOT_VIEW_ID = "root_view_id";
    protected static final String TAG = "DocComponent";
    protected DecorLayout mDecorLayout;
    protected AppInfo mAppInfo;
    protected boolean mOpenWithAnim;
    private int mPageId = -1;
    private volatile int mWebComponentCount;
    private Page mPage;
    private boolean mIsSecure;
    private Map<String, SingleChoice> mSingleChoices;
    private Map<String, Integer> mViewIds;
    private FloatingHelper mFloatingHelper;

    protected PageEnterListener mPageEnterListener;
    protected PageExitListener mPageExitListener;
    protected PageMoveListener mPageMoveListener;

    public DocComponent(
            HapEngine hapEngine,
            Context context,
            int pageId,
            RenderEventCallback jsCallback,
            ViewGroup view,
            AppInfo appInfo) {
        super(hapEngine, context, null, VElement.ID_DOC, jsCallback, null);
        mPageId = pageId;
        mHost = view;
        mAppInfo = appInfo;

        RootView rootView = (RootView) mHost;
        mPage = rootView.getPageManager().getPageById(mPageId);
        Page page = rootView.getPageManager().getPageById(mPageId);
        mDecorLayout = new DecorLayout(mContext, page, rootView);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        ViewUtils.fitParentLayoutParams(lp, rootView);
        ViewUtils.fitMultiModeLayoutParams(mContext, lp);
        mDecorLayout.setLayoutParams(lp);
        mCallback.addActivityStateListener(this);
    }

    public boolean hasWebComponent() {
        return mWebComponentCount > 0;
    }

    public void increaseWebComponentCount() {
        this.mWebComponentCount++;
    }

    public void attachChildren(boolean open, int animType, MultiWindowManager.MultiWindowPageChangeExtraInfo extraInfo, PageEnterListener pageEnterListener) {
        mPageEnterListener = pageEnterListener;
        removeDecorLayout();

        mOpenWithAnim = open && animType > DocAnimator.TYPE_UNDEFINED;

        if (extraInfo != null && extraInfo.isMultiWindowMode()) {
            if (extraInfo.isShoppingMode()) {
                ((ViewGroup) mHost).addView(mDecorLayout, 0);
            } else if (extraInfo.isNavigationMode()) {
                if (open) {
                    if (extraInfo.isOpenFirstPage()) {
                        ((ViewGroup) mHost).addView(mDecorLayout);
                    } else if (extraInfo.isOpenSecondPage()) {
                        ((ViewGroup) mHost).addView(mDecorLayout, 0);
                    } else {
                        ((ViewGroup) mHost).addView(mDecorLayout, (((ViewGroup) mHost).getChildCount() - 2));
                    }
                } else {
                    ((ViewGroup) mHost).addView(mDecorLayout, 0);
                }
            }
        } else {
            if (open) {
                ((ViewGroup) mHost).addView(mDecorLayout);
            } else {
                ((ViewGroup) mHost).addView(mDecorLayout, 0);
            }
        }

        executePageEnterStart();

        if (animType > DocAnimator.TYPE_UNDEFINED) {
            DocAnimator animator = new DocAnimator(mContext, mDecorLayout, animType);
            animator.setListener(new AttachAnimatorListener());
            animator.start();
            if (mPageEnterListener != null) {
                mPageEnterListener.onEnd();
            }
        } else {
            mDecorLayout.setAlpha(1);
            if (extraInfo != null && extraInfo.isMultiWindowMode()) {
                mDecorLayout.setTranslationX(extraInfo.getTranslationXWithNoAnim(mContext));
            } else {
                mDecorLayout.setX(0);
            }
            executePageEnterEnd();
        }

        mDecorLayout.clearDisplay();
        mDecorLayout.setupDisplay();
    }

    public void detachChildren(int animType, PageExitListener pageExitListener, boolean open) {
        mPageExitListener = pageExitListener;

        if (mDecorLayout.getFocusedChild() != null) {
            mDecorLayout.clearFocus();
        }
        executePageExitStart();
        if (animType > DocAnimator.TYPE_UNDEFINED) {
            DocAnimator animator = new DocAnimator(mContext, mDecorLayout, animType);
            animator.setListener(new DetachAnimatorListener(open));
            animator.start();
        } else {
            executePageExitEnd(open);
        }
    }

    public void moveChildren(int animType, PageMoveListener pageMoveListener) {
        mPageMoveListener = pageMoveListener;

        mDecorLayout.bringToFront();

        executePageMoveStart();
        if (animType > DocAnimator.TYPE_UNDEFINED) {
            DocAnimator animator = new DocAnimator(mContext, mDecorLayout, animType);
            animator.setListener(new MoveAnimatorListener());
            animator.start();
        } else {
            executePageMoveEnd();
        }
    }

    public void updateTitleBar(Map<String, Object> titles, int pageId) {
        RootView rootView = (RootView) mHost;
        Page page = rootView.getCurrentPage();
        if (mAppInfo.getDisplayInfo() != null && page != null && page.pageId == pageId) {
            mDecorLayout.updateTitleBar(titles, pageId);
        }
    }

    public void enterFullscreen(
            Component component, int screenOrientation, boolean fullScreenContainer) {
        enterFullscreen(component, screenOrientation, false, fullScreenContainer);
    }

    public void enterFullscreen(
            Component component,
            int screenOrientation,
            boolean showStatusBar,
            boolean fullScreenContainer) {
        mDecorLayout
                .enterFullscreen(component, screenOrientation, showStatusBar, fullScreenContainer);
    }

    public void exitFullscreen() {
        mDecorLayout.exitFullscreen();
    }

    public void setLightStatusBar(boolean lightStatusBar) {
        mDecorLayout.setLightStatusBar(lightStatusBar);
    }

    public void resetStatusBar() {
        mDecorLayout.resetStatusBar();
    }

    public void setSecure(int pageId, boolean isSecure) {
        RootView rootView = (RootView) mHost;
        Page page = rootView.getCurrentPage();
        if (page != null && page.pageId == pageId && mIsSecure != isSecure) {
            mIsSecure = isSecure;
            setSecure();
        }
    }

    private void setSecure() {
        Window window = ((Activity) mContext).getWindow();
        if (mIsSecure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        mDecorLayout.onActivityResume();
    }

    public void updateStatusBar(Map<String, Object> titles, int pageId) {
        RootView rootView = (RootView) mHost;
        Page page = rootView.getCurrentPage();
        if (mAppInfo.getDisplayInfo() != null && page != null && page.pageId == pageId) {
            mDecorLayout.updateStatusBar(titles, pageId);
        }
    }

    public void scrollPage(HapEngine hapEngine, Map<String, Object> scrolls, int pageId) {
        RootView rootView = (RootView) mHost;
        Page page = rootView.getCurrentPage();
        if (mAppInfo.getDisplayInfo() != null && page != null && page.pageId == pageId) {
            mDecorLayout.scrollPage(hapEngine, scrolls, pageId);
        }
    }

    @Override
    protected View createViewImpl() {
        return mHost;
    }

    @Override
    public void addView(View childView, int index) {
        ViewGroup viewGroup = getInnerView();

        if (viewGroup == null || childView == null) {
            return;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) childView.getLayoutParams();
        if (lp == null) {
            lp =
                    new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Rect insets = mDecorLayout.getContentInsets();
        lp.topMargin += insets.top;
        lp.leftMargin += insets.left;
        lp.bottomMargin += insets.bottom;
        lp.rightMargin += insets.right;

        viewGroup.addView(childView, lp);

        if (childView instanceof ScrollView) {
            childView.setAlpha(0);
            childView.animate().setDuration(200).alpha(1.0f).start();
        }
    }

    @Override
    public ViewGroup getInnerView() {
        return mDecorLayout;
    }

    protected void removeDecorLayout() {
        mDecorLayout.setIsDetachAnimation(false);
        ViewGroup parent = (ViewGroup) mHost;
        int index = parent.indexOfChild(mDecorLayout);
        if (index >= 0) {
            parent.removeViewAt(index);
            if (mPageExitListener != null) {
                mPageExitListener.onEnd();
            }
        }
        mDecorLayout.setVisibility(View.VISIBLE);
    }

    public interface PageEnterListener {
        void onStart();

        void onEnd();
    }

    public interface PageExitListener {
        void onStart();

        void onEnd();
    }

    public interface PageMoveListener {
        void onStart();

        void onEnd();
    }

    public class AttachAnimatorListener extends AnimatorListenerAdapter {

        @Override
        public void onAnimationStart(Animator animation) {
            mDecorLayout.setIsAttachAnimation(true);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mOpenWithAnim) {
                mOpenWithAnim = false;
            }

            mDecorLayout.setIsAttachAnimation(false);
        }
    }

    public class DetachAnimatorListener extends AnimatorListenerAdapter {
        boolean open;

        DetachAnimatorListener(boolean open) {
            this.open = open;
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(final Animator animation) {
            executePageExitEnd(open);
        }
    }

    public class MoveAnimatorListener extends AnimatorListenerAdapter {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            executePageMoveEnd();
        }
    }

    public void executePageEnterStart() {
        if (mPageEnterListener != null) {
            mPageEnterListener.onStart();
        }

        setSecure();
    }

    public void executePageEnterEnd() {
        if (mOpenWithAnim) {
            mOpenWithAnim = false;
        }

        mDecorLayout.setIsAttachAnimation(false);

        if (mPageEnterListener != null) {
            mPageEnterListener.onEnd();
        }
    }

    public void executePageExitStart() {
        if (mPageExitListener != null) {
            mPageExitListener.onStart();
        }
        mDecorLayout.setIsDetachAnimation(true);
        InputMethodManager imm =
                (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mHost.getWindowToken(), 0);
        }
    }

    public void executePageExitEnd(boolean open) {
        if (mDecorLayout.isDetachAnimation()) {
            mDecorLayout.setVisibility(View.GONE);
            if (mHost.getHandler() != null) {
                mHost
                        .getHandler()
                        .postAtFrontOfQueue(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mDecorLayout.isDetachAnimation()) {
                                            removeDecorLayout();
                                        }
                                    }
                                });
            }
        }
    }

    public void executePageMoveStart() {
        mPageMoveListener.onStart();
    }

    public void executePageMoveEnd() {
        mPageMoveListener.onEnd();
    }

    @Override
    public int getPageId() {
        return mPageId;
    }

    @Override
    public Page getPage() {
        return mPage;
    }

    public AppInfo getAppInfo() {
        return mAppInfo;
    }

    public boolean isCardMode() {
        return HapEngine.getInstance(mAppInfo.getPackage()).isCardMode();
    }

    public void handleSingleChoice(String key, SingleChoice singleChoice) {
        if (mSingleChoices == null) {
            mSingleChoices = new HashMap<>();
        }

        SingleChoice lastSingleChoice = mSingleChoices.get(key);
        if (lastSingleChoice == null) {
            mSingleChoices.put(key, singleChoice);
            return;
        }

        if (singleChoice != lastSingleChoice) {
            lastSingleChoice.setChecked(false);
            mSingleChoices.put(key, singleChoice);
        }
    }

    public FloatingHelper getFloatingHelper() {
        if (mFloatingHelper == null) {
            mFloatingHelper = new FloatingHelper();
        }
        return mFloatingHelper;
    }

    public boolean isOpenWithAnimation() {
        return mOpenWithAnim;
    }

    public void showProgress() {
        mDecorLayout.showProgress();
    }

    public void hideProgress() {
        mDecorLayout.hideProgress();
    }

    @Override
    public void destroy() {
        super.destroy();
        hideProgress();
        mPage = null;
        mCallback.removeActivityStateListener(this);
    }

    public int getViewId(String key) {
        if (!TextUtils.isEmpty(key) && mViewIds != null && !mViewIds.isEmpty()) {
            Integer id = mViewIds.get(key);
            return id != null ? id : View.NO_ID;
        }
        return View.NO_ID;
    }

    public void setViewId(String key, int id) {
        if (!TextUtils.isEmpty(key)) {
            if (mViewIds == null) {
                mViewIds = new HashMap<>();
            }
            mViewIds.put(key, id);
        }
    }

    public DecorLayout getDecorLayout() {
        return mDecorLayout;
    }
}
