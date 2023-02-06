/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class PackageUtils {
    public static final String TAG = "PackageUtils";

    private static final int VERSION_CODE_INVALID = -1;

    public static boolean isPackageInstalled(Context context, String packageName) {
        return getAppVersionCode(context, packageName) != VERSION_CODE_INVALID;
    }

    public static int getAppVersionCode(Context context) {
        return getAppVersionCode(context, context.getPackageName());
    }

    public static int getAppVersionCode(Context context, String packageName) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                PackageInfo pi;
                try {
                    pi = pm.getPackageInfo(packageName, 0);
                    if (pi != null) {
                        return pi.versionCode;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "getAppVersionCode: ", e);
                }
            }
        }
        return VERSION_CODE_INVALID;
    }

    public static int getPackageVersionCode(Context context, String apkPath) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                PackageInfo pi;
                try {
                    pi = pm.getPackageArchiveInfo(apkPath, 0);
                    if (pi != null) {
                        return pi.versionCode;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getPackageVersionCode: ", e);
                }
            }
        }
        return VERSION_CODE_INVALID;
    }

    public static String getAppVersionName(Context context) {
        return getAppVersionName(context, context.getPackageName());
    }

    public static String getAppVersionName(Context context, String packageName) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                PackageInfo pi;
                try {
                    pi = pm.getPackageInfo(packageName, 0);
                    if (pi != null) {
                        return pi.versionName;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "getAppVersionName: ", e);
                }
            }
        }
        return null;
    }

    public static boolean isSystemApp(Context context, String remotePkg) {
        if (context == null) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }

        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(remotePkg, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "isSystemApp: ", e);
        }
        return applicationInfo != null
                && (applicationInfo.flags
                & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
                != 0;
    }
}
