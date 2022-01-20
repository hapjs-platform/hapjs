/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.impl;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.distribution.AppDistributionMeta;
import org.hapjs.distribution.DistributionProvider;
import org.hapjs.distribution.InstalledSubpackageManager;
import org.hapjs.distribution.PreviewInfo;
import org.hapjs.distribution.ServerSettings;
import org.hapjs.mockup.app.AppItem;
import org.hapjs.mockup.app.AppManager;
import org.hapjs.model.SubpackageInfo;

public class DistributionProviderImpl implements DistributionProvider {
    private static final String TAG = "DistributionProviderImp";
    private Context mContext;

    public DistributionProviderImpl(Context context) {
        mContext = context.getApplicationContext();
    }

    private File fetchPackageFile(String packageName, String subpackageName) {
        Log.d(TAG,
                "fetchPackage packageName=" + packageName + ", subpackageName=" + subpackageName);
        AppItem appItem = AppManager.getApp(mContext, packageName);
        if (appItem != null) {
            return AppManager.getPackageFile(mContext, packageName, subpackageName);
        }
        return null;
    }

    @Override
    public int fetch(AppDistributionMeta distributionMeta, String subpackageName, String destFile) {
        File packageFile = fetchPackageFile(distributionMeta.getPackage(), subpackageName);
        if (packageFile == null) {
            return CacheErrorCode.PACKAGE_UNAVAILABLE;
        } else {
            boolean success = FileUtils.copyFile(packageFile, new File(destFile));
            if (!TextUtils.isEmpty(subpackageName)) {
                // delete tmp file
                packageFile.delete();
            }
            return success ? CacheErrorCode.OK : CacheErrorCode.PACKAGE_UNAVAILABLE;
        }
    }

    @Override
    public InputStream fetch(AppDistributionMeta distributionMeta, final String subpackageName)
            throws CacheException {
        final File packageFile = fetchPackageFile(distributionMeta.getPackage(), subpackageName);
        if (packageFile != null) {
            try {
                return new FileInputStream(packageFile) {
                    public void close() throws IOException {
                        if (!TextUtils.isEmpty(subpackageName)) {
                            // delete tmp file
                            packageFile.delete();
                        }
                        super.close();
                    }
                };
            } catch (FileNotFoundException e) {
                throw new CacheException(CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST, "rpk not found");
            }
        }
        return null;
    }

    @Override
    public boolean needUpdate(String packageName) {
        AppItem appItem = AppManager.getApp(mContext, packageName);
        return appItem != null && appItem.getState() == AppItem.STATE_UPDATE_AVAILABLE;
    }

    @Override
    public PreviewInfo getPreviewInfo(String packageName) {
        return null;
    }

    @Override
    public ServerSettings getServerSettings(String packageName) {
        return null;
    }

    @Override
    public boolean needSubpackageUpdate(String packageName, String subpackageName) {
        AppItem appItem = AppManager.getApp(mContext, packageName);
        if (appItem != null) {
            return !InstalledSubpackageManager.checkInstalled(
                    mContext, packageName, subpackageName, appItem.getVersion());
        }
        return false;
    }

    public AppDistributionMeta getAppDistributionMeta(String packageName, int versionCode)
            throws CacheException {
        AppItem appItem = AppManager.getApp(mContext, packageName);
        if (appItem != null) {
            int version = appItem.getVersion();
            List<SubpackageInfo> subpackageInfos =
                    AppManager.getSubpackageInfo(mContext, packageName);
            List<SubpackageInfo> needUpdateSubpackages = new ArrayList<>();
            if (subpackageInfos != null && !subpackageInfos.isEmpty()) {
                List<String> installedList =
                        InstalledSubpackageManager.queryInstallList(
                                mContext, packageName, appItem.getVersion());
                if (installedList == null) {
                    needUpdateSubpackages.addAll(subpackageInfos);
                } else {
                    for (SubpackageInfo info : subpackageInfos) {
                        if (!installedList.contains(info.getName())) {
                            needUpdateSubpackages.add(info);
                        }
                    }
                }
            }
            return new AppDistributionMeta(
                    packageName, version, null, subpackageInfos, needUpdateSubpackages);
        }

        throw new CacheException(
                CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST,
                "Package file does not exist: " + packageName);
    }

    @Override
    public int download(String pkg, int versionCode) {
        Log.e(TAG, "mockup doesnot support download operation");
        return CacheErrorCode.UNKNOWN;
    }
}
