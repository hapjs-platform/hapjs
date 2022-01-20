/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.model.SubpackageInfo;

public class StreamPackageInstaller extends SrpkPackageInstallerBase {
    private static final String TAG = "StreamPackageInstaller";

    private StreamSignature mStreamSignature;

    public StreamPackageInstaller(
            Context context,
            final String pkg,
            int versionCode,
            long size,
            boolean isUpdate,
            SubpackageInfo subpackageInfo,
            File archiveFile,
            StreamZipExtractor extractor,
            StreamSignature streamSignature,
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
        mStreamSignature = streamSignature;

        mExtractor.setOnFileSavedListener(
                new ZipExtractor.OnFileSavedListener() {
                    @Override
                    public void onFileSaved(File file) {
                        dispatchFileInstalled(pkg, file);
                    }
                });
    }

    @Override
    protected void installInner(File resourceDir, File signatureFile)
            throws IOException, CacheException {
        mStreamSignature.verifySignature(mContext, signatureFile);
        dispatchFileList(mPackageName, mSubpackageInfo, mStreamSignature.getDigests().keySet());
        mExtractor.extract(resourceDir.getAbsoluteFile());
        PackageUtils
                .checkPackage(mContext, mSubpackageInfo, mArchiveFile, signatureFile, mPackageName);
    }
}
