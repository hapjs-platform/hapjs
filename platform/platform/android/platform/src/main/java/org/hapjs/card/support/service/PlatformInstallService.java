/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.service;

import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheStorage;
import org.hapjs.card.api.DownloadListener;
import org.hapjs.card.api.InstallListener;
import org.hapjs.distribution.DistributionManager;
import org.hapjs.distribution.DistributionManager.PackageInstallResultListener;
import org.hapjs.distribution.DistributionProvider;
import org.hapjs.logging.Source;
import org.hapjs.runtime.ProviderManager;

public class PlatformInstallService extends RemoteInstallService {
    private static final String TAG = "PackageInstallService";

    @Override
    protected void distribution(
            String pkg, String downloadUrl, String path, String callingPkg,
            final Messenger replyTo) {
        Source source = new Source();
        source.setPackageName(callingPkg);
        source.setType(Source.TYPE_OTHER);
        source.putInternal("scene", "card");
        if (!TextUtils.isEmpty(downloadUrl)) {
            source.putInternal(DOWNLOAD_URL, downloadUrl);
        }
        if (!TextUtils.isEmpty(path)) {
            source.putInternal(PATH, path);
        }

        PackageInstallResultListener listener =
                new PackageInstallResultListener() {
                    @Override
                    public void onPackageInstallResult(
                            String pkg, String subpackage, int statusCode, int errorCode) {
                        if (sendDistributionResultIfNeeded(replyTo, statusCode, errorCode)) {
                            DistributionManager.getInstance()
                                    .removeInstallResultListener(this, pkg, null);
                        }
                    }
                };
        DistributionManager.getInstance().addInstallResultListener(listener, pkg, null);
        DistributionManager.getInstance().scheduleInstall(pkg, path, source, false);
    }

    @Override
    protected void distribution(String pkg, int versionCode, String callingPkg, Messenger replyTo) {
        Source source = new Source();
        source.setPackageName(callingPkg);
        source.setType(Source.TYPE_OTHER);
        source.putInternal("scene", "card");
        source.putInternal(VERSION_CODE, String.valueOf(versionCode));

        PackageInstallResultListener listener =
                new PackageInstallResultListener() {
                    @Override
                    public void onPackageInstallResult(
                            String pkg, String subpackage, int statusCode, int errorCode) {
                        boolean resultSent = false;
                        if (statusCode == DistributionManager.CODE_INSTALL_OK && versionCode > 0) {
                            Cache cache = CacheStorage.getInstance(PlatformInstallService.this)
                                    .getCache(pkg);
                            int newVersion = -1;
                            if (cache != null && cache.getAppInfo() != null) {
                                newVersion = cache.getAppInfo().getVersionCode();
                            }
                            if (newVersion == versionCode) {
                                resultSent = sendDistributionResultIfNeeded(replyTo, statusCode,
                                        errorCode);
                            } else {
                                resultSent =
                                        sendDistributionResultIfNeeded(
                                                replyTo, DistributionManager.CODE_INSTALL_ERROR,
                                                CacheErrorCode.UNKNOWN);
                                Log.e(
                                        TAG,
                                        "install ok but version code not match. expected="
                                                + versionCode
                                                + ", got="
                                                + newVersion);
                            }
                        } else {
                            resultSent =
                                    sendDistributionResultIfNeeded(replyTo, statusCode, errorCode);
                        }
                        if (resultSent) {
                            DistributionManager.getInstance()
                                    .removeInstallResultListener(this, pkg, null);
                        }
                    }
                };
        DistributionManager.getInstance().addInstallResultListener(listener, pkg, null);
        DistributionManager.getInstance().scheduleInstall(pkg, versionCode, source, false);
    }

    private boolean sendDistributionResultIfNeeded(Messenger replyTo, int statusCode,
                                                   int errorCode) {
        if (statusCode == DistributionManager.CODE_INSTALL_OK) {
            sendResult(replyTo, InstallListener.INSTALL_RESULT_OK,
                    InstallListener.INSTALL_ERROR_UNKNOWN);
            return true;
        } else if (statusCode == DistributionManager.CODE_INSTALL_ERROR) {
            sendResult(replyTo, InstallListener.INSTALL_RESULT_FAILED,
                    mapInstallErrorCode(errorCode));
            return true;
        } else if (statusCode == DistributionManager.CODE_INSTALL_TIMEOUT) {
            sendResult(
                    replyTo, InstallListener.INSTALL_RESULT_FAILED,
                    InstallListener.INSTALL_ERROR_TIMEOUT);
            return true;
        } else if (statusCode == DistributionManager.CODE_INSTALL_CANCEL) {
            sendResult(
                    replyTo, InstallListener.INSTALL_RESULT_FAILED,
                    InstallListener.INSTALL_ERROR_UNKNOWN);
            return true;
        }
        return false;
    }

    @Override
    protected void download(String pkg, int versionCode, Messenger replyTo) {
        DistributionProvider distributionProvider =
                ProviderManager.getDefault().getProvider(DistributionProvider.NAME);
        if (distributionProvider != null) {
            int resultCode = distributionProvider.download(pkg, versionCode);
            if (resultCode == CacheErrorCode.OK) {
                sendResult(
                        replyTo, DownloadListener.DOWNLOAD_RESULT_OK,
                        DownloadListener.DOWNLOAD_ERROR_UNKNOWN);
            } else {
                sendResult(
                        replyTo, DownloadListener.DOWNLOAD_RESULT_FAILED,
                        mapDownloadErrorCode(resultCode));
            }
        } else {
            sendResult(
                    replyTo,
                    DownloadListener.DOWNLOAD_RESULT_FAILED,
                    DownloadListener.DOWNLOAD_ERROR_UNKNOWN);
            Log.e(TAG, "DistributionProvider not found");
        }
    }

    private int mapDownloadErrorCode(int errorCode) {
        switch (errorCode) {
            case CacheErrorCode.ALREADY_INSTALLED:
                errorCode = DownloadListener.DOWNLOAD_ERROR_ALREADY_INSTALLED;
                break;
            case CacheErrorCode.NO_ANY_PACKAGE:
            case CacheErrorCode.PACKAGE_UNAVAILABLE:
                errorCode = DownloadListener.DOWNLOAD_ERROR_PACKAGE_UNAVAILABLE;
                break;
            case CacheErrorCode.NETWORK_UNAVAILABLE:
                errorCode = DownloadListener.DOWNLOAD_ERROR_NETWORK_UNAVAILABLE;
                break;
            default:
                errorCode = DownloadListener.DOWNLOAD_ERROR_UNKNOWN;
                break;
        }
        return errorCode;
    }
}
