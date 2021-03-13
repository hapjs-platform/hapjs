/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.HybridRequest;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppDependency;
import org.hapjs.model.CardInfo;
import org.hapjs.pm.NativePackageProvider;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.runtime.CardConfig;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RouterManageProvider;

import java.util.Map;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_PACKAGE;

public class PackageUtils {
    private static final String TAG = "PackageUtils";

    private static final String MIN_APP_VERSION = "__MAV__";

    public static boolean openNativePackage(Context context, String rpkPkg, String appPkg) {
        boolean result = true;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(appPkg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PackageManager packageManager = context.getPackageManager();
        ResolveInfo info = packageManager.resolveActivity(intent, 0);
        if (info == null) {
            result = false;
            RuntimeLogManager.getDefault()
                    .logAppRouterNativeApp(rpkPkg, "", appPkg, "", VALUE_ROUTER_APP_FROM_PACKAGE,
                            result, "no compatible activity found", null);
            Log.i(TAG, "Fail to open native package: " + appPkg + ", resolveInfo is null");
        } else {
            result =
                    NavigationUtils.openNativeApp(
                            (Activity) context,
                            packageManager,
                            rpkPkg,
                            intent,
                            info,
                            VALUE_ROUTER_APP_FROM_PACKAGE,
                            "",
                            null);
        }
        return result;
    }

    public static boolean openHapPackage(
            Context context,
            String currentPackage,
            PageManager pm,
            HybridRequest request,
            Bundle extras,
            String routerFrom) {
        Intent intent = new Intent();
        if (!TextUtils.isEmpty(currentPackage)
                && HapEngine.getInstance(currentPackage).isCardMode()) {
            String intentPackage = CardConfig.getInstance().getPlatform();
            if (!TextUtils.isEmpty(intentPackage)) {
                intent.setPackage(intentPackage);
            }
        } else {
            intent.setPackage(context.getPackageName());
        }
        intent.setAction(Intent.ACTION_VIEW);

        Uri uri = Uri.parse(request.getUri());
        if (HapEngine.getInstance(currentPackage).isCardMode()
                && uri.getQueryParameter(MIN_APP_VERSION) == null) {
            Page page = pm.getCurrPage();
            if (page.getRoutableInfo() instanceof CardInfo) {
                CardInfo cardInfo = (CardInfo) page.getRoutableInfo();
                String targetPkg = request.getPackage();
                Map<String, AppDependency> dependencies = cardInfo.getQuickAppDependencies();
                if (dependencies != null && dependencies.get(targetPkg) != null) {
                    int minVersion = dependencies.get(targetPkg).minVersion;
                    uri =
                            uri.buildUpon()
                                    .appendQueryParameter(MIN_APP_VERSION,
                                            String.valueOf(minVersion))
                                    .build();
                }
            }
        }
        intent.setData(uri);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (extras != null) {
            intent.putExtras(extras);
        }

        PackageManager packageManager = context.getPackageManager();
        ResolveInfo info = packageManager.resolveActivity(intent, 0);
        if (info == null) {
            RuntimeLogManager.getDefault().logRouterQuickApp(currentPackage, "", routerFrom, false, "no compatible activity found");
            return false;
        }

        RouterManageProvider provider = ProviderManager.getDefault().getProvider(RouterManageProvider.NAME);
        if (UriUtils.isHybridUri(uri.toString())) {
            String targetPkg = UriUtils.getPkgFromHybridUri(uri);
            if (TextUtils.isEmpty(targetPkg)) {
                return false;
            }

            if (isSourcePkg(targetPkg) && provider.canGoBackToSourcePkg()) { //即将被调起的rpk是当前rpk的启动来源,允许直接调起
                Log.d(TAG, "go back to source pkg");
                context.startActivity(intent);
                RuntimeLogManager.getDefault().logRouterQuickApp(currentPackage, targetPkg, routerFrom, true, "go back to source pkg");
            } else {
                if (provider.inRouterRpkForbiddenList(context, currentPackage, targetPkg)) { //调起rpk限制名单
                    Log.d(TAG, "Fail to launch rpk: match router forbidden list");
                    RuntimeLogManager.getDefault().logRouterQuickApp(currentPackage, targetPkg, routerFrom, false, "match router forbidden list");
                    return false;
                }
                if (!provider.inRouterRpkDialogList(context, currentPackage, targetPkg)) { //调起rpk前弹窗提示用户
                    context.startActivity(intent);
                    RuntimeLogManager.getDefault().logRouterQuickApp(currentPackage, targetPkg, routerFrom, true, "do not display dialog");
                } else {
                    NavigationUtils.showRouterConfirmDialog((Activity) context, intent, currentPackage, uri.toString(), routerFrom, info, packageManager, "", true, targetPkg);
                    Log.d(TAG, "show open rpk dialog");
                }
            }
        } else {
            context.startActivity(intent);
        }
        return true;
    }


    public static boolean isSystemPackage(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            return pi != null && (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            // ignored
        }
        return false;
    }

    public static boolean isSystemSignature(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.checkSignatures("android", packageName) >= 0;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public static boolean isSystemAppOrSignature(Context context, String packageName) {
        return isSystemPackage(context, packageName) || isSystemSignature(context, packageName);
    }

    private static NativePackageProvider getNativePackageProvider() {
        return ProviderManager.getDefault().getProvider(NativePackageProvider.NAME);
    }

    public static PackageInfo getPackageInfo(Context context, String pkg, int flag) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(pkg, flag);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "package not found", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get package info", e);
        }
        return null;
    }

    //判断要调起的rpk是否是当前rpk的启动来源
    public static boolean isSourcePkg(String targetPkg) {
        Source source = Source.currentSource();
        if (source != null && TextUtils.equals(targetPkg, source.getPackageName())) {
            return true;
        }
        return false;
    }
}
