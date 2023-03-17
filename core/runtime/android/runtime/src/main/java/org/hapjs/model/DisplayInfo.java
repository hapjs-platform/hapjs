/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import org.hapjs.render.MultiWindowManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DisplayInfo {

    private static final String TAG = "DisplayInfo";
    private static final String KEY_PAGES = "pages";
    private static final String KEY_THEME_MODE = "themeMode";
    private static final String KEY_FIT_WIDE_SCREEN = "fitWideScreenMode";
    public static final String MODE_ORIGINAL = "original";
    public static final String MODE_FIT_SCREEN = "fitScreen";
    public static final String MODE_FILL_SCREEN = "fillScreen";
    public static final String MODE_MULTI_WINDOW = "multiWindow";
    private static final String KEY_MULTI_WINDOW_MODE = "multiWindowMode";
    private static final String KEY_MULTI_WINDOW_CONFIG = "multiWindowConfig";
    private static final String KEY_MULTI_WINDOW_CONFIG_MODE = "mode";
    public static final String MODE_SHOPPING = "shopping";
    public static final String MODE_NAVIGATION = "navigation";
    public static final String MODE_ADAPTIVE_SCREEN = "adaptiveScreen";

    private static final String KEY_PAGE_ANIMATION = "pageAnimation";

    private Style mDefaultStyle;
    private Map<String, Style> mPagesStyle;
    private int mThemeMode; // dark-no--0,dark_yes--1,auto-- -1
    private JSONObject mPageAnimation;
    private String mFitMode;
    public static final String KEY_NOT_VALID = "FALSE";
    public static final String KEY_VALID = "TRUE";

    public static DisplayInfo parse(JSONObject jsonObject) {
        DisplayInfo displayInfo = new DisplayInfo();
        displayInfo.mDefaultStyle = Style.parse(jsonObject);
        displayInfo.mThemeMode = jsonObject.optInt(KEY_THEME_MODE, -1);
        displayInfo.mPageAnimation = jsonObject.optJSONObject(KEY_PAGE_ANIMATION);

        String fitMode = jsonObject.optString(KEY_FIT_WIDE_SCREEN);
        if (!TextUtils.isEmpty(fitMode)) {
            displayInfo.mFitMode = fitMode;
            if (TextUtils.equals(fitMode, MODE_MULTI_WINDOW)) {
                String multiWindowMode = jsonObject.optString(KEY_MULTI_WINDOW_MODE);
                if (TextUtils.isEmpty(multiWindowMode)) {
                    JSONObject multiWindowConfigJsonObject = jsonObject.optJSONObject(KEY_MULTI_WINDOW_CONFIG);
                    if (multiWindowConfigJsonObject != null) {
                        multiWindowMode = multiWindowConfigJsonObject.optString(KEY_MULTI_WINDOW_CONFIG_MODE);
                    }
                }
                MultiWindowManager.setMultiWindowModeType(multiWindowMode);
            }
        }

        JSONObject pagesObject = jsonObject.optJSONObject(KEY_PAGES);
        if (pagesObject != null) {
            displayInfo.mPagesStyle = new HashMap<>();
            Iterator<String> pagesIterator = pagesObject.keys();
            while (pagesIterator.hasNext()) {
                String pageName = pagesIterator.next();
                try {
                    JSONObject pageObject = pagesObject.getJSONObject(pageName);
                    Style pageStyle = Style.parse(pageObject);
                    displayInfo.mPagesStyle.put(pageName, pageStyle);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return displayInfo;
    }

    public String getFitMode() {
        return mFitMode;
    }

    public void setFitMode(String fitMode) {
        mFitMode = fitMode;
    }

    public Style getDefaultStyle() {
        return mDefaultStyle;
    }

    public int getThemeMode() {
        return mThemeMode;
    }

    public Style getPageStyle(String pageName) {
        if (mPagesStyle == null) {
            return null;
        }
        return mPagesStyle.get(pageName);
    }

    public JSONObject getPageAnimation() {
        return mPageAnimation;
    }

    public List<TabBarInfo> getTabBarInfos() {
        if (null == mDefaultStyle) {
            return null;
        }
        return mDefaultStyle.mTabBarInfos;
    }

    public void setPageStyle(String pageName, Style pageStyle) {
        if (mPagesStyle != null && !TextUtils.isEmpty(pageName)) {
            mPagesStyle.put(pageName, pageStyle);
        }
    }

    public boolean isAppForceDark() {
        if (mDefaultStyle != null) {
            String forceDark = mDefaultStyle.get(DisplayInfo.Style.KEY_FORCE_DARK);
            if (TextUtils.isEmpty(forceDark)) {
                return true;
            }
            return Boolean.parseBoolean(forceDark);
        }
        return true;
    }

    public static class Style {
        public static final String KEY_BACKGROUND_COLOR = "backgroundColor";
        public static final String KEY_TITLE_BAR_BG_COLOR = "titleBarBackgroundColor";
        public static final String KEY_TITLE_BAR_BG_OPACITY = "titleBarBackgroundOpacity";
        public static final String KEY_TITLE_BAR_TEXT_COLOR = "titleBarTextColor";
        public static final String KEY_TITLE_BAR_TEXT = "titleBarText";

        public static final String KEY_FULL_SCREEN = "fullScreen";
        public static final String KEY_TITLE_BAR = "titleBar";
        public static final String KEY_MENU = "menu";
        public static final String KEY_PAGE_CACHE = "pageCache";
        public static final String KEY_PAGE_CACHE_DURATION = "cacheDuration";
        public static final String KEY_MENUBAR_DATA = "menuBarData";
        public static final String KEY_MENUBAR = "menuBar";
        public static final String KEY_MENU_STYLE = "menuBarStyle";
        public static final String KEY_MENUBAR_SHARE_TITLE = "shareTitle";
        public static final String KEY_MENUBAR_SHARE_DESCRIPTION = "shareDescription";
        public static final String KEY_MENUBAR_SHARE_ICON = "shareIcon";
        public static final String PARAM_SHARE_CURRENT_PAGE = "shareCurrentPage";
        public static final String PARAM_SHARE_URL = "shareUrl";
        public static final String PARAM_SHARE_PARAMS = "shareParams";
        public static final String PARAM_SHARE_USE_PAGE_PARAMS = "usePageParams";
        public static final String KEY_TABBAR_DATA = "tabBar";
        public static final String KEY_TABBAR_COLOR = "color";
        public static final String KEY_TABBAR_SELECTEDCOLOR = "selectedColor";
        public static final String KEY_TABBAR_BGCOLOR = "tabbarBackgroundColor";
        public static final String KEY_TABBAR_LIST = "list";
        public static final String KEY_MENUBAR_TIPS_CONTENT = "content";

        public static final String KEY_WINDOW_SOFT_INPUT_MODE = "windowSoftInputMode";
        public static final String KEY_ADJUST_PAN = "adjustPan";
        public static final String KEY_ADJUST_RESIZE = "adjustResize";
        public static final String KEY_ORIENTATION = "orientation";
        public static final String KEY_STATUS_BAR_IMMERSIVE = "statusBarImmersive";
        public static final String KEY_STATUS_BAR_TEXT_STYLE = "statusBarTextStyle";
        public static final String KEY_STATUS_BAR_BACKGROUND_COLOR = "statusBarBackgroundColor";
        public static final String KEY_STATUS_BAR_BACKGROUND_OPACITY = "statusBarBackgroundOpacity";
        public static final String KEY_STATUS_BAR_TEXT_LIGHT = "light";
        public static final String KEY_STATUS_BAR_TEXT_DARK = "dark";
        public static final String KEY_STATUS_BAR_TEXT_AUTO = "auto";
        public static final String KEY_FIT_CUTOUT = "fitCutout";
        public static final String KEY_FORCE_DARK = "forceDark";

        public static final String KEY_TEXT_SIZE_ADJUST = "textSizeAdjust";
        public static final String MENU_BAR_DARK_STYLE = "dark";
        public static final String MENU_BAR_LIGHT_STYLE = "light";
        public static final String KEY_PAGE_ANIMATION = "pageAnimation";

        private String mBackgroundColor;
        private String mTitleBarBgColor;
        private String mTitleBarBgOpacity;
        private String mTitleBarTextColor;
        private String mTitleBarText;
        private String mPageCache;
        private String mPageCacheDuration;

        private String mFullScreen;
        private String mTitleBar;
        private String mMenu;
        private String mMenuBar;
        private String mMenuBarStyle;
        private String mMenuBarTitle;
        private String mMenuBarDescription;
        private String mMenuBarIcon;
        private boolean mMenuBarCurrenPage;
        private boolean mConfigShareCurrentPage;
        private String mMenuBarShareUrl;
        private String mMenuBarShareParams;
        private String mMenuBarUsePageParams;
        private String mIsValidTabBar = KEY_NOT_VALID;
        private String mTabBarColor;
        private String mTabBarSelectedColor;
        private String mTabBarBgColor;
        private List<TabBarInfo> mTabBarInfos;
        private String mWindowSoftInputMode;
        private String mOrientation;
        private String mStatusBarImmersive;
        private String mStatusBarTextStyle;
        private String mStatusBarBackgroundColor;
        private String mStatusBarBackgroundOpacity;
        private String mTextSizeAdjust;
        private String mFitCutout;
        private String mForceDark;

        private JSONObject mPageAnimation;

        public static Style parse(JSONObject jsonObject) {
            Style style = new Style();
            style.mBackgroundColor = jsonObject.optString(KEY_BACKGROUND_COLOR, null);
            style.mTitleBarBgColor = jsonObject.optString(KEY_TITLE_BAR_BG_COLOR, null);
            style.mTitleBarBgOpacity = jsonObject.optString(KEY_TITLE_BAR_BG_OPACITY, null);
            style.mTitleBarTextColor = jsonObject.optString(KEY_TITLE_BAR_TEXT_COLOR, null);
            style.mTitleBarText = jsonObject.optString(KEY_TITLE_BAR_TEXT, null);
            style.mPageCache = jsonObject.optString(KEY_PAGE_CACHE, null);
            style.mPageCacheDuration = jsonObject.optString(KEY_PAGE_CACHE_DURATION, null);

            style.mFullScreen = jsonObject.optString(KEY_FULL_SCREEN, null);
            style.mTitleBar = jsonObject.optString(KEY_TITLE_BAR, null);
            style.mMenu = jsonObject.optString(KEY_MENU, null);
            style.mWindowSoftInputMode = jsonObject.optString(KEY_WINDOW_SOFT_INPUT_MODE, null);
            style.mOrientation = jsonObject.optString(KEY_ORIENTATION, null);

            style.mStatusBarImmersive = jsonObject.optString(KEY_STATUS_BAR_IMMERSIVE, null);
            style.mStatusBarTextStyle = jsonObject.optString(KEY_STATUS_BAR_TEXT_STYLE, null);
            style.mStatusBarBackgroundColor =
                    jsonObject.optString(KEY_STATUS_BAR_BACKGROUND_COLOR, null);
            style.mStatusBarBackgroundOpacity =
                    jsonObject.optString(KEY_STATUS_BAR_BACKGROUND_OPACITY, null);
            style.mTextSizeAdjust = jsonObject.optString(KEY_TEXT_SIZE_ADJUST, null);
            style.mFitCutout = jsonObject.optString(KEY_FIT_CUTOUT, null);
            style.mForceDark = jsonObject.optString(KEY_FORCE_DARK, null);

            JSONObject menudatajson = jsonObject.optJSONObject(KEY_MENUBAR_DATA);
            if (null != menudatajson) {
                style.mMenuBar = menudatajson.optString(KEY_MENUBAR, null);
                style.mMenuBarStyle = menudatajson.optString(KEY_MENU_STYLE, null);
                style.mMenuBarTitle = menudatajson.optString(KEY_MENUBAR_SHARE_TITLE, null);
                style.mMenuBarDescription =
                        menudatajson.optString(KEY_MENUBAR_SHARE_DESCRIPTION, null);
                style.mMenuBarIcon = menudatajson.optString(KEY_MENUBAR_SHARE_ICON, null);
                if (menudatajson.has(PARAM_SHARE_CURRENT_PAGE)) {
                    style.mConfigShareCurrentPage = true;
                }
                style.mMenuBarCurrenPage = menudatajson.optBoolean(PARAM_SHARE_CURRENT_PAGE, false);
                if (menudatajson.has(PARAM_SHARE_USE_PAGE_PARAMS)) {
                    style.mMenuBarUsePageParams = menudatajson.optBoolean(PARAM_SHARE_USE_PAGE_PARAMS, false) ? "true" : "false";
                } else {
                    style.mMenuBarUsePageParams = "";
                }
                style.mMenuBarShareUrl = menudatajson.optString(PARAM_SHARE_URL, "");
                style.mMenuBarShareParams = menudatajson.optString(PARAM_SHARE_PARAMS, "");
            } else {
                style.mMenuBarUsePageParams = "";
            }

            style.mPageAnimation = jsonObject.optJSONObject(KEY_PAGE_ANIMATION);
            JSONObject tabbarJson = jsonObject.optJSONObject(KEY_TABBAR_DATA);
            if (null != tabbarJson) {
                style.mIsValidTabBar = KEY_VALID;
                style.mTabBarColor = tabbarJson.optString(KEY_TABBAR_COLOR, null);
                style.mTabBarSelectedColor = tabbarJson.optString(KEY_TABBAR_SELECTEDCOLOR, null);
                style.mTabBarBgColor = tabbarJson.optString(KEY_TABBAR_BGCOLOR, null);
                JSONArray tabbarInfos = tabbarJson.optJSONArray(KEY_TABBAR_LIST);
                if (null != tabbarInfos) {
                    TabBarInfo tabBarInfo = null;
                    JSONObject tabbarObj = null;
                    style.mTabBarInfos = new ArrayList<>();
                    String tabBarPagePath = "";
                    String tabBarPageParams = "";
                    String tabBarIconPath = "";
                    String tabBarSelectedIconPath = "";
                    String tabbarText = "";
                    try {
                        for (int i = 0; i < tabbarInfos.length(); i++) {
                            tabbarObj = tabbarInfos.getJSONObject(i);
                            tabBarInfo = new TabBarInfo();
                            if (null != tabbarObj) {
                                tabBarPagePath = tabbarObj.optString(TabBarInfo.KEY_TABBAR_PAGE_PATH, "");
                                tabBarIconPath = tabbarObj.optString(TabBarInfo.KEY_TABBAR_ICONPATH, "");
                                tabBarSelectedIconPath = tabbarObj.optString(TabBarInfo.KEY_TABBAR_SELECTEDICONPATH, "");
                                tabbarText = tabbarObj.optString(TabBarInfo.KEY_TABBAR_TEXT, "");
                                tabBarPageParams = tabbarObj.optString(TabBarInfo.KEY_TABBAR_PAGEPARAMS, "");
                            } else {
                                tabBarPagePath = "";
                                tabBarIconPath = "";
                                tabBarSelectedIconPath = "";
                                tabbarText = "";
                                tabBarPageParams = "";
                            }
                            tabBarInfo.mTabBarPagePath = tabBarPagePath;
                            tabBarInfo.mTabBarIconPath = tabBarIconPath;
                            tabBarInfo.mTabBarSelectedIconPath = tabBarSelectedIconPath;
                            tabBarInfo.mTabBarText = tabbarText;
                            tabBarInfo.mTabBarPageParams = tabBarPageParams;
                            style.mTabBarInfos.add(tabBarInfo);
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "parse tabbarInfos exception : " + e.getMessage());
                    }
                }
            }
            return style;
        }

        public String get(String key) {
            switch (key) {
                case KEY_BACKGROUND_COLOR:
                    return mBackgroundColor;
                case KEY_TITLE_BAR_BG_COLOR:
                    return mTitleBarBgColor;
                case KEY_TITLE_BAR_BG_OPACITY:
                    return mTitleBarBgOpacity;
                case KEY_TITLE_BAR_TEXT_COLOR:
                    return mTitleBarTextColor;
                case KEY_TITLE_BAR_TEXT:
                    return mTitleBarText;
                case KEY_PAGE_CACHE:
                    return mPageCache;
                case KEY_PAGE_CACHE_DURATION:
                    return mPageCacheDuration;
                case KEY_FULL_SCREEN:
                    return mFullScreen;
                case KEY_TITLE_BAR:
                    return mTitleBar;
                case KEY_MENU:
                    return mMenu;
                case KEY_WINDOW_SOFT_INPUT_MODE:
                    return mWindowSoftInputMode;
                case KEY_ORIENTATION:
                    return mOrientation;
                case KEY_STATUS_BAR_IMMERSIVE:
                    return mStatusBarImmersive;
                case KEY_STATUS_BAR_TEXT_STYLE:
                    return mStatusBarTextStyle;
                case KEY_STATUS_BAR_BACKGROUND_COLOR:
                    return mStatusBarBackgroundColor;
                case KEY_STATUS_BAR_BACKGROUND_OPACITY:
                    return mStatusBarBackgroundOpacity;
                case KEY_TEXT_SIZE_ADJUST:
                    return mTextSizeAdjust;
                case KEY_FIT_CUTOUT:
                    return mFitCutout;
                case KEY_MENUBAR:
                    return mMenuBar;
                case KEY_MENU_STYLE:
                    return mMenuBarStyle;
                case KEY_MENUBAR_SHARE_TITLE:
                    return mMenuBarTitle;
                case KEY_MENUBAR_SHARE_DESCRIPTION:
                    return mMenuBarDescription;
                case KEY_MENUBAR_SHARE_ICON:
                    return mMenuBarIcon;
                case PARAM_SHARE_CURRENT_PAGE:
                    return mMenuBarCurrenPage ? "true" : (mConfigShareCurrentPage ? "false" : "");
                case PARAM_SHARE_USE_PAGE_PARAMS:
                    return mMenuBarUsePageParams;
                case PARAM_SHARE_URL:
                    return mMenuBarShareUrl;
                case PARAM_SHARE_PARAMS:
                    return mMenuBarShareParams;
                case KEY_FORCE_DARK:
                    return mForceDark;
                case KEY_TABBAR_COLOR:
                    return mTabBarColor;
                case KEY_TABBAR_SELECTEDCOLOR:
                    return mTabBarSelectedColor;
                case KEY_TABBAR_BGCOLOR:
                    return mTabBarBgColor;
                case KEY_TABBAR_DATA:
                    return mIsValidTabBar;
                default:
                    break;
            }

            return null;
        }

        public JSONObject getPageAnimation() {
            return mPageAnimation;
        }
    }
}
