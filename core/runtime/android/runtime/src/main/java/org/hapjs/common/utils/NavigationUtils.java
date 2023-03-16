/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.hapjs.bridge.HybridRequest;
import org.hapjs.cache.CacheStorage;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RouterManageProvider;
import org.hapjs.system.SysOpProvider;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NavigationUtils {
    public static final String EXTRA_SMS_BODY = "sms_body";
    private static final String TAG = "NavigationUtils";
    private static final String SCHEMA_TEL = "tel";
    private static final String SCHEMA_SMS = "sms";
    private static final String SCHEMA_MAILTO = "mailto";
    private static final String HOST_HAP_SETTINGS = "settings";
    private static final String PATH_LOCATION_SOURCE_MANAGER = "/location_source_manager";
    private static final String PATH_WLAN_MANAGER = "/wlan_manager";
    private static final String PATH_BLUETOOTH_MANAGER = "/bluetooth_manager";
    private static final String PATH_5G_MANAGER = "/5g";
    private static final String PATH_PERMISSIONS = "/permissions";
    private static final String GOOGLE_PLAY_PACKAGE = "com.android.vending";
    private static final String GOOGLE_SERVICE_PACKAGE = "com.google.android.gms";
    private static final String PATH_NFC_MANAGER = "/nfc_manager";

    private static final Set<String> WHITE_APP_SET = new HashSet<>();
    private static final Map<String, String> SETTING_MAP = new HashMap<>();
    private static WeakReference<AlertDialog> sDialogRef;
    private static final SysOpProvider sSysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);

    static {
        WHITE_APP_SET.add(GOOGLE_PLAY_PACKAGE);
        WHITE_APP_SET.add(GOOGLE_SERVICE_PACKAGE);
        SETTING_MAP.put(PATH_LOCATION_SOURCE_MANAGER, Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        SETTING_MAP.put(PATH_WLAN_MANAGER, Settings.ACTION_WIFI_SETTINGS);
        SETTING_MAP.put(PATH_BLUETOOTH_MANAGER, Settings.ACTION_BLUETOOTH_SETTINGS);
        SETTING_MAP.put(PATH_NFC_MANAGER, Settings.ACTION_NFC_SETTINGS);
    }

    public static boolean navigate(Context context, String pkg, HybridRequest request, Bundle extras, String routerAppFrom, String sourceH5) {
        if (request == null) {
            return false;
        }
        String url = request.getUri();
        if (url == null || url.startsWith("android-app://")) {
            return false;
        }

        Uri uri = Uri.parse(url);
        String schema = uri.getScheme();
        if (TextUtils.isEmpty(schema) || UriUtils.isWebSchema(schema)) {
            return false;
        }

        if (UriUtils.isHybridSchema(schema)) {
            return handleHapSetting(context, uri, pkg);
        }

        try {
            if (SCHEMA_TEL.equals(schema)) {
                dial(context, pkg, uri, extras, routerAppFrom, sourceH5);
            } else if (SCHEMA_SMS.equals(schema) || SCHEMA_MAILTO.equals(schema)) {
                sendto(context, pkg, uri, request, extras, routerAppFrom, sourceH5);
            } else {
                boolean isDeeplink = request.isDeepLink();
                return view(context, pkg, url, isDeeplink, extras, routerAppFrom, sourceH5);
            }
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Fail to navigate to url: " + url, e);
            return false;
        }
    }

    private static boolean handleHapSetting(Context context, Uri uri, String pkg) {
        String host = uri.getHost();
        if (HOST_HAP_SETTINGS.equals(host)) {
            String path = uri.getPath();
            String setting = SETTING_MAP.get(path);
            if (!TextUtils.isEmpty(setting)) {
                Intent intent = new Intent(setting);
                context.startActivity(intent);
                return true;
            } else if (TextUtils.equals(PATH_PERMISSIONS, path)) {
                return checkAndStartActivity(context, getPermissionActivityIntent(pkg));
            } else if (TextUtils.equals(PATH_5G_MANAGER, path)) {
                return checkAndStartActivity(context, get5gMgrIntent());
            }
        }
        return false;
    }

    private static Intent getPermissionActivityIntent(String pkg) {
        return sSysOpProvider.getPermissionActivityIntent(pkg);
    }

    private static Intent get5gMgrIntent() {
        ComponentName componentName = sSysOpProvider.get5gMgrComponent();
        Intent intent = new Intent();
        intent.setComponent(componentName);
        return intent;
    }

    private static boolean checkAndStartActivity(Context context, Intent intent) {
        if (intent == null) return false;
        ResolveInfo resolveInfo =
                context.getPackageManager()
                        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (null != resolveInfo) {
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed route to 5g mgr.", e);
            }
        } else {
            Log.e(TAG, "null of resolve info.");
        }
        return false;
    }

    private static void dial(Context context, String pkg, Uri uri, Bundle extras, String routerAppFrom, String sourceH5) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(uri);
        intent.putExtras(extras);
        context.startActivity(intent);

        statRouterNativeApp(context, pkg, uri.toString(), intent, routerAppFrom, true, "dial", sourceH5);
    }

    private static void sendto(
            Context context,
            String pkg,
            Uri uri,
            HybridRequest request,
            Bundle extras,
            String routerAppFrom,
            String sourceH5) {
        if (request != null && request.getParams() != null) {
            for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                if ("body".equals(entry.getKey())) {
                    extras.putString(EXTRA_SMS_BODY, entry.getValue());
                } else {
                    extras.putString(entry.getKey(), entry.getValue());
                }
            }
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(uri);
        intent.putExtras(extras);
        context.startActivity(intent);

        statRouterNativeApp(context, pkg, uri.toString(), intent, routerAppFrom, true, "sendto", sourceH5);
    }

    private static boolean view(
            Context context,
            String pkg,
            String url,
            boolean isDeepLink,
            Bundle extras,
            String routerAppFrom,
            String sourceH5) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            PackageManager packageManager = context.getPackageManager();
            ResolveInfo info = packageManager.resolveActivity(intent, 0);
            if (info == null) {
                statRouterNativeApp(context, pkg, url, intent, routerAppFrom, false, "no compatible activity found", sourceH5);
                return false;
            }
            String packageName = info.activityInfo.packageName;
            if (isDeepLink) {
                if (!isInWhiteList(packageName)
                        && !PackageUtils.isSystemPackage(context, packageName)) {
                    statRouterNativeApp(context, pkg, url, intent, routerAppFrom, false, "not in whitelist and not system app", sourceH5);
                    return false;
                }
            }
            if (url.startsWith(UriUtils.SCHEMA_INTENT)
                    &&
                    (packageName.equals(context.getPackageName()) || !info.activityInfo.exported)) {
                return false;
            }
            if (extras != null) {
                intent.putExtras(extras);
            }
            return openNativeApp((Activity) context, packageManager, pkg, intent, info, routerAppFrom, url, sourceH5);
        } catch (URISyntaxException e) {
            // ignore
        }
        return false;
    }

    public static boolean openNativeApp(
            Activity activity,
            PackageManager packageManager,
            String rpkPkg,
            Intent intent,
            ResolveInfo info,
            String routerAppFrom,
            String url,
            String sourceH5) {
        if (packageManager == null) {
            packageManager = activity.getPackageManager();
        }
        String packageName = info.activityInfo.packageName;
        RouterManageProvider routerProvider = ProviderManager.getDefault().getProvider(RouterManageProvider.NAME);
        if (routerProvider.inRouterForbiddenList(activity, rpkPkg, packageName, info) || !routerProvider.triggeredByGestureEvent(activity, rpkPkg)) {
            Log.d(TAG, "Fail to launch app: match router blacklist or open app without user input.");
            statRouterNativeApp(activity, rpkPkg, url, intent, routerAppFrom, false, "match router blacklist or open app without user input", sourceH5);
            return false;
        }

        if (!routerProvider.inRouterDialogList(activity, rpkPkg, packageName, info)) {
            if (!routerProvider.startActivityIfNeeded(activity, intent, rpkPkg)) {
                Log.d(TAG, "Fail to launch app: no matched apps.");
                statRouterNativeApp(activity, rpkPkg, url, intent, routerAppFrom, false, "no matched apps", sourceH5);
                return false;
            }
            statRouterNativeApp(activity, rpkPkg, url, intent, routerAppFrom, true, "do not display dialog", sourceH5);
        } else {
            Log.d(TAG, "show open app dialog");
            showRouterConfirmDialog(activity, intent, rpkPkg, url, routerAppFrom, info, packageManager, sourceH5, false, null);
        }

        return true;
    }

    public static void showRouterConfirmDialog(
            Activity activity,
            Intent intent,
            String rpkPkg,
            String url,
            String routerAppFrom,
            ResolveInfo info,
            PackageManager packageManager,
            String sourceH5, boolean startRpk, String targetRpk) {
        if (info == null) {
            return;
        }
        String message = getDialogMsg(activity, rpkPkg, info, packageManager, startRpk, targetRpk);
        if (TextUtils.isEmpty(message)) {
            return;
        }
        AlertDialog.Builder builder =
                new AlertDialog.Builder(
                        new ContextThemeWrapper(activity, ThemeUtils.getAlertDialogTheme()));
        builder.setMessage(message);
        builder.setCancelable(true);

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            Log.d(TAG, "activity is finishing");
                            return;
                        }
                        AlertDialog tempDialog = sDialogRef == null ? null : sDialogRef.get();
                        if (tempDialog != null && tempDialog.isShowing()) {
                            Log.d(TAG, "dialog already exists");
                            return;
                        }
                        Application.ActivityLifecycleCallbacks activityLifecycle =
                                new SimpleActivityLifecycleCallbacks() {
                                    @Override
                                    public void onActivityStopped(Activity activity) {
                                        AlertDialog tempDialog =
                                                sDialogRef == null ? null : sDialogRef.get();
                                        if (tempDialog != null && tempDialog.isShowing()) {
                                            tempDialog.dismiss();
                                        }
                                        sDialogRef = null;
                                        activity.getApplication()
                                                .unregisterActivityLifecycleCallbacks(this);
                                    }
                                };
                        DialogInterface.OnClickListener listener =
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sDialogRef = null;
                                        boolean tempResult = false;
                                        String failureMsg = "dialog confirm";
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            RouterManageProvider routerProvider =
                                                    ProviderManager.getDefault().getProvider(RouterManageProvider.NAME);
                                            tempResult = routerProvider.startActivityIfNeeded(activity, intent, rpkPkg);
                                            if (!tempResult) {
                                                failureMsg = "no matched apps";
                                            }
                                        } else {
                                            Log.d(TAG, "Fail to open native package: " + rpkPkg
                                                    + ", user denied");
                                            failureMsg = "dialog user denied";
                                        }
                                        activity
                                                .getApplication()
                                                .unregisterActivityLifecycleCallbacks(
                                                        activityLifecycle);
                                        if (!startRpk) {
                                            statRouterNativeApp(activity, rpkPkg, url, intent, routerAppFrom, tempResult,
                                                    failureMsg, sourceH5);
                                            RuntimeLogManager.getDefault()
                                                    .logRouterDialogClick(rpkPkg,
                                                            info.activityInfo.packageName, tempResult);
                                        } else {
                                            RuntimeLogManager.getDefault()
                                                    .logRouterQuickApp(rpkPkg, targetRpk, routerAppFrom, tempResult, failureMsg);
                                            RuntimeLogManager.getDefault().logRouterRpkDialogClick(rpkPkg, info.activityInfo.packageName, tempResult);
                                        }
                                    }
                                };
                        builder.setNegativeButton(android.R.string.cancel, listener);
                        builder.setPositiveButton(android.R.string.ok, listener);
                        builder.setOnCancelListener(
                                new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Log.d(TAG, "Fail to open native package: " + rpkPkg
                                                + ", canceled");
                                        sDialogRef = null;
                                        if (!startRpk) {
                                            statRouterNativeApp(activity, rpkPkg, url, intent,
                                                    routerAppFrom, false, "dialog user canceled", sourceH5);
                                            RuntimeLogManager.getDefault()
                                                    .logRouterDialogClick(rpkPkg,
                                                            info.activityInfo.packageName, false);
                                        } else {
                                            RuntimeLogManager.getDefault().logRouterQuickApp(rpkPkg, targetRpk, routerAppFrom, false, "dialog user canceled");
                                            RuntimeLogManager.getDefault().logRouterRpkDialogClick(rpkPkg, targetRpk, false);
                                        }
                                        activity
                                                .getApplication()
                                                .unregisterActivityLifecycleCallbacks(
                                                        activityLifecycle);
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        sDialogRef = new WeakReference<>(dialog);
                        activity.getApplication()
                                .registerActivityLifecycleCallbacks(activityLifecycle);
                        dialog.show();
                        if (!startRpk) {
                            RuntimeLogManager.getDefault()
                                    .logRouterDialogShow(rpkPkg, info.activityInfo.packageName);
                        } else {
                            RuntimeLogManager.getDefault().logRouterRpkDialogShow(rpkPkg, targetRpk);
                        }
                    }
                });
    }

    public static String getDialogMsg(Activity activity,
            String rpkPkg,
            ResolveInfo info,
            PackageManager packageManager,
            boolean isStartRpk,
            String targetRpk) {
        String message;
        if (isStartRpk) {
            if (TextUtils.isEmpty(targetRpk)) {
                return "";
            }
            String currentName;
            CacheStorage cacheStorage = CacheStorage.getInstance(activity);
            if (!cacheStorage.hasCache(rpkPkg)) {
                Log.d(TAG, "current rpk cache is null");
                return "";
            } else {
                currentName = cacheStorage.getCache(rpkPkg).getAppInfo().getName();
            }

            if (cacheStorage.hasCache(targetRpk)) {
                message = activity.getString(R.string.quick_app_open_quick_app_with_target, currentName, cacheStorage.getCache(targetRpk).getAppInfo().getName());
            } else {
                Log.d(TAG, "target rpk cache is null");
                message = activity.getString(R.string.quick_app_open_quick_app, currentName);
            }
        } else {
            if (info == null) {
                Log.d(TAG, "target app info is null");
                return "";
            }
            String appName = info.loadLabel(packageManager).toString();
            message = activity.getString(R.string.quick_app_open_native, appName);
        }
        return message;
    }

    public static void statRouterNativeApp(
            Context context,
            String pkg,
            String uri,
            Intent intent,
            String routerAppFrom,
            boolean result,
            String resultDesc,
            String sourceH5) {
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, 0);
        if (info != null) {
            RuntimeLogManager.getDefault()
                    .logAppRouterNativeApp(
                            pkg,
                            uri,
                            info.activityInfo.packageName,
                            info.activityInfo.name,
                            routerAppFrom,
                            result,
                            resultDesc,
                            sourceH5);
        }
    }

    private static boolean isInWhiteList(String pkg) {
        return WHITE_APP_SET.contains(pkg);
    }
}
