/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.FitWindowsViewGroup;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hapjs.cache.CacheStorage;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.HmacUtils;
import org.hapjs.common.utils.MenubarUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Scroller;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.FullscreenHelper;
import org.hapjs.component.view.BaseTitleDialog;
import org.hapjs.component.view.MenubarView;
import org.hapjs.component.view.TitleLinearLayout;
import org.hapjs.component.view.keyevent.KeyEventManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.FeatureInfo;
import org.hapjs.model.MenubarItemData;
import org.hapjs.net.NetLoadResult;
import org.hapjs.net.NetLoaderProvider;
import org.hapjs.render.cutout.CutoutSupportFactory;
import org.hapjs.render.cutout.ICutoutSupport;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.vdom.VElement;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class Display implements ConfigurationManager.ConfigurationListener {
    public static final int DISPLAY_STATUS_FINISH = 0;
    public static final int DISPLAY_LOCATION_START = 1;
    public static final int DISPLAY_RECORD_START = 2;
    public static final int MENUBAR_ENTER_FULLSCREEN_TAG = 1;
    public static final int MENUBAR_EXIT_FULLSCREEN_TAG = 2;
    private static final String TAG = "Display";
    private static final int DEFAULT_TITLE_HEIGHT = 56; // dp
    private static final int DEFAULT_MENUBAR_RIGHT_MARGIN = 9;
    private static final int DEFAULT_MENUBAR_TOP_MARGIN = 11;
    private static boolean mHasFetchConfig = false;
    private static boolean mIsInitFetchConfig = false;
    private static boolean mIsRequest = false;
    private static int mShowPointCount = -1;
    private static final float BRIGHTNESS_THRESHOLD = 0.7f;
    private final Map<String, String> mExtraShareData = new HashMap<>();
    private DecorLayout mDecorLayout;
    private Window mWindow;
    private Page mPage;
    private RootView mRootView;
    private boolean mIsCardMode;
    private boolean mIsInsetMode;
    private Toolbar mToolbar;
    private MenubarView mMenubarView;
    private TitleLinearLayout mTitleInnerLayout = null;
    private View mNaviBarView;
    private View mStatusBarView;
    private AppCompatImageButton mMenuView;
    private Drawable mMenuBackground;
    private ProgressBarController mProgressBarController;
    private FullscreenHelper mFullscreenHelper;
    private Context mContext;
    private int mTitleHeight;
    private View mTopCutoutView;
    private View mLeftCutoutView;
    private String mRpkName = "";
    private String mRpkIcon = "";
    private String mRpkPackage = "";
    private String mShareRpkName = "";
    private String mShareRpkDescription = "";
    private String mShareRpkIconUrl = "";
    private String mShareCurrentPage = "";
    private String mShareUrl = "";
    private String mShareParams = "";
    private String mUsePageParams = "";
    private boolean mIsShowMenuBar = false;
    private boolean mIsAllowMenubarMove = true;
    private int mDefaultMenubarStatus = View.GONE;
    private boolean mIsConfigShowPointTip = false;
    private boolean mIsConfigShortCutStatus = false;
    private AppInfo mAppInfo = null;
    private Runnable mHideTipsRunnable = null;
    private LinearLayout mBottomTipsContainer = null;
    private ImageView mTopTipsArrow = null;
    private Handler mCurrentHandler = null;
    private boolean mIsShortcutInstalled = false;
    private MenubarView.MenubarLifeCycleCallback mMenubarLifeCycleCallback = null;
    private String mTipsContent = "";
    private int mTipsShowTime = MenubarView.MENUBAR_TIPS_SHOW_TIME_DURATION;

    public Display(DecorLayout decorLayout, Window window, Page page, RootView rootView) {
        mContext = decorLayout.getContext().getApplicationContext();

        mDecorLayout = decorLayout;
        mWindow = window;
        mPage = page;
        mRootView = rootView;

        mTitleHeight =
                (int) (DEFAULT_TITLE_HEIGHT
                        * mDecorLayout.getResources().getDisplayMetrics().density);
        mIsCardMode = HapEngine.getInstance(mRootView.getPackage()).isCardMode();
        mIsInsetMode = HapEngine.getInstance(mRootView.getPackage()).isInsetMode();
        if (null != mRootView) {
            mAppInfo = mRootView.getAppInfo();
            if (null != mAppInfo) {
                mRpkName = mAppInfo.getName();
                mRpkPackage = mAppInfo.getPackage();
                mRpkIcon = mAppInfo.getIcon();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowInsets windowInsets = mRootView.getRootWindowInsets();
                if (windowInsets != null) {
                    DisplayUtil.setWindowInsets(windowInsets);
                } else {
                    mRootView.setOnApplyWindowInsetsListener(
                            new View.OnApplyWindowInsetsListener() {
                                @Override
                                public WindowInsets onApplyWindowInsets(View v,
                                                                        WindowInsets insets) {
                                    // 判断导航栏和设置 Cutout 需要依赖 window insets,当 window insets 改变后重新 setup
                                    if (insets != null
                                            && !insets.equals(DisplayUtil.getWindowInsets())) {
                                        DisplayUtil.setWindowInsets(insets);
                                        setup();
                                    }
                                    mRootView.setOnApplyWindowInsetsListener(null);
                                    return insets;
                                }
                            });
                }
            }
        }
    }

    static void initSystemUI(final Window window, RootView rootView) {
        // 设置透明状态栏, 有些4.4的系统上面状态栏并不是全透明的，而是渐变的, 4.4 以上会移除 android.R.id.statusBarBackground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        // Make Content Appear Behind the Status Bar --
        // https://developer.android.com/training/system-ui/status
        rootView.setSystemUiVisibility(
                rootView.getSystemUiVisibility()
                        | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | SYSTEM_UI_FLAG_LAYOUT_STABLE);
        rootView.setFitsSystemWindows(true);

        rootView.setOnFitSystemWindowsListener(
                new FitWindowsViewGroup.OnFitSystemWindowsListener() {
                    @Override
                    public void onFitSystemWindows(Rect insets) {
                        ViewGroup decorView = (ViewGroup) window.getDecorView();

                        // display behind navigationBar if necessary.
                        int sysUiVisibility = decorView.getWindowSystemUiVisibility();
                        if ((sysUiVisibility & SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0
                                || (sysUiVisibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0) {
                            insets.bottom = 0;
                            insets.right = 0;
                            insets.left = 0;
                        }

                        // display behind statusBar.
                        insets.top = 0; // 竖屏时 insets.top 为刘海高度
                        insets.left = 0; // 横屏时 insets.left 为刘海高度
                    }
                });
    }

    void setup() {
        if (mAppInfo.getDisplayInfo() != null) {
            mDecorLayout.setBackgroundColor(mPage.getBackgroundColor());
            setupCutout();
            setupStatusBar();
            setupTitleBar();
            setupFullScreen();
            setupWindowSoftInputMode();
            setupOrientation();
            setupForceDark();
        }
        setupMenuBar();
    }

    private void setupNavigationBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (DisplayUtil.hasNavigationBar(mDecorLayout.getContext())) {
            addNavigationBar();
        } else {
            removeNavigationBar();
        }
    }

    private void addNavigationBar() {
        if (mNaviBarView == null) {
            mNaviBarView = new View(mDecorLayout.getContext());
            int naviHeight = DisplayUtil.getNavigationBarHeight(mDecorLayout.getContext());

            if (!isLandscape()) {
                RelativeLayout.LayoutParams lp;
                lp = new RelativeLayout.LayoutParams(DecorLayout.LayoutParams.MATCH_PARENT,
                        naviHeight);
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                mDecorLayout.addView(mNaviBarView, lp);
            } else {
                RelativeLayout.LayoutParams lp;
                lp = new RelativeLayout.LayoutParams(naviHeight,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                mDecorLayout.addView(mNaviBarView, lp);
            }
        }

        boolean lightNaviBar = true;
        if (DarkThemeUtil.isDarkMode()) {
            lightNaviBar = false;
            mNaviBarView.setBackgroundColor(Color.BLACK);
        } else {
            mNaviBarView.setBackgroundColor(Color.WHITE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int uiOptions = mWindow.getDecorView().getSystemUiVisibility();
            if (lightNaviBar) {
                uiOptions |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                uiOptions &= ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            mWindow.getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    private void removeNavigationBar() {
        if (mNaviBarView != null) {
            mDecorLayout.removeView(mNaviBarView);
            mNaviBarView = null;
        }
    }

    private boolean isLandscape() {
        return mPage.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void setupForceDark() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SysOpProvider sysOpProvider =
                    ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (sysOpProvider.isCloseGlobalDefaultNightMode()) {
                mRootView.setForceDarkAllowed(false);
                return;
            }
            boolean isForceDark = mPage.isForceDark();
            mRootView.setForceDarkAllowed(isForceDark);
        }
    }

    public void onAttachedFromWindow() {
        ConfigurationManager.getInstance().addListener(this);
    }

    public void onDetachedFromWindow() {
        ConfigurationManager.getInstance().removeListener(this);
        KeyEventManager.getInstance().clear(mPage == null ? -1 : mPage.getPageId());
    }

    void clear() {
        mDecorLayout.setBackground(null);
        clearFullScreen();
    }

    private void setupCutout() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        String fitCutout = mPage.getFitCutout();
        if (TextUtils.isEmpty(fitCutout)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (mLeftCutoutView == null) {
                mLeftCutoutView = new View(mContext);
                RelativeLayout.LayoutParams leftCutoutViewParams =
                        new RelativeLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                leftCutoutViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                mLeftCutoutView.setId(R.id.left_cutout_view);
                mLeftCutoutView.setBackgroundColor(Color.BLACK);
                mDecorLayout.addView(mLeftCutoutView, leftCutoutViewParams);
            }

            if (mTopCutoutView == null) {
                mTopCutoutView = new View(mContext);
                RelativeLayout.LayoutParams topCutoutViewParams =
                        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                topCutoutViewParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_cutout_view);
                mTopCutoutView.setId(R.id.top_cutout_view);
                mTopCutoutView.setBackgroundColor(Color.BLACK);
                mDecorLayout.addView(mTopCutoutView, topCutoutViewParams);
            }
        }

        if (mLeftCutoutView != null) {
            ViewGroup.LayoutParams layoutParams = mLeftCutoutView.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.width = 0;
            }
            mLeftCutoutView.requestLayout();
        }

        if (mTopCutoutView != null) {
            ViewGroup.LayoutParams layoutParams = mTopCutoutView.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = 0;
            }
            mTopCutoutView.requestLayout();
        }

        if (mWindow == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 初始化页面前需要重置cutoutMode
            WindowManager.LayoutParams attributes = mWindow.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            mWindow.setAttributes(attributes);
        }

        boolean fullScreen = mPage.isFullScreen();
        int orientation;
        if (mPage.hasSetOrientation()) {
            orientation = mPage.getOrientation();
        } else {
            orientation =
                    BuildPlatform.isTV()
                            ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }

        ICutoutSupport cutoutSupport = CutoutSupportFactory.createCutoutSupport();
        if (fullScreen && orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            // 竖屏只在全屏下适配
            cutoutSupport.fit(mContext, mWindow, mTopCutoutView, true, fitCutout);
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            // 适配横屏
            cutoutSupport.fit(mContext, mWindow, mLeftCutoutView, false, fitCutout);
        }
    }

    private void ensureStatusBarView() {
        if (mStatusBarView == null) {
            mStatusBarView = new View(mDecorLayout.getContext());
            RelativeLayout.LayoutParams statusBarViewParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            mStatusBarView.setId(R.id.status_bar_view);

            if (mTopCutoutView != null) {
                statusBarViewParams.addRule(RelativeLayout.BELOW, R.id.top_cutout_view);
                statusBarViewParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_cutout_view);
            }
            mDecorLayout.addView(mStatusBarView, statusBarViewParams);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mStatusBarView.setZ(mPage.isStatusBarImmersive() ? Float.MAX_VALUE : 0);
                mStatusBarView.setOutlineProvider(null);
            }
        }
    }

    void setupStatusBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        String style = mPage.getStatusBarTextStyle();
        boolean lightStatusBar;
        switch (style) {
            case DisplayInfo.Style.KEY_STATUS_BAR_TEXT_LIGHT:
                lightStatusBar = false;
                break;
            case DisplayInfo.Style.KEY_STATUS_BAR_TEXT_DARK:
                lightStatusBar = true;
                break;
            case DisplayInfo.Style.KEY_STATUS_BAR_TEXT_AUTO:
            default:
                lightStatusBar =
                        ColorUtil.getGrayscaleFromColor(mPage.getStatusBarBackgroundColor())
                                > BRIGHTNESS_THRESHOLD;
                break;
        }
        if (DarkThemeUtil.isDarkMode()) {
            lightStatusBar = false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (lightStatusBar) {
                mWindow.getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                mWindow
                        .getDecorView()
                        .setSystemUiVisibility(
                                mDecorLayout.getSystemUiVisibility()
                                        & ~SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (provider != null) {
            provider.setupStatusBar(mWindow, lightStatusBar);
        }

        ensureStatusBarView();
        int statusBarBackgroundColor =
                ColorUtil.multiplyColorAlpha(
                        mPage.getStatusBarBackgroundColor(),
                        (int) (mPage.getStatusBarBackgroundOpacity() * 0xFF));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                &&
                ColorUtil.getGrayscaleFromColor(statusBarBackgroundColor) > BRIGHTNESS_THRESHOLD) {
            // status bar does not support change text color below android 6.0
            // Add black mask layer manually in order to avoid white background with white text
            statusBarBackgroundColor =
                    ColorUtils.blendARGB(statusBarBackgroundColor, Color.BLACK, 0.2f);
        }
        mStatusBarView.setBackgroundColor(statusBarBackgroundColor);
        if (mPage.isFullScreen()) {
            mStatusBarView.getLayoutParams().height = 0;
        } else {
            mStatusBarView.getLayoutParams().height =
                    DisplayUtil.getStatusBarHeight(mDecorLayout.getContext());
        }
    }

    private void ensureMenuBarView(boolean isMenuBarClick) {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        mIsRequest = false;
        if (mMenubarView == null) {
            if (mIsConfigShortCutStatus) {
                mMenubarLifeCycleCallback =
                        new MenubarView.MenubarLifeCycleCallback() {
                            @Override
                            public void onActivityResume() {
                                checkRedPointStatus();
                                if (mIsConfigShortCutStatus) {
                                    MenubarUtils.isShortCutInstalled(
                                            mContext,
                                            (null != mAppInfo ? mAppInfo.getPackage() : ""),
                                            mRootView,
                                            new MenubarUtils.MenubarStatusCallback() {
                                                @Override
                                                public void onMenubarStatusCallback(
                                                        HashMap<String, Object> datas) {
                                                    if (null != datas) {
                                                        boolean isContain =
                                                                datas.containsKey(
                                                                        MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                        Object content = null;
                                                        if (isContain) {
                                                            content = datas.get(
                                                                    MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                        }
                                                        if (content instanceof Boolean) {
                                                            mIsShortcutInstalled =
                                                                    (Boolean) content;
                                                        }
                                                    }
                                                }
                                            },
                                            null);
                                }
                                SysOpProvider provider =
                                        ProviderManager.getDefault()
                                                .getProvider(SysOpProvider.NAME);
                                if (null != provider) {
                                    provider.onActivityResume(
                                            mAppInfo,
                                            getHybridContext(),
                                            (null != mMenubarView ? mMenubarView.getContext() :
                                                    null),
                                            null);
                                }
                            }

                            @Override
                            public void onActivityPause() {
                                SysOpProvider provider =
                                        ProviderManager.getDefault()
                                                .getProvider(SysOpProvider.NAME);
                                if (null != provider) {
                                    provider.onActivityPause(
                                            mAppInfo,
                                            getHybridContext(),
                                            (null != mMenubarView ? mMenubarView.getContext() :
                                                    null),
                                            null);
                                }
                            }
                        };
            }
            mMenubarView =
                    new MenubarView(mDecorLayout.getContext(), mRpkName, mMenubarLifeCycleCallback);
            if (!mIsAllowMenubarMove) {
                mMenubarView.setIsNeedMove(false);
            }
            mMenubarView.setOnLeftClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!isMenuBarClick) {
                                mRootView.showMenu();
                            } else {
                                initShowMenubarDialog();
                            }
                        }
                    });
            mMenubarView.setOnRightClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideTipsView();
                            if (null != mRootView) {
                                Context context = mRootView.getContext();
                                if (context instanceof RuntimeActivity) {
                                    SysOpProvider provider =
                                            ProviderManager.getDefault()
                                                    .getProvider(SysOpProvider.NAME);
                                    final String backgroundStr =
                                            context.getResources()
                                                    .getString(R.string.menubar_dlg_background_run);
                                    boolean consumed = false;
                                    if (null != provider) {
                                        consumed =
                                                provider.onMenuBarItemClick(
                                                        context, -1, backgroundStr, null, mAppInfo,
                                                        mRootView, null, null);
                                    }
                                    if (!consumed) {
                                        ((RuntimeActivity) context).moveTaskToBack(true);
                                    }
                                }
                            }
                        }
                    });
        } else {
            if (mIsConfigShortCutStatus) {
                mMenubarView.setOnMenubarLifeCycleCallback(mMenubarLifeCycleCallback);
            }
        }
        int titlebarHeight =
                (int)
                        (TitleLinearLayout.DEFAULT_MENUBAR_HEIGHT_SIZE
                                * mDecorLayout.getResources().getDisplayMetrics().density);
        int titlebarWidth =
                (int)
                        (TitleLinearLayout.DEFAULT_MENUBAR_WIDTH_SIZE
                                * mDecorLayout.getResources().getDisplayMetrics().density);
        if (null != mPage && mPage.getMenuBarStyle() == MenubarView.TITLEBAR_STYLE_LIGHT) {
            mMenubarView.updateTitlebarStyle(MenubarView.TITLEBAR_STYLE_LIGHT);
        }
        FrameLayout.LayoutParams menuViewParams =
                new FrameLayout.LayoutParams(titlebarWidth, titlebarHeight);
        menuViewParams.gravity = Gravity.END | Gravity.TOP;
        menuViewParams.rightMargin =
                (int)
                        (MenubarView.DEFAULT_MENUBAR_RIGHT_MARGIN
                                * mDecorLayout.getResources().getDisplayMetrics().density)
                        + getContentInsets().right;
        if (mPage.hasTitleBar()) {
            menuViewParams.topMargin = mTitleHeight / 2 - titlebarHeight / 2;
        } else {
            menuViewParams.topMargin =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_TOP_MARGIN
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
        }
        mTitleInnerLayout = mMenubarView.findViewById(R.id.titlebarview);
        mTitleInnerLayout.setLayoutParams(menuViewParams);
        int statusBarHeight = 0;
        if (mPage.isFullScreen()) {
            statusBarHeight = 0;
        } else {
            statusBarHeight = DisplayUtil.getStatusBarHeight(mDecorLayout.getContext());
        }
        mTitleInnerLayout.refreshScreenWidthHeight(mPage.getOrientation(), statusBarHeight);
        addMenubarView();
        if (mIsConfigShowPointTip) {
            boolean isShowMenuPoint = MenubarUtils.getMenuPointStatus(getHybridContext(), mContext);
            ImageView menubarPointIv = mMenubarView.findViewById(R.id.menubar_point_iv);
            if (isShowMenuPoint) {
                if (null != menubarPointIv) {
                    menubarPointIv.setVisibility(View.VISIBLE);
                }
            } else {
                if (null != menubarPointIv) {
                    menubarPointIv.setVisibility(View.GONE);
                }
            }
        }
        if (mIsConfigShortCutStatus) {
            MenubarUtils.isShortCutInstalled(
                    mContext,
                    (null != mAppInfo ? mAppInfo.getPackage() : ""),
                    mRootView,
                    new MenubarUtils.MenubarStatusCallback() {
                        @Override
                        public void onMenubarStatusCallback(HashMap<String, Object> datas) {
                            if (null != datas) {
                                boolean isContain = datas.containsKey(
                                        MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                Object content = null;
                                if (isContain) {
                                    content =
                                            datas.get(MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                }
                                if (content instanceof Boolean) {
                                    mIsShortcutInstalled = (Boolean) content;
                                }
                            }
                        }
                    },
                    null);
        }
        initMenubarTips(mMenubarView, titlebarWidth, titlebarHeight, mTitleHeight, menuViewParams.topMargin, false);
        mDefaultMenubarStatus = mMenubarView.getVisibility();
    }

    private void checkRedPointStatus() {
        if (mIsConfigShowPointTip && null != mMenubarView && null != mContext) {
            boolean isShowMenuPoint = MenubarUtils.getMenuPointStatus(getHybridContext(), mContext);
            ImageView menubarPointIv = mMenubarView.findViewById(R.id.menubar_point_iv);
            if (isShowMenuPoint) {
                if (null != menubarPointIv) {
                    menubarPointIv.setVisibility(View.VISIBLE);
                }
            } else {
                if (null != menubarPointIv) {
                    menubarPointIv.setVisibility(View.GONE);
                }
            }
        } else {
            Log.e(
                    TAG,
                    "checkRedPointStatus mIsConfigShowPointTip : "
                            + mIsConfigShowPointTip
                            + " mMenubarView : "
                            + mMenubarView
                            + " mContext : "
                            + mContext);
        }
    }

    public boolean initShowMenubarDialog() {
        boolean isConsumeMenu = true;
        if (!isShowMenubar() || null == mMenubarView) {
            isConsumeMenu = false;
            return isConsumeMenu;
        }
        hideTipsView();
        if (TextUtils.isEmpty(mShareRpkName)
                && TextUtils.isEmpty(mShareRpkDescription)
                && TextUtils.isEmpty(mShareRpkIconUrl)) {
            if (null != mPage) {
                if (!(TextUtils.isEmpty(mPage.getMenuBarTitle())
                        && TextUtils.isEmpty(mPage.getMenuBarDescription())
                        && TextUtils.isEmpty(mPage.getMenuBarIcon()))) {
                    mShareRpkName = mPage.getMenuBarTitle();
                    mShareRpkDescription = mPage.getMenuBarDescription();
                    mShareRpkIconUrl = mPage.getMenuBarIcon();
                } else {
                    getRpkShareInfo();
                }
            } else {
                getRpkShareInfo();
            }
        }
        if (null != mPage) {
            if (TextUtils.isEmpty(mShareCurrentPage)) {
                mShareCurrentPage = mPage.getMenuBarShareCurrentPage();
            }
            if (TextUtils.isEmpty(mShareUrl)) {
                mShareUrl = mPage.getMenuBarShareUrl();
            }
            if (TextUtils.isEmpty(mShareParams)) {
                mShareParams = mPage.getMenuBarShareParams();
            }
            if (TextUtils.isEmpty(mUsePageParams)) {
                mUsePageParams = mPage.getMenuBarUsePageParams();
                if (TextUtils.isEmpty(mUsePageParams)) {
                    mUsePageParams = mShareCurrentPage;
                }
            }
        }
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        final String menuStr = mContext.getResources().getString(R.string.menubar_dlg_menu);
        final String defaultShareDesc =
                mContext.getResources().getString(R.string.menubar_share_default_description);
        mExtraShareData.put(
                DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE,
                TextUtils.isEmpty(mShareRpkName) ? mRpkName : mShareRpkName);
        mExtraShareData.put(
                DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION,
                TextUtils.isEmpty(mShareRpkDescription) ? defaultShareDesc : mShareRpkDescription);
        mExtraShareData.put(
                DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON,
                TextUtils.isEmpty(mShareRpkIconUrl) ? mRpkIcon : mShareRpkIconUrl);
        mExtraShareData.put(
                DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE,
                TextUtils.isEmpty(mShareCurrentPage) ? "false" : mShareCurrentPage);
        mExtraShareData.put(
                DisplayInfo.Style.PARAM_SHARE_URL, TextUtils.isEmpty(mShareUrl) ? "" : mShareUrl);
        mExtraShareData.put(
                DisplayInfo.Style.PARAM_SHARE_PARAMS,
                TextUtils.isEmpty(mShareParams) ? "" : mShareParams);
        mExtraShareData.put(MenubarUtils.PARAM_PACKAGE, mRpkPackage);
        String pagePath = "";
        if ("true".equalsIgnoreCase(mUsePageParams)) {
            JSONObject pageParams = new JSONObject();
            try {
                if (null != mPage && null != mPage.params && mPage.params.size() > 0) {
                    HmacUtils.mapToJSONObject(pageParams, mPage.params);
                }
            } catch (JSONException e) {
                Log.e(TAG, "initShowMenubarDialog  mapToJSONObject error : " + e.getMessage());
            }
            mExtraShareData.put(MenubarUtils.PARAM_PAGE_PARAMS, pageParams.toString());
        } else {
            mExtraShareData.put(MenubarUtils.PARAM_PAGE_PARAMS, "");
        }
        if (null != mPage) {
            pagePath = mPage.getPath();
        }
        mExtraShareData
                .put(MenubarUtils.PARAM_PAGE_PATH, TextUtils.isEmpty(pagePath) ? "" : pagePath);
        boolean consumed =
                provider.onMenuBarItemClick(
                        mMenubarView.getContext(),
                        -1,
                        menuStr,
                        null,
                        mAppInfo,
                        mRootView,
                        mExtraShareData,
                        null);
        if (!consumed) {
            showMenuDialog();
        }
        return isConsumeMenu;
    }

    /**
     * this.$page.setMenubarData(
     * { shareTitle:'分享标题' ,
     * shareDescription:'分享描述',
     * shareIcon:'https://doc.quickapp.cn/assets/images/logo.png',
     * shareCurrentPage:true ,
     * shareUrl:"cp配置分享url,//在无法跳转快应用时候使用"
     * shareParams: { a: 1, b: 'abc' },//配置透传给分享页面的参数
     * usePageParams: true
     * }
     * )
     *
     * @param
     */
    public void refreshMenubarShareData(JSONObject datas) {
        if (null == datas) {
            Log.e(TAG, "refreshMenubarShareData data is null.");
            return;
        }
        String tmpStr = "";
        if (datas.has(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE)) {
            try {
                tmpStr = datas.getString(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE);
                if (null != tmpStr) {
                    mShareRpkName = tmpStr;
                }
            } catch (JSONException e) {
                Log.e(TAG, "refreshMenubarShareData KEY_MENUBAR_SHARE_TITLE error : "
                        + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION)) {
            try {
                tmpStr = datas.getString(DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION);
                if (null != tmpStr) {
                    mShareRpkDescription = tmpStr;
                }
            } catch (JSONException e) {
                Log.e(
                        TAG, "refreshMenubarShareData KEY_MENUBAR_SHARE_DESCRIPTION error : "
                                + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON)) {
            try {
                tmpStr = datas.getString(DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON);
                if (null != tmpStr) {
                    mShareRpkIconUrl = tmpStr;
                }
            } catch (JSONException e) {
                Log.e(TAG,
                        "refreshMenubarShareData KEY_MENUBAR_SHARE_ICON error : " + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE)) {
            try {
                boolean tmpValue = datas.getBoolean(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE);
                mShareCurrentPage = (tmpValue ? "true" : "false");
            } catch (JSONException e) {
                Log.e(TAG, "refreshMenubarShareData PARAM_SHARE_CURRENT_PAGE error : "
                        + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.PARAM_SHARE_URL)) {
            try {
                tmpStr = datas.getString(DisplayInfo.Style.PARAM_SHARE_URL);
                if (null != tmpStr) {
                    mShareUrl = tmpStr;
                }
            } catch (JSONException e) {
                Log.e(TAG, "refreshMenubarShareData PARAM_SHARE_URL error : " + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.PARAM_SHARE_PARAMS)) {
            try {
                tmpStr = datas.getString(DisplayInfo.Style.PARAM_SHARE_PARAMS);
                if (null != tmpStr) {
                    mShareParams = tmpStr;
                }
            } catch (JSONException e) {
                Log.e(TAG, "refreshMenubarShareData PARAM_SHARE_PARAMS error : " + e.getMessage());
            }
        }
        if (datas.has(DisplayInfo.Style.PARAM_SHARE_USE_PAGE_PARAMS)) {
            try {
                boolean tmpValue = datas.getBoolean(DisplayInfo.Style.PARAM_SHARE_USE_PAGE_PARAMS);
                mUsePageParams = (tmpValue ? "true" : "false");
            } catch (JSONException e) {
                Log.e(TAG, "refreshMenubarShareData PARAM_SHARE_USE_PAGE_PARAMS error : " + e.getMessage());
            }
        } else {
            mUsePageParams = mShareCurrentPage;
        }
    }

    private void showMenuDialog() {
        final Context context = mMenubarView.getContext();
        final String shareStr = context.getResources().getString(R.string.menubar_dlg_share);

        String tmpShortCutStr =
                context.getResources().getString(R.string.menubar_dlg_create_shortcut);
        if (mIsConfigShortCutStatus && mIsShortcutInstalled) {
            tmpShortCutStr =
                    context.getResources().getString(R.string.menubar_dlg_already_added_shortcut);
        }
        final String createShortCutStr = tmpShortCutStr;
        final String homeStr = context.getResources().getString(R.string.menubar_dlg_go_home);
        final String aboutStr = context.getResources().getString(R.string.menubar_dlg_about);
        final int shareSourceId =
                context
                        .getResources()
                        .getIdentifier(
                                MenubarView.MENUBAR_DIALOG_SHARE_IMAGE_NAME, "drawable",
                                context.getPackageName());
        final int createSourceId =
                context
                        .getResources()
                        .getIdentifier(
                                MenubarView.MENUBAR_DIALOG_SHORTCUT_IMAGE_NAME,
                                "drawable",
                                context.getPackageName());
        final int homeSourceId =
                context
                        .getResources()
                        .getIdentifier(
                                MenubarView.MENUBAR_DIALOG_HOME_IMAGE_NAME, "drawable",
                                context.getPackageName());
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        List<MenubarItemData> tmpDatas =
                provider.getMenuBarData(
                        mAppInfo,
                        getHybridContext(),
                        mContext,
                        new SysOpProvider.OnUpdateMenubarDataCallback() {

                            @Override
                            public void onUpdateMenubarData(MenubarItemData itemData) {
                                if (null != mMenubarView) {
                                    mMenubarView.updateMenuData(itemData);
                                } else {
                                    Log.e(TAG, "onUpdateMenubarData mMenubarView is null.");
                                }
                            }

                            @Override
                            public void isMenubarDataCollect(String packageName,
                                                             boolean isCollected) {
                            }
                        });
        final Map<String, String> shareIdMap = provider.getMenuBarShareId(mContext);
        mExtraShareData.put(MenubarUtils.PARAM_PACKAGE, mRpkPackage);
        List<MenubarItemData> datas = new ArrayList<>();
        MenubarItemData itemData =
                new MenubarItemData(shareStr, shareSourceId, MenubarItemData.TOP_ITEM_LOCATION_TAG,
                        false);
        itemData.setKey(MenubarView.MENUBAR_DIALOG_SHARE_IMAGE_NAME);
        datas.add(itemData);
        itemData =
                new MenubarItemData(
                        createShortCutStr, createSourceId, MenubarItemData.TOP_ITEM_LOCATION_TAG,
                        false);
        itemData.setKey(MenubarView.MENUBAR_DIALOG_SHORTCUT_IMAGE_NAME);
        datas.add(itemData);
        // for 1080 local menubardata update
        itemData =
                new MenubarItemData(homeStr, homeSourceId, MenubarItemData.BOTTOM_ITEM_LOCATION_TAG,
                        true);
        itemData.setKey(MenubarView.MENUBAR_DIALOG_HOME_IMAGE_NAME);
        datas.add(itemData);
        if (null != tmpDatas && tmpDatas.size() > 0) {
            datas.addAll(tmpDatas);
        }
        BaseTitleDialog.MenuBarClickCallback callback =
                new BaseTitleDialog.MenuBarClickCallback() {
                    @Override
                    public void onMenuBarItemClick(
                            int position, String content, String key,
                            MenubarItemData menubarItemData) {
                        if (mIsConfigShowPointTip && mShowPointCount > 0
                                && null != menubarItemData) {
                            Context hybridContext = getHybridContext();
                            MenubarUtils.refreshMenubarPointStatus(hybridContext,
                                    menubarItemData.getKey());
                            if (menubarItemData.isShowPoint()) {
                                mShowPointCount--;
                                if (mShowPointCount <= 0) {
                                    if (null != mMenubarView) {
                                        ImageView menubarPointIv =
                                                mMenubarView.findViewById(R.id.menubar_point_iv);
                                        if (null != menubarPointIv) {
                                            menubarPointIv.setVisibility(View.GONE);
                                        }
                                    }
                                    MenubarUtils.setMenubarValue(
                                            hybridContext, MenubarView.MENUBAR_POINT_MENU_STATUS,
                                            false);
                                }
                            }
                        }
                        boolean isConsume =
                                provider.onMenuBarItemClick(
                                        context,
                                        position,
                                        content,
                                        menubarItemData,
                                        mAppInfo,
                                        mRootView,
                                        mExtraShareData,
                                        new SysOpProvider.OnMenubarCallback() {
                                            @Override
                                            public void onMenubarClickCallback(
                                                    int position,
                                                    String content,
                                                    MenubarItemData data,
                                                    HashMap<String, Object> datas) {
                                                String key = "";
                                                boolean isNeedUpdate = false;
                                                if (null != data) {
                                                    key = data.getKey();
                                                    isNeedUpdate = data.isNeedUpdate();
                                                }
                                                if (mIsConfigShortCutStatus
                                                        &&
                                                        MenubarView.MENUBAR_DIALOG_SHORTCUT_IMAGE_NAME
                                                                .equals(key)) {
                                                    if (null != datas) {
                                                        boolean isContain =
                                                                datas.containsKey(
                                                                        MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                        Object tmpcontent = null;
                                                        if (isContain) {
                                                            tmpcontent = datas.get(
                                                                    MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                        }
                                                        if (tmpcontent instanceof Boolean) {
                                                            mIsShortcutInstalled =
                                                                    (Boolean) tmpcontent;
                                                        }
                                                        if (mIsShortcutInstalled
                                                                && null != mMenubarView
                                                                && null != mContext) {
                                                            if (null == mCurrentHandler) {
                                                                mCurrentHandler = new Handler(
                                                                        Looper.getMainLooper());
                                                            }
                                                            mCurrentHandler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    if (null != mMenubarView) {
                                                                        mMenubarView.updateMenuData(
                                                                                MenubarItemData.NAME_TAG,
                                                                                menubarItemData.getTag(),
                                                                                position,
                                                                                mContext.getResources().getString(R.string
                                                                                        .menubar_dlg_already_added_shortcut));
                                                                    } else {
                                                                        Log.e(TAG, "onMenubarClickCallback mMenubarView is null.");
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                } else {
                                                    if (isNeedUpdate) {
                                                        if (null != mMenubarView) {
                                                            mMenubarView.updateMenuData(data);
                                                        } else {
                                                            Log.e(TAG, "onMenubarClickCallback mMenubarView is null "
                                                                    + ", isNeedUpdate true");
                                                        }
                                                    }
                                                }
                                            }
                                        });
                        if (!isConsume) {
                            if (shareStr.equals(content)) {
                                MenubarUtils.startShare(shareIdMap, mExtraShareData, mRootView, null, null);
                            } else if (MenubarView.MENUBAR_DIALOG_SHORTCUT_IMAGE_NAME.equals(key)) {
                                if (mIsConfigShortCutStatus && mIsShortcutInstalled) {
                                    if (null != mMenubarView && null != mContext) {
                                        mMenubarView.updateMenuData(
                                                MenubarItemData.NAME_TAG,
                                                menubarItemData.getTag(),
                                                position,
                                                mContext
                                                        .getResources()
                                                        .getString(
                                                                R.string.menubar_dlg_already_added_shortcut));
                                    }
                                } else {
                                    MenubarUtils.createShortCut(
                                            mRootView,
                                            data -> {
                                                if (mIsConfigShortCutStatus && null != data) {
                                                    boolean isContain =
                                                            data.containsKey(
                                                                    MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                    Object dataContent = null;
                                                    if (isContain) {
                                                        dataContent = data.get(
                                                                MenubarUtils.MENUBAR_HAS_SHORTCUT_INSTALLED);
                                                    }
                                                    if (dataContent instanceof Boolean) {
                                                        mIsShortcutInstalled =
                                                                (Boolean) dataContent;
                                                    }
                                                    if (mIsShortcutInstalled
                                                            && null != mMenubarView
                                                            && null != mContext) {
                                                        mMenubarView.updateMenuData(
                                                                MenubarItemData.NAME_TAG,
                                                                menubarItemData.getTag(),
                                                                position,
                                                                mContext
                                                                        .getResources()
                                                                        .getString(
                                                                                R.string.menubar_dlg_already_added_shortcut));
                                                    }
                                                }
                                            });
                                }
                            } else if (aboutStr.equals(content)) {
                                MenubarUtils.openAboutPage(mRootView);
                            } else if (homeStr.equals(content)) {
                                MenubarUtils.goHomePage(mRootView);
                            } else {
                                Log.e(TAG, "ensureMenuBarView no consume content : " + content);
                            }
                        }
                    }
                };
        final Uri iconUri = CacheStorage.getInstance(context).getCache(mRpkPackage).getIconUri();
        HashMap<String, Object> otherDatas = new HashMap<>();
        otherDatas.put(MenubarView.MENUBAR_DIALOG_RPK_ICON, iconUri);
        if (mIsInitFetchConfig) {
            otherDatas.put(MenubarView.MENUBAR_DIALOG_SHOW_ABOUT_ICON, mHasFetchConfig);
        } else {
            mHasFetchConfig = hasFetchConfig();
            mIsInitFetchConfig = true;
            otherDatas.put(MenubarView.MENUBAR_DIALOG_SHOW_ABOUT_ICON, mHasFetchConfig);
        }
        if (mIsConfigShowPointTip) {
            if (mShowPointCount > 0 || mShowPointCount == -1) {
                mShowPointCount = MenubarUtils.updateMenubarData(getHybridContext(), datas);
            } else {
                if (mShowPointCount == 0) {
                    int allsize = datas.size();
                    MenubarItemData tmpItemData = null;
                    for (int i = 0; i < allsize; i++) {
                        tmpItemData = datas.get(i);
                        if (null != tmpItemData) {
                            tmpItemData.setShowPoint(false);
                        }
                    }
                } else {
                    Log.e(TAG, "showMenuDialog error mShowPointCount : " + mShowPointCount);
                }
            }
        } else {
            int allsize = datas.size();
            MenubarItemData tmpItemData = null;
            for (int i = 0; i < allsize; i++) {
                tmpItemData = datas.get(i);
                if (null != tmpItemData) {
                    tmpItemData.setShowPoint(false);
                }
            }
        }
        mMenubarView.showMenuDialog(datas, callback, otherDatas);
    }

    private boolean hasFetchConfig() {
        boolean hasFetchConfig = false;
        if (null != mAppInfo) {
            List<FeatureInfo> featureInfos = mAppInfo.getFeatureInfos();
            FeatureInfo tmpFeatureInfo = null;
            if (null == featureInfos) {
                return hasFetchConfig;
            }
            int allSize = featureInfos.size();
            for (int i = 0; i < allSize; i++) {
                tmpFeatureInfo = featureInfos.get(i);
                if (null != tmpFeatureInfo && "system.fetch".equals(tmpFeatureInfo.getName())) {
                    hasFetchConfig = true;
                    break;
                }
            }
        }
        return hasFetchConfig;
    }

    public Context getHybridContext() {
        JsThread jsThread = null;
        Context platformeContext = null;
        if (null != mRootView) {
            jsThread = mRootView.getJsThread();
        }
        if (null != jsThread) {
            platformeContext = jsThread.getPlatformContext(mContext);
        }
        return platformeContext;
    }

    public boolean isTipsShow(Context context) {
        boolean isShow = false;
        if (null != context) {
            SharedPreferences sp =
                    context.getSharedPreferences(
                            MenubarView.MENUBAR_DIALOG_SHARE_PREFERENCE_NAME,
                            Context.MODE_MULTI_PROCESS);
            isShow = sp.getBoolean(MenubarView.MENUBAR_DIALOG_SHOW_TIPS_KEY, true);
            if (isShow) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(MenubarView.MENUBAR_DIALOG_SHOW_TIPS_KEY, false);
                editor.apply();
            }
        }
        return isShow;
    }

    public boolean showMenubarTips(JSONObject datas) {
        boolean isSuccess = false;
        if (null == mMenubarView ||
                null == mDecorLayout ||
                null == mPage ||
                mTitleHeight == 0 || null == datas) {
            Log.w(TAG, "showMenubaTips mMenubarView or mDecorLayout or mPage or mTitleHeight or datas is invalid.");
            return isSuccess;
        }
        if (mIsAllowMenubarMove) {
            Log.w(TAG, "showMenubaTips move menubar,  menubar tips no show.");
            return isSuccess;
        }
        String content = "";
        if (datas.has(DisplayInfo.Style.KEY_MENUBAR_TIPS_CONTENT)) {
            try {
                content = datas.getString(DisplayInfo.Style.KEY_MENUBAR_TIPS_CONTENT);
            } catch (JSONException e) {
                Log.e(TAG, "showMenubaTips KEY_MENUBAR_TIPS_CONTENT error : " + e.getMessage());
            }
        }
        mTipsContent = content;
        mTipsShowTime = MenubarView.MENUBAR_PAGE_TIPS_SHOW_TIME_DURATION;
        hideTipsView();
        int menubarHeight = (int) (TitleLinearLayout.DEFAULT_MENUBAR_HEIGHT_SIZE *
                mDecorLayout.getResources().getDisplayMetrics().density);
        int menubarWidth = (int) (TitleLinearLayout.DEFAULT_MENUBAR_WIDTH_SIZE *
                mDecorLayout.getResources().getDisplayMetrics().density);
        int menubarTopMargin = 0;
        if (mPage.hasTitleBar()) {
            menubarTopMargin = mTitleHeight / 2 - menubarHeight / 2;
        } else {
            menubarTopMargin = (int) (MenubarView.DEFAULT_MENUBAR_TOP_MARGIN *
                    mDecorLayout.getResources().getDisplayMetrics().density);
        }
        isSuccess = initMenubarTips(mMenubarView, menubarWidth, menubarHeight, mTitleHeight, menubarTopMargin, true);
        return isSuccess;
    }

    private boolean initMenubarTips(MenubarView menubarView, int menubarLayoutWidth, int menubarLayoutHeight, int titlebarHeight, int menubarTopMargin
            , boolean isOutTipsShow) {
        boolean isSuccess = false;
        if (!((mIsAllowMenubarMove && !isOutTipsShow) || isOutTipsShow)) {
            Log.w(TAG, "initMenubarTips is not allow show");
            return isSuccess;
        }
        boolean isShowMenubar = isOutTipsShow ? true : isTipsShow(getHybridContext());
        if (null != menubarView && isShowMenubar) {
            int imageHeight =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_TOP_TIPS_ARROW_HEIGHT
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
            int imageWidth =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_TOP_TIPS_ARROW_WIDTH
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams topLayoutparams =
                    new FrameLayout.LayoutParams(imageWidth, imageHeight);
            topLayoutparams.gravity = Gravity.END | Gravity.TOP;
            topLayoutparams.rightMargin =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_RIGHT_MARGIN
                                    * mDecorLayout.getResources().getDisplayMetrics().density)
                            + menubarLayoutWidth / 2
                            - imageWidth / 2;
            topLayoutparams.topMargin =
                    menubarTopMargin + menubarLayoutHeight / 2 + titlebarHeight / 2 - imageHeight;
            mTopTipsArrow = menubarView.findViewById(R.id.menubar_tips_top_img);
            mTopTipsArrow.setLayoutParams(topLayoutparams);
            mTopTipsArrow.setVisibility(View.VISIBLE);
            int tipsBottomHeight =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_BOTTOM_TIPS_HEIGHT
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
            int tipsBottomRightMargin =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_BOTTOM_TIPS_RIGHT_MARGIN
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
            int tipsMoveMargin =
                    (int)
                            (MenubarView.DEFAULT_MENUBAR_TIPS_MOVE_MARGIN
                                    * mDecorLayout.getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams bottomLayoutparams =
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            tipsBottomHeight);
            bottomLayoutparams.gravity = Gravity.END | Gravity.TOP;
            bottomLayoutparams.rightMargin = tipsBottomRightMargin;
            bottomLayoutparams.topMargin =
                    menubarTopMargin + menubarLayoutHeight / 2 + titlebarHeight / 2
                            - tipsMoveMargin;
            mBottomTipsContainer = menubarView.findViewById(R.id.menubar_tips_bottom_container);
            String showTipsContent = mContext.getResources().getString(R.string.menubar_tips);
            if (isOutTipsShow) {
                if (TextUtils.isEmpty(mTipsContent)) {
                    showTipsContent = String.format(mContext.getResources().getString(R.string.menubar_tips_special), mRpkName);
                } else {
                    showTipsContent = mTipsContent;
                }
            }
            TextView tipsTv = menubarView.findViewById(R.id.menubar_tips_tv);
            tipsTv.setText(showTipsContent);
            mBottomTipsContainer.setLayoutParams(bottomLayoutparams);
            mBottomTipsContainer.setVisibility(View.VISIBLE);
            Handler handler = mBottomTipsContainer.getHandler();
            mHideTipsRunnable =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (null != mTopTipsArrow) {
                                mTopTipsArrow.setVisibility(View.GONE);
                            }
                            if (null != mBottomTipsContainer) {
                                mBottomTipsContainer.setVisibility(View.GONE);
                            }
                        }
                    };
            if (null == mCurrentHandler) {
                mCurrentHandler = new Handler(Looper.getMainLooper());
            }
            isSuccess = true;
            mCurrentHandler.postDelayed(mHideTipsRunnable, (isOutTipsShow ? mTipsShowTime : MenubarView.MENUBAR_TIPS_SHOW_TIME_DURATION));
            if (null != mTitleInnerLayout) {
                mTitleInnerLayout.setMenubarMoveListener(
                        new TitleLinearLayout.MenubarMoveListener() {
                            @Override
                            public void onMenubarMove() {
                                hideTipsView();
                            }
                        });
            }
        }
        return isSuccess;
    }

    private void hideTipsView() {
        if (!mIsAllowMenubarMove) {
            Log.w(TAG, "hideTipsView no show no hide");
            return;
        }
        if (null != mHideTipsRunnable
                && null != mTopTipsArrow
                && mTopTipsArrow.getVisibility() == View.VISIBLE) {
            if (null == mCurrentHandler) {
                mCurrentHandler = new Handler(Looper.getMainLooper());
            }
            mCurrentHandler.removeCallbacks(mHideTipsRunnable);
        }
        if (null != mTopTipsArrow && mTopTipsArrow.getVisibility() == View.VISIBLE) {
            mTopTipsArrow.setVisibility(View.GONE);
        }
        if (null != mBottomTipsContainer && mBottomTipsContainer.getVisibility() == View.VISIBLE) {
            mBottomTipsContainer.setVisibility(View.GONE);
        }
    }

    public View addMenubarView() {
        if (null != mMenubarView
                && null != mDecorLayout
                && mDecorLayout.indexOfChild(mMenubarView) == -1) {
            RelativeLayout.LayoutParams titlebarviewLayoutParams =
                    new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            titlebarviewLayoutParams.addRule(RelativeLayout.BELOW, R.id.status_bar_view);
            if (mLeftCutoutView != null) {
                titlebarviewLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_cutout_view);
            }
            mDecorLayout.addView(mMenubarView, titlebarviewLayoutParams);
        }
        return mMenubarView;
    }

    public void bringFrontTitlebarView() {
        if (null != mMenubarView) {
            mMenubarView.bringToFront();
        }
    }

    /**
     * 获取rpk信息
     */
    private void getRpkShareInfo() {
        if (mIsRequest) {
            return;
        }
        mIsRequest = true;
        if (null == mDecorLayout) {
            return;
        }
        Context context = mDecorLayout.getContext();
        if (null == context) {
            return;
        }

        NetLoaderProvider provider =
                ProviderManager.getDefault().getProvider(NetLoaderProvider.NAME);
        if (provider == null) {
            Log.e(TAG, "error getRpkShareInfo provider null.");
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("rpkPackage", mRpkPackage);
        Map<String, String> realparams = provider.getMenubarParams(params);
        provider.loadData(
                provider.getMenubarUrl(),
                realparams,
                new NetLoaderProvider.DataLoadedCallback<String>() {
                    @Override
                    public void onSuccess(NetLoadResult<String> loadResult) {
                        String result = loadResult.getOriginData();
                        JSONObject resultJsonObj = null;
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                resultJsonObj = new JSONObject(result);
                                JSONObject jsonObject = null;
                                if (null != resultJsonObj && resultJsonObj.has("data")) {
                                    jsonObject = resultJsonObj.getJSONObject("data");
                                }
                                if (null != jsonObject) {
                                    if (jsonObject.has("rpkName")) {
                                        mShareRpkName = jsonObject.getString("rpkName");
                                        if (!TextUtils.isEmpty(mShareRpkName)) {
                                            mExtraShareData
                                                    .put(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE,
                                                            mShareRpkName);
                                        }
                                    }
                                    if (jsonObject.has("simpleDesc")) {
                                        mShareRpkDescription = jsonObject.getString("simpleDesc");
                                        if (!TextUtils.isEmpty(mShareRpkDescription)) {
                                            mExtraShareData.put(
                                                    DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION,
                                                    mShareRpkDescription);
                                        }
                                    }
                                    if (jsonObject.has("icon")) {
                                        mShareRpkIconUrl = jsonObject.getString("icon");
                                        if (!TextUtils.isEmpty(mShareRpkIconUrl)) {
                                            mExtraShareData.put(
                                                    DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON,
                                                    mShareRpkIconUrl);
                                        }
                                    }
                                    if (TextUtils.isEmpty(mShareRpkName)
                                            && TextUtils.isEmpty(mShareRpkDescription)
                                            && TextUtils.isEmpty(mShareRpkIconUrl)) {
                                        Log.e(
                                                TAG,
                                                "getRpkShareInfo error no mShareRpkName mShareRpkDescription mShareRpkIconUrl");
                                    } else {
                                        Log.i(TAG, "getRpkShareInfo success");
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "getRpkShareInfo onSuccess error exception msg : "
                                        + e.getMessage());
                            }
                        }
                        mIsRequest = false;
                    }

                    @Override
                    public void onFailure(NetLoadResult<String> loadResult) {
                        Exception exception = loadResult.getException();
                        Log.e(
                                TAG,
                                "getRpkShareInfo onFailure error exception msg : "
                                        + (null != exception ? exception.getMessage() :
                                        " exception null"));
                        mIsRequest = false;
                    }
                },
                provider.getMenubarPostType());
    }

    private void clearMenuBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mMenubarView != null) {
            mMenubarView.getLayoutParams().height = 0;
        }
    }

    public View getMenuBar() {
        return mTitleInnerLayout;
    }

    public void notifyFeatureStatus(int status) {
        if (null != mRootView) {
            mRootView.setMenubarStatus(status);
        }
        if (null != mMenubarView) {
            mMenubarView.updateLeftMenubg(status);
        }
    }

    private void ensureTitleBarView() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mToolbar == null) {
            mToolbar = new Toolbar(mDecorLayout.getContext());
            RelativeLayout.LayoutParams toolBarParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            mToolbar.setId(android.R.id.title);
            toolBarParams.addRule(RelativeLayout.BELOW, R.id.status_bar_view);
            if (mLeftCutoutView != null) {
                toolBarParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_cutout_view);
            }
            mDecorLayout.addView(mToolbar, toolBarParams);
        }
    }

    private void setupTitleBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mPage.hasTitleBar()) {
            ensureTitleBarView();
            mToolbar.getLayoutParams().height = mTitleHeight;
            int titleBarBackgroundColor =
                    ColorUtil.multiplyColorAlpha(
                            mPage.getTitleBarBackgroundColor(),
                            (int) (mPage.getTitleBarBackgroundOpacity() * 0xFF));
            mToolbar.setBackgroundColor(titleBarBackgroundColor);
            mToolbar.setTitleTextColor(mPage.getTitleBarTextColor());
            mToolbar.setTitle(mPage.getTitleBarText());

            if (mRootView.getPageManager().getCurrIndex() != 0) {
                mToolbar.setNavigationIcon(R.drawable.ic_back);
                Drawable navigationIcon = mToolbar.getNavigationIcon();
                if (navigationIcon != null) {
                    navigationIcon
                            .setColorFilter(mPage.getTitleBarTextColor(), PorterDuff.Mode.MULTIPLY);
                }
                mToolbar.setNavigationOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mRootView.goBack();
                            }
                        });
            } else {
                mToolbar.setNavigationIcon(null);
            }

        } else {
            clearTitleBar();
        }
    }

    private void setupMenuBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }

        boolean hasMenu = mPage.hasMenu();
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        boolean isVendorEnableMenubar = true;
        boolean isVendorDefaultShow = true;
        int cpShowStatus = Page.MENUBAR_DEFAULT;

        if (null != provider) {
            isVendorEnableMenubar = provider.isShowMenuBar(mContext, mAppInfo, null);
            isVendorDefaultShow = provider.isShowMenuBarDefault(mContext, mAppInfo, null);
            mIsConfigShowPointTip = provider.isShowMenuBarPointTip(mContext, mAppInfo, null);
            mIsConfigShortCutStatus = provider.isUseAddShortCutStatus(mContext, mAppInfo, null);
            mIsAllowMenubarMove = provider.isAllowMenubarMove(mContext, mAppInfo, null);
            cpShowStatus = provider.getCpMenuBarStatus(mPage);
        } else {
            Log.w(TAG, "setupTitleBar provider is null.");
        }

        if (mPage.getInnerPageTag() == Page.PAGE_TAG_MENUBAR_ABOUT || !isVendorEnableMenubar) {
            mIsShowMenuBar = false;
        } else if (cpShowStatus != Page.MENUBAR_DEFAULT) {
            mIsShowMenuBar = cpShowStatus == Page.MENUBAR_SHOW;
        } else {
            mIsShowMenuBar = isVendorDefaultShow && !mPage.isFullScreen();
        }
        if (mIsShowMenuBar) {
            if (hasMenu) {
                ensureMenuBarView(false);
            } else {
                ensureMenuBarView(true);
                if (mMenuView != null) {
                    mMenuView.setVisibility(View.GONE);
                }
            }
        } else {
            clearMenuBar();
            initOriginTitleBar();
        }
    }

    public boolean isShowMenubar() {
        return mIsShowMenuBar;
    }

    private void initOriginTitleBar() {
        if (mPage.hasTitleBar()) {
            if (mPage.hasMenu() && mPage.getInnerPageTag() == Page.PAGE_TAG_DEFAULT) {
                if (mMenuView == null) {
                    mMenuView =
                            new AppCompatImageButton(
                                    mDecorLayout.getContext(),
                                    null,
                                    androidx.appcompat.R.attr.toolbarNavigationButtonStyle);
                    Toolbar.LayoutParams menuViewParams =
                            new Toolbar.LayoutParams(mTitleHeight, mTitleHeight);
                    menuViewParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                    mMenuBackground = mDecorLayout.getResources().getDrawable(R.drawable.ic_menu);
                    mMenuView.setImageDrawable(mMenuBackground);
                    mMenuView.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mRootView.showMenu();
                                }
                            });
                    mToolbar.addView(mMenuView, menuViewParams);
                }
                mMenuView.setVisibility(View.VISIBLE);
                mMenuBackground
                        .setColorFilter(mPage.getTitleBarTextColor(), PorterDuff.Mode.MULTIPLY);
            } else {
                if (mMenuView != null) {
                    mMenuView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void clearTitleBar() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mToolbar != null) {
            mToolbar.getLayoutParams().height = 0;
        }
    }

    private void setupFullScreen() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mPage.isFullScreen()) {
            if (mStatusBarView != null) {
                mStatusBarView.getLayoutParams().height = 0;
            }
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                View decorView = mWindow.getDecorView();
                if (null != decorView) {
                    decorView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != decorView) {
                                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() |
                                        View.SYSTEM_UI_FLAG_FULLSCREEN);
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "setupFullScreen decorView is null.");
                }
            }
        } else {
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                View decorView = mWindow.getDecorView();
                if (null != decorView) {
                    decorView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != decorView) {
                                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() &
                                        (~View.SYSTEM_UI_FLAG_FULLSCREEN));
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "setupFullScreen else decorView is null.");
                }
            }
        }
    }

    private void clearFullScreen() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void setupWindowSoftInputMode() {
        final int windowSoftInputMode = mPage.getWindowSoftInputMode();
        // the value will be ignored, when FULLSCREEN
        mWindow.setSoftInputMode(windowSoftInputMode);
    }

    public boolean enterFullscreen(
            Component component,
            int screenOrientation,
            boolean showStatusBar,
            boolean fullScreenContainer) {
        if (mFullscreenHelper == null) {
            mFullscreenHelper = new FullscreenHelper(mDecorLayout);
        }
        changeMenuBarStatus(MENUBAR_ENTER_FULLSCREEN_TAG);
        return mFullscreenHelper.enterFullscreen(
                mRootView.getContext(), component, screenOrientation, showStatusBar,
                fullScreenContainer);
    }

    public boolean exitFullscreen() {
        if (mFullscreenHelper != null) {
            changeMenuBarStatus(MENUBAR_EXIT_FULLSCREEN_TAG);
            return mFullscreenHelper.exitFullscreen(mRootView.getContext());
        }
        return false;
    }

    public void changeMenuBarStatus(int tag) {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        int visibility = View.GONE;
        if (tag == MENUBAR_ENTER_FULLSCREEN_TAG) {
            visibility = View.INVISIBLE;
        } else {
            visibility = mDefaultMenubarStatus;
        }
        if (null != mMenubarView && mIsShowMenuBar) {
            if (mMenubarView.getVisibility() != visibility) {
                mMenubarView.setVisibility(visibility);
            }
        }
    }

    public void setLightStatusBar(boolean lightStatusBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWindow
                    .getDecorView()
                    .setSystemUiVisibility(
                            lightStatusBar
                                    ? mWindow.getDecorView().getSystemUiVisibility()
                                    | SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                    : mWindow.getDecorView().getSystemUiVisibility()
                                    & ~SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public void onActivityResume() {
        PageManager pageManager = null;
        Page page = null;
        if (mRootView != null) {
            pageManager = mRootView.getPageManager();
        }
        if (null != pageManager) {
            try {
                page = pageManager.getCurrPage();
            } catch (Exception e) {
                Log.w(TAG, "onActivityResume getCurrPage error : " + e.getMessage());
            }
        }
        if (null == page || page != mPage) {
            return;
        }
        setupFullScreen();
        if (mFullscreenHelper != null) {
            mFullscreenHelper.onActivityResume();
        }
    }

    private void setupOrientation() {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        Context context = mRootView.getContext();
        if (context instanceof Activity) {
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            ((Activity) context).setRequestedOrientation(provider.getScreenOrientation(mPage, mAppInfo));
        }
    }

    private void ensureProgressBarView() {
        if (mIsCardMode) {
            return;
        }

        if (mProgressBarController == null) {
            ProgressBar progressBar =
                    new ProgressBar(
                            mDecorLayout.getContext(), null,
                            android.R.attr.progressBarStyleHorizontal);
            RelativeLayout.LayoutParams progressBarParams =
                    new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) mDecorLayout.getResources()
                                    .getDimension(R.dimen.page_loading_progress_height));
            progressBar.setProgressDrawable(
                    mDecorLayout.getResources().getDrawable(R.drawable.page_loading_progress));
            progressBar.setId(R.id.progress_bar_view);
            progressBarParams.addRule(
                    RelativeLayout.BELOW,
                    mPage.hasTitleBar() ? android.R.id.title : R.id.status_bar_view);

            if (mLeftCutoutView != null) {
                progressBarParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_cutout_view);
            }

            progressBar.setIndeterminate(false);
            progressBar.setMax(100);

            mDecorLayout.addView(progressBar, progressBarParams);
            mProgressBarController = new ProgressBarController(progressBar);
        }
    }

    void showProgress() {
        if (mIsCardMode) {
            return;
        }

        ensureProgressBarView();
        mProgressBarController.showLoading();
    }

    void hideProgress() {
        if (mProgressBarController != null) {
            mDecorLayout.removeView(mProgressBarController.mProgressBar);
            mProgressBarController.hideLoading();
            mProgressBarController = null;
        }
    }

    void updateTitleBar(Map<String, Object> titles, int pageId) {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        boolean statusBarDirty = false;
        if (mPage != null && pageId == mPage.pageId) {
            for (String key : titles.keySet()) {
                Object value = titles.get(key);
                switch (key) {
                    case "backgroundColor":
                        String backgroundColorStr = Attributes.getString(value);
                        mPage.setExtraTitleBarBackgroundColor(backgroundColorStr);
                        statusBarDirty = true;
                        break;
                    case "backgroundOpacity":
                        String backgroundOpacityStr = Attributes.getString(value);
                        mPage.setExtraTitleBarBackgroundOpacity(backgroundOpacityStr);
                        statusBarDirty = true;
                        break;
                    case "textColor":
                        String textColor = Attributes.getString(value);
                        mPage.setExtraTitleBarTextColor(textColor);
                        statusBarDirty = true;
                        break;
                    case "text":
                        String text = Attributes.getString(value);
                        mPage.setExtraTitleBarText(text);
                        break;
                    case "menu":
                        String menuStr = Attributes.getString(value);
                        mPage.setExtraHasMenu(menuStr);
                        break;
                    default:
                        Log.e(TAG, "Unsupported key :" + key);
                }
            }

            setupTitleBar();
            if (statusBarDirty) {
                setupStatusBar();
            }
        }
    }

    void updateStatusBar(Map<String, Object> titles, int pageId) {
        if (mIsCardMode || mIsInsetMode) {
            return;
        }
        if (mPage != null && pageId == mPage.pageId) {
            for (String key : titles.keySet()) {
                Object value = titles.get(key);
                switch (key) {
                    case "backgroundColor":
                        String backgroundColorStr = Attributes.getString(value);
                        mPage.setExtraStatusBarBackgroundColor(backgroundColorStr);
                        break;
                    case "backgroundOpacity":
                        String backgroundOpacityStr = Attributes.getString(value);
                        mPage.setExtraStatusBarBackgroundOpacity(backgroundOpacityStr);
                        break;
                    case "textStyle":
                        String textColor = Attributes.getString(value);
                        mPage.setExtraStatusBarTextStyle(textColor);
                        break;
                    case "immersive":
                        String immersive = Attributes.getString(value);
                        mPage.setExtraStatusBarImmersive(immersive);
                        break;
                    default:
                        Log.e(TAG, "Unsupported key :" + key);
                }
            }
            setupStatusBar();
        }
    }

    void scrollPage(HapEngine hapEngine, Map<String, Object> scrolls, int pageId) {
        if (mPage != null && pageId == mPage.pageId) {
            String scrollType =
                    Attributes.getString(
                            scrolls.get(Page.KEY_PAGE_SCROLL_TYPE),
                            Page.PAGE_SCROLL_TYPE_NOT_DEFINE);
            if (!scrolls.containsKey("top")) {
                return;
            }
            int coordinateY = Attributes.getInt(hapEngine, scrolls.get("top"), 0);
            String scrollBehavior =
                    Attributes.getString(scrolls.get("behavior"), Page.PAGE_SCROLL_BEHAVIOR_AUTO);

            boolean isSmooth;
            if (Page.PAGE_SCROLL_BEHAVIOR_SMOOTH.equals(scrollBehavior)) {
                isSmooth = true;
            } else if (Page.PAGE_SCROLL_BEHAVIOR_INSTANT.equals(scrollBehavior)
                    || Page.PAGE_SCROLL_BEHAVIOR_AUTO.equals(scrollBehavior)) {
                isSmooth = false;
            } else {
                Log.e(TAG, "Unsupported scrollBehavior :" + scrollBehavior);
                return;
            }

            if (Page.PAGE_SCROLL_TYPE_TO.equals(scrollType)) {
                scrollTo(coordinateY, isSmooth);
            } else if (Page.PAGE_SCROLL_TYPE_BY.equals(scrollType)) {
                scrollBy(coordinateY, isSmooth);
            } else {
                Log.e(TAG, "Unsupported scrollType :" + scrollType);
            }
        }
    }

    private void scrollTo(int y, boolean isSmooth) {
        if (mRootView == null) {
            return;
        }
        VElement scrollerElement = mRootView.getDocument().getElementById(VElement.ID_BODY);
        if (scrollerElement != null) {
            Scroller scroller = (Scroller) scrollerElement.getComponent();
            if (isSmooth) {
                scroller.smoothScrollTo(y);
            } else {
                scroller.scrollTo(y);
            }
        }
    }

    private void scrollBy(int y, boolean isSmooth) {
        if (mRootView == null || y == 0) {
            return;
        }
        VElement scrollerElement = mRootView.getDocument().getElementById(VElement.ID_BODY);
        if (scrollerElement != null) {
            Scroller scroller = (Scroller) scrollerElement.getComponent();
            if (isSmooth) {
                scroller.smoothScrollBy(y);
            } else {
                scroller.scrollBy(y);
            }
        }
    }

    Rect getContentInsets() {
        int topCutoutHeight = mTopCutoutView != null ? mTopCutoutView.getLayoutParams().height : 0;
        int leftCutouWidth = mLeftCutoutView != null ? mLeftCutoutView.getLayoutParams().width : 0;
        int titleHeight = mToolbar == null ? 0 : mToolbar.getLayoutParams().height;
        int statusBarHeight =
                mStatusBarView == null || (mPage.isStatusBarImmersive() && !mPage.hasTitleBar())
                        ? 0
                        : mStatusBarView.getLayoutParams().height;
        Rect rect = new Rect(0, 0, 0, 0);
        if (null != mDecorLayout
                && mPage.getInnerPageTag() == Page.PAGE_TAG_MENUBAR_ABOUT
                && statusBarHeight == 0) {
            rect.top = topCutoutHeight + DisplayUtil.getStatusBarHeight(mDecorLayout.getContext());
        } else {
            rect.top = topCutoutHeight + titleHeight + statusBarHeight;
        }
        rect.left = leftCutouWidth;
        return rect;
    }

    int getStatusBarHeight() {
        return mStatusBarView == null ? 0 : mStatusBarView.getLayoutParams().height;
    }

    int getTitleHeight() {
        return mToolbar == null ? 0 : mToolbar.getLayoutParams().height;
    }

    public View getStatusBarView() {
        return mStatusBarView;
    }

    @Override
    public void onConfigurationChanged(HapConfiguration newConfig) {
        if (newConfig.getLastUiMode() != newConfig.getUiMode()) {
            setupStatusBar();
        }
    }

    private static class ProgressBarController {
        private static final int MESSAGE_PROGRESS_UPDATE = 1;
        private static final int PROGRESS_UPDATE_TIME = 500;
        protected boolean mLoading;
        private ProgressBar mProgressBar;
        private Handler mHandler =
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (!mLoading || mProgressBar == null) {
                            return;
                        }
                        switch (msg.what) {
                            case MESSAGE_PROGRESS_UPDATE:
                                int progress = mProgressBar.getProgress();
                                if (progress < 60) {
                                    mProgressBar.setProgress(progress + 10);
                                } else if (progress < 80) {
                                    mProgressBar.setProgress(progress + 5);
                                } else if (progress < 95) {
                                    mProgressBar.setProgress(progress + 1);
                                }
                                mHandler.sendEmptyMessageDelayed(MESSAGE_PROGRESS_UPDATE,
                                        PROGRESS_UPDATE_TIME);
                                break;
                            default:
                                break;
                        }
                    }
                };

        ProgressBarController(ProgressBar progressBar) {
            mProgressBar = progressBar;
        }

        public void showLoading() {
            mLoading = true;
            if (mProgressBar != null) {
                mProgressBar.setProgress(0);
                mHandler.sendEmptyMessageDelayed(MESSAGE_PROGRESS_UPDATE, PROGRESS_UPDATE_TIME);
            }
        }

        public void hideLoading() {
            mLoading = false;
            mHandler.removeMessages(MESSAGE_PROGRESS_UPDATE);
            mProgressBar = null;
        }
    }
}
