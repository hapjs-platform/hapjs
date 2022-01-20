/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.utils;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.mockup.app.AppManager;
import org.hapjs.model.SubpackageInfo;

public class PackageUtils {
    private static final String TAG = "PackageUtils";

    public static String getRpksPackageName(File packageFile) {
        ZipInputStream zipInputStream = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(packageFile);
            zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream));
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    if (name.endsWith(AppManager.RPK_FILE_EXTENSION)) {
                        String postfix = AppManager.RPK_FILE_EXTENSION;
                        return name.substring(0, name.length() - postfix.length());
                    } else if (name.endsWith(
                            "." + SubpackageInfo.BASE_PKG_NAME + AppManager.SRPK_FILE_EXTENSION)) {
                        String postfix =
                                "." + SubpackageInfo.BASE_PKG_NAME + AppManager.SRPK_FILE_EXTENSION;
                        return name.substring(0, name.length() - postfix.length());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to read packageFile", e);
        } finally {
            FileUtils.closeQuietly(zipInputStream);
            FileUtils.closeQuietly(fileInputStream);
        }

        Log.w(TAG, "Fail to get package name");
        return null;
    }
}
