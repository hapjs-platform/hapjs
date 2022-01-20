/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.card.api.DownloadListener;
import org.hapjs.card.api.GetAllAppsListener;
import org.hapjs.card.api.InstallListener;
import org.hapjs.card.api.UninstallListener;
import org.hapjs.card.support.internal.InternalInstallListener;
import org.hapjs.card.support.service.RemoteInstallService;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.Runtime;

public class CardInstaller {
    private static final String TAG = "CardInstaller";

    private static final int SERVICE_STATUS_UNBIND = 0;
    private static final int SERVICE_STATUS_BINDING = 1;
    private static final int SERVICE_STATUS_BOUND = 2;
    private Context mContext;
    private String mPlatform;
    private volatile Messenger mServerMessenger;
    private Handler mHandler;
    private ServiceConnection mServiceConnection;
    private List<Runnable> mPendingTasks;
    private int mBindStatus = SERVICE_STATUS_UNBIND;

    private CardInstaller() {
        mContext = Runtime.getInstance().getContext();
        mPlatform = ResourceConfig.getInstance().getPlatform();
        mPendingTasks = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
        mServiceConnection =
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        mBindStatus = SERVICE_STATUS_BOUND;
                        mServerMessenger = new Messenger(service);
                        for (Runnable task : mPendingTasks) {
                            task.run();
                        }
                        mPendingTasks.clear();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        mServerMessenger = null;
                        mBindStatus = SERVICE_STATUS_UNBIND;
                    }
                };
    }

    public static CardInstaller getInstance() {
        return Holder.INSTANCE;
    }

    private static void handleInstallResult(
            String pkg, InstallListener listener, int resultCode, int errorCode) {
        HapEngine.getInstance(pkg).getApplicationContext().reset();
        if (listener != null) {
            try {
                listener.onInstallResult(pkg, resultCode, errorCode);
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "handleInstallResult: invoke onInstallResult failed", e);
                listener.onInstallResult(pkg, resultCode);
            }
        }
        RuntimeLogManager.getDefault().logCardInstall(pkg, resultCode, errorCode);
    }

    private static void handleInstallResult(
            String pkg, InternalInstallListener listener, int resultCode, int errorCode) {
        HapEngine.getInstance(pkg).getApplicationContext().reset();
        if (listener != null) {
            listener.onInstallResult(pkg, resultCode, errorCode);
        }
        RuntimeLogManager.getDefault().logCardInstall(pkg, resultCode, errorCode);
    }

    private static void handleDownloadResult(
            String pkg, DownloadListener listener, int resultCode, int errorCode) {
        if (listener != null) {
            listener.onDownloadResult(pkg, resultCode, errorCode);
        }
        RuntimeLogManager.getDefault().logCardDownload(pkg, resultCode, errorCode);
    }

    private static void handleUninstallResult(
            String pkg, UninstallListener listener, int resultCode, int errorCode) {
        HapEngine.getInstance(pkg).getApplicationContext().reset();
        if (listener != null) {
            listener.onUninstallResult(pkg, resultCode, errorCode);
        }
        RuntimeLogManager.getDefault().logCardUninstall(pkg, resultCode, errorCode);
    }

    /**
     * 预下载指定版本的包
     */
    public void download(String pkg, int versionCode, DownloadListener listener) {
        if (!checkVersion(pkg, versionCode, listener)) {
            return;
        }

        Messenger replyTo =
                new Messenger(new DownloadMessengerHandler(pkg, listener, Looper.getMainLooper()));
        Bundle extra = new Bundle();
        extra.putString(RemoteInstallService.PACKAGE, pkg);
        extra.putInt(RemoteInstallService.VERSION_CODE, versionCode);
        extra.putString(RemoteInstallService.CALLING_PACKAGE, mContext.getPackageName());
        sendMessage(RemoteInstallService.INSTALL_MODE_DOWNLOAD, extra, replyTo);
    }

    /**
     * 安装指定的包
     */
    public void install(String pkg, String fileUri, InstallListener listener) {
        if (!fileUri.startsWith("file://")) {
            throw new IllegalArgumentException("uri has a wrong scheme which must be file");
        }
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            installLocal(pkg, fileUri, listener);
        } else {
            installRemote(pkg, fileUri, listener);
        }
    }

    /**
     * 使用平台分发服务安装指定版本的包
     */
    public void install(String pkg, int versionCode, InstallListener listener) {
        if (!checkVersion(pkg, versionCode, listener)) {
            return;
        }

        Messenger replyTo =
                new Messenger(new InstallMessengerHandler(pkg, listener, Looper.getMainLooper()));
        Bundle extra = new Bundle();
        extra.putString(RemoteInstallService.PACKAGE, pkg);
        extra.putInt(RemoteInstallService.VERSION_CODE, versionCode);
        extra.putString(RemoteInstallService.CALLING_PACKAGE, mContext.getPackageName());
        sendMessage(RemoteInstallService.INSTALL_MODE_DISTRIBUTION, extra, replyTo);
    }

    /**
     * 使用平台的分发服务安装，
     */
    public void installCard(
            final String pkg,
            final String downloadUrl,
            String path,
            final InternalInstallListener listener) {
        Messenger replyTo =
                new Messenger(new InstallMessengerHandler(pkg, listener, Looper.getMainLooper()));
        Bundle extra = new Bundle();
        extra.putString(RemoteInstallService.PACKAGE, pkg);
        if (!TextUtils.isEmpty(downloadUrl)) {
            extra.putString(RemoteInstallService.DOWNLOAD_URL, downloadUrl);
        }
        if (!TextUtils.isEmpty(path)) {
            extra.putString(RemoteInstallService.PATH, path);
        }
        sendMessage(RemoteInstallService.INSTALL_MODE_DISTRIBUTION, extra, replyTo);
    }

    private void installLocal(String pkg, String fileUri, InstallListener listener) {
        File file;
        if (UriUtils.isAssetUri(fileUri)) {
            file = Cache.getArchiveFile(mContext, pkg);
            boolean copyResult = copyAssetFile(fileUri, file);
            if (!copyResult) {
                handleInstallResult(
                        pkg, listener, InstallListener.INSTALL_RESULT_FAILED,
                        InstallListener.INSTALL_ERROR_IO);
                return;
            }
        } else {
            file = new File(Uri.parse(fileUri).getPath());
        }

        if (!checkVersion(pkg, file, listener)) {
            return;
        }

        try {
            CacheStorage.getInstance(mContext).install(pkg, file.getAbsolutePath());
            handleInstallResult(
                    pkg, listener, InstallListener.INSTALL_RESULT_OK,
                    InstallListener.INSTALL_RESULT_OK);
        } catch (CacheException e) {
            Log.i(TAG, "install local failed", e);
            handleInstallResult(
                    pkg, listener, InstallListener.INSTALL_RESULT_FAILED,
                    InstallListener.INSTALL_ERROR_IO);
        }
    }

    private boolean copyAssetFile(String assetUri, File destFile) {
        InputStream assetIs = null;
        try {
            assetIs = mContext.getAssets().open(UriUtils.getAssetPath(assetUri));
            boolean copyResult = FileUtils.saveToFile(assetIs, destFile);
            if (!copyResult) {
                Log.e(TAG, "copy to external storage failed for: " + assetUri);
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "file not exist for: " + assetUri);
            return false;
        } finally {
            FileUtils.closeQuietly(assetIs);
        }
        return true;
    }

    private void installRemote(String pkg, String fileUri, InstallListener listener) {
        Messenger replyTo =
                new Messenger(new InstallMessengerHandler(pkg, listener, Looper.getMainLooper()));

        File file;
        if (UriUtils.isAssetUri(fileUri)) {
            try {
                file = File.createTempFile(pkg, ".rpk");
            } catch (IOException e) {
                Log.e(TAG, "failed to create temp file", e);
                handleInstallResult(
                        pkg, listener, InstallListener.INSTALL_RESULT_FAILED,
                        InstallListener.INSTALL_ERROR_IO);
                return;
            }

            boolean copyResult = copyAssetFile(fileUri, file);
            if (!copyResult) {
                handleInstallResult(
                        pkg, listener, InstallListener.INSTALL_RESULT_FAILED,
                        InstallListener.INSTALL_ERROR_IO);
                return;
            }
        } else {
            file = new File(Uri.parse(fileUri).getPath());
        }

        if (!checkVersion(pkg, file, listener)) {
            return;
        }

        Bundle extra = new Bundle();
        try {
            ParcelFileDescriptor fd =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            extra.putParcelable(RemoteInstallService.PACKAGE_FILE_DESCRIPTOR, fd);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to open file", e);
            handleInstallResult(
                    pkg, listener, InstallListener.INSTALL_RESULT_FAILED,
                    InstallListener.INSTALL_ERROR_IO);
            return;
        }

        extra.putString(RemoteInstallService.PACKAGE, pkg);
        extra.putString(RemoteInstallService.CALLING_PACKAGE, mContext.getPackageName());
        sendMessage(RemoteInstallService.INSTALL_MODE_FILE, extra, replyTo);
    }

    private boolean checkVersion(String pkg, File archive, InstallListener listener) {
        AppInfo appInfo = PackageUtils.getAppInfo(archive);
        if (appInfo == null) {
            handleInstallResult(
                    pkg,
                    listener,
                    InstallListener.INSTALL_RESULT_FAILED,
                    InstallListener.INSTALL_ERROR_CORRUPTION);
            Log.e(TAG, "failed to get AppInfo from archive. pkg=" + pkg);
            return false;
        } else {
            return checkVersion(pkg, appInfo.getVersionCode(), listener);
        }
    }

    private boolean checkVersion(String pkg, int targetVersion, InstallListener listener) {
        AppInfo oldAppInfo = HapEngine.getInstance(pkg).getApplicationContext().getAppInfo();
        if (oldAppInfo != null) {
            if (targetVersion < oldAppInfo.getVersionCode()) {
                handleInstallResult(
                        pkg,
                        listener,
                        InstallListener.INSTALL_RESULT_FAILED,
                        InstallListener.INSTALL_ERROR_DOWNGRADE);
                Log.e(
                        TAG,
                        "cannot download grade. pkg="
                                + pkg
                                + ", oldVersion="
                                + oldAppInfo.getVersionCode()
                                + ", newVersion="
                                + targetVersion);
                return false;
            }
        }
        return true;
    }

    private boolean checkVersion(String pkg, int targetVersion, DownloadListener listener) {
        AppInfo oldAppInfo = HapEngine.getInstance(pkg).getApplicationContext().getAppInfo();
        if (oldAppInfo != null) {
            if (targetVersion < oldAppInfo.getVersionCode()) {
                listener.onDownloadResult(
                        pkg,
                        DownloadListener.DOWNLOAD_RESULT_FAILED,
                        DownloadListener.DOWNLOAD_ERROR_DOWNGRADE);
                Log.e(
                        TAG,
                        "cannot download grade. pkg="
                                + pkg
                                + ", oldVersion="
                                + oldAppInfo.getVersionCode()
                                + ", newVersion="
                                + targetVersion);
                return false;
            } else if (targetVersion == oldAppInfo.getVersionCode()) {
                listener.onDownloadResult(
                        pkg,
                        DownloadListener.DOWNLOAD_RESULT_FAILED,
                        DownloadListener.DOWNLOAD_ERROR_ALREADY_INSTALLED);
                Log.e(
                        TAG,
                        "already installed. pkg="
                                + pkg
                                + ", oldVersion="
                                + oldAppInfo.getVersionCode()
                                + ", newVersion="
                                + targetVersion);
                return false;
            }
        }
        return true;
    }

    private Intent buildRemoteInstallIntent() {
        Intent intent = new Intent();
        intent.setPackage(mPlatform);
        intent.setAction(mPlatform + ".action.INSTALL");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        return intent;
    }

    private void bindService(Context context, ServiceConnection connection) {
        if (mBindStatus == SERVICE_STATUS_UNBIND) {
            mBindStatus = SERVICE_STATUS_BINDING;
            Intent intent = buildRemoteInstallIntent();
            mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (mBindStatus == SERVICE_STATUS_BOUND) {
            mContext.unbindService(mServiceConnection);
            mBindStatus = SERVICE_STATUS_UNBIND;
            mServerMessenger = null;
        }
    }

    private void sendMessage(final int what, final Bundle obj, Messenger replyTo) {
        Messenger messenger = mServerMessenger;
        if (messenger == null) {
            Runnable sendMsgTask =
                    new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(what, obj, replyTo);
                        }
                    };
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mPendingTasks.add(sendMsgTask);
                bindService(mContext, mServiceConnection);
            } else {
                mHandler.post(sendMsgTask);
            }
        } else {
            Message message = Message.obtain();
            message.what = what;
            message.obj = obj;
            message.replyTo = replyTo;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "sendMessage fail! ", e);
            }
        }
    }

    public void uninstall(String pkg, UninstallListener listener) {
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            uninstallLocal(pkg, listener);
        } else {
            uninstallRemote(pkg, listener);
        }
    }

    private void uninstallLocal(String pkg, UninstallListener listener) {
        CacheStorage cacheStorage = CacheStorage.getInstance(mContext);
        if (cacheStorage.hasCache(pkg)) {
            cacheStorage.uninstall(pkg);
            handleUninstallResult(
                    pkg,
                    listener,
                    UninstallListener.UNINSTALL_RESULT_OK,
                    UninstallListener.UNINSTALL_ERROR_UNKNOWN);
        } else {
            handleUninstallResult(
                    pkg,
                    listener,
                    UninstallListener.UNINSTALL_RESULT_FAILED,
                    UninstallListener.UNINSTALL_ERROR_NOT_EXISTS);
        }
    }

    private void uninstallRemote(String pkg, UninstallListener listener) {
        Messenger replyTo =
                new Messenger(new UninstallMessageHandler(pkg, listener, Looper.getMainLooper()));
        Bundle extra = new Bundle();
        extra.putString(RemoteInstallService.PACKAGE, pkg);
        extra.putString(RemoteInstallService.CALLING_PACKAGE, mContext.getPackageName());
        sendMessage(RemoteInstallService.INSTALL_MODE_UNINSTALL, extra, replyTo);
    }

    public void getAllApps(GetAllAppsListener listener) {
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            getAllAppsLocal(listener);
        } else {
            getAllAppsRemote(listener);
        }
    }

    private void getAllAppsLocal(GetAllAppsListener listener) {
        CacheStorage cacheStorage = CacheStorage.getInstance(mContext);
        List<Cache> caches = cacheStorage.availableCaches();
        ArrayList<org.hapjs.card.api.AppInfo> allInfos = new ArrayList<>();
        for (Cache cache : caches) {
            AppInfo appInfo = cache.getAppInfo();
            if (appInfo != null) {
                allInfos.add(
                        new org.hapjs.card.api.AppInfo(
                                appInfo.getPackage(),
                                appInfo.getName(),
                                appInfo.getVersionName(),
                                appInfo.getVersionCode(),
                                appInfo.getMinPlatformVersion()));
            }
        }

        if (listener != null) {
            listener.onAllApps(allInfos);
        }
    }

    private void getAllAppsRemote(GetAllAppsListener listener) {
        Messenger replyTo =
                new Messenger(new GetAllAppsMessageHandler(listener, Looper.getMainLooper()));
        Bundle extra = new Bundle();
        extra.putString(RemoteInstallService.CALLING_PACKAGE, mContext.getPackageName());
        sendMessage(RemoteInstallService.INSTALL_MODE_GET_ALL_APPS, extra, replyTo);
    }

    private static class Holder {
        static CardInstaller INSTANCE = new CardInstaller();
    }

    private static class InstallMessengerHandler extends Handler {
        String mPkg;
        InternalInstallListener mInstallListener;

        InstallMessengerHandler(String pkg, InternalInstallListener listener, Looper looper) {
            super(looper);
            mPkg = pkg;
            mInstallListener =
                    new InternalInstallListener() {
                        @Override
                        public void onInstallResult(String pkg, int resultCode, int errorCode) {
                            handleInstallResult(pkg, listener, resultCode, errorCode);
                        }
                    };
        }

        InstallMessengerHandler(String pkg, InstallListener listener, Looper looper) {
            super(looper);
            mPkg = pkg;
            mInstallListener =
                    new InternalInstallListener() {
                        @Override
                        public void onInstallResult(String pkg, int resultCode, int errorCode) {
                            handleInstallResult(pkg, listener, resultCode, errorCode);
                        }
                    };
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteInstallService.RESULT) {
                Bundle data = msg.getData();
                int statusCode = data.getInt(CardConstants.STATUS_CODE);
                int errorCode = data.getInt(CardConstants.ERROR_CODE);
                handleInstallResult(mPkg, mInstallListener, statusCode, errorCode);
                Log.d(TAG, "handleMessage: InstallMessengerHandler , " + statusCode + ", "
                        + errorCode);
            }
        }
    }

    private static class DownloadMessengerHandler extends Handler {
        String mPkg;
        DownloadListener mInstallListener;

        DownloadMessengerHandler(String pkg, DownloadListener listener, Looper looper) {
            super(looper);
            mPkg = pkg;
            mInstallListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteInstallService.RESULT) {
                Bundle data = msg.getData();
                int statusCode = data.getInt(CardConstants.STATUS_CODE);
                int errorCode = data.getInt(CardConstants.ERROR_CODE);
                handleDownloadResult(mPkg, mInstallListener, statusCode, errorCode);
                Log.d(TAG, "handleMessage: DownloadMessengerHandler , " + statusCode + ", "
                        + errorCode);
            }
        }
    }

    private static class UninstallMessageHandler extends Handler {
        String mPkg;
        UninstallListener mUninstallListener;

        UninstallMessageHandler(String pkg, UninstallListener listener, Looper looper) {
            super(looper);
            mPkg = pkg;
            mUninstallListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteInstallService.RESULT) {
                Bundle data = msg.getData();
                int statusCode = data.getInt(CardConstants.STATUS_CODE);
                int errorCode = data.getInt(CardConstants.ERROR_CODE);
                handleUninstallResult(mPkg, mUninstallListener, statusCode, errorCode);
                Log.d(TAG, "handleMessage: UninstallMessageHandler , " + statusCode + ", "
                        + errorCode);
            }
        }
    }

    private static class GetAllAppsMessageHandler extends Handler {
        GetAllAppsListener mGetAllAppsListener;

        GetAllAppsMessageHandler(GetAllAppsListener listener, Looper looper) {
            super(looper);
            mGetAllAppsListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteInstallService.RESULT) {
                Bundle data = msg.getData();
                data.setClassLoader(getClass().getClassLoader());
                List<org.hapjs.card.api.AppInfo> appInfos =
                        data.getParcelableArrayList(CardConstants.INSTALLED_APPS);
                if (mGetAllAppsListener != null) {
                    mGetAllAppsListener.onAllApps(appInfos);
                }
                Log.d(TAG, "handleMessage: GetAllAppsMessageHandler, " + appInfos);
            }
        }
    }
}
