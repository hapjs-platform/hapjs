/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.distribution.InstallProgressManager;
import org.hapjs.distribution.InstalledSubpackageManager;
import org.hapjs.event.EventManager;
import org.hapjs.event.ManifestAvailableEvent;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.ProviderManager;

public abstract class SrpkPackageInstallerBase extends PackageInstaller {
    private static final String TAG = "SrpkInstallerBase";
    protected static InstallListener sListener;
    protected File mArchiveFile;
    protected TeeZipExtractor mExtractor;
    private int mVersionCode;
    private long mSize;
    private InstallFlag mInstallFlag;

    public SrpkPackageInstallerBase(
            Context context,
            final String pkg,
            int versionCode,
            long size,
            boolean isUpdate,
            SubpackageInfo subpackageInfo,
            File archiveFile,
            TeeZipExtractor extractor,
            InstallFlag installFlag) {
        super(context, pkg, subpackageInfo, isUpdate);
        mVersionCode = versionCode;
        mSize = size;
        mArchiveFile = archiveFile;
        mExtractor = extractor;
        mInstallFlag = installFlag;

        mExtractor.setStreamProgressListener(
                new ProgressInputStream.StreamProgressListener() {
                    @Override
                    public void onStreamProgress(long savedSize) {
                        dispatchInstallProgress(pkg, savedSize);
                    }
                });
    }

    private void cleanWhenFinish(boolean succ) {
        FileUtils.rmRF(mArchiveFile);
        if (mInstallFlag.increaseFinishAndCheckAll(succ)) { // 所有任务结束
            cleanWhenFinish(mContext, mPackageName, mInstallFlag.hasSuccess());
            // 安装成功后，清除之前分包安装记录
            if (mInstallFlag.hasSuccess()) {
                InstalledSubpackageManager.getInstance()
                        .clearOutdatedSubpackages(mContext, mPackageName, mVersionCode);
            }
            InstallInterceptProvider installProvider =
                    ProviderManager.getDefault().getProvider(InstallInterceptProvider.NAME);
            installProvider
                    .onPostInstall(mContext, mPackageName, isUpdate(), !mInstallFlag.hasSuccess());
        }
    }

    public static void cleanWhenFinish(Context context, String pkg, boolean hasSucc) {
        InstallFileFlagManager.clearFlag(context, pkg);
        dispatchPackageInstalled(pkg);

        if (hasSucc) {
            // 成功删除备份
            FileUtils.rmRF(getTempResourceDir2(context, pkg));
        } else {
            // 失败回滚
            rollback(context, pkg);
        }
    }

    private static void rollback(Context context, String pkg) {
        File tmpResourceDir2 = getTempResourceDir2(context, pkg);
        if (tmpResourceDir2.exists()) {
            File resourceDir = Cache.getResourceDir(context, pkg);
            // 没有任何包成功，删除已安装的文件内容
            FileUtils.rmRF(resourceDir);
            if (!tmpResourceDir2.renameTo(resourceDir)) {
                Log.w(TAG, "roll back failed!");
            } else {
                Log.d(TAG, "roll back succ.");
            }
        } else {
            Log.d(TAG, "backup not exists.");
        }
    }

    private static void dispatchPackageInstalled(String pkg) {
        InstallListener listener = sListener;
        if (listener != null) {
            listener.onPackageInstalled(pkg);
        }
    }

    public static void setStreamInstallListener(InstallListener listener) {
        sListener = listener;
    }

    @Override
    public int getVersionCode() {
        return mVersionCode;
    }

    @Override
    public void install(File resourceDir, File signatureFile) throws CacheException {
        boolean success = true;
        try {
            startPackageInstall(mContext, mPackageName);
            installInner(resourceDir, signatureFile);
            // mSize单位是K, 从下载字节流统计得到的大小单位是B, 二者可能不相等. 这里强行dispatch一个100%进度.
            dispatchInstallProgress(getPackage(), mSize * 1024);
        } catch (IOException e) {
            success = false;
            throw new CacheException(CacheErrorCode.PACKAGE_UNZIP_FAILED, "Fail to install", e);
        } catch (CacheException e) {
            success = false;
            throw e;
        } finally {
            cleanWhenFinish(success);
        }
    }

