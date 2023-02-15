/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.system;

import static org.hapjs.render.MultiWindowManager.MULTI_WINDOW_DIVIDER_WIDTH;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hapjs.bridge.BaseJsSdkBridge;
import org.hapjs.bridge.HybridManager;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.FoldingUtils;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.common.utils.PackageUtils;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.model.ConfigInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.MenubarItemData;
import org.hapjs.render.MultiWindowManager;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.HybridDialog;
import org.hapjs.runtime.HybridDialogProvider;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RuntimeActivity;

public class DefaultSysOpProviderImpl implements SysOpProvider {

    // shareurl
    public static final String DEFAULT_WECHAT_APPID = "";
    public static final String DEFAULT_QQ_APPID = "";
    // test
    // public static final String DEFAULT_SHARE_URL = "http://dev.m.so-quick.cn/share/index.html";
    // product
    //    public static final String DEFAULT_SHARE_URL = "https://m.quickapp.cn/share/index.html";
    public static final String DEFAULT_SHARE_URL = "https://user.quickapp.cn/";
    public static final String DEFAULT_APPSIGN_VALUE = "";
    public static final String DEFAULT_PACKAGE_NAME = "";
    public static final String DEFAULT_SINA_APPID = "";
    public static final String DEFAULT_5G_MGR_PACKAGE = "com.android.phone";
    public static final String DEFAULT_5G_MGR_ACTIVITY = "com.android.phone.MobileNetworkSettings";
    private static final String TAG = "DefaultSysOpProvider";
    private static final String UNINSTALL_SHORTCUT_ACTION =
            "com.android.launcher.action.UNINSTALL_SHORTCUT";

    @Override
    public boolean shouldCreateShortcutByPlatform(Context context, String pkg) {
        return true;
    }

