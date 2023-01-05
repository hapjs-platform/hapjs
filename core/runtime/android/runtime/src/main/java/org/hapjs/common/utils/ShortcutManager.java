/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.shortcut.ShortcutInstaller;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.system.SysOpProvider;

public class ShortcutManager {
    private static final String TAG = "ShortcutManager";

    private static final String INSTALL_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static SysOpProvider IMPL;
    private static Set<String> sDisableSystemPromptApps = new HashSet<>();

    static {
        IMPL = (SysOpProvider) ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
    }

    private ShortcutManager() {
    }

    public static boolean hasShortcutInstalled(Context context, String pkg) {
        return hasShortcutInstalled(context, pkg, "");
    }

    public static boolean hasShortcutInstalled(Context context, String pkg, String path) {
        return IMPL.hasShortcutInstalled(context, pkg, path);
    }

    public static boolean install(
            Context context, String pkg, String appName, Uri iconUri, Source source) {
        return install(context, pkg, "", "", appName, iconUri, source);
    }

    public static boolean install(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Uri iconUri,
            Source source) {
        Bitmap icon = IconUtils.getIconBitmap(context, iconUri);

        boolean result = installInner(context, pkg, path, appName, icon, source);
        IMPL.onShortcutInstallComplete(context, pkg, path, params, appName, iconUri,
                "", "", source, result);
        return result;
    }

    public static boolean install(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            String type,
            String serverIconUrl,
            Bitmap icon,
            Source source) {
        boolean result = installInner(context, pkg, path, appName, icon, source);
        IMPL.onShortcutInstallComplete(context, pkg, path, params, appName, null,
                type, serverIconUrl, source, result);
        return result;
    }

    private static boolean installInner(
            Context context, String pkg, String path, String appName, Bitmap icon, Source source) {
        if (icon == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT < 26) {
            return installOnBase(context, pkg, path, appName, icon, source);
        } else {
            return installAboveOreoInner(context, pkg, path, appName, icon, source, null, null);
        }
    }

    /**
     * 为已经安装的 app 添加快捷方式
     */
    public static boolean installInBackground(Context context, String pkg, Source source) {
        CacheStorage storage = CacheStorage.getInstance(context);
        if (!storage.hasCache(pkg)) {
            Log.w(TAG, "app is not installed, can't add shortcut, pkg: " + pkg);
            return false;
        }

        Cache cache = storage.getCache(pkg);
        AppInfo appInfo = cache.getAppInfo();
        if (appInfo == null) {
            Log.w(TAG, "parse app info failed, can't add shortcut, pkg: " + pkg);
            return false;
        }
        String appName = appInfo.getName();
        Bitmap icon = IconUtils.getIconBitmap(context, cache.getIconUri());
        return installInBackground(context, pkg, "", "", appName, icon, source);
    }

    public static boolean installInBackground(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Bitmap icon,
            Source source) {
        if (icon == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT < 26) {
            boolean result = installOnBase(context, pkg, path, appName, icon, source);
            IMPL.onShortcutInstallComplete(context, pkg, path, params, appName, null,
                    "", "", source, result);
            return result;
        } else {
            ShortcutInstaller.ResultLatch resultLatch =
                    ShortcutInstaller.getInstance()
                            .scheduleInstall(pkg, path, params, appName, icon, source);
            return resultLatch.waitForResult();
        }
    }

