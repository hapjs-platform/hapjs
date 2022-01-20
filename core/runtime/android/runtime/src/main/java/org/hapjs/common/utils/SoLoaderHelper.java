/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.soloader.DirectorySoSource;
import com.facebook.soloader.SoLoader;
import java.io.File;
import org.hapjs.runtime.Runtime;

public class SoLoaderHelper {
    private static final String TAG = "SoLoaderHelper";
    private static volatile boolean sInited = false;

    public static void initialize(Context context) {
        if (sInited) {
            return;
        }
        synchronized (SoLoaderHelper.class) {
            if (sInited) {
                return;
            }

            SoLoader.init(context, false);
            // BUGFIX: 内置后，fb.so等异常问题。增加内置应用及内置应用升级后的so path。
            try {
                File nativeLibraryDir =
                        new File(getNativeLibraryDir(getApplicationInfo(context).nativeLibraryDir));
                SoLoader.prependSoSource(new DirectorySoSource(nativeLibraryDir, 0));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            sInited = true;
        }
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

    private static ApplicationInfo getApplicationInfo(Context context) {
        String platform = Runtime.getInstance().getPlatform();
        try {
            return TextUtils.isEmpty(platform)
                    ? context.getApplicationInfo()
                    : context.getPackageManager().getApplicationInfo(platform, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Not found the platform for Runtime!");
        }
    }
}
