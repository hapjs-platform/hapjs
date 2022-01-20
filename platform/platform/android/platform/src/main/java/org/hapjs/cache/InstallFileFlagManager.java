/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import org.hapjs.common.utils.FileUtils;

public class InstallFileFlagManager {
    private static final String TAG = "InstallFileFlagManager";

    public static boolean isPackageInstalling(Context context, String pkg) {
        return getInstallingFlag(context, pkg).exists();
    }

    private static File getInstallingFlag(Context context, String pkg) {
        return new File(getInstallingFlagDir(context), pkg + ".installing");
    }

    private static File getInstallingFlagDir(Context context) {
        return new File(context.getCacheDir(), "installFlags");
    }

    public static void clearFlag(Context context, String pkg) {
        getInstallingFlag(context, pkg).delete();
    }

    public static void clearAllFlags(Context context) {
        FileUtils.rmRF(getInstallingFlagDir(context));
    }

    public static boolean createFlag(Context context, String pkg) {
        try {
            File flag = getInstallingFlag(context, pkg);
            if (!flag.exists()) {
                File dir = flag.getParentFile();
                if (!dir.exists()) {
                    dir.mkdir();
                    if (!dir.exists()) {
                        Log.e(TAG, "failed to mkdir. pkg=" + pkg);
                        return false;
                    }
                }

                boolean result = flag.createNewFile();
                if (!result) {
                    if (!flag.exists()) {
                        Log.e(TAG, "failed to create flag. pkg=" + pkg);
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "failed to createNewFile", e);
        }
        return false;
    }
}
