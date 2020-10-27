/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.model.MenubarItemData;
import org.hapjs.render.Display;
import org.hapjs.render.RootView;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RuntimeActivity;

public class MenubarView extends FrameLayout {
    public static final int DEFAULT_MENUBAR_RIGHT_MARGIN = 16;
    public static final int DEFAULT_MENUBAR_BOTTOM_TIPS_RIGHT_MARGIN = 26;
    public static final int DEFAULT_MENUBAR_BOTTOM_TIPS_HEIGHT = 36;
    public static final int DEFAULT_MENUBAR_TOP_TIPS_ARROW_HEIGHT = 8;
    public static final int DEFAULT_MENUBAR_TOP_TIPS_ARROW_WIDTH = 14;
    public static final int DEFAULT_MENUBAR_TOP_MARGIN = 8;
    public static final int DEFAULT_MENUBAR_TIPS_MOVE_MARGIN = 1;
    public static final int MENUBAR_TIPS_SHOW_TIME_DURATION = 5000;
    public static final int MENUBAR_PAGE_TIPS_SHOW_TIME_DURATION = 10 * 1000;
    public static final String MENUBAR_DIALOG_RPK_ICON = "RPK_ICON";
    public static final String MENUBAR_DIALOG_SHOW_ABOUT_ICON = "SHOW_ABOUT_ICON";
    public static final String MENUBAR_DIALOG_MENU_STATUS = "MENU_STATUS";
    public static final String MENUBAR_DIALOG_RPK_NAME = "RPK_NAME";
    public static final String MENUBAR_DIALOG_SHARE_PREFERENCE_NAME = "menubar_prefs";
    public static final String MENUBAR_DIALOG_SHOW_TIPS_KEY = "MENUBAR_TIPS_SHOW";
    public static final String MENUBAR_DIALOG_SHARE_IMAGE_NAME = "menubar_share_img";
    public static final String MENUBAR_DIALOG_SHORTCUT_IMAGE_NAME = "menubar_shortcut_img";
    public static final String MENUBAR_DIALOG_HOME_IMAGE_NAME = "menubar_home_img";
    public static final String MENUBAR_POINT_EVER_SAVE = "menubar_point_ever_save";
    public static final String MENUBAR_COLLECT = "menubar_collect";
    public static final String MENUBAR_HOME = "menubar_home";
    public static final String MENUBAR_POINT_MENU_STATUS = "menubar_point_menu_status";
    public static final String MENUBAR_POINT_MENU_EVER_SAVE = "menubar_point_menu_ever_save";
    public static final int MENUBAR_ABOUT_PAGE_VALID_1080_DESIGNWIDTH = 1080;
    public static final int MENUBAR_ABOUT_PAGE_VALID_750_DESIGNWIDTH = 750;
    public static final int TITLEBAR_STYLE_DARK = 1;
    public static final int TITLEBAR_STYLE_LIGHT = 2;
    private static final String TAG = "TitlebarView";
    private View mTitlebarView;
    private LinearLayout mLeftMenuLayout;
    private ImageView mLeftImageView;
    private View mMiddleViewContainer;
    private View mMiddleView;
    private LinearLayout mRightCloseLayout;
    private ImageView mRightImageView;
    private int mCurStyle = -1;
    private int mCurMenuStatus = Display.DISPLAY_STATUS_FINISH;
    private Runnable mCurRunTask = null;
    private int mAnimationCount = 0;
    private boolean mIsAnimation = false;
    private OnClickListener mOnLeftMenuClickListener;
    private OnClickListener mOnRightCloseClickListener;
    private boolean mIsNeedMove = true;
    private String mRpkName = "";
    private BaseTitleDialog mBaseTitleDialog = null;
    private Animation mHideAnimation = null;
    private Animation mShowAnimation = null;
    private LifecycleListener mLifecycleListener;
    private MenubarLifeCycleCallback mMenubarLifeCycleCallback;
    private boolean mIsMenubarTypeWeb = true;

