/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.MenubarView;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.RoutableInfo;
import org.hapjs.render.vdom.DocAnimator;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.BuildConfig;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.LocaleResourcesParser;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class Page implements IPage {
    /**
     * newly created page
     */
    public static final int STATE_NONE = 0;
    /**
     * page that has created in js but has not initialized
     */
    public static final int STATE_CREATED = 1;
    /**
     * page that has initialized but is not visible to user
     */
    public static final int STATE_INITIALIZED = 2;
    /**
     * page that visible to user
     */
    public static final int STATE_VISIBLE = 3;

    public static final int JS_LOAD_RESULT_NONE = 0;
    public static final int JS_LOAD_RESULT_SUCC = 1;
    public static final int JS_LOAD_RESULT_FAIL = 2;
    // page orientation: landscape
    public static final String ORIENTATION_LANDSCAPE = "landscape";
    // page orientation: portrait
    public static final String ORIENTATION_PORTRAIT = "portrait";
    public static final String KEY_PAGE_SCROLL_TYPE = "page_scroll_type";
    public static final String PAGE_SCROLL_TYPE_NOT_DEFINE = "not_define";
    public static final String PAGE_SCROLL_TYPE_TO = "to";
    public static final String PAGE_SCROLL_TYPE_BY = "by";
    public static final String PAGE_SCROLL_BEHAVIOR_AUTO = "auto";
    public static final String PAGE_SCROLL_BEHAVIOR_SMOOTH = "smooth";
    public static final String PAGE_SCROLL_BEHAVIOR_INSTANT = "instant";
    public static final int MENUBAR_DEFAULT = 0;
    public static final int MENUBAR_SHOW = 1;
    public static final int MENUBAR_HIDE = 2;
    public static final int PAGE_TAG_DEFAULT = 0;
    public static final int PAGE_TAG_MENUBAR_ABOUT = 1;
    private static final String PAGE_NAME = "currentPageName";
    private static final String PAGE_WINDOW_WIDTH = "windowWidth";
    private static final String PAGE_WINDOW_HEIGHT = "windowHeight";
    private static final String PAGE_STATUS_BAR_HEIGHT = "statusBarHeight";
    private static final String PAGE_TITLE_BAR_HEIGHT = "titleBarHeight";
    private static final String META_NAME = "name";
    private static final String META_PATH = "path";
    private static final String META_COMP = "component";
    private static final String PAGE_ORIENTATION = "orientation";
    private static final String MENU_BAR_DARK_STYLE = "dark";
    private static final String MENU_BAR_LIGHT_STYLE = "light";
    private static final String TAG = "Page";

    public final Map<String, ?> intent;
    public final Map<String, ?> meta;
    public final int pageId;
    private final AppInfo appInfo;
    private final RoutableInfo routableInfo;
    private final List<String> launchFlags;
    public Map<String, ?> params;
    private int mState;
    private VDocument mCacheDoc;
    private Page mReferrer;
    private Queue<RenderAction> mRenderActions;
    private HybridRequest mRequest;
    private HapConfiguration mConfiguration;
    private boolean mShouldReload;
    private boolean mShouldRefresh;
    private volatile int mLoadJsResult = JS_LOAD_RESULT_NONE;
    private int mInnerPageTag = PAGE_TAG_DEFAULT;
    private boolean mPageShowTitleBar = true;
    private boolean mIsMultiWindowLeftPage = false;

    private String mExtraTitleBarBackgroundColor;
    private String mExtraTitleBarBackgroundOpacity;
    private String mExtraTitleBarTextColor;
    private String mExtraTitleBarText;
    private String mExtraHasMenu;
    private String mExtraStatusBarBackgroundColor;
    private String mExtraStatusBarBackgroundOpacity;
    private String mExtraStatusBarTextStyle;
    private String mExtraStatusBarImmersive;
    private boolean mPageNotFound;
    private String mTargetPageUri;
    private long mPageLastUsedTime;
    private boolean mCleanCache = false;
    private PageAnimationConfig mPageAnimationConfig;
    private boolean mIsTabPage = false;

    public Page(
            AppInfo appInfo,
            RoutableInfo routableInfo,
            Map<String, ?> params,
            Map<String, ?> intent,
            int pageId,
            List<String> launchFlags) {
        this.params = params;
        this.appInfo = appInfo;
        this.routableInfo = routableInfo;
        this.pageId = pageId;
        this.intent = appendPageInfoToIntent(intent);
        this.meta = makePageMetaInfo(routableInfo);
        this.launchFlags = launchFlags;
        mPageLastUsedTime = System.currentTimeMillis();
        mPageAnimationConfig = parsePageAnimationConfig();
    }

    @Override
    public int getPageId() {
        return pageId;
    }

    private Map<String, ?> appendPageInfoToIntent(Map<String, ?> intent) {
        Map<String, Object> newIntent = new HashMap<>();
        newIntent.putAll(intent);
        newIntent.put(PAGE_NAME, routableInfo.getName());
        if (getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            newIntent.put(PAGE_ORIENTATION, ORIENTATION_LANDSCAPE);
        } else {
            newIntent.put(
                    PAGE_ORIENTATION,
                    BuildPlatform.isTV() ? ORIENTATION_LANDSCAPE : ORIENTATION_PORTRAIT);
        }
        return newIntent;
    }

    private Map<String, ?> makePageMetaInfo(RoutableInfo routableInfo) {
        Map<String, Object> metaInfo = new HashMap<>();
        metaInfo.put(META_NAME, routableInfo.getName());
        metaInfo.put(META_PATH, routableInfo.getPath());
        metaInfo.put(META_COMP, routableInfo.getComponent());
        return metaInfo;
    }

    public boolean isTabPage() {
        return mIsTabPage;
    }

    public void setTabPage(boolean mIsTabPage) {
        this.mIsTabPage = mIsTabPage;
    }

    public int getInnerPageTag() {
        return mInnerPageTag;
    }

    public void setInnerPageTag(int tag) {
        this.mInnerPageTag = tag;
    }

    public void setPageShowTitlebar(boolean isShow) {
        this.mPageShowTitleBar = isShow;
    }

    public String getName() {
        return routableInfo.getName();
    }

    @Override
    public String getPath() {
        return routableInfo.getPath();
    }

    public String getComponent() {
        return routableInfo.getComponent();
    }

    public List<String> getLaunchFlags() {
        return launchFlags;
    }

    public RoutableInfo getRoutableInfo() {
        return routableInfo;
    }

    public HybridRequest getRequest() {
        return mRequest;
    }

    public void setRequest(HybridRequest request) {
        mRequest = request;
    }

    public void setShouldReload(boolean shouldReload) {
        mShouldReload = shouldReload;
    }

    public boolean shouldReload() {
        return mShouldReload;
    }

    public void setShouldRefresh(boolean shouldRefresh) {
        mShouldRefresh = shouldRefresh;
    }

    public boolean shouldRefresh() {
        return mShouldRefresh;
    }

    public boolean isPageNotFound() {
        return mPageNotFound;
    }

    public void setPageNotFound(boolean pageNotFound) {
        mPageNotFound = pageNotFound;
    }

    public String getTargetPageUri() {
        return mTargetPageUri;
    }

    public void setTargetPageUri(String targetPageUri) {
        mTargetPageUri = targetPageUri;
    }

    public void setDisplayInfo(VDocument vdoc) {
        Map<String, Object> newIntent = new HashMap<>();
        newIntent.putAll(intent);
        int mStatusBarHeight = 0;
        int mTitleBarHeight = 0;
        int mWindowWidth = 0;
        int mWindowHeight = 0;
        DecorLayout decorLayout = (DecorLayout) vdoc.getComponent().getInnerView();
        if (decorLayout != null) {
            mStatusBarHeight = decorLayout.getStatusBarHeight();
            mTitleBarHeight = decorLayout.getTitleHeight();
            mWindowWidth = ((ViewGroup) decorLayout.getParent()).getMeasuredWidth();
            mWindowHeight =
                    ((ViewGroup) decorLayout.getParent()).getMeasuredHeight()
                            - decorLayout.getContentInsets().top;
        }
        newIntent.put(PAGE_STATUS_BAR_HEIGHT, mStatusBarHeight);
        newIntent.put(PAGE_TITLE_BAR_HEIGHT, mTitleBarHeight);
        newIntent.put(PAGE_WINDOW_WIDTH, mWindowWidth);
        newIntent.put(PAGE_WINDOW_HEIGHT, mWindowHeight);
        this.intent.putAll((Map) newIntent);
    }

    public VDocument getCacheDoc() {
        return mCacheDoc;
    }

    public void setCacheDoc(VDocument vdoc) {
        if (BuildConfig.DEBUG) {
            ThreadUtils.checkInMainThread();
        }

        mCacheDoc = vdoc;
    }

    public void clearCache() {
        clearRenderActions();

        if (mCacheDoc != null) {
            mCacheDoc.destroy();
            setCacheDoc(null);
        }
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        if (BuildConfig.DEBUG) {
            ThreadUtils.checkInMainThread();
        }

        mState = state;
    }

    public int getLoadJsResult() {
        return mLoadJsResult;
    }

    public void setIsMultiWindowLeftPage(boolean isMultiWindowLeftPage) {
        mIsMultiWindowLeftPage = isMultiWindowLeftPage;
    }

    public boolean getIsMultiWindowLeftPage() {
        return mIsMultiWindowLeftPage;
    }

    public void setLoadJsResult(int loadJsResult) {
        mLoadJsResult = loadJsResult;
    }

    public Page getReferrer() {
        return mReferrer;
    }

    public void setReferrer(Page referrer) {
        mReferrer = referrer;
    }

    public HapConfiguration getHapConfiguration() {
        return mConfiguration;
    }

    public void setHapConfiguration(HapConfiguration configuration) {
        mConfiguration = configuration;
    }

    public void pushRenderAction(RenderAction action) {
        if (mRenderActions == null) {
            mRenderActions = new LinkedList<>();
        }
        mRenderActions.add(action);
    }

    public RenderAction pollRenderAction() {
        return mRenderActions == null ? null : mRenderActions.poll();
    }

    public void clearRenderActions() {
        mRenderActions = null;
    }

    public boolean hasRenderActions() {
        return mRenderActions != null && mRenderActions.size() > 0;
    }

    public void setExtraTitleBarBackgroundColor(String color) {
        mExtraTitleBarBackgroundColor = color;
    }

    public void setExtraTitleBarBackgroundOpacity(String opacity) {
        mExtraTitleBarBackgroundOpacity = opacity;
    }

    public void setExtraTitleBarTextColor(String color) {
        mExtraTitleBarTextColor = color;
    }

    public void setExtraTitleBarText(String text) {
        mExtraTitleBarText = text;
    }

    public void setExtraHasMenu(String hasMenu) {
        mExtraHasMenu = hasMenu;
    }

    public void setExtraStatusBarBackgroundColor(String color) {
        mExtraStatusBarBackgroundColor = color;
    }

    public void setExtraStatusBarBackgroundOpacity(String opacity) {
        mExtraStatusBarBackgroundOpacity = opacity;
    }

    public void setExtraStatusBarTextStyle(String style) {
        mExtraStatusBarTextStyle = style;
    }

    public void setExtraStatusBarImmersive(String immersive) {
        mExtraStatusBarImmersive = immersive;
    }

    public boolean isForceDark() {
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        if (displayInfo != null && !displayInfo.isAppForceDark()) {
            return false;
        }

        String forceDark = getStyle(DisplayInfo.Style.KEY_FORCE_DARK, null, "true");
        if (TextUtils.isEmpty(forceDark)) {
            return true;
        }
        return Boolean.parseBoolean(forceDark);
    }

    public boolean isFullScreen() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_FULL_SCREEN, null, null);

        if (TextUtils.isEmpty(parseValue)) {
            return false;
        }

        return Boolean.valueOf(parseValue);
    }

    public boolean isTextSizeAdjustAuto() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_TEXT_SIZE_ADJUST, null, null);

        if (TextUtils.equals("auto", parseValue)) {
            return true;
        } else if (TextUtils.equals("none", parseValue)) {
            return false;
        }

        // default value
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        return sysOpProvider.isTextSizeAdjustAuto();
    }

    public boolean hasTitleBar() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_TITLE_BAR, null, null);

        if (TextUtils.isEmpty(parseValue)) {
            return mPageShowTitleBar;
        }

        return Boolean.valueOf(parseValue) && mPageShowTitleBar;
    }

    public boolean hasMenu() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENU, mExtraHasMenu, null);

        if (TextUtils.isEmpty(parseValue)) {
            return false;
        }

        return Boolean.valueOf(parseValue);
    }

    public int getMenuBarStatus() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENUBAR, null, null);

        if (TextUtils.isEmpty(parseValue)) {
            return MENUBAR_DEFAULT;
        }

        if (Boolean.valueOf(parseValue)) {
            return MENUBAR_SHOW;
        } else {
            return MENUBAR_HIDE;
        }
    }

    public int getMenuBarStyle() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENU_STYLE, null, null);

        if (TextUtils.isEmpty(parseValue)) {
            return MenubarView.TITLEBAR_STYLE_DARK;
        }
        if (MENU_BAR_LIGHT_STYLE.equals(parseValue)) {
            return MenubarView.TITLEBAR_STYLE_LIGHT;
        } else {
            return MenubarView.TITLEBAR_STYLE_DARK;
        }
    }

    public String getMenuBarShareCurrentPage() {
        String parseValue = getStyle(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE,
                null, "");
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarUsePageParams() {
        String parseValue = getPageStyle(DisplayInfo.Style.PARAM_SHARE_USE_PAGE_PARAMS,
                null, "");
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarShareUrl() {
        String parseValue = getStyle(DisplayInfo.Style.PARAM_SHARE_URL, null, null);
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarShareParams() {
        String parseValue = getPageStyle(DisplayInfo.Style.PARAM_SHARE_PARAMS, null, null);
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarTitle() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE, null, null);
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarDescription() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION, null, null);
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public String getMenuBarIcon() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON, null, null);
        if (TextUtils.isEmpty(parseValue)) {
            return "";
        }
        return parseValue;
    }

    public boolean hasSetOrientation() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_ORIENTATION, null, null);
        if (!TextUtils.isEmpty(parseValue)) {
            return true;
        }
        return false;
    }

    public int getOrientation() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_ORIENTATION, null, null);
        if (!TextUtils.isEmpty(parseValue) && ORIENTATION_LANDSCAPE.equals(parseValue)) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    public int getTitleBarBackgroundColor() {
        return getColor(
                DisplayInfo.Style.KEY_TITLE_BAR_BG_COLOR, mExtraTitleBarBackgroundColor,
                Color.BLACK);
    }

    public float getTitleBarBackgroundOpacity() {
        String parseValue =
                getStyle(DisplayInfo.Style.KEY_TITLE_BAR_BG_OPACITY,
                        mExtraTitleBarBackgroundOpacity, null);

        if (TextUtils.isEmpty(parseValue)) {
            return 1.0f;
        }

        return Float.parseFloat(parseValue);
    }

    public int getTitleBarTextColor() {
        return getColor(
                DisplayInfo.Style.KEY_TITLE_BAR_TEXT_COLOR, mExtraTitleBarTextColor, Color.WHITE);
    }

    public int getBackgroundColor() {
        return getColor(DisplayInfo.Style.KEY_BACKGROUND_COLOR, null, Color.WHITE);
    }

    public String getTitleBarText() {
        String titleBarText = mExtraTitleBarText;
        if (TextUtils.isEmpty(titleBarText)) {
            if (params != null) {
                titleBarText = (String) params.get(DisplayInfo.Style.KEY_TITLE_BAR_TEXT);
            }
        }

        String text = getStyle(DisplayInfo.Style.KEY_TITLE_BAR_TEXT, titleBarText, "");
        Locale locale = mConfiguration != null ? mConfiguration.getLocale() : Locale.getDefault();
        return LocaleResourcesParser.getInstance().getText(appInfo.getPackage(), locale, text);
    }

    public boolean isStatusBarImmersive() {
        String parseValue =
                getStyle(DisplayInfo.Style.KEY_STATUS_BAR_IMMERSIVE, mExtraStatusBarImmersive,
                        null);
        if (TextUtils.isEmpty(parseValue)) {
            return false;
        }
        return Boolean.valueOf(parseValue);
    }

    public String getStatusBarTextStyle() {
        String parseValue =
                getStyle(DisplayInfo.Style.KEY_STATUS_BAR_TEXT_STYLE, mExtraStatusBarTextStyle,
                        null);
        if (TextUtils.isEmpty(parseValue)) {
            return DisplayInfo.Style.KEY_STATUS_BAR_TEXT_AUTO;
        }
        return parseValue;
    }

    public int getStatusBarBackgroundColor() {
        return getColor(
                DisplayInfo.Style.KEY_STATUS_BAR_BACKGROUND_COLOR,
                mExtraStatusBarBackgroundColor,
                getTitleBarBackgroundColor());
    }

    public float getStatusBarBackgroundOpacity() {
        String parseValue =
                getStyle(
                        DisplayInfo.Style.KEY_STATUS_BAR_BACKGROUND_OPACITY,
                        mExtraStatusBarBackgroundOpacity,
                        null);

        if (TextUtils.isEmpty(parseValue)) {
            return getTitleBarBackgroundOpacity();
        }
        return Float.parseFloat(parseValue);
    }

    public int getWindowSoftInputMode() {
        final String modeValue =
                getStyle(
                        DisplayInfo.Style.KEY_WINDOW_SOFT_INPUT_MODE, null,
                        DisplayInfo.Style.KEY_ADJUST_PAN);
        if (TextUtils.isEmpty(modeValue)) {
            // default
            return WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
        }
        int softInputMode;
        switch (modeValue) {
            case DisplayInfo.Style.KEY_ADJUST_PAN: {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                break;
            }
            case DisplayInfo.Style.KEY_ADJUST_RESIZE: {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                break;
            }
            default: {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
            }
        }
        return softInputMode;
    }

    public String getFitCutout() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_FIT_CUTOUT, null, "none");

        return parseValue.toLowerCase();
    }

    public boolean shouldCache() {
        String parseValue = getStyle(DisplayInfo.Style.KEY_PAGE_CACHE, null, "false");

        if (!"true".equals(parseValue) && !"false".equals(parseValue)) {
            return false;
        }

        return Boolean.parseBoolean(parseValue) && !mCleanCache;
    }

    public void cleanCache() {
        mCleanCache = true;
    }

    public long getCacheExpiredTime() {
        // 默认的缓存时间是一个小时
        String parseValue =
                getStyle(DisplayInfo.Style.KEY_PAGE_CACHE_DURATION, null,
                        String.valueOf(60 * 1000 * 60));
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(parseValue);
        if (!isNum.matches()) {
            parseValue = String.valueOf(60 * 1000 * 60);
        }
        return Long.parseLong(parseValue);
    }

    public void touchCacheUsedTime() {
        mPageLastUsedTime = System.currentTimeMillis();
    }

    public long getPageLastUsedTime() {
        return mPageLastUsedTime;
    }

    private int getColor(String key, String extraValue, int defaultValue) {
        String parseValue = getStyle(key, extraValue, null);

        if (TextUtils.isEmpty(parseValue)) {
            return defaultValue;
        }

        return ColorUtil.getColor(parseValue, defaultValue);
    }

    private String getStyle(String key, String extraValue, String defaultValue) {
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        if (displayInfo == null) {
            return defaultValue;
        }

        String parseValue = extraValue;
        if (TextUtils.isEmpty(parseValue)) {
            DisplayInfo.Style pageStyle = displayInfo.getPageStyle(getName());
            if (pageStyle != null) {
                parseValue = pageStyle.get(key);
            }
        }

        if (TextUtils.isEmpty(parseValue)) {
            DisplayInfo.Style defaultStyle = displayInfo.getDefaultStyle();
            if (defaultStyle != null) {
                parseValue = defaultStyle.get(key);
            }
        }

        if (TextUtils.isEmpty(parseValue)) {
            return defaultValue;
        }

        return parseValue;
    }

    private String getPageStyle(String key, String extraValue, String defaultValue) {
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        if (displayInfo == null) {
            return defaultValue;
        }
        String parseValue = extraValue;
        if (TextUtils.isEmpty(parseValue)) {
            DisplayInfo.Style pageStyle = displayInfo.getPageStyle(getName());
            if (pageStyle != null) {
                parseValue = pageStyle.get(key);
            }
        }
        if (TextUtils.isEmpty(parseValue)) {
            return defaultValue;
        }
        return parseValue;
    }

    private PageAnimationConfig parsePageAnimationConfig() {
        PageAnimationConfig config = null;

        // router.push()优先
        JSONObject pageAnimateSettingObj = null;
        if (params != null && params.size() > 0) {
            Object obj = params.get(HybridRequest.PARAM_PAGE_ANIMATION);
            if (obj instanceof String) {
                String animationStr = obj.toString().trim();
                try {
                    pageAnimateSettingObj = new JSONObject(animationStr);
                } catch (JSONException e) {
                    Log.e(TAG, "parsePageAnimationConfig: ", e);
                }

            }
        }
        // manifest.json配置的单个页面其次
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        DisplayInfo.Style pageStyle = displayInfo != null ? displayInfo.getPageStyle(getName()) : null;
        if (pageAnimateSettingObj == null && pageStyle != null) {
            pageAnimateSettingObj = pageStyle.getPageAnimation();
        }
        // manifest.json配置的全局最后
        if (pageAnimateSettingObj == null && displayInfo != null) {
            pageAnimateSettingObj = displayInfo.getPageAnimation();
        }

        if (pageAnimateSettingObj != null) {
            Object openEnterObj = pageAnimateSettingObj.opt(Attributes.PageAnimation.ACTION_OPEN_ENTER);
            Object openExitObj = pageAnimateSettingObj.opt(Attributes.PageAnimation.ACTION_OPEN_EXIT);
            Object closeEnterObj = pageAnimateSettingObj.opt(Attributes.PageAnimation.ACTION_CLOSE_ENTER);
            Object closeExitObj = pageAnimateSettingObj.opt(Attributes.PageAnimation.ACTION_CLOSE_EXIT);
            config = new PageAnimationConfig();
            if (openEnterObj instanceof String) {
                config.openEnter = openEnterObj.toString().trim();
            }
            if (openExitObj instanceof String) {
                config.openExit = openExitObj.toString().trim();
            }
            if (closeEnterObj instanceof String) {
                config.closeEnter = closeEnterObj.toString().trim();
            }
            if (closeExitObj instanceof String) {
                config.closeExit = closeExitObj.toString().trim();
            }
        }
        return config;
    }


    public int getPageAnimation(String animationType, int defValue) {
        if (animationType == null || mPageAnimationConfig == null) {
            return defValue;
        }
        String animation;
        int animationId = defValue;
        switch (animationType) {
            case Attributes.PageAnimation.ACTION_OPEN_ENTER:
                animation = mPageAnimationConfig.openEnter;
                animationId = DocAnimator.TYPE_PAGE_OPEN_ENTER;
                break;
            case Attributes.PageAnimation.ACTION_OPEN_EXIT:
                animation = mPageAnimationConfig.openExit;
                animationId = DocAnimator.TYPE_PAGE_OPEN_EXIT;
                break;
            case Attributes.PageAnimation.ACTION_CLOSE_ENTER:
                animation = mPageAnimationConfig.closeEnter;
                animationId = DocAnimator.TYPE_PAGE_CLOSE_ENTER;
                break;
            case Attributes.PageAnimation.ACTION_CLOSE_EXIT:
                animation = mPageAnimationConfig.closeExit;
                animationId = DocAnimator.TYPE_PAGE_CLOSE_EXIT;
                break;
            default:
                animation = Attributes.PageAnimation.NONE;
        }

        //default animation : slide
        if (animation == null) {
            animation = Attributes.PageAnimation.SLIDE;
        }

        if (Attributes.PageAnimation.SLIDE.equalsIgnoreCase(animation)) {
            return animationId;
        } else if (Attributes.PageAnimation.NONE.equalsIgnoreCase(animation)) {
            //0表示无动画
            return DocAnimator.TYPE_UNDEFINED;
        }
        return animationId;
    }


    @Override
    public String toString() {
        return "app: "
                + appInfo
                + ", routableInfo: "
                + routableInfo
                + ", params: "
                + params
                + ", state: "
                + mState;
    }

    public interface LoadPageJsListener {
        void onLoadStart(Page page);

        void onLoadFinish(Page page);
    }

    private static class PageAnimationConfig {

        private String openEnter;
        private String closeEnter;
        private String openExit;
        private String closeExit;

        private PageAnimationConfig() {
        }

        @Override
        public String toString() {
            return "PageAnimationConfig{" +
                    "mOpenEnter='" + openEnter + '\'' +
                    ", mCloseEnter='" + closeEnter + '\'' +
                    ", mOpenExit='" + openExit + '\'' +
                    ", mCloseExit='" + closeExit + '\'' +
                    '}';
        }
    }
}
