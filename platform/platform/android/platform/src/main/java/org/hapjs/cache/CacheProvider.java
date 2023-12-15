/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.AbstractContentProvider;
import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.distribution.DistributionManager;
import org.hapjs.logging.Source;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.PermissionChecker;
import org.hapjs.runtime.resource.CacheProviderContracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheProvider extends AbstractContentProvider {

    private static final String TAG = "CacheProvider";

    private ConcurrentMap<String, Object> mResourceLocks = new ConcurrentHashMap<>();

    @Override
    public boolean onCreate() {
        PackageInstallListener listener = new PackageInstallListener();
        SrpkPackageInstallerBase.setStreamInstallListener(listener);
        return true;
    }

    @Override
    public Bundle doCall(String method, String arg, Bundle extras) {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return null;
        }
        if (CacheProviderContracts.METHOD_GET_SIZE.equals(method)) {
            return getSize(arg);
        } else if (CacheProviderContracts.METHOD_GET_FILE_NAME_LIST.equals(method)) {
            return getFileNameList(extras);
        }
        return null;
    }

    private Bundle getSize(String pkg) {
        CacheStorage cacheStorage = CacheStorage.getInstance(getContext());
        if (!TextUtils.isEmpty(pkg) && cacheStorage.hasCache(pkg)) {
            long size = cacheStorage.getCache(pkg).size();
            Bundle result = new Bundle();
            result.putLong(CacheProviderContracts.RESULT_SIZE, size);
            return result;
        } else {
            Log.w(TAG, "no cache for " + pkg);
            return null;
        }
    }

    private Bundle getFileNameList(Bundle extras) {
        String pkg = extras.getString(CacheProviderContracts.PARAM_PACKAGE);
        String resourcePath = extras.getString(CacheProviderContracts.PARAM_RESOURCE_PATH);
        Resource resourceDir = HapEngine.getInstance(pkg).getApplicationContext().getResource(resourcePath);
        if (resourceDir == null || resourceDir.getUnderlyingFile() == null) {
            return null;
        }
        File[] files = resourceDir.getUnderlyingFile().listFiles();
        if (files == null) {
            return null;
        }
        ArrayList<String> fileNameList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                String fullFileName = file.getName();
                String fileName = fullFileName.substring(0, fullFileName.lastIndexOf("."));
                fileNameList.add(fileName);
            }
        }
        Bundle result = new Bundle();
        result.putStringArrayList(CacheProviderContracts.RESULT_FILE_NAMES, fileNameList);
        return result;
    }

    @Override
    public ParcelFileDescriptor doOpenFile(Uri uri, String mode) throws FileNotFoundException {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return null;
        }
        File file = getFileForUri(uri);
        if (file == null) {
            throw new FileNotFoundException("not match file, uri=" + uri);
        }
        Log.d(TAG, "openFile: " + file.getAbsolutePath());
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }

    private File getFileForUri(Uri uri) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "getFileForUri: context is null");
            return null;
        }
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File resourceRootDir = Cache.getResourceRootDir(context);
        File file = new File(resourceRootDir, path);
        boolean result = checkResourcePath(resourceRootDir, file);
        if (!result) {
            return null;
        }
        if (file.exists()) {
            return file;
        }

        int splitIndex = path.indexOf('/', 1);
        String pkg = path.substring(0, splitIndex);
        String resPath = path.substring(splitIndex);
        if (pkg.startsWith("/")) {
            pkg = pkg.substring(1);
        }
        if (resPath.startsWith("/")) {
            resPath = resPath.substring(1);
        }
        if (TextUtils.isEmpty(pkg)) {
            return file;
        }

        // 1 如果确定是无效文件, 直接返回
        if (PackageFilesValidator.isInvalidResource(pkg, resPath)) {
            Log.w(TAG, "getFileForUri: res is invalid, res=" + resPath);
            return file;
        }

        // 2 如果正在下载, 等待
        if (InstallFileFlagManager.isPackageInstalling(context, pkg)) {
            // 获取page.js(即打开某个页面)时重新调度优先级
            if (resPath.endsWith(".js") && !"app.js".equals(resPath)) {
                scheduleInstall(pkg, resPath);
            }
            return waitForFileInstalling(context, pkg, file);
        } else if (isPartialInstalled(context, pkg)) {
            // 3 如果部分安装, 重新调度安装并等待
            scheduleInstall(pkg, resPath);
            return waitForFileInstalling(context, pkg, file);
        } else {
            // 4 如果完全未安装或已完整安装, 直接返回
            return file;
        }
    }

    private boolean checkResourcePath(File resourceDir, File resourcePath) {
        try {
            String dirPath = resourceDir.getCanonicalPath();
            if (!dirPath.endsWith("/")) {
                dirPath += "/";
            }
            return resourcePath.getCanonicalPath().startsWith(dirPath);
        } catch (IOException e) {
            return false;
        }
    }

    private void scheduleInstall(String pkg, String resPath) {
        Log.d(TAG, "scheduleInstall pkg=" + pkg + ", resPath=" + resPath);
        Source source = Source.currentSource();
        DistributionManager.getInstance().scheduleInstall(pkg, resPath, source);
    }

    private File waitForFileInstalling(Context context, String pkg, File file) {
        String filepath = file.getAbsolutePath();
        boolean everCheckSucc = false;
        long startTime = System.currentTimeMillis();
        long maxTimeBeforeCheckInstalling = 2000;
        // scheduleResourceRequest()是异步调用, 从发起scheduleResourceRequest()调用到真正进入安装状态
        // (isPackageInstalling()返回true)需要时间, 所以这里加了最多2s的等待.
        while ((!everCheckSucc
                && System.currentTimeMillis() - startTime < maxTimeBeforeCheckInstalling)
                || InstallFileFlagManager.isPackageInstalling(context, pkg)) {
            if (!everCheckSucc) {
                everCheckSucc = InstallFileFlagManager.isPackageInstalling(context, pkg);
            }

            Object lock = getOrCreateResourceLock(filepath);
            synchronized (lock) {
                try {
                    if (file.exists()) {
                        removeResourceLock(filepath);
                        break;
                    } else {
                        lock.wait(50);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "getFileForUri: ", e);
                    break;
                }
            }
        }
        return file;
    }

    private boolean isPartialInstalled(Context context, String pkg) {
        if (!CacheStorage.getInstance(context).hasCache(pkg)) {
            return false;
        }

        // 重置IPC identity, 让debugger可以访问到HybridProvider.
        long token = Binder.clearCallingIdentity();
        try {
            // 已经完整安装则无需发起安装流程
            if (DistributionManager.getInstance().isPackageComplete(pkg)) {
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        return true;
    }

    private Object getOrCreateResourceLock(String filepath) {
        Object resLock = mResourceLocks.get(filepath);
        if (resLock == null) {
            Object tmpResLock = new Object();
            resLock = mResourceLocks.putIfAbsent(filepath, tmpResLock);
            if (resLock == null) {
                resLock = tmpResLock;
            }
        }
        return resLock;
    }

    private Object removeResourceLock(String filepath) {
        return mResourceLocks.remove(filepath);
    }

    private void notifyPackageInstalled(String pkg) {
        PackageFilesValidator.clear(pkg);
        Set<String> keys = new HashSet<>();
        keys.addAll(mResourceLocks.keySet());
        if (keys.isEmpty()) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        String prefix = new File(Cache.getResourceRootDir(context), pkg).getAbsolutePath();
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            if (key.startsWith(prefix)) {
                notifyFileInstalled(key);
            }
        }
    }

    private void notifyFileInstalled(String filepath) {
        Object lock = removeResourceLock(filepath);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private void notifyFileList(String pkg, String subpackage, Set<String> files) {
        PackageFilesValidator.addAppFiles(pkg, subpackage, files);
        Set<String> keys = mResourceLocks.keySet();
        if (keys == null || keys.isEmpty()) {
            return;
        }
        String prefix = new File(Cache.getResourceRootDir(getContext()), pkg).getAbsolutePath();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                String resPath = key.substring(prefix.length());
                if (PackageFilesValidator.isInvalidResource(pkg, subpackage, resPath)) {
                    notifyFileInstalled(key);
                }
            }
        }
    }

    private class PackageInstallListener implements SrpkPackageInstallerBase.InstallListener {
        @Override
        public void onFileList(String pkg, String subpackage, Set<String> files) {
            notifyFileList(pkg, subpackage, files);
        }

        @Override
        public void onFileInstalled(String pkg, File file) {
            notifyFileInstalled(file.getAbsolutePath());
        }

        @Override
        public void onPackageInstalled(String pkg) {
            notifyPackageInstalled(pkg);
        }
    }
}
