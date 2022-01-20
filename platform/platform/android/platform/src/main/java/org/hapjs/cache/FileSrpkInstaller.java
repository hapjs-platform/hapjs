/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.model.SubpackageInfo;

public class FileSrpkInstaller extends SrpkPackageInstallerBase {
    private static final String TAG = "FileSrpkInstaller";

    public FileSrpkInstaller(
            Context context,
            final String pkg,
            int versionCode,
            long size,
            boolean isUpdate,
            SubpackageInfo subpackageInfo,
            File archiveFile,
            TeeZipExtractor extractor,
            InstallFlag installFlag) {
        super(
                context,
                pkg,
                versionCode,
                size,
                isUpdate,
                subpackageInfo,
                archiveFile,
                extractor,
                installFlag);
    }

    private static List<String> listFiles(File file, String cutPrefix) {
        List<String> allFiles = new ArrayList<>();
        if (file.isFile()) {
            String path = file.getAbsolutePath();
            if (path.startsWith(cutPrefix + "/")) {
                String relativePath = path.substring(cutPrefix.length() + 1);
                allFiles.add(relativePath);
            } else {
                Log.e(TAG, "path '" + path + "' not starts with '" + cutPrefix + "'");
            }
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    allFiles.addAll(listFiles(child, cutPrefix));
                }
            }
        }
        return allFiles;
    }

    @Override
    protected void installInner(File resourceDir, File signatureFile)
            throws IOException, CacheException {
        File tmpDir1 = getTempResourceDir1(mContext, mPackageName);
        File tmpDir = null;
        if (mSubpackageInfo == null) {
            tmpDir = tmpDir1;
        } else {
            tmpDir = new File(tmpDir1, mSubpackageInfo.getName());
        }

        try {
            mExtractor.extract(tmpDir);
            PackageUtils.checkPackage(
                    mContext, mSubpackageInfo, mArchiveFile, signatureFile, mPackageName);

            moveFiles(tmpDir, resourceDir);

            dispatchInstallProgress(mPackageName, mArchiveFile.length());
        } finally {
            FileUtils.rmRF(tmpDir);
        }
    }

    private void moveFiles(File srcDir, File dstDir) throws IOException {
        File[] srcFiles = srcDir.listFiles();
        if (srcFiles != null && srcFiles.length > 0) {
            if (!dstDir.exists()) {
                dstDir.mkdirs();
            }
            for (File srcFile : srcFiles) {
                File dstFile = new File(dstDir, srcFile.getName());
                if (!dstFile.exists()) {
                    if (srcFile.renameTo(dstFile)) {
                        if (dstFile.isFile()) {
                            dispatchFileInstalled(mPackageName, dstFile);
                        }
                    } else {
                        throw new IOException(
                                "Fail to rename file:"
                                        + srcFile.getAbsolutePath()
                                        + " to "
                                        + dstFile.getAbsolutePath());
                    }
                } else {
                    if (srcFile.isDirectory() && dstFile.isDirectory()) {
                        moveFiles(srcFile, dstFile);
                    } else if (!srcFile.isDirectory() && !dstFile.isDirectory()) {
                        Log.d(TAG, "skip existent file: " + srcFile.getAbsolutePath());
                    } else {
                        Log.e(TAG, "file type is not the same: " + srcFile.getAbsolutePath());
                    }
                }
            }
        }
    }
}
