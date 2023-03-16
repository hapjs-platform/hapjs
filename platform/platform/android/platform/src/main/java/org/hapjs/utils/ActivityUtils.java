/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ActivityUtils {
    private static final String TAG = "ActivityUtils";

    private static Object sIActivityManager;
    private static Method sGetLaunchedFromPackageMethod;
    private static Method sGetActivityTokenMethod;
    private static WeakHashMap<Activity, String> sCacheLaunchedFromPkgs = new WeakHashMap<>();
    private static ConcurrentMap<String, Boolean> sCachePackage = new ConcurrentHashMap<>();
    private static String sHomePackage = "";

    static {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Class<?> activityManagerNativeClass =
                        Class.forName("android.app.ActivityManagerNative");
                Method getDefaultMethod = activityManagerNativeClass.getMethod("getDefault");
                sIActivityManager = getDefaultMethod.invoke(null);
            } else {
                Method getServiceMethod = ActivityManager.class.getMethod("getService");
                sIActivityManager = getServiceMethod.invoke(null);
            }
            sGetLaunchedFromPackageMethod =
                    sIActivityManager.getClass().getMethod("getLaunchedFromPackage", IBinder.class);
            sGetActivityTokenMethod = Activity.class.getMethod("getActivityToken");
        } catch (Exception e) {
            Log.e(TAG, "Fail to get IActivityManager", e);
        }
    }

    public synchronized static String getCallingPackage(Activity activity) {
        if (sIActivityManager == null
                || sGetLaunchedFromPackageMethod == null
                || sGetActivityTokenMethod == null) {
            return null;
        }

        String callingPkg = sCacheLaunchedFromPkgs.get(activity);
        if (!TextUtils.isEmpty(callingPkg)) {
            return callingPkg;
        }

        try {
            IBinder binder = (IBinder) sGetActivityTokenMethod.invoke(activity);
            callingPkg = (String) sGetLaunchedFromPackageMethod.invoke(sIActivityManager, binder);
            if (!TextUtils.isEmpty(callingPkg)) {
                sCacheLaunchedFromPkgs.put(activity, callingPkg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to getCallingPackage", e);
        }
        return callingPkg;
    }

    public static boolean shouldOverrideExitAnimation(Activity activity) {
        if (!activity.isTaskRoot()) {
            return false;
        }

        ActivityManager m = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = m.getRunningTasks(2);
        if (runningTaskInfoList != null && runningTaskInfoList.size() >= 2) {
            ComponentName topActivity = runningTaskInfoList.get(1).topActivity;
            return !isHomeActivity(activity, topActivity);
        }

        return true;
    }

    private static boolean isHomeActivity(Context context, ComponentName componentName) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList =
                context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo ri : resolveInfoList) {
            if (componentName.getPackageName().equals(ri.activityInfo.packageName)) {
                String classname = ri.activityInfo.name;
                if (classname.charAt(0) == '.') {
                    classname = ri.activityInfo.packageName + classname;
                }
                if (componentName.getClassName().equals(classname)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHomePackage(Context context, String pkgName) {
        if (sCachePackage.containsKey(pkgName)) {
            Boolean isHomePackage = sCachePackage.get(pkgName);
            return isHomePackage == null ? false : isHomePackage;
        }
        boolean isHome = false;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(pkgName);
        List<ResolveInfo> resolveInfoList =
                context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo ri : resolveInfoList) {
            if (pkgName.equals(ri.activityInfo.packageName)) {
                isHome = true;
                break;
            }
        }
        sCachePackage.put(pkgName, isHome);
        return isHome;
    }

    /**
     * 获取默认桌面的包名
     */
    public static String getHomePackage(Context context) {
        if (!TextUtils.isEmpty(sHomePackage)) {
            return sHomePackage;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        ResolveInfo ri =
                context.getPackageManager()
                        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri != null) {
            sHomePackage = ri.activityInfo.packageName;
        }
        return sHomePackage;
    }
}
