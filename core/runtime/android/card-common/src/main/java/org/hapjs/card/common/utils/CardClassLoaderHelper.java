/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.common.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

public class CardClassLoaderHelper {
    private static String TAG = "CardClassLoaderHelper";
    private static volatile ClassLoader sClassLoader;
    private static Object sLock = new Object();

    public static ClassLoader getClassLoader() {
        return sClassLoader;
    }

    public static ClassLoader getClassLoader(
            Context context, ClassLoader classLoader, String platform) {
        if (sClassLoader == null) {
            synchronized (sLock) {
                if (sClassLoader == null) {
                    try {
                        if (classLoader == null) {
                            classLoader = context.getClassLoader();
                        }

                        ApplicationInfo ai =
                                context.getPackageManager().getApplicationInfo(platform, 0);
                        sClassLoader =
                                new CardClassLoader(
                                        context,
                                        platform,
                                        ai.sourceDir,
                                        context.getDir("odex", Context.MODE_PRIVATE)
                                                .getAbsolutePath(),
                                        getNativeLibraryDir(ai.nativeLibraryDir),
                                        classLoader);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.i(TAG, " getClassLoader failed", e);
                    }
                }
            }
        }
        return sClassLoader;
    }

    public static void clearClassLoader() {
        sClassLoader = null;
    }

    // judge host app is user 64 so
    private static boolean isHost64() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Process.is64Bit();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0].contains("arm64");
        } else {
            return Build.CPU_ABI.contains("arm64");
        }
    }

    /* if host app is armeabi-v8a(32),need set classLoader librarySearchPath to arm directory*/
    private static String getNativeLibraryDir(String platformNativeLibraryDir) {
        try {
            if (TextUtils.isEmpty(platformNativeLibraryDir)) {
                return platformNativeLibraryDir;
            }
            Log.d(TAG, "original load so from: " + platformNativeLibraryDir);
            boolean isPlatform64 = platformNativeLibraryDir.endsWith("arm64");
            if (isHost64()) {
                return isPlatform64 ? platformNativeLibraryDir : platformNativeLibraryDir + "64";
            } else {
                return isPlatform64
                        ?
                        platformNativeLibraryDir.substring(0, platformNativeLibraryDir.length() - 2)
                        : platformNativeLibraryDir;
            }
        } catch (Exception e) {
            Log.d(TAG, "getNativeLibraryDir:exception:", e);
        }
        return platformNativeLibraryDir;
    }
}