    @Override
    public boolean hasShortcutInstalled(Context context, String pkg, String path) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 26) {
            result = hasShortcutInstalledAboveOreo(context, pkg, path);
        }
        return result || hasShortcutInstalledOnBase(context, pkg, path);
    }

    @Override
    public boolean updateShortcut(Context context, String pkg, String path, String params, String appName,
                                  Bitmap icon, boolean isOpIconUpdate) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 26) {
            result = updateShortcutAboveOreo(context, pkg, path, appName, icon);
        }
        return result || updateShortcutOnBase(context, pkg, path, appName, icon);
    }

    @Override
    public boolean uninstallShortcut(Context context, String pkg, String appName) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 26) {
            result = uninstallShortcutAboveOreo(context, pkg, appName);
        }
        return result || uninstallShortcutOnBase(context, pkg, appName);
    }

    @Override
    public void onShortcutInstallComplete(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Uri iconUri,
            String type,
            String serverIconUrl,
            Source source,
            boolean isComplete) {
    }

    protected boolean hasShortcutInstalledOnBase(Context context, String pkg, String path) {
        return getShortcutPendingIntentOnBase(context, pkg, path) != null;
    }

    private Intent getShortcutPendingIntentOnBase(Context context, String pkg, String path) {
        Uri uri = getQueryUri();
        String[] projection = new String[]{"intent"};
        String selection = "itemType=1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, null, null);
            if (cursor == null) {
                return null;
            }
            while (cursor.moveToNext()) {
                String intentText = cursor.getString(0);
                try {
                    Intent intent = Intent.parseUri(intentText, 0);
                    if (IntentUtils.getLaunchAction(context).equals(intent.getAction())) {
                        String extraPackage = intent.getStringExtra(RuntimeActivity.EXTRA_APP);
                        String extraPath = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);
                        String extraShortcutId =
                                org.hapjs.common.utils.ShortcutManager
                                        .getShortcutId(extraPackage, extraPath);
                        String shortcutId =
                                org.hapjs.common.utils.ShortcutManager.getShortcutId(pkg, path);
                        if (TextUtils.equals(extraShortcutId, shortcutId)) {
                            return intent;
                        }
                    }
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getShortcutPendingIntentOnBase: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return null;
    }

    protected Uri getQueryUri() {
        String uriStr;
        if (Build.VERSION.SDK_INT < 8) {
            uriStr = "content://com.android.launcher.settings/favorites?Notify=true";
        } else if (Build.VERSION.SDK_INT < 19) {
            uriStr = "content://com.android.launcher2.settings/favorites?Notify=true";
        } else {
            uriStr = "content://com.android.launcher3.settings/favorites?Notify=true";
        }

        return Uri.parse(uriStr.toString());
    }

    protected boolean updateShortcutOnBase(
            Context context, String pkg, String path, String appName, Bitmap icon) {
        return false;
    }

    protected boolean uninstallShortcutOnBase(Context context, String pkg, String appName) {
        Intent shortcutIntent = getShortcutPendingIntentOnBase(context, pkg, "");
        if (shortcutIntent == null) {
            return false;
        }
        Intent intent = new Intent(UNINSTALL_SHORTCUT_ACTION);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        context.sendBroadcast(intent);
        return true;
    }

    @TargetApi(26)
    protected boolean hasShortcutInstalledAboveOreo(Context context, String pkg, String path) {
        return getShortcutInfoAboveOreo(context, pkg, path) != null;
    }

    @TargetApi(26)
    protected boolean updateShortcutAboveOreo(
            Context context, String pkg, String path, String appName, Bitmap icon) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        ShortcutInfo shortcutInfo = getShortcutInfoAboveOreo(context, pkg, path);
        if (shortcutInfo != null) {
            ShortcutInfo pinShortcutInfo =
                    new ShortcutInfo.Builder(context, shortcutInfo.getId())
                            .setIcon(Icon.createWithBitmap(icon))
                            .setShortLabel(appName)
                            .setIntent(shortcutInfo.getIntent())
                            .setActivity(shortcutInfo.getActivity())
                            .build();
            return shortcutManager.updateShortcuts(Arrays.asList(pinShortcutInfo));
        }
        return false;
    }

    @TargetApi(26)
    protected boolean uninstallShortcutAboveOreo(Context context, String pkg, String appName) {
        return false;
    }

    @TargetApi(26)
    private ShortcutInfo getShortcutInfoAboveOreo(Context context, String pkg, String path) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = shortcutManager.getPinnedShortcuts();
        String shortcutId = org.hapjs.common.utils.ShortcutManager.getShortcutId(pkg, path);
        if (shortcuts != null) {
            for (ShortcutInfo shortcutInfo : shortcuts) {
                if (TextUtils.equals(shortcutInfo.getId(), shortcutId)) {
                    return shortcutInfo;
                }
            }
        } else {
            Log.e(TAG, "getShortcutInfoAboveOreo shortcuts null");
        }
        return null;
    }

    @Override
    public void setupStatusBar(Window window, boolean darkMode) {
    }

    @Override
    public boolean isNotificationEnabled(Context context, String pkg) {
        return true;
    }

    @Override
    public boolean isTextSizeAdjustAuto() {
        return false;
    }

    @Override
    public void showSystemMenu(final Context context, final AppInfo appInfo) {
        HybridDialogProvider provider =
                ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
        HybridDialog dialog = provider.createAlertDialog(context, ThemeUtils.getAlertDialogTheme());
        ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1);
        adapter.add(context.getString(R.string.create_shortcut));
        dialog.setAdapter(
                adapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Executors.io()
                                        .execute(
                                                () -> {
                                                    Source source = new Source();
                                                    source.putExtra(Source.EXTRA_SCENE,
                                                            Source.SHORTCUT_SCENE_MENU);
                                                    String pkg = appInfo.getPackage();
                                                    if (!hasShortcutInstalled(context, pkg, "")) {
                                                        org.hapjs.common.utils.ShortcutManager
                                                                .install(
                                                                        context,
                                                                        pkg,
                                                                        appInfo.getName(),
                                                                        HapEngine.getInstance(pkg)
                                                                                .getApplicationContext()
                                                                                .getIcon(),
                                                                        source);
                                                    }
                                                });
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.createDialog();
        if (dialog instanceof Dialog) {
            DarkThemeUtil.disableForceDark((Dialog) dialog);
        }
        dialog.show();
    }

    @Override
    public Intent getAudioIntent() {
        return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    }

    @Override
    public boolean isDisableShortcutPrompt(Context context, Source source) {
        String sourcePackage = source != null ? source.getPackageName() : "";
        return !TextUtils.isEmpty(sourcePackage)
                && PackageUtils.isSystemAppOrSignature(context, sourcePackage);
    }

    public List<MenubarItemData> getMenuBarData(
            AppInfo appInfo,
            Context hybridContext,
            Context context,
            OnUpdateMenubarDataCallback onUpdateMenubarDataCallback) {
        return null;
    }

    @Override
    public boolean onMenuBarItemClick(
            Context context,
            int position,
            String content,
            MenubarItemData menubarItemData,
            AppInfo appInfo,
            RootView rootView,
            Map<String, String> extra,
            OnMenubarCallback onMenubarCallback) {
        return false;
    }

    @Override
    public Map<String, String> getMenuBarShareId(Context context) {
        Map<String, String> shareMap = new HashMap<>();
        shareMap.put(SysOpProvider.PARAM_KEY_QQ, DEFAULT_QQ_APPID);
        shareMap.put(SysOpProvider.PARAM_KEY_WX, DEFAULT_WECHAT_APPID);
        shareMap.put(SysOpProvider.PARAM_KEY_SINA, DEFAULT_SINA_APPID);
        shareMap.put(SysOpProvider.PARAM_SHARE_URL, DEFAULT_SHARE_URL);
        shareMap.put(SysOpProvider.PARAM_APPSIGN_KEY, DEFAULT_APPSIGN_VALUE);
        shareMap.put(SysOpProvider.PARAM_PACKAGE_KEY, DEFAULT_PACKAGE_NAME);
        return shareMap;
    }

    @Override
    public void onActivityResume(
            AppInfo appInfo, Context hybridContext, Context context, Map<String, String> extra) {
    }

    @Override
    public void onActivityPause(
            AppInfo appInfo, Context hybridContext, Context context, Map<String, String> extra) {
    }

    @Override
    public void onMenubarStatus(
            int status,
            AppInfo appInfo,
            Context hybridContext,
            Context context,
            Map<String, String> extra) {
    }

    @Override
    public boolean isShowMenuBar(Context context, AppInfo appInfo, Map<String, Object> otherdatas) {
        return true;
    }

    @Override
    public boolean isShowMenuBarDefault(
            Context context, AppInfo appInfo, Map<String, Object> otherdatas) {
        return true;
    }

    @Override
    public boolean isShowMenuBarPointTip(
            Context context, AppInfo appInfo, Map<String, Object> otherdatas) {
        return false;
    }

    @Override
    public int getCpMenuBarStatus(Page page) {
        return page.getMenuBarStatus();
    }

    @Override
    public boolean isUseAddShortCutStatus(
            Context context, AppInfo appInfo, Map<String, Object> otherdatas) {
        return false;
    }

    @Override
    public boolean isCloseGlobalDefaultNightMode() {
        return false;
    }

    @Override
    public boolean handleImageForceDark(ImageView imageView, boolean forceDark) {
        return false;
    }

    @Override
    public boolean allowNightModeInAndroidVersion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAllowProfiler() {
        return false;
    }

    @Override
    public ComponentName get5gMgrComponent() {
        return new ComponentName(DEFAULT_5G_MGR_PACKAGE, DEFAULT_5G_MGR_ACTIVITY);
    }

    @Override
    public int getDesignWidth(Context context, AppInfo appInfo) {
        ConfigInfo info = appInfo.getConfigInfo();
        return info == null ? ConfigInfo.DEFAULT_DESIGN_WIDTH : info.getDesignWidth();
    }

    @Override
    public Intent getPermissionActivityIntent(String pkg) {
        return null;
    }

    @Override
    public BaseJsSdkBridge getJsBridge(HybridManager hybridManager) {
        return null;
    }

    @Override
    public boolean isAllowMenubarMove(
            Context context, AppInfo appInfo, Map<String, Object> otherdatas) {
        return true;
    }

    public View getToolBarView(Context context) {
        return null;
    }

    public boolean isDefaultToolBar(Context context) {
        return true;
    }

    public void initTitlebar(
            RootView rootView, AppInfo appInfo, Toolbar toolbar, HashMap<String, Object> datas) {
    }

    public void updateTitlebar(
            RootView rootView, AppInfo appInfo, Toolbar toolbar, HashMap<String, Object> datas) {
    }

    @Override
    public float getDensityScaledRatio(Context context) {
        return 1f;
    }

    @Override
    public int getScreenWidthPixels(Context context, int platformVersion,HashMap<String, Object> datas) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        boolean isLandscapeMode = DisplayUtil.isLandscapeMode(context);
        int safeAreaWidth = getSafeAreaWidth(context);
        if (isFoldableDevice(context) && FoldingUtils.isMultiWindowMode()) {
            if (isFoldStatusByDisplay(context)) {
                return isLandscapeMode ? displayMetrics.heightPixels : displayMetrics.widthPixels;
            } else {
                int fullScreenWidth = isLandscapeMode ? displayMetrics.widthPixels + safeAreaWidth : displayMetrics.widthPixels;
                return (fullScreenWidth - MULTI_WINDOW_DIVIDER_WIDTH) / 2;
            }
        }
        if (platformVersion < 1063 && !BuildPlatform.isTV()) {
            return DisplayUtil.isLandscapeMode(context) ? displayMetrics.heightPixels : displayMetrics.widthPixels;
        }
        return displayMetrics.widthPixels;
    }

    @Override
    public int getScreenHeightPixels(Context context, int platformVersion,HashMap<String, Object> datas) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (platformVersion < 1063 && !BuildPlatform.isTV()) {
            return DisplayUtil.isLandscapeMode(context) ? displayMetrics.widthPixels : displayMetrics.heightPixels;
        }
        return displayMetrics.heightPixels;
    }

    @Override
    public int getScreenOrientation(Page page, AppInfo info) {
        int screenOrientation;
        if (page.hasSetOrientation()) {
            screenOrientation = page.getOrientation();
        } else {
            screenOrientation =
                    BuildPlatform.isTV() | BuildPlatform.isCar() ?
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        return screenOrientation;
    }

    @Override
    public boolean isFoldableDevice(Context context) {
        return false;
    }

    @Override
    public boolean isFoldStatusByDisplay(Context context) {
        return false;
    }

    @Override
    public int getFoldDisplayWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    @Override
    public int getSafeAreaWidth(Context context) {
        return 0;
    }

}