    public MenubarView(Context context, String rpkName) {
        super(context);
        initTitlebarView(context);
        mRpkName = rpkName;
    }

    public MenubarView(
            Context context, String rpkName, MenubarLifeCycleCallback menubarLifeCycleCallback) {
        super(context);
        mIsMenubarTypeWeb = false;
        this.mMenubarLifeCycleCallback = menubarLifeCycleCallback;
        initTitlebarView(context);
        mRpkName = rpkName;
    }

    public MenubarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mIsNeedMove = false;
        initTitlebarView(context);
    }

    public MenubarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTitlebarView(context);
    }

    public void setIsNeedMove(boolean isNeedMove) {
        this.mIsNeedMove = isNeedMove;
        if (!isNeedMove && null != mTitlebarView) {
            View titleLinearLayout = mTitlebarView.findViewById(R.id.titlebarview);
            if (titleLinearLayout instanceof TitleLinearLayout) {
                ((TitleLinearLayout) titleLinearLayout).setIsNeedMove(false);
            }
        }
    }

    public void setRpkName(String rpkName) {
        this.mRpkName = rpkName;
    }

    private void initTitlebarView(Context context) {
        mTitlebarView = LayoutInflater.from(context).inflate(R.layout.titlebar_view, null);
        View titleLinearLayout = mTitlebarView.findViewById(R.id.titlebarview);
        if (!mIsNeedMove && titleLinearLayout instanceof TitleLinearLayout) {
            ((TitleLinearLayout) titleLinearLayout).setIsNeedMove(false);
        }
        mLeftMenuLayout = mTitlebarView.findViewById(R.id.left_menu_layout);
        mLeftImageView = mTitlebarView.findViewById(R.id.left_menu_iv);
        mMiddleViewContainer = mTitlebarView.findViewById(R.id.middle_view_container);
        mMiddleView = mTitlebarView.findViewById(R.id.middle_view);
        mRightCloseLayout = mTitlebarView.findViewById(R.id.right_close_layout);
        mRightImageView = mTitlebarView.findViewById(R.id.right_menu_iv);
        addView(mTitlebarView);
        mCurMenuStatus = Display.DISPLAY_STATUS_FINISH;
        mHideAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.menu_hide_animation);
        mShowAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.menu_show_animation);
        updateTitlebarStyle(TITLEBAR_STYLE_DARK);
        initDialog();
    }

    public HybridManager getHybridManager() {
        Context context = getContext();
        if (!(context instanceof RuntimeActivity)) {
            Log.e(TAG,
                    "initLifecycleListener error: context is not an instance of RuntimeActivity.");
            return null;
        }
        final RuntimeActivity act = (RuntimeActivity) context;
        HybridView hybridView = act.getHybridView();
        HybridManager tmpHybridManager = null;
        if (null != hybridView) {
            tmpHybridManager = hybridView.getHybridManager();
        }
        if (null == tmpHybridManager) {
            Log.e(TAG, "initLifecycleListener error hybridManager is null.");
            return null;
        }
        return tmpHybridManager;
    }

    public void initLifecycleListener() {
        if (null == this.mMenubarLifeCycleCallback) {
            return;
        }
        final HybridManager hybridManager = getHybridManager();
        mLifecycleListener =
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        if (null != hybridManager) {
                            hybridManager.removeLifecycleListener(this);
                        } else {
                            Log.e(TAG, "initLifecycleListener onDestroy error hybridManager null.");
                        }
                    }

                    @Override
                    public void onResume() {
                        super.onResume();
                        if (null != mMenubarLifeCycleCallback) {
                            mMenubarLifeCycleCallback.onActivityResume();
                        }
                    }

                    @Override
                    public void onPause() {
                        super.onPause();
                        if (null != mMenubarLifeCycleCallback) {
                            mMenubarLifeCycleCallback.onActivityPause();
                        }
                    }
                };
        if (null != hybridManager) {
            hybridManager.addLifecycleListener(mLifecycleListener);
        }
    }

    public void removeLifecycleListener() {
        if (null == this.mMenubarLifeCycleCallback) {
            return;
        }
        final HybridManager hybridManager = getHybridManager();
        if (null != hybridManager && null != mLifecycleListener) {
            hybridManager.removeLifecycleListener(mLifecycleListener);
        }
    }

    public void setOnLeftClickListener(OnClickListener onClickListener) {
        mOnLeftMenuClickListener = onClickListener;
        mLeftMenuLayout.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mOnLeftMenuClickListener) {
                            mOnLeftMenuClickListener.onClick(v);
                        }
                    }
                });
    }

    public void setOnRightClickListener(OnClickListener onClickListener) {
        mOnRightCloseClickListener = onClickListener;
        mRightCloseLayout.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mOnRightCloseClickListener) {
                            mOnRightCloseClickListener.onClick(v);
                        }
                    }
                });
    }

    public void updateTitlebarStyle(int titlebarType) {
        if (mCurStyle == titlebarType) {
            Log.w(
                    TAG, "updateTitlebarStyle titlebarType : " + titlebarType + " mCurStyle : "
                            + mCurStyle);
            return;
        }
        mCurStyle = titlebarType;
        if (titlebarType == TITLEBAR_STYLE_LIGHT) {
            mLeftMenuLayout.setBackground(
                    getContext().getResources()
                            .getDrawable(R.drawable.titlebar_bg_left_click_selector));
            mMiddleViewContainer.setBackgroundColor(
                    getContext().getResources().getColor(R.color.titlebar_bg));
            mRightCloseLayout.setBackground(
                    getContext().getResources()
                            .getDrawable(R.drawable.titlebar_bg_right_click_selector));
            mLeftImageView.setImageDrawable(
                    getContext().getResources().getDrawable(R.drawable.menu_dot_light));
            mRightImageView.setImageDrawable(
                    getContext().getResources().getDrawable(R.drawable.menu_close_light));
        } else {
            mLeftMenuLayout.setBackground(
                    getContext()
                            .getResources()
                            .getDrawable(R.drawable.titlebar_bg_left_light_click_selector));
            mMiddleViewContainer.setBackgroundColor(
                    getContext().getResources().getColor(R.color.titlebar_bg_light));
            mRightCloseLayout.setBackground(
                    getContext()
                            .getResources()
                            .getDrawable(R.drawable.titlebar_bg_right_light_click_selector));
            mLeftImageView
                    .setImageDrawable(getContext().getResources().getDrawable(R.drawable.menu_dot));
            mRightImageView.setImageDrawable(
                    getContext().getResources().getDrawable(R.drawable.menu_close));
        }
    }

    public void updateLeftMenubg(int menustatus) {
        if (menustatus == mCurMenuStatus) {
            Log.w(
                    TAG,
                    "updateLeftMenubg menustatus : " + menustatus + " mCurMenuStatus : "
                            + mCurMenuStatus);

            return;
        }
        if (mIsAnimation && menustatus == Display.DISPLAY_STATUS_FINISH) {
            mCurMenuStatus = menustatus;
            Log.w(
                    TAG,
                    "updateLeftMenubg menustatus : "
                            + menustatus
                            + " mCurMenuStatus : "
                            + mCurMenuStatus
                            + " mIsAnimation : "
                            + mIsAnimation);
            return;
        }
        changeMenuStatus(menustatus);
        refreshMenuIcon();
    }

    private void changeMenuStatus(int menustatus) {
        mCurMenuStatus = menustatus;
        if (mCurStyle == TITLEBAR_STYLE_LIGHT) {
            if (mCurMenuStatus == Display.DISPLAY_LOCATION_START) {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_location_light));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }

            } else if (mCurMenuStatus == Display.DISPLAY_RECORD_START) {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_voice_light));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }
            } else {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_dot_light));
                mRightImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_close_light));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }
            }
        } else {
            if (mCurMenuStatus == Display.DISPLAY_LOCATION_START) {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_location));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }
            } else if (mCurMenuStatus == Display.DISPLAY_RECORD_START) {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_voice));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }
            } else {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_dot));
                mRightImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_close));
                if (null != mBaseTitleDialog) {
                    mBaseTitleDialog.notifyDialogStatus(mCurMenuStatus);
                }
            }
        }
    }

    private void refreshMenuIcon() {
        if (!mIsAnimation) {
            mCurRunTask =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCurMenuStatus != Display.DISPLAY_STATUS_FINISH) {
                                if (mAnimationCount == 0) {
                                    startHideMenuAnimation(mLeftImageView,
                                            R.anim.menu_hide_animation);
                                } else {
                                    startMenuShowAnimation(mLeftImageView,
                                            R.anim.menu_show_animation);
                                }
                            } else {
                                mAnimationCount = 0;
                                mIsAnimation = false;
                                resetStatus();
                            }
                        }
                    };
            post(mCurRunTask);
        }
    }

    private void startHideMenuAnimation(View view, int animResId) {
        if (null == mHideAnimation) {
            mHideAnimation = AnimationUtils.loadAnimation(view.getContext(), animResId);
        }
        view.clearAnimation();
        view.startAnimation(mHideAnimation);
        mHideAnimation.setAnimationListener(new HideAnimationListener());
    }

    private void startMenuShowAnimation(View view, int animResId) {
        if (null == mShowAnimation) {
            mShowAnimation = AnimationUtils.loadAnimation(view.getContext(), animResId);
        }
        view.clearAnimation();
        view.startAnimation(mShowAnimation);
        mShowAnimation.setAnimationListener(new ShowAnimationListener());
    }

    private void resetStatus() {
        if (null != mLeftImageView) {
            mLeftImageView.clearAnimation();
            mLeftImageView.setVisibility(View.VISIBLE);
            if (mCurStyle == TITLEBAR_STYLE_LIGHT) {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_dot_light));
                mRightImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_close_light));
            } else {
                mLeftImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_dot));
                mRightImageView.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menu_close));
            }
        }
        if (null != mBaseTitleDialog) {
            mBaseTitleDialog.notifyDialogStatus(Display.DISPLAY_STATUS_FINISH);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mIsMenubarTypeWeb) {
            initLifecycleListener();
        }
        Context context = getContext();
        if (!(context instanceof RuntimeActivity)) {
            Log.e(TAG, "onAttachedToWindow error context is not instanceof RuntimeActivity");
            return;
        }
        HybridView hybridView = ((RuntimeActivity) context).getHybridView();
        View view = null;
        if (null != hybridView) {
            view = hybridView.getWebView();
        }
        if (view instanceof RootView) {
            int curMenustatus = ((RootView) view).getMenubarStatus();
            updateLeftMenubg(curMenustatus);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeLifecycleListener();
        mMenubarLifeCycleCallback = null;
        mLifecycleListener = null;
        removeCallbacks(mCurRunTask);
        if (null != mLeftImageView) {
            mLeftImageView.clearAnimation();
        }
        changeMenuStatus(Display.DISPLAY_STATUS_FINISH);
        if (null != mBaseTitleDialog) {
            mBaseTitleDialog.clearBaseTitleDialog();
        }
        mIsAnimation = false;
        mAnimationCount = 0;
    }

    private void initDialog() {
        if (!(getContext() instanceof RuntimeActivity)) {
            Log.e(TAG, "initDialog error: getContext() is not an instance of RuntimeActivity.");
            return;
        }
        final RuntimeActivity act = (RuntimeActivity) getContext();
        if (null == act) {
            Log.e(TAG, "initDialog error: act is null.");
            return;
        }
        mBaseTitleDialog = new BaseTitleDialog(getContext());
        mBaseTitleDialog.initDialog(getContext());
    }

    public void updateMenuData(MenubarItemData menubarItemData) {
        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (null != mBaseTitleDialog && null != menubarItemData) {
                            List<MenubarItemData> topDatas = mBaseTitleDialog.getTopItemDatas();
                            List<MenubarItemData> btmDatas = mBaseTitleDialog.getBottomItemDatas();
                            int topPostion = topDatas.indexOf(menubarItemData);
                            int bottomPosition = -1;
                            if (topPostion == -1) {
                                bottomPosition = btmDatas.indexOf(menubarItemData);
                                if (bottomPosition != -1) {
                                    mBaseTitleDialog.notifyDataChange(
                                            MenubarItemData.BOTTOM_ITEM_LOCATION_TAG,
                                            bottomPosition);
                                }
                            } else {
                                mBaseTitleDialog.notifyDataChange(
                                        MenubarItemData.TOP_ITEM_LOCATION_TAG, topPostion);
                            }
                        } else {
                            Log.e(TAG,
                                    "updateMenuData mBaseTitleDialog or menubarItemData is null.");
                        }
                    }
                });
    }

    public void updateMenuData(int valueTag, int locationTag, int position, Object content) {
        if (null != mBaseTitleDialog) {
            mBaseTitleDialog.updateDatas(valueTag, locationTag, position, content);
        } else {
            Log.e(TAG, "updateMenuData mBaseTitleDialog is null.");
        }
    }

    public void showMenuDialog(
            final List<MenubarItemData> datas,
            BaseTitleDialog.MenuBarClickCallback menuBarClickCallback,
            HashMap<String, Object> otherDatas) {
        if (null != mBaseTitleDialog && mBaseTitleDialog.isShowing()) {
            mBaseTitleDialog.dismiss();
        }
        if (null == datas) {
            Log.e(TAG, "showMenuDialog error datas null.");
            return;
        }
        List<MenubarItemData> topDatas = new ArrayList<>();
        List<MenubarItemData> bottomDatas = new ArrayList<>();
        int allSize = datas.size();
        MenubarItemData menubarItemData = null;
        for (int i = 0; i < allSize; i++) {
            menubarItemData = datas.get(i);
            if (null != menubarItemData) {
                if (menubarItemData.getTag() == MenubarItemData.BOTTOM_ITEM_LOCATION_TAG) {
                    bottomDatas.add(menubarItemData);
                } else {
                    topDatas.add(menubarItemData);
                }
            }
        }
        if (null != menuBarClickCallback && null != mBaseTitleDialog) {
            if (null != otherDatas) {
                otherDatas.put(MenubarView.MENUBAR_DIALOG_MENU_STATUS, mCurMenuStatus);
                otherDatas.put(MenubarView.MENUBAR_DIALOG_RPK_NAME, mRpkName);
            }
            mBaseTitleDialog
                    .showMenuDialog(topDatas, bottomDatas, menuBarClickCallback, otherDatas);
        } else {
            Log.e(TAG, "showMenuDialog error menuBarClickCallback or mBaseTitleDialog is null.");
        }
    }

    public void setOnMenubarLifeCycleCallback(MenubarLifeCycleCallback menubarLifeCycleCallback) {
        this.mMenubarLifeCycleCallback = menubarLifeCycleCallback;
        initLifecycleListener();
    }

    public interface MenubarLifeCycleCallback {
        void onActivityResume();

        void onActivityPause();
    }

    private class HideAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimation = true;
        }

        @Override
        public void onAnimationEnd(final Animation animation) {
            mIsAnimation = false;
            mAnimationCount = 1;
            if (mCurMenuStatus != Display.DISPLAY_STATUS_FINISH) {
                refreshMenuIcon();
            } else {
                mAnimationCount = 0;
                mIsAnimation = false;
                resetStatus();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private class ShowAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimation = true;
        }

        @Override
        public void onAnimationEnd(final Animation animation) {
            mIsAnimation = false;
            mAnimationCount = 0;
            if (mCurMenuStatus != Display.DISPLAY_STATUS_FINISH) {
                refreshMenuIcon();
            } else {
                resetStatus();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
