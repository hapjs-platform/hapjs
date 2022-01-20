/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.service;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.card.api.InstallListener;
import org.hapjs.card.api.UninstallListener;
import org.hapjs.card.support.CardConstants;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.model.RouterInfo;
import org.hapjs.runtime.PermissionEnhanceService;

public class RemoteInstallService extends PermissionEnhanceService {
    public static final String INSTALL_MODE = "install_mode";
    public static final String CALLING_PACKAGE = "calling_package";
    public static final String DOWNLOAD_URL = "downloadUrl";
    public static final String PATH = "path";
    public static final String PACKAGE = "package";
    public static final String PACKAGE_FILE_DESCRIPTOR = "package_file_descriptor";
    public static final String VERSION_CODE = "versionCode";
    public static final int INSTALL_MODE_FILE = 1;
    public static final int INSTALL_MODE_DISTRIBUTION = 2;
    public static final int INSTALL_MODE_DOWNLOAD = 3;
    public static final int INSTALL_MODE_UNINSTALL = 4;
    public static final int INSTALL_MODE_GET_ALL_APPS = 5;
    public static final int RESULT = 1;
    private static final String TAG = "PackageInstallService";

    public RemoteInstallService() {
        super(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected void distribution(
            String pkg, String downloadUrl, String path, String callingPkg, Messenger replyTo) {
        throw new UnsupportedOperationException();
    }

    protected void distribution(String pkg, int versionCode, String callingPkg, Messenger replyTo) {
        throw new UnsupportedOperationException();
    }

    protected void download(String pkg, int versionCode, Messenger replyTo) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onInvokeAccepted(Message msg) {
        Bundle extra = ((Bundle) msg.obj);
        String pkg = extra.getString(PACKAGE);
        String path = extra.getString(PATH);
        String callingPkg = extra.getString(CALLING_PACKAGE);
        int versionCode = extra.getInt(VERSION_CODE, -1);
        Messenger replyTo = msg.replyTo;
        switch (msg.what) {
            case INSTALL_MODE_FILE:
                File pkgFile = null;
                boolean saveResult = false;
                ParcelFileDescriptor fd = extra.getParcelable(PACKAGE_FILE_DESCRIPTOR);
                if (fd != null) {
                    try {
                        pkgFile = File.createTempFile(pkg, ".rpk");
                        saveResult = FileUtils.saveToFile(fd, pkgFile);
                    } catch (IOException e) {
                        Log.e(TAG, "failed to create temp file", e);
                    } finally {
                        try {
                            fd.close();
                        } catch (IOException e) {
                            Log.e(TAG, "failed to close fd", e);
                        }
                    }
                }
                if (!saveResult) {
                    Log.e(TAG, "install failed for failing to save pkgFile");
                    sendResult(
                            replyTo, InstallListener.INSTALL_RESULT_FAILED,
                            InstallListener.INSTALL_ERROR_IO);
                    return;
                }

                try {
                    CacheStorage.getInstance(RemoteInstallService.this).install(pkg, pkgFile);
                    sendResult(replyTo, InstallListener.INSTALL_RESULT_OK,
                            InstallListener.INSTALL_RESULT_OK);
                } catch (CacheException e) {
                    Log.i(TAG, "install failed", e);
                    int errorCode = e.getErrorCode();
                    sendResult(
                            replyTo, InstallListener.INSTALL_RESULT_FAILED,
                            mapInstallErrorCode(errorCode));
                }
                break;
            case INSTALL_MODE_DISTRIBUTION:
                String downloadUrl = extra.getString(DOWNLOAD_URL);
                if (versionCode > 0) {
                    distribution(pkg, versionCode, callingPkg, replyTo);
                } else {
                    distribution(pkg, downloadUrl, path, callingPkg, replyTo);
                }
                break;
            case INSTALL_MODE_DOWNLOAD:
                download(pkg, versionCode, replyTo);
                break;
            case INSTALL_MODE_UNINSTALL:
                uninstall(replyTo, pkg, callingPkg);
                break;
            case INSTALL_MODE_GET_ALL_APPS:
                getAllApps(replyTo);
                break;
            default:
                Log.e(TAG, "onInvokeAccepted: illegal install mode");
                break;
        }
    }

    @Override
    protected void onInvokeRejected(Message msg) {
        // do nothing
    }

    protected final void sendResult(Messenger messenger, int statusCode, int errorCode) {
        if (messenger == null) {
            Log.w(TAG, "sendResult: messenger is null");
            return;
        }
        Message message = Message.obtain(null, RESULT);
        Bundle bundle = new Bundle();
        bundle.putInt(CardConstants.STATUS_CODE, statusCode);
        bundle.putInt(CardConstants.ERROR_CODE, errorCode);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            Log.e(TAG, "sendResult: ", e);
        }
    }

    protected int mapInstallErrorCode(int errorCode) {
        switch (errorCode) {
            case CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST:
            case CacheErrorCode.PACKAGE_READ_FAILED:
            case CacheErrorCode.PACKAGE_UNZIP_FAILED:
            case CacheErrorCode.PACKAGE_HAS_NO_MANIFEST_JSON:
            case CacheErrorCode.PACKAGE_MANIFEST_JSON_INVALID:
            case CacheErrorCode.PACKAGE_HAS_NO_APP_JS:
            case CacheErrorCode.PACKAGE_NAME_CHANGED:
            case CacheErrorCode.PACKAGE_VERIFY_DIGEST_FAILED:
            case CacheErrorCode.PACKAGE_CACHE_OBSOLETE:
                errorCode = InstallListener.INSTALL_ERROR_CORRUPTION;
                break;
            case CacheErrorCode.PACKAGE_HAS_NO_SIGNATURE:
            case CacheErrorCode.PACKAGE_VERIFY_SIGNATURE_FAILED:
            case CacheErrorCode.PACKAGE_PARSE_CERTIFICATE_FAILED:
            case CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED:
                errorCode = InstallListener.INSTALL_ERROR_SIGNATURE;
                break;
            case CacheErrorCode.RESOURCE_DIR_MOVE_FAILED:
            case CacheErrorCode.LOAD_EXISTED_CERTIFICATE_FAILED:
            case CacheErrorCode.SAVE_CERTIFICATE_FAILED:
            case CacheErrorCode.EMPTY_RESOURCE_PATH:
            case CacheErrorCode.ARCHIVE_FILE_NOT_FOUND:
            case CacheErrorCode.SERVER_ERROR:
                errorCode = InstallListener.INSTALL_ERROR_IO;
                break;
            case CacheErrorCode.PACKAGE_INCOMPATIBLE:
                errorCode = InstallListener.INSTALL_ERROR_INCOMPATIBLE;
                break;
            case CacheErrorCode.NO_ANY_PACKAGE:
            case CacheErrorCode.PACKAGE_UNAVAILABLE:
                errorCode = InstallListener.INSTALL_ERROR_UNAVAILABLE;
                break;
            default:
                errorCode = InstallListener.INSTALL_ERROR_UNKNOWN;
                break;
        }
        return errorCode;
    }

    private void uninstall(Messenger messenger, String pkg, String callingPkg) {
        CacheStorage cacheStorage = CacheStorage.getInstance(this);
        int resultCode = UninstallListener.UNINSTALL_RESULT_FAILED;
        int errorCode = UninstallListener.UNINSTALL_ERROR_UNKNOWN;
        if (!cacheStorage.hasCache(pkg)) {
            errorCode = UninstallListener.UNINSTALL_ERROR_NOT_EXISTS;
        } else {
            Cache cache = cacheStorage.getCache(pkg);
            AppInfo appInfo = cache == null ? null : cache.getAppInfo();
            if (appInfo == null) {
                errorCode = UninstallListener.UNINSTALL_ERROR_NOT_EXISTS;
            } else {
                if (isCardOnly(appInfo)) {
                    cacheStorage.uninstall(pkg);
                    resultCode = UninstallListener.UNINSTALL_RESULT_OK;
                } else {
                    errorCode = UninstallListener.UNINSTALL_ERROR_HAS_PAGES;
                }
            }
        }

        Log.d(
                TAG,
                "uninstalled "
                        + pkg
                        + ", resultCode="
                        + resultCode
                        + ", errorCode="
                        + errorCode
                        + ", by "
                        + callingPkg);
        sendResult(messenger, resultCode, errorCode);
    }

    private boolean isCardOnly(AppInfo appInfo) {
        RouterInfo routerInfo = appInfo.getRouterInfo();
        return routerInfo == null
                || routerInfo.getPageInfos() == null
                || routerInfo.getPageInfos().isEmpty();
    }

    private void getAllApps(Messenger messenger) {
        CacheStorage cacheStorage = CacheStorage.getInstance(this);
        List<Cache> caches = cacheStorage.availableCaches();
        ArrayList<org.hapjs.card.api.AppInfo> allInfos = new ArrayList<>();
        for (Cache cache : caches) {
            AppInfo appInfo = cache.getAppInfo();
            if (isCardOnly(appInfo)) {
                allInfos.add(
                        new org.hapjs.card.api.AppInfo(
                                appInfo.getPackage(),
                                appInfo.getName(),
                                appInfo.getVersionName(),
                                appInfo.getVersionCode(),
                                appInfo.getMinPlatformVersion()));
            }
        }

        Message message = Message.obtain(null, RESULT);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(CardConstants.INSTALLED_APPS, allInfos);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            Log.e(TAG, "sendResult: ", e);
        }
    }
}
