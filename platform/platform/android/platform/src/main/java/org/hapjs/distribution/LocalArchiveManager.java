/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hapjs.cache.Cache;
import org.hapjs.common.utils.FileUtils;

public class LocalArchiveManager {
    private static final String TAG = "LocalArchiveManager";

    public static boolean isLocalArchiveVersionMatches(
            Context context, int curVersion, String pkg, String subpackage) {
        File archiveFile = Cache.getArchiveFile(context, pkg, subpackage);
        if (archiveFile.isFile()) {
            File versionTagFile = Cache.getArchiveVersionTagFile(context, pkg, subpackage);
            if (versionTagFile.isFile()) {
                try {
                    String versionStr =
                            FileUtils.readFileAsString(versionTagFile.getAbsolutePath());
                    if (!TextUtils.isEmpty(versionStr)) {
                        return versionStr.equals(String.valueOf(curVersion));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "failed to read local archive's version", e);
                }
            } else {
                Log.w(TAG, "version tag file not found");
            }
        } else {
            Log.d(TAG, "archive not exists: " + archiveFile.getAbsolutePath());
        }
        return false;
    }

    public static void saveLocalArchiveVersionCode(
            Context context, int curVersion, String pkg, String subpackage) {
        File archiveFile = Cache.getArchiveFile(context, pkg, subpackage);
        if (archiveFile.isFile()) {
            File versionTagFile = Cache.getArchiveVersionTagFile(context, pkg, subpackage);
            FileUtils.saveToFile(
                    String.valueOf(curVersion).getBytes(StandardCharsets.UTF_8), versionTagFile);
        }
    }

    public static void removeLocalArchive(Context context, String pkg, String subpackage) {
        File archiveFile = Cache.getArchiveFile(context, pkg, subpackage);
        File versionTagFile = Cache.getArchiveVersionTagFile(context, pkg, subpackage);
        FileUtils.rmRF(archiveFile);
        FileUtils.rmRF(versionTagFile);
    }
}
