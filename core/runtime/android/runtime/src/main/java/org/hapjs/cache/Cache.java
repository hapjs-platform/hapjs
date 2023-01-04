/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.hapjs.bridge.AppInfoProvider;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.cache.utils.SignatureStore;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

import java.io.File;
import java.io.IOException;

public class Cache {
    private static final String TAG = "Cache";
    private static FileNotFoundHandler sDefaultFileNotFoundHandler;
    private CacheStorage mCacheStorage;
    private Context mContext;
    private String mPackageName;
    private File mResourceDir;
    private File mArchiveFile;
    private File mSignatureFile;
    private AppInfo mAppInfo;
    private long mLastManifestUpdateTime;

    public Cache(CacheStorage cacheStorage, String packageName) {
        mCacheStorage = cacheStorage;
        mContext = cacheStorage.getContext();
        mPackageName = packageName;
    }

    // these root dirs must be created when get
    public static File getResourceRootDir(Context context) {
        return context.getDir("resource", Context.MODE_PRIVATE);
    }

    /**
     * package
     */
    static File getArchiveRootDir(Context context) {
        return context.getDir("archive", Context.MODE_PRIVATE);
    }

    /**
     * package
     */
    static File getSignatureRootDir(Context context) {
        return context.getDir("signature", Context.MODE_PRIVATE);
    }

    public static File getArchiveFile(Context context, String pkg) {
        return getArchiveFile(context, pkg, null);
    }

    private File getArchiveFile() {
        if (mArchiveFile == null) {
            mArchiveFile = getArchiveFile(mContext, mPackageName);
        }
        return mArchiveFile;
    }

    public static File getArchiveFile(Context context, String pkg, String subpackage) {
        if (TextUtils.isEmpty(subpackage)) {
            return new File(getArchiveRootDir(context), pkg + ".rpk");
        } else {
            return new File(getArchiveRootDir(context), pkg + "." + subpackage + ".srpk");
        }
    }

    public static File getArchiveVersionTagFile(Context context, String pkg, String subpackage) {
        if (TextUtils.isEmpty(subpackage)) {
            return new File(getArchiveRootDir(context), pkg + ".rpk" + ".version_tag");
        } else {
            return new File(
                    getArchiveRootDir(context), pkg + "." + subpackage + ".srpk" + ".version_tag");
        }
    }

    private File getResourceDir() {
        if (mResourceDir == null) {
            mResourceDir = getResourceDir(mContext, mPackageName);
        }
        return mResourceDir;
    }

    public static File getResourceDir(Context context, String pkg) {
        return new File(getResourceRootDir(context), pkg);
    }

    private File getManifestFile() {
        if (BuildPlatform.isTV()) {
            File file = new File(getResourceDir(), PackageUtils.FILENAME_MANIFEST_TV);
            boolean exists = file.exists();
            return exists ? file : new File(getResourceDir(), PackageUtils.FILENAME_MANIFEST);
        } else if (BuildPlatform.isPhone()) {
            File file = new File(getResourceDir(), PackageUtils.FILENAME_MANIFEST_PHONE);
            boolean exists = file.exists();
            return exists ? file : new File(getResourceDir(), PackageUtils.FILENAME_MANIFEST);
        } else {
            return new File(getResourceDir(), PackageUtils.FILENAME_MANIFEST);
        }
    }

    public static File getManifestFile(Context context, String pkg) {
        return new File(getResourceDir(context, pkg), PackageUtils.FILENAME_MANIFEST);
    }

    public static void setDefaultFileNotFoundHandler(FileNotFoundHandler handler) {
        sDefaultFileNotFoundHandler = handler;
    }

    public String getPackageName() {
        return mPackageName;
    }

    // the resourcePath start at root dir
    public Uri get(String resourcePath) throws CacheException {
        return get(resourcePath, null);
    }

    /**
     * Return a file specified by resourcePath, eg.
     *
     * <p>load("app.json")
     *
     * <p>load("common/button_bg.png") First try to retrieve resource from local file system, if not
     * found, then download the package of the app from app store server, and then unpack it to local
     * file system, and then retrieve the resource from local file system again, if not found, return
     * null
     *
     * @param resourcePath the resourcePath start at page dir
     * @param pageName     the pageName of app
     * @return
     */
    public Uri get(String resourcePath, String pageName) throws CacheException {
        Log.d(TAG, "get: app=" + mPackageName + ", page=" + pageName + ", path=" + resourcePath);
        try {
            String resPath = CacheUtils.normalizePath(getResourceDir(), resourcePath, pageName);
            File file = new File(getResourceDir(), resPath);

            boolean isPageJs = resourcePath.endsWith(".js") && !resourcePath.endsWith("/app.js");
            if (isPageJs) {
                RuntimeLogManager.getDefault().logPageJsHit(mPackageName, resPath, file.exists());
            }

            if (file.exists()) {
                return Uri.fromFile(file);
            } else {
                FileNotFoundHandler handler = sDefaultFileNotFoundHandler;
                if (handler != null) {
                    Uri uri = handler.handleFileNotFound(mPackageName, resPath);
                    if (uri != null) {
                        return uri;
                    }
                }
                throw new CacheException(CacheErrorCode.RESOURCE_UNAVAILABLE,
                        "Package resource not found");
            }
        } catch (CacheSecurityException e) {
            throw new CacheException(
                    CacheErrorCode.RESOURCE_PATH_INVALID, "Package resource path is invalid", e);
        }
    }

