/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.app;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.cache.utils.ZipUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.distribution.InstalledSubpackageManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.Runtime;

public class AppManager {
    public static final String RPK_FILE_EXTENSION = ".rpk";
    public static final String RPKS_FILE_EXTENSION = ".rpks";
    public static final String SRPK_FILE_EXTENSION = ".srpk";
    private static final String TAG = "AppManager";
    private static final String RPK_DIR_PATH = "rpks";
    private static Map<String, AppItem> sAppMap;

    public static Collection<AppItem> getAllApps(Context context) {
        return getAppMap(context).values();
    }

    public static AppItem getApp(Context context, String packageName) {
        return getAppMap(context).get(packageName);
    }

    private static Map<String, AppItem> getAppMap(Context context) {
        if (sAppMap == null || sAppMap.isEmpty()) {
            Map<String, AppItem> appMap = new HashMap<>();
            addInstalledApp(context, appMap);
            addRpkFileList(appMap);
            sAppMap = appMap;
        }
        return sAppMap;
    }

    private static void addInstalledApp(Context context, Map<String, AppItem> appItemMap) {
        CacheStorage cacheStorage = CacheStorage.getInstance(context);
        List<Cache> caches = cacheStorage.availableCaches();
        if (caches != null) {
            for (Cache cache : caches) {
                AppInfo appInfo = cache.getAppInfo();
                AppItem appItem = new AppItem(null, appInfo, AppItem.STATE_OK);
                appItemMap.put(appItem.getPackageName(), appItem);
            }
        }
    }

    private static void addRpkFileList(final Map<String, AppItem> appItemMap) {
        File rpkRoot = getRpkRoot();
        if (rpkRoot == null) {
            return;
        }

        rpkRoot.listFiles(
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        AppInfo appInfo = null;
                        String ext = FileUtils.getFileExtension(f);
                        if (RPK_FILE_EXTENSION.equals(ext)) {
                            appInfo = PackageUtils.getAppInfo(f.getPath());
                        } else if (RPKS_FILE_EXTENSION.equals(ext)) {
                            String packageName =
                                    org.hapjs.mockup.utils.PackageUtils.getRpksPackageName(f);
                            if (!TextUtils.isEmpty(packageName)) {
                                appInfo = getSplitRpkAppInfo(f, packageName);
                            }
                        }

                        if (appInfo != null) {
                            AppItem appItem = appItemMap.get(appInfo.getPackage());
                            if (appItem == null) {
                                appItem = new AppItem(f, appInfo, AppItem.STATE_INSTALL_AVAILABLE);
                                appItemMap.put(appItem.getPackageName(), appItem);
                            } else {
                                if (appItem.getVersion() < appInfo.getVersionCode()) {
                                    appItem =
                                            new AppItem(f, appInfo, AppItem.STATE_UPDATE_AVAILABLE);
                                    appItemMap.put(appItem.getPackageName(), appItem);
                                    InstalledSubpackageManager.clearSubpackages(
                                            Runtime.getInstance().getContext(),
                                            appItem.getPackageName());
                                }
                            }
                        }
                        return false;
                    }
                });
    }

    public static File getRpkRoot() {
        File sdcardRoot = Environment.getExternalStorageDirectory();
        if (sdcardRoot == null || !sdcardRoot.exists()) {
            return null;
        }

        File rpkRoot = new File(sdcardRoot.getPath() + "/" + RPK_DIR_PATH);
        if (!rpkRoot.exists()) {
            return null;
        }

        return rpkRoot;
    }

    public static List<SubpackageInfo> getSubpackageInfo(Context context, String packageName) {
        AppItem appItem = getApp(context, packageName);
        if (appItem != null) {
            AppInfo appInfo = getSplitRpkAppInfo(appItem.getRpkFile(), packageName);
            return appInfo == null ? null : appInfo.getSubpackageInfos();
        }
        return null;
    }

    private static AppInfo getSplitRpkAppInfo(File archiveFile, String packageName) {
        File tmpBaseSubpackageFile =
                getPackageFile(archiveFile, packageName, SubpackageInfo.BASE_PKG_NAME);
        if (tmpBaseSubpackageFile != null) {
            AppInfo appInfo = PackageUtils.getAppInfo(tmpBaseSubpackageFile);
            tmpBaseSubpackageFile.delete();
            return appInfo;
        } else {
            Log.e(TAG, "failed to retrive base subpackage");
        }
        return null;
    }

    public static File getPackageFile(Context context, String packageName, String subpackageName) {
        AppItem appItem = getApp(context, packageName);
        if (appItem != null) {
            return getPackageFile(appItem.getRpkFile(), packageName, subpackageName);
        }
        return null;
    }

    private static File getPackageFile(File archiveFile, String packageName,
                                       String subpackageName) {
        if (TextUtils.isEmpty(subpackageName)) {
            return archiveFile;
        } else {
            String baseSubpackageName = packageName + "." + subpackageName + SRPK_FILE_EXTENSION;
            try {
                File tmpBaseSubpackageFile =
                        File.createTempFile(packageName + "." + subpackageName,
                                SRPK_FILE_EXTENSION);
                boolean ret =
                        ZipUtils.retrive(archiveFile, baseSubpackageName, tmpBaseSubpackageFile);
                if (ret) {
                    return tmpBaseSubpackageFile;
                } else {
                    Log.e(TAG, "failed to retrive base subpackage");
                }
            } catch (IOException e) {
                Log.e(TAG, "failed to getSubpackageInfo", e);
            }
            return null;
        }
    }
}
