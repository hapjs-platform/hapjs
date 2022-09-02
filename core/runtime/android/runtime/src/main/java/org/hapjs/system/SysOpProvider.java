/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.BaseJsSdkBridge;
import org.hapjs.bridge.HybridManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.MenubarItemData;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;

public interface SysOpProvider {
    String NAME = "sysop";
    String PARAM_KEY_QQ = "qqKey";
    String PARAM_KEY_WX = "wxKey";
    String PARAM_KEY_SINA = "sinaKey";
    String PARAM_SHARE_URL = "targetUrl";
    String PARAM_APPSIGN_KEY = "appSign";
    String PARAM_PACKAGE_KEY = "package";
    String PARAM_MENUBAR_KEY = "menubar";
    String PARAM_SHAREBUTTON_KEY = "sharebutton";
    String PARAM_TITLEBAR_TITLE_KEY = "title_name";
    String PARAM_TITLEBAR_TITLE_COLOR_KEY = "title_color";
    String PARAM_TITLEBAR_HOME_KEY = "title_home";
    final int MENUBAR_STATUS_REFRESH = 1;

    boolean shouldCreateShortcutByPlatform(Context context, String pkg);

    boolean hasShortcutInstalled(Context context, String pkg, String path);

    boolean updateShortcut(Context context, String pkg, String path, String params, String appName,
                           Bitmap icon, boolean isOpIconUpdate);

    boolean uninstallShortcut(Context context, String pkg, String appName);

    void onShortcutInstallComplete(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Uri iconUri,
            String type,
            String serverIconUrl,
            Source source,
            boolean isComplete);

    void setupStatusBar(Window window, boolean darkMode);

    boolean isNotificationEnabled(Context context, String pkg);

    boolean isTextSizeAdjustAuto();

    void showSystemMenu(Context context, AppInfo appInfo);

    Intent getAudioIntent();

    boolean isDisableShortcutPrompt(Context context, Source source);

    List<MenubarItemData> getMenuBarData(
            AppInfo appInfo,
            Context hybridContext,
            Context context,
            OnUpdateMenubarDataCallback onUpdateMenubarDataCallback);

    boolean onMenuBarItemClick(
            Context context,
            int position,
            String content,
            MenubarItemData menubarItemData,
            AppInfo appInfo,
            RootView rootView,
            Map<String, String> extra,
            OnMenubarCallback onMenubarCallback);

    Map<String, String> getMenuBarShareId(Context context);

    void onActivityResume(
            AppInfo appInfo, Context hybridContext, Context context, Map<String, String> extra);

    void onActivityPause(
            AppInfo appInfo, Context hybridContext, Context context, Map<String, String> extra);

    void onMenubarStatus(
            int status,
            AppInfo appInfo,
            Context hybridContext,
            Context context,
            Map<String, String> extra);

    boolean isShowMenuBar(Context context, AppInfo appInfo, Map<String, Object> otherdatas);

    boolean isShowMenuBarDefault(Context context, AppInfo appInfo, Map<String, Object> otherdatas);

    boolean isShowMenuBarPointTip(Context context, AppInfo appInfo, Map<String, Object> otherdatas);

    int getCpMenuBarStatus(Page page);

    boolean isUseAddShortCutStatus(Context context, AppInfo appInfo,
                                   Map<String, Object> otherdatas);

    boolean isCloseGlobalDefaultNightMode();

    boolean allowNightModeInAndroidVersion();

    boolean handleImageForceDark(ImageView imageView, boolean forceDark);

    boolean isAllowProfiler();

    ComponentName get5gMgrComponent();

    Intent getPermissionActivityIntent( String pkg);

    int getDesignWidth(Context context, AppInfo appInfo);

    public BaseJsSdkBridge getJsBridge(HybridManager hybridManager);

    boolean isAllowMenubarMove(Context context, AppInfo appInfo, Map<String, Object> otherdatas);

    View getToolBarView(Context context);

    boolean isDefaultToolBar(Context context);

    void initTitlebar(
            RootView rootView, AppInfo appInfo, Toolbar toolbar, HashMap<String, Object> datas);

    void updateTitlebar(
            RootView rootView, AppInfo appInfo, Toolbar toolbar, HashMap<String, Object> datas);

    float getDensityScaledRatio(Context context);

    public interface OnMenubarCallback {
        void onMenubarClickCallback(
                int position,
                String content,
                MenubarItemData menubarItemData,
                HashMap<String, Object> datas);
    }

    interface OnUpdateMenubarDataCallback {
        void onUpdateMenubarData(MenubarItemData menubarItemData);

        void isMenubarDataCollect(String packageName, boolean isCollected);
    }

    int getScreenWidthPixels(Context context, int platformVersion,HashMap<String, Object> datas);

    int getScreenHeightPixels(Context context, int platformVersion,HashMap<String, Object> datas);

    int getScreenOrientation(Page page, AppInfo info);

    boolean isFoldableDevice(Context context);

    boolean isFoldStatusByDisplay(Context context);

    int getFoldDisplayWidth(Context context);

    int getSafeAreaWidth(Context context);
}
