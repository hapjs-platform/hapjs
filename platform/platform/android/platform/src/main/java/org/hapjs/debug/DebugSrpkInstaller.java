/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.hapjs.bridge.AppInfoProvider;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.InstallFlag;
import org.hapjs.cache.InstallFlagImpl;
import org.hapjs.cache.OneShotInstallFlag;
import org.hapjs.cache.PackageInstaller;
import org.hapjs.cache.PackageInstallerFactory;
import org.hapjs.cache.SrpkPackageInstallerBase;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.cache.utils.ZipUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.debug.DebugService.ResultCode;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.ProviderManager;

public class DebugSrpkInstaller {
    private static final String TAG = "DebugRpkInstaller";
    private static final String RPK_CACHE_DIR = "debugRpkCache";
    private static final String RPK_POSTFIX = ".rpk";
    private static final String SRPK_POSTFIX = ".srpk";

    private static int saveToCacheFolder(Context context, String pkg, Uri uri) {
        File tmpFile = null;
        InputStream in = null;
        try {
            tmpFile = File.createTempFile(pkg, null);
            in = context.getContentResolver().openInputStream(uri);
            FileUtils.saveToFile(in, tmpFile);
        } catch (IOException e) {
            Log.e(TAG, "Fail to save to cache folder", e);
            if (tmpFile != null) {
                tmpFile.delete();
            }
            return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_CREATE_TEMP_FILE;
        } finally {
            FileUtils.closeQuietly(in);
        }

        File cacheFolder = getCacheFolder(context, pkg);
        if (cacheFolder.exists()) {
            if (!FileUtils.rmRF(cacheFolder)) {
                Log.e(TAG, "Fail to rm dir: " + cacheFolder.getAbsolutePath());
                return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_DELETE_CACHE_DIR;
            }
        }
        if (!cacheFolder.mkdirs()) {
            Log.e(TAG, "Fail to create dir: " + cacheFolder.getAbsolutePath());
            return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_CREATE_CACHE_DIR;
        }

        try {
            File manifestArchive = null;
            if (isSplitRpk(tmpFile)) {
                if (!ZipUtils.unzip(tmpFile, cacheFolder)) {
                    Log.e(TAG, "Fail to unzip split rpk");
                    return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_UNZIP_SPLIT_RPK;
                }
                manifestArchive = getCacheArchiveFile(context, pkg, SubpackageInfo.BASE_PKG_NAME);
            } else {
                File dest = getCacheArchiveFile(context, pkg, null);
                if (!tmpFile.renameTo(dest)) {
                    Log.e(TAG, "Fail to move file");
                    return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_MOVE_CACHE_FILE;
                }
                manifestArchive = getCacheArchiveFile(context, pkg, null);
            }
            if (!saveManifestToCacheFolder(context, pkg, manifestArchive)) {
                Log.e(TAG, "Fail to save manifest");
                return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_SAVE_MANIFESTS;
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to check isSplitRpk", e);
            return ResultCode.CODE_INSTALL_ERROR_FAIL_TO_CHECK_SPLIT_FILE;
        } finally {
            tmpFile.delete();
        }

        Log.d(TAG, "saveToCacheFolder succ");
        return ResultCode.CODE_OK;
    }

    private static int installFromCacheFolder(Context context, String packageName) {
        List<SubpackageInfo> subpackageInfos = getSubpackageInfo(context, packageName);
        if (subpackageInfos == null || subpackageInfos.isEmpty()) {
            File archiveFile = getCacheArchiveFile(context, packageName, null);
            return installPackage(context, packageName, archiveFile);
        } else {
            int versionCode = getVersionCode(context, packageName);
            boolean isUpdate = CacheStorage.getInstance(context).getCache(packageName).isUpdate();
            InstallFlag installFlag =
                    new InstallFlagImpl(subpackageInfos.size(), subpackageInfos.size());

            int result = ResultCode.CODE_GENERIC_ERROR;
            for (SubpackageInfo info : subpackageInfos) {
                File archive = getCacheArchiveFile(context, packageName, info.getName());
                result =
                        installSubpackage(
                                context, packageName, info, archive, versionCode, isUpdate,
                                installFlag);
                if (result != ResultCode.CODE_OK) {
                    break;
                }
            }

            SrpkPackageInstallerBase
                    .cleanWhenFinish(context, packageName, result == ResultCode.CODE_OK);
            return result;
        }
    }

    public static int installPackage(Context context, String pkg, Uri uri) {
        Log.d(TAG, "installPackage: pkg=" + pkg + ", uri=" + uri);
        if (TextUtils.isEmpty(pkg)) {
            Log.e(TAG, "Invalid package: " + pkg);
            return ResultCode.CODE_INSTALL_ERROR_INVALID_PACKAGE;
        }
        if (uri == null) {
            Log.e(TAG, "package uri can't be null");
            return ResultCode.CODE_INSTALL_ERROR_INVALID_URI;
        }

        int result = saveToCacheFolder(context, pkg, uri);

        if (result == ResultCode.CODE_OK) {
            result = installFromCacheFolder(context, pkg);
        }

        FileUtils.rmRF(getCacheFolder(context, pkg));

        Log.d(TAG, "installPackage: pkg=" + pkg + ", result=" + result);
        return result;
    }

    private static int installPackage(Context context, String packageName, File archiveFile) {
        try {
            CacheStorage.getInstance(context).install(packageName, archiveFile.getAbsolutePath());
            return ResultCode.CODE_OK;
        } catch (CacheException e) {
            Log.e(TAG, "failed to install package: " + packageName, e);
            if (e.getErrorCode() == CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED) {
                return ResultCode.CODE_CERTIFICATION_MISMATCH;
            }
            return e.getErrorCode();
        }
    }

    private static int installSubpackage(
            Context context,
            String packageName,
            SubpackageInfo info,
            File archiveFile,
            int versionCode,
            boolean isUpdate,
            InstallFlag flag) {
        try {
            PackageInstaller installer =
                    PackageInstallerFactory.createInstaller(
                            context,
                            packageName,
                            versionCode,
                            info.getSize(),
                            info,
                            new FileInputStream(archiveFile),
                            isUpdate,
                            new OneShotInstallFlag(flag));
            if (installer instanceof SrpkPackageInstallerBase) {
                ((SrpkPackageInstallerBase) installer).prepare();
                CacheStorage.getInstance(context).install(packageName, installer);
                return ResultCode.CODE_OK;
            } else {
                Log.e(TAG, "illegal installer: " + installer);
                return ResultCode.CODE_INSTALL_ERROR_ILLEGAL_INSTALLER;
            }
        } catch (CacheException e) {
            Log.e(TAG, "failed to install subpackage: " + info.getName(), e);
            if (e.getErrorCode() == CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED) {
                return ResultCode.CODE_CERTIFICATION_MISMATCH;
            }
            return e.getErrorCode();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to install subpackage: " + info.getName(), e);
            return ResultCode.CODE_INSTALL_ERROR_INSTALL_FILE_NOT_FOUND;
        }
    }

    private static List<SubpackageInfo> getSubpackageInfo(Context context, String packageName) {
        File baseArchiveFile =
                getCacheArchiveFile(context, packageName, SubpackageInfo.BASE_PKG_NAME);
        // 安装分包时返回分包信息, 安装整包时不返回.
        if (baseArchiveFile.isFile()) {
            File manifest = getCacheManifest(context, packageName);
            AppInfoProvider provider =
                    ProviderManager.getDefault().getProvider(AppInfoProvider.NAME);
            AppInfo appInfo = provider.fromFile(manifest);
            return appInfo == null ? null : appInfo.getSubpackageInfos();
        }
        return null;
    }

    private static int getVersionCode(Context context, String packageName) {
        File manifest = getCacheManifest(context, packageName);
        AppInfoProvider provider = ProviderManager.getDefault().getProvider(AppInfoProvider.NAME);
        AppInfo appInfo = provider.fromFile(manifest);
        return appInfo == null ? -1 : appInfo.getVersionCode();
    }

    private static File getCacheFolder(Context context, String pkg) {
        return new File(
                context.getCacheDir() + File.separator + RPK_CACHE_DIR + File.separator + pkg);
    }

    private static File[] getCacheArchiveFiles(Context context, String pkg) {
        return getCacheFolder(context, pkg)
                .listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name != null
                                        && (name.endsWith(RPK_POSTFIX) || name.endsWith(SRPK_POSTFIX));
                            }
                        });
    }

    private static File getCacheArchiveFile(Context context, String pkg, String subPackageName) {
        String fileName;
        if (TextUtils.isEmpty(subPackageName)) {
            fileName = pkg + RPK_POSTFIX;
        } else {
            fileName = pkg + "." + subPackageName + SRPK_POSTFIX;
        }
        return new File(getCacheFolder(context, pkg), fileName);
    }

    private static File getCacheManifest(Context context, String pkg) {
        return new File(getCacheFolder(context, pkg), PackageUtils.FILENAME_MANIFEST);
    }

    private static boolean isSplitRpk(File packageFile) throws IOException {
        ZipInputStream zis = null;
        try {
            FileInputStream is = new FileInputStream(packageFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                if (!ze.isDirectory()
                        && !filename.contains(File.separator)
                        && filename.endsWith(SRPK_POSTFIX)) {
                    return true;
                }
            }
            return false;
        } finally {
            FileUtils.closeQuietly(zis);
        }
    }

    private static boolean saveManifestToCacheFolder(Context context, String pkg,
                                                     File packageFile) {
        ZipFile zipFile = null;
        InputStream inputStream = null;
        try {
            zipFile = new ZipFile(packageFile);
            ZipEntry zipEntry = zipFile.getEntry(PackageUtils.FILENAME_MANIFEST);
            if (zipEntry != null) {
                inputStream = zipFile.getInputStream(zipEntry);
                File dest = new File(getCacheFolder(context, pkg), PackageUtils.FILENAME_MANIFEST);
                return FileUtils.saveToFile(inputStream, dest);
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to read manifest.json", e);
        } finally {
            FileUtils.closeQuietly(zipFile);
            FileUtils.closeQuietly(inputStream);
        }
        return false;
    }
}
