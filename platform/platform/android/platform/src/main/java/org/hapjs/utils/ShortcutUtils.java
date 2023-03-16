/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.distribution.DistributionManager;
import org.hapjs.distribution.ServerSettings;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;

public class ShortcutUtils {
    public static final int REMIND_LEAST_USE_TIMES = 3;
    public static final long REMIND_LAUNCH_DELAY = 5 * 1000; // 5 seconds
    public static final long REMIND_LEAST_USE_DURATION = 5 * 60 * 1000; // 5 minutes
    private static final String TAG = "ShortcutUtils";
    private static final long REMIND_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 1 day
    private static final long REFUSE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 1 day
    private static final long FORBIDDEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000; // 7 day

    public static boolean shouldCreateShortcutByPlatform(Context context, String pkg) {
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        return ((provider == null) ? true : provider.shouldCreateShortcutByPlatform(context, pkg))
                && !hasShortcutInstalled(context, pkg)
                && !SystemController.getInstance().isDisableSystemPrompt()
                && ShortcutManager.isSystemPromptEnabledByApp(pkg)
                && !isShortcutForbidden(pkg)
                && isRefuseByCountExpired(pkg)
                && !hasNativeAppInstalled(context, pkg);
    }

    public static boolean shouldCreateShortcutByFeature(String pkg) {
        return !isShortcutForbidden(pkg);
    }

    public static boolean isShortcutForbidden(String pkg) {
        return PreferenceUtils.getShortcutForbiddenTime(pkg)
                > System.currentTimeMillis() - FORBIDDEN_EXPIRE_TIME;
    }

    public static boolean isUseTimesReachRemind(String pkg) {
        List<String> record = PreferenceUtils.getUseRecord(pkg);
        if (record.size() < REMIND_LEAST_USE_TIMES) {
            return false;
        }
        return Long.parseLong(record.get(0)) > System.currentTimeMillis() - REFUSE_EXPIRE_TIME;
    }

    public static boolean installShortcut(Context context, String pkg, Source source) {
        if (CacheStorage.getInstance(context).hasCache(pkg)) {
            Cache cache = CacheStorage.getInstance(context).getCache(pkg);
            AppInfo appInfo = cache.getAppInfo();
            if (appInfo != null) {
                return ShortcutManager
                        .install(context, pkg, appInfo.getName(), cache.getIconUri(), source);
            } else {
                Log.e(TAG, "install shortcut failed");
                return false;
            }
        } else {
            Log.e(TAG, "install shortcut failed");
            return false;
        }
    }

    public static boolean uninstallShortcut(Context context, String pkg) {
        if (CacheStorage.getInstance(context).hasCache(pkg)) {
            Cache cache = CacheStorage.getInstance(context).getCache(pkg);
            AppInfo appInfo = cache.getAppInfo();
            if (appInfo != null) {
                return ShortcutManager.uninstall(context, pkg, appInfo.getName());
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "uninstall shortcut failed");
            return false;
        }
    }

    public static boolean isRefuseByCountExpired(String pkg) {
        return PreferenceUtils.getShortcutRefusedTimeByCount(pkg)
                <= System.currentTimeMillis() - REMIND_EXPIRE_TIME;
    }

    public static boolean hasNativeAppInstalled(Context context, String pkg) {
        ServerSettings serverSettings = DistributionManager.getInstance().getServerSettings(pkg);
        if (serverSettings == null) {
            return false;
        }
        String[] nativePackages = serverSettings.getNativePackages();
        if (nativePackages == null || nativePackages.length == 0) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        boolean hasInstalled = false;
        for (String nativePkg : nativePackages) {
            try {
                //flag 0 means packageInfo only contains versionCode and versionName.
                PackageInfo packageInfo = pm.getPackageInfo(nativePkg, 0);
                if (packageInfo != null) {
                    hasInstalled = true;
                    break;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, nativePkg+" not installed.", e);
            }
        }

        return hasInstalled;
    }

    public static boolean hasShortcutInstalled(Context context, String pkg) {
        return ShortcutManager.hasShortcutInstalled(context, pkg);
    }

    public static void updateAllShortcutsAsync(final Context context) {
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(context.getPackageName(),
                                            "updateAllShortcutsAsync");
                            List<Cache> caches =
                                    CacheStorage.getInstance(context).availableCaches();
                            if (caches == null || caches.isEmpty()) {
                                return;
                            }
                            for (Cache cache : caches) {
                                if (hasShortcutInstalled(context, cache.getPackageName())) {
                                    updateShortcut(context, cache.getPackageName());
                                }
                            }
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(context.getPackageName(),
                                            "updateAllShortcutsAsync");
                        });
    }

    public static void updateShortcutAsync(final Context context, final String packageName) {
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(context.getPackageName(),
                                            "updateShortcutAsync");
                            updateShortcut(context, packageName);
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(context.getPackageName(),
                                            "updateShortcutAsync");
                        });
    }

    private static void updateShortcut(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        Cache cache = CacheStorage.getInstance(context).getCache(packageName);
        AppInfo appInfo = cache.getAppInfo();
        if (appInfo == null) {
            return;
        }

        String appName = appInfo.getName();
        ShortcutManager.update(context, packageName, appName, cache.getIconUri());
    }
}