    public void install(PackageInstaller installer) throws CacheException {
        installer.install(getResourceDir(), getSignatureFile());
        if (installer.isSubpackage()) {
            mCacheStorage.dispatchSubpackageInstalled(
                    owner(), installer.getSubpackageInfo(), installer.getVersionCode());
        }
        if (installer.isAllSuccess()) {
            mCacheStorage.dispatchPackageInstalled(owner(), getAppInfo(), installer.isUpdate());
        }
    }

    public void uninstall() {
        ApplicationContext context = HapEngine.getInstance(mPackageName).getApplicationContext();
        context.clearData();
        remove();
    }

    protected String owner() {
        return mPackageName;
    }

    public boolean ready() {
        boolean result = getManifestFile().exists();
        Log.d(TAG, "ready: " + result + ", app: " + mPackageName);
        return result;
    }

    public long size() {
        long result = 0;
        if (ready()) {
            result =
                    FileUtils.getDiskUsage(getResourceDir())
                            + FileUtils.getDiskUsage(getArchiveFile())
                            + FileUtils.getDiskUsage(getSignatureFile());
        }
        Log.d(TAG, "size: " + result);
        return result;
    }

    protected void remove() {
        doRemove();
    }

    public synchronized void clearAppInfo() {
        mAppInfo = null;
    }

    public synchronized AppInfo getAppInfo(boolean useCache) {
        if (mAppInfo == null || !useCache) {
            return getAppInfoInner(useCache);
        }
        return mAppInfo;
    }

    public AppInfo getAppInfo() {
        return getAppInfoInner(true);
    }

    /**
     * Get latest AppInfo from file or network.
     *
     * @param useCache
     *      If <code>true</code>, use cache when manifest file do not exist.
     */
    private synchronized AppInfo getAppInfoInner(boolean useCache) {
        File file = getManifestFile();
        if (file.exists()) {
            long lastManifestUpdateTime = file.lastModified();
            if (mAppInfo == null || mLastManifestUpdateTime < lastManifestUpdateTime) {
                AppInfo appInfo = AppInfoProviderHolder.get().fromFile(file);
                if (appInfo != null) {
                    mAppInfo = appInfo;
                    mLastManifestUpdateTime = lastManifestUpdateTime;
                } else {
                    // use old version to prevent null pointer issue or anr issue
                }
            }
        } else {
            if (mAppInfo == null || !useCache) {
                mAppInfo = AppInfoProviderHolder.get().create(mContext, mPackageName);
            }
        }
        return mAppInfo;
    }

    public boolean hasIcon() {
        if (!ready()) {
            return false;
        }
        AppInfo appInfo = getAppInfo();
        if (appInfo != null) {
            return getResourceFile(appInfo.getIcon()).exists();
        }
        return false;
    }

    public Uri getIconUri() {
        if (!ready()) {
            return null;
        }
        AppInfo appInfo = getAppInfo();
        if (appInfo != null) {
            String iconPath = appInfo.getIcon();
            try {
                return get(iconPath);
            } catch (CacheException e) {
                Log.w(TAG, "Failed to get iconUri", e);
            }
        }
        return null;
    }

    String getPackageSign() {
        File signatureFile = getSignatureFile();
        if (!SignatureStore.exist(signatureFile)) {
            return null;
        }
        try {
            byte[] existCert = SignatureStore.load(signatureFile);
            return Base64.encodeToString(existCert, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, "fail to getPackageSign", e);
            return null;
        }
    }

    public File getResourceFile(String path) {
        path = FileHelper.getValidUri(path);
        return new File(getResourceDir().getPath() + path);
    }

    public long getFirstInstallTime() {
        return getSignatureFile().lastModified();
    }

    public long getLastUpdateTime() {
        return getResourceDir().lastModified();
    }

    public boolean isUpdate() {
        return getManifestFile().exists();
    }

    private void doRemove() {
        Log.d(TAG, "doRemove");
        File[] files = new File[] {getArchiveFile(), getResourceDir(), getSignatureFile()};
        for (File f : files) {
            FileUtils.rmRF(f);
        }
        mCacheStorage.dispatchPackageRemoved(owner());
    }

    public File getSignatureFile() {
        if (mSignatureFile == null) {
            mSignatureFile = new File(getSignatureRootDir(mContext), mPackageName);
        }
        return mSignatureFile;
    }

    public interface FileNotFoundHandler {
        Uri handleFileNotFound(String pkg, String resourcePath);
    }

    private static class AppInfoProviderHolder {
        private static volatile AppInfoProvider sAppInfoProvider;

        static AppInfoProvider get() {
            if (sAppInfoProvider == null) {
                sAppInfoProvider = ProviderManager.getDefault().getProvider(AppInfoProvider.NAME);
            }
            return sAppInfoProvider;
        }
    }
}
