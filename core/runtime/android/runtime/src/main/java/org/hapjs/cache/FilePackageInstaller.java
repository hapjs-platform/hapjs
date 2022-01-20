/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.event.EventManager;
import org.hapjs.event.ManifestAvailableEvent;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.ProviderManager;

public class FilePackageInstaller extends PackageInstaller {

    private static final String TAG = "FilePackageInstaller";

    private File mArchiveFile;

    public FilePackageInstaller(Context context, String pkg, File archiveFile, boolean isUpdate) {
        super(context, pkg, isUpdate);
        mArchiveFile = archiveFile;
    }

    @Override
    public int getVersionCode() {
        AppInfo appInfo = PackageUtils.getAppInfo(mArchiveFile);
        return appInfo == null ? 0 : appInfo.getVersionCode();
    }

    @Override
    public void install(File resourceDir, File signatureFile) throws CacheException {
        Log.d(TAG, "install: pkg=" + mPackageName);
        if (!mArchiveFile.exists()) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST, "Package file does not exist");
        }

        InstallInterceptProvider installProvider =
                ProviderManager.getDefault().getProvider(InstallInterceptProvider.NAME);
        installProvider.onPreInstall(mContext, mPackageName);

        try {
            PackageUtils.checkPackage(mContext, null, mArchiveFile, signatureFile, mPackageName);
        } catch (CacheException e) {
            mArchiveFile.delete();
            throw e;
        }

        File tempResourceDir1 = getTempResourceDir1(mContext, mPackageName);
        File tempResourceDir2 = getTempResourceDir2(mContext, mPackageName);

        FileUtils.rmRF(tempResourceDir1);
        FileUtils.rmRF(tempResourceDir2);

        boolean result = false;
        try {
            try {
                ZipExtractor extractor = ZipExtractor.create(mArchiveFile);
                extractor.extract(tempResourceDir1);
            } catch (FileNotFoundException e) {
                throw new CacheException(
                        CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST, "Package file does not exist", e);
            } catch (IOException e) {
                throw new CacheException(
                        CacheErrorCode.PACKAGE_UNZIP_FAILED, "Package file unzip failed", e);
            }
            if (resourceDir.exists()) {
                result = resourceDir.renameTo(tempResourceDir2);
                Log.d(TAG, resourceDir + " renameTo " + tempResourceDir2 + " result:" + result);
            }
            result = tempResourceDir1.renameTo(resourceDir);
            if (result) {
                EventManager.getInstance()
                        .invoke(new ManifestAvailableEvent(mPackageName, isUpdate()));
            } else {
                FileUtils.rmRF(resourceDir);
                if (tempResourceDir2.exists()) {
                    tempResourceDir2.renameTo(resourceDir);
                }
                throw new CacheException(
                        CacheErrorCode.RESOURCE_DIR_MOVE_FAILED, "Resource dir move failed");
            }
            Log.d(TAG, tempResourceDir1 + " renameTo " + resourceDir + " result:" + result);
        } finally {
            FileUtils.rmRF(tempResourceDir1);
            FileUtils.rmRF(tempResourceDir2);
            mArchiveFile.delete();
            installProvider.onPostInstall(mContext, mPackageName, isUpdate(), !result);
        }
    }
}
