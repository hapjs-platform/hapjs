/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_PACKAGE;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppDependency;
import org.hapjs.model.CardInfo;
import org.hapjs.pm.NativePackageProvider;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.runtime.CardConfig;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

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
                            result);
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
                            "");
        }
        return result;
    }

    public static boolean openHapPackage(
            Context context,
            String currentPackage,
            PageManager pm,
            HybridRequest request,
            Bundle extras) {
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
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.i(TAG, "Fail to open package: " + request.getPackage(), e);
            return false;
        }
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
}
