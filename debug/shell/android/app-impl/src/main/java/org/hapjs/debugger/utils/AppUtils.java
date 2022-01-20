/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import org.hapjs.debugger.DebuggerApplication;

public class AppUtils {

    public static final int INSTALL_REQUEST_CODE = 101;
    public static final int UNINSTALL_REQUEST_CODE = 102;
    private static final String TAG = "AppUtils";

    public static boolean hasInstalled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            return pi != null;
        } catch (Exception e) {
            // ignored
        }
        return false;
    }

    public static int getVersionCode(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            if (pi != null) {
                return pi.versionCode;
            }
        } catch (Exception e) {
            // ignored
        }
        return -1;
    }

    public static int getPlatformVersion(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (ai != null) {
                return ai.metaData.getInt("platformVersion");
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignored
        }
        return -1;
    }

    public static int findSpecifiedDebugCoreVersion(Context context, String platformPackageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(platformPackageName, PackageManager.GET_META_DATA);
            if (ai != null) {
                return ai.metaData.getInt("debuggerVersion");
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignored
        }
        return -1;
    }

    public static int getVersionCodeByApk(Context context, String apkpath) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(apkpath, 0);
            if (pi != null) {
                return pi.versionCode;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get apk version code", e);
        }
        return -1;
    }

    public static String getPackageNameByApk(Context context, String apkpath) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(apkpath, 0);
            if (pi != null) {
                return pi.packageName;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get apk package name", e);
        }
        return null;
    }

    public static void installApk(Context context, String apkpath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                File file = new File(apkpath);
                uri = FileProvider.getUriForFile(context, "org.hapjs.debugger.file", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(new File(apkpath));
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, INSTALL_REQUEST_CODE);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to install apk", e);
        }
    }

    public static void uninstallApk(Context context, String packageName) {
        try {
            Uri packageUri = Uri.parse("package:" + packageName);
            Intent intent = new Intent(Intent.ACTION_DELETE, packageUri);
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, UNINSTALL_REQUEST_CODE);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to uninstall apk", e);
        }
    }

    public static String getSerialNumber() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Build.SERIAL;
        } else if (Build.VERSION.SDK_INT < 29) {
            String serial = "";
            try {
                serial = Build.getSerial();
            } catch (SecurityException e) {
                Log.e(TAG, "getSerialNumber: ", e);
            }
            return serial;
        } else {
            return PreferenceUtils.getSerialNumber(DebuggerApplication.getInstance());
        }
    }
}