    private static boolean installOnBase(
            Context context, String pkg, String path, String appName, Bitmap icon, Source source) {
        if (icon == null) {
            return false;
        }
        Intent intent = new Intent(INSTALL_ACTION);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getLaunchIntent(context, pkg, path, source));
        intent.putExtra("duplicate", false);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
        context.sendBroadcast(intent);
        return true;
    }

    public static boolean installAboveOreo(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Bitmap icon,
            Source source,
            IntentSender intentSender) {
        boolean result =
                installAboveOreoInner(context, pkg, path, appName, icon, source, intentSender, null);
        IMPL.onShortcutInstallComplete(context, pkg, path, params, appName, null, "", "", source, result);
        return result;
    }

    public static boolean installAboveOreo(
            Context context,
            String pkg,
            String appName,
            Bitmap icon,
            String type,
            String serverIconUrl,
            Source source,
            IntentSender intentSender,
            PersistableBundle bundle) {
        boolean result = installAboveOreoInner(context, pkg, "", appName, icon, source, intentSender, bundle);
        IMPL.onShortcutInstallComplete(context, pkg, "", "", appName, null, type,
                serverIconUrl, source, result);
        return result;
    }

    @TargetApi(26)
    private static boolean installAboveOreoInner(
            Context context,
            String pkg,
            String path,
            String appName,
            Bitmap icon,
            Source source,
            IntentSender intentSender,
            PersistableBundle bundle) {
        android.content.pm.ShortcutManager shortcutManager =
                context.getSystemService(android.content.pm.ShortcutManager.class);
        if (shortcutManager.isRequestPinShortcutSupported()) {
            ShortcutInfo.Builder builder =
                    new ShortcutInfo.Builder(context, getShortcutId(pkg, path))
                            .setIcon(Icon.createWithBitmap(icon))
                            .setShortLabel(appName)
                            .setIntent(getLaunchIntent(context, pkg, path, source));
            ComponentName componentName = getLauncherActivity(context);
            //增加extra参数
            if (bundle != null) {
                builder.setExtras(bundle);
            }
            if (componentName != null) {
                builder.setActivity(componentName);
            }
            try {
                return shortcutManager.requestPinShortcut(builder.build(), intentSender);
            } catch (IllegalStateException e) {
                Log.e(TAG, "fail to requestPinShortcut:", e);
                return false;
            }
        }
        return false;
    }

    private static ComponentName getLauncherActivity(Context context) {
        Intent intent = new Intent(IntentUtils.getLaunchAction(context));
        intent.setPackage(context.getPackageName());
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> info = pm.queryIntentActivities(intent, 0);
        if (info != null && info.size() > 0) {
            ResolveInfo resolveInfo = info.get(0);
            return new ComponentName(context.getPackageName(), resolveInfo.activityInfo.name);
        }
        return null;
    }

    private static Intent getLaunchIntent(Context context, String pkg, String path, Source source) {
        Intent shortcutIntent = new Intent(IntentUtils.getLaunchAction(context));
        shortcutIntent.setPackage(context.getPackageName());
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
        if (!TextUtils.isEmpty(path)) {
            shortcutIntent.putExtra(RuntimeActivity.EXTRA_PATH, path);
        }
        source.setType(Source.TYPE_SHORTCUT);
        Source currentSource = Source.currentSource();
        if (currentSource != null) {
            // 检查 currentSource 中是否存在 entry，如果存在则传递到桌面
            if (currentSource.getInternal().containsKey(Source.INTERNAL_ENTRY)) {
                source.putInternal(Source.INTERNAL_ENTRY,
                        currentSource.getEntry().toJson().toString());
                currentSource.getInternal().remove(Source.INTERNAL_ENTRY);
            }
            // 检查 source 中是否已经设置了 original，没有的话把 currentSource 设置上
            if (!source.getExtra().containsKey(Source.EXTRA_ORIGINAL)) {
                source.putExtra(Source.EXTRA_ORIGINAL, currentSource.toJson().toString());
            }
        }
        shortcutIntent.putExtra(RuntimeActivity.EXTRA_SOURCE, source.toJson().toString());
        shortcutIntent
                .putExtra(RuntimeActivity.EXTRA_MODE, RuntimeActivity.MODE_LAUNCHED_FROM_HISTORY);
        return shortcutIntent;
    }

    public static boolean update(Context context, String pkg, String appName, Uri iconUri) {
        return update(context, pkg, "", appName, iconUri, false);
    }

    public static boolean update(Context context, String pkg, String path, String appName, Uri iconUri,
                                 boolean isOpIconUpdate) {
        Bitmap icon = IconUtils.getIconBitmap(context, iconUri);
        if (icon == null) {
            return false;
        }
        return update(context, pkg, path, "", appName, icon, isOpIconUpdate);
    }

    public static boolean update(Context context, String pkg, String path, String params, String appName,
                                 Bitmap icon, boolean isOpIconUpdate) {
        return IMPL.updateShortcut(context, pkg, path, params, appName, icon, isOpIconUpdate);
    }

    public static boolean uninstall(Context context, String pkg, String appName) {
        return IMPL.uninstallShortcut(context, pkg, appName);
    }

    public static synchronized void enableSystemPromptByApp(String pkg, boolean enabled) {
        if (enabled) {
            sDisableSystemPromptApps.remove(pkg);
        } else {
            sDisableSystemPromptApps.add(pkg);
        }
    }

    public static synchronized boolean isSystemPromptEnabledByApp(String pkg) {
        return !sDisableSystemPromptApps.contains(pkg);
    }

    public static String getShortcutId(String pkg, String path) {
        String shortcutId = pkg;
        if (!TextUtils.isEmpty(path)) {
            shortcutId = shortcutId + "/" + path;
        }
        return shortcutId;
    }
}