    protected abstract void installInner(File resourceDir, File signatureFile)
            throws IOException, CacheException;

    @Override
    public boolean isAllSuccess() {
        return mInstallFlag.isAllSuccess();
    }

    public SubpackageInfo getSubpackageInfo() {
        return mSubpackageInfo;
    }

    private void backup(Context context, String pkg) throws IOException {
        File resourceDir = Cache.getResourceDir(context, pkg);
        File tmpResourceDir2 = getTempResourceDir2(context, pkg);
        FileUtils.rmRF(tmpResourceDir2);
        if (resourceDir.exists()) {
            boolean result = resourceDir.renameTo(tmpResourceDir2);
            if (!result) {
                throw new IOException("back up resource failed");
            }
        } else {
            // 创建备份目录，防止第一次安装失败rollback时无法删除已下载的文件而导致异常
            if (!tmpResourceDir2.exists()) {
                boolean result = tmpResourceDir2.mkdirs();
                if (!result) {
                    throw new IOException("mkdirs back up resource failed");
                }
            }
        }
    }

    private void startPackageInstall(Context context, String pkg) throws IOException {
        if (mInstallFlag.startInstall()) {
            InstallInterceptProvider installProvider =
                    ProviderManager.getDefault().getProvider(InstallInterceptProvider.NAME);
            installProvider.onPreInstall(context, pkg);
            backup(context, pkg);
        } else {
            Log.i(TAG, "some subpackage has been updated. no need to backup.");
        }
    }

    protected void dispatchFileInstalled(String pkg, File file) {
        InstallListener listener = sListener;
        if (listener != null) {
            listener.onFileInstalled(pkg, file);
            notifyManifestAvailable(pkg, file);
        }
    }

    private void notifyManifestAvailable(String pkg, File file) {
        if (PackageUtils.FILENAME_MANIFEST.equals(file.getName())) {
            File manifestFile = Cache.getManifestFile(mContext, pkg);
            if (file.getAbsolutePath().equals(manifestFile.getAbsolutePath())) {
                EventManager.getInstance().invoke(new ManifestAvailableEvent(pkg, isUpdate()));
            }
        }
    }

    protected void dispatchFileList(String pkg, SubpackageInfo subpackageInfo, Set<String> files) {
        InstallListener listener = sListener;
        if (sListener != null) {
            String subpackage = subpackageInfo != null ? subpackageInfo.getName() : null;
            listener.onFileList(pkg, subpackage, files);
        }
    }

    protected void dispatchInstallProgress(String pkg, long savedSize) {
        SubpackageInfo info = getSubpackageInfo();
        savedSize = Math.min(savedSize, mSize * 1024);
        if (info == null) {
            InstallProgressManager.getInstance()
                    .postOnInstallProgress(pkg, null, savedSize, mSize * 1024);
        } else {
            InstallProgressManager.getInstance()
                    .postOnInstallProgress(pkg, info.getName(), savedSize, mSize * 1024);
        }
    }

    public void prepare() throws CacheException {
        try {
            startPackageInstall(mContext, mPackageName);
        } catch (IOException e) {
            throw new CacheException(CacheErrorCode.UNKNOWN, "create installing flag file failed");
        }
    }

    public void cancel() throws CacheException {
        mExtractor.cancel();

        SubpackageInfo subpackageInfo = getSubpackageInfo();
        String subpackageName = subpackageInfo == null ? null : subpackageInfo.getName();
        mArchiveFile.renameTo(Cache.getArchiveFile(mContext, mPackageName, subpackageName));
    }

    public interface InstallListener {
        void onFileList(String pkg, String subpackage, Set<String> files);

        void onFileInstalled(String pkg, File file);

        void onPackageInstalled(String pkg);
    }
}
