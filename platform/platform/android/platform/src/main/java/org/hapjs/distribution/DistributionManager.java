/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.hapjs.PlatformLogManager;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.InstallFileFlagManager;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.utils.SystemController;

public class DistributionManager {
    public static final String EXTRA_APP = "app";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_SUBPACKAGE = "subpackage";
    public static final String EXTRA_STATUS_CODE = "statusCode";
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_PREVIEW_INFO = "previewInfo";
    public static final String EXTRA_IS_BACKGROUND = "isBackground";
    public static final String EXTRA_LISTENER_NAME = "listenerName";
    public static final String EXTRA_LOAD_SIZE = "loadSize";
    public static final String EXTRA_TOTAL_SIZE = "totalSize";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_SESSION = "session";
    public static final String EXTRA_MIN_APP_VERSION = "minAppVersion";
    public static final String EXTRA_VERSION_CODE = "versionCode";
    public static final int CODE_INSTALL_OK = 0;
    public static final int CODE_INSTALLING = 1;
    public static final int CODE_INSTALL_ERROR = 2;
    public static final int CODE_INSTALL_TIMEOUT = 3;
    public static final int CODE_INSTALL_UNKNOWN = 4;
    public static final int CODE_INSTALL_CANCEL = 5;
    public static final int CODE_APPLY_UPDATE_DELAYED = 6;
    public static final int CODE_INSTALL_STREAM = 7;
    // can not use -1 as new app status value
    public static final int APP_STATUS_NONE = 0;
    public static final int APP_STATUS_UPDATE = 1;
    public static final int APP_STATUS_READY = 2;
    public static final int APP_STATUS_INCOMPLETE = 3;
    static final int MESSAGE_INSTALL_STATUS = 1;
    static final int MESSAGE_PREVIEW_INFO = 2;
    static final int MESSAGE_UNBIND_SERVICE = 3;
    static final int MESSAGE_PACKAGE_INSTALL_RESULT = 4;
    static final int MESSAGE_PACKAGE_INSTALL_PROGRESS = 5;
    private static final String TAG = "DistributionManager";
    private static final int SERVICE_STATUS_UNBIND = 0;
    private static final int SERVICE_STATUS_BINDING = 1;
    private static final int SERVICE_STATUS_BOUND = 2;
    private static final int UNBIND_DELAY = 5 * 60 * 1000;
    private final ServiceConnection mServiceConnection;
    private final IBinder.DeathRecipient mDeathRecipient;
    private final Map<String, InstallStatusListener> mInstallStatusListeners;
    private final Map<String, PackageInstallResultListener> mInstallResultListeners;
    private final Map<String, PackageInstallProgressListener> mInstallProgressListeners;
    private Context mContext;
    private CacheStorage mCacheStorage;
    private DistributionProvider mDistributionProvider;
    private Map<String, Source> mInstallSourceMap;
    private Handler mHandler;
    private Messenger mLocalMessenger;
    private Messenger mServiceMessenger;
    private IBinder mService;
    private List<Runnable> mPendingTasks;
    private int mBindStatus = SERVICE_STATUS_UNBIND;
    private AtomicInteger mListenerId = new AtomicInteger();

    private DistributionManager(final Context context) {
        mContext = context.getApplicationContext();
        mCacheStorage = CacheStorage.getInstance(context);
        mDistributionProvider = ProviderManager.getDefault().getProvider(DistributionProvider.NAME);
        mInstallSourceMap = new ConcurrentHashMap<>();
        mInstallStatusListeners = new ConcurrentHashMap<>();
        mInstallResultListeners = new ConcurrentHashMap<>();
        mInstallProgressListeners = new ConcurrentHashMap<>();
        mPendingTasks = new ArrayList<>();
        mHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.w(TAG, "handleMessage " + msg.what);
                        Bundle bundle = msg.getData();
                        if (bundle == null) {
                            return;
                        }
                        if (msg.what == MESSAGE_INSTALL_STATUS) {
                            String pkg = bundle.getString(DistributionManager.EXTRA_APP);
                            int statusCode = bundle.getInt(DistributionManager.EXTRA_STATUS_CODE);
                            int errorCode = bundle.getInt(DistributionManager.EXTRA_ERROR_CODE);
                            String name = bundle.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            notifyResult(pkg, statusCode, errorCode, name);
                        } else if (msg.what == MESSAGE_PREVIEW_INFO) {
                            bundle.setClassLoader(PreviewInfo.class.getClassLoader());
                            String pkg = bundle.getString(DistributionManager.EXTRA_APP);
                            PreviewInfo previewInfo =
                                    bundle.getParcelable(DistributionManager.EXTRA_PREVIEW_INFO);
                            String name = bundle.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            notifyPreviewInfo(pkg, previewInfo, name);
                        } else if (msg.what == MESSAGE_UNBIND_SERVICE) {
                            unbindInstallService();
                        } else if (msg.what == MESSAGE_PACKAGE_INSTALL_PROGRESS) {
                            String pkg = bundle.getString(DistributionManager.EXTRA_APP);
                            String subpackage =
                                    bundle.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = bundle.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            long loadSize = bundle.getLong(DistributionManager.EXTRA_LOAD_SIZE);
                            long totalSize = bundle.getLong(DistributionManager.EXTRA_TOTAL_SIZE);
                            notifySubpackageProgress(pkg, subpackage, loadSize, totalSize, name);
                        } else if (msg.what == MESSAGE_PACKAGE_INSTALL_RESULT) {
                            String pkg = bundle.getString(DistributionManager.EXTRA_APP);
                            String subpackage =
                                    bundle.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = bundle.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            int statusCode = bundle.getInt(DistributionManager.EXTRA_STATUS_CODE);
                            int errorCode = bundle.getInt(DistributionManager.EXTRA_ERROR_CODE);
                            notifySubpackageResult(pkg, subpackage, statusCode, errorCode, name);
                        }
                    }
                };

        mDeathRecipient =
                new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        mHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mServiceConnection.onServiceDisconnected(null);
                                        for (String pkg : mInstallSourceMap.keySet()) {
                                            notifyResult(pkg, CODE_INSTALL_ERROR,
                                                    CacheErrorCode.UNKNOWN);
                                        }
                                        bindInstallService(context, mServiceConnection);
                                    }
                                });
                    }
                };

        mServiceConnection =
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        mBindStatus = SERVICE_STATUS_BOUND;
                        mServiceMessenger = new Messenger(service);
                        mLocalMessenger = new Messenger(mHandler);
                        mService = service;
                        try {
                            service.linkToDeath(mDeathRecipient, 0);
                        } catch (RemoteException e) {
                            Log.d(TAG, "linkToDeath", e);
                        }
                        for (Runnable task : mPendingTasks) {
                            task.run();
                        }
                        mPendingTasks.clear();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        mBindStatus = SERVICE_STATUS_UNBIND;
                        mServiceMessenger = null;
                        mLocalMessenger = null;
                    }
                };
    }

    public static DistributionManager getInstance() {
        return Holder.INSTANCE;
    }

    public PreviewInfo getPreviewInfo(String pkg) throws CacheException {
        DistributionProvider distributionProvider = mDistributionProvider;
        if (distributionProvider == null) {
            return null;
        } else {
            return distributionProvider.getPreviewInfo(pkg);
        }
    }

    public ServerSettings getServerSettings(String pkg) {
        DistributionProvider distributionProvider = mDistributionProvider;
        if (distributionProvider == null) {
            return null;
        } else {
            return distributionProvider.getServerSettings(pkg);
        }
    }

    public boolean isAppReady(String pkg) {
        return getAppStatus(pkg) == APP_STATUS_READY;
    }

    public boolean isPackageComplete(String pkg) {
        if (!mCacheStorage.hasCache(pkg)) {
            return false;
        }

        Cache cache = mCacheStorage.getCache(pkg);
        AppInfo appInfo = cache.getAppInfo();
        if (appInfo == null) {
            Log.e(TAG, "appInfo is null.");
            return false;
        }
        List<SubpackageInfo> subpackageInfos = appInfo.getSubpackageInfos();
        if (subpackageInfos == null || subpackageInfos.size() == 0) {
            // 如果没有分包信息，说明通过整包方式安装，正在安装中则说明不完整
            return !InstallFileFlagManager.isPackageInstalling(mContext, pkg);
        }

        int versionCode = appInfo.getVersionCode();
        List<String> installedList =
                InstalledSubpackageManager.getInstance().queryInstallList(mContext, pkg, versionCode);
        if (installedList == null || installedList.isEmpty()) {
            // 每个分包安装完都会在数据库保存记录. 如果数据库没有记录, 那说明不是通过分包而是整包安装的.如果正在安装中则说明不完整
            return !InstallFileFlagManager.isPackageInstalling(mContext, pkg);
        }

        for (SubpackageInfo info : subpackageInfos) {
            if (!installedList.contains(info.getName())) {
                return false;
            }
        }
        return true;
    }

    public int getAppStatus(String pkg) {
        if (!mCacheStorage.hasCache(pkg)) {
            return APP_STATUS_NONE;
        }
        if (mDistributionProvider != null && mDistributionProvider.needUpdate(pkg)) {
            return APP_STATUS_UPDATE;
        }
        if (!isPackageComplete(pkg)) {
            return APP_STATUS_INCOMPLETE;
        }
        return APP_STATUS_READY;
    }

    private void bindInstallService(Context context, ServiceConnection connection) {
        if (mBindStatus == SERVICE_STATUS_UNBIND) {
            mBindStatus = SERVICE_STATUS_BINDING;
            Intent intent = new Intent(context, DistributionService.class);
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindInstallService() {
        if (mBindStatus == SERVICE_STATUS_BOUND) {
            mBindStatus = SERVICE_STATUS_UNBIND;
            if (mService != null) {
                mService.unlinkToDeath(mDeathRecipient, 0);
            }
            mContext.unbindService(mServiceConnection);
            mServiceMessenger = null;
            mLocalMessenger = null;
            mBindStatus = SERVICE_STATUS_UNBIND;
        }
    }

    private void scheduleUnbind() {
        mHandler.removeMessages(MESSAGE_UNBIND_SERVICE);
        if (mInstallStatusListeners.size() == 0) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_UNBIND_SERVICE, UNBIND_DELAY);
        }
    }

    public void addInstallStatusListener(InstallStatusListener listener) {
        if (listener == null) {
            return;
        }

        // 安装过程中主进程crash导致安装失败, mInstallStatusListeners并不会清空. 应用进程重新发起安装时, 需要
        // 重新向主进程注册listener, 否则收不到回调. 因而即使mInstallStatusListeners.contains(listener),
        // 也需要向主进程注册.
        String listenerName = (String) getKeyByValue(mInstallStatusListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            listenerName = generateListenerName();
            mInstallStatusListeners.put(listenerName, listener);
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_ADD_INSTALL_STATUS_LISTENER, bundle, true);
    }

    private String generateListenerName() {
        return "Listener:" + Process.myPid() + "_" + mListenerId.incrementAndGet();
    }

    public void removeInstallStatusListener(InstallStatusListener listener) {
        if (listener == null) {
            return;
        }
        String listenerName = (String) getKeyByValue(mInstallStatusListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            Log.w(TAG, "Failed to remove, listener not exists");
            return;
        }
        mInstallStatusListeners.remove(listenerName);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_REMOVE_INSTALL_STATUS_LISTENER, bundle, false);
    }

    public void addInstallResultListener(
            PackageInstallResultListener listener, String pkg, String subpackage) {
        if (listener == null) {
            return;
        }

        String listenerName = (String) getKeyByValue(mInstallResultListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            listenerName = generateListenerName();
            mInstallResultListeners.put(listenerName, listener);
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putString(EXTRA_SUBPACKAGE, subpackage);
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_ADD_INSTALL_RESULT_LISTENER, bundle, true);
    }

    public void removeInstallResultListener(
            PackageInstallResultListener listener, String pkg, String subpackage) {
        if (listener == null) {
            return;
        }
        String listenerName = (String) getKeyByValue(mInstallResultListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            Log.w(TAG, "Failed to remove PackageInstallResultListener, listener not exists");
            return;
        }
        mInstallResultListeners.remove(listenerName);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putString(EXTRA_SUBPACKAGE, subpackage);
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_REMOVE_INSTALL_RESULT_LISTENER, bundle, false);
    }

    public void addInstallProgressListener(
            PackageInstallProgressListener listener, String pkg, String subpackage) {
        if (listener == null) {
            return;
        }
        String listenerName = (String) getKeyByValue(mInstallProgressListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            listenerName = generateListenerName();
            mInstallProgressListeners.put(listenerName, listener);
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putString(EXTRA_SUBPACKAGE, subpackage);
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_ADD_INSTALL_PROGRESS_LISTENER, bundle, true);
    }

    public void removeInstallProgressListener(
            PackageInstallProgressListener listener, String pkg, String subpackage) {
        if (listener == null) {
            return;
        }
        String listenerName = (String) getKeyByValue(mInstallProgressListeners, listener);
        if (TextUtils.isEmpty(listenerName)) {
            Log.w(TAG, "Failed to remove PackageInstallProgressListener, listener not exists");
            return;
        }
        mInstallProgressListeners.remove(listenerName);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putString(EXTRA_SUBPACKAGE, subpackage);
        bundle.putString(EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionService.MSG_REMOVE_INSTALL_PROGRESS_LISTENER, bundle, false);
    }

    public void scheduleInstall(String pkg, Source source, boolean isBackground) {
        scheduleInstall(pkg, null, source, isBackground);
    }

    public void scheduleInstall(String pkg, String path, Source source) {
        scheduleInstall(pkg, path, source, false);
    }

    public void scheduleInstall(String pkg, int versoinCode, Source source, boolean isBackground) {
        scheduleInstall(pkg, versoinCode, null, source, isBackground);
    }

    public void scheduleInstall(String pkg, String path, Source source, boolean isBackground) {
        scheduleInstall(pkg, -1, path, source, isBackground);
    }

    private void scheduleInstall(
            String pkg, int versionCode, String path, Source source, boolean isBackground) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        if (source != null) {
            mInstallSourceMap.put(pkg, source);
        }
        PlatformLogManager.getDefault().logAppScheduleInstall(pkg, source);
        Bundle extra = new Bundle();
        extra.putString(EXTRA_APP, pkg);
        extra.putString(EXTRA_PATH, path);
        extra.putInt(EXTRA_VERSION_CODE, versionCode);
        extra.putBoolean(EXTRA_IS_BACKGROUND, isBackground);
        if (source != null) {
            extra.putString(EXTRA_SOURCE, source.toJson().toString());
        }
        extra.putString(EXTRA_SESSION, LogHelper.getSession(pkg));
        extra.putInt(EXTRA_MIN_APP_VERSION, SystemController.getInstance().getMinAppVersion(pkg));
        sendMessage(DistributionService.MSG_SCHEDULE_INSTALL, extra, false);
    }

    public void scheduleInstallSubpackage(String pkg, String subpackage, Source source) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        if (source != null) {
            mInstallSourceMap.put(pkg, source);
        }

        PlatformLogManager.getDefault().logAppScheduleInstall(pkg, source);
        Bundle extra = new Bundle();
        extra.putString(EXTRA_APP, pkg);
        extra.putString(EXTRA_SUBPACKAGE, subpackage);
        if (source != null) {
            extra.putString(EXTRA_SOURCE, source.toJson().toString());
        }
        extra.putString(EXTRA_SESSION, LogHelper.getSession(pkg));
        extra.putInt(EXTRA_MIN_APP_VERSION, SystemController.getInstance().getMinAppVersion(pkg));
        sendMessage(DistributionService.MSG_SCHEDULE_INSTALL, extra, false);
    }

    public void cancelInstall(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        mInstallStatusListeners.remove(pkg);
        sendMessage(DistributionService.MSG_CANCEL_INSTALL, pkg, false);
    }

    public void delayApplyUpdate(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        sendMessage(DistributionService.MSG_DELAY_APPLY_UPDATE, pkg, true);
    }

    public void applyUpdate(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        sendMessage(DistributionService.MSG_APPLY_UPDATE, pkg, false);
    }

    private Object getKeyByValue(Map map, Object value) {
        if (value != null) {
            Iterator<Map.Entry> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = iterator.next();
                if (Objects.equals(entry.getValue(), value)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void sendMessage(int what, String pkg, boolean reply) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_APP, pkg);
        sendMessage(what, extra, reply);
    }

    private void sendMessage(final int what, final Bundle obj, final boolean reply) {
        final Messenger serviceMessenger = mServiceMessenger;
        final Messenger localMessenger = mLocalMessenger;
        if (serviceMessenger == null || localMessenger == null) {
            Log.w(TAG, "mServiceMessenger is null");
            Runnable sendMsgTask =
                    new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(what, obj, reply);
                        }
                    };
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mPendingTasks.add(sendMsgTask);
                bindInstallService(mContext, mServiceConnection);
            } else {
                mHandler.post(sendMsgTask);
            }
        } else {
            final Message message = Message.obtain();
            message.what = what;
            message.obj = obj;
            if (reply) {
                message.replyTo = localMessenger;
            }
            try {
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                Log.w(TAG, "sendMessage", e);
                String pkg = obj.getString(EXTRA_APP);
                if (!TextUtils.isEmpty(pkg)) {
                    notifyResult(pkg, CODE_INSTALL_ERROR, CacheErrorCode.UNKNOWN);
                }
            }
            scheduleUnbind();
        }
    }

    private void notifyResult(String pkg, int statusCode, int errorCode) {
        notifyResult(pkg, statusCode, errorCode, null);
    }

    private void notifyResult(String pkg, int statusCode, int errorCode, String listenerName) {
        if (!TextUtils.isEmpty(listenerName)) {
            InstallStatusListener listener = mInstallStatusListeners.get(listenerName);
            if (listener != null) {
                listener.onInstallResult(pkg, statusCode, errorCode);
            }
        } else {
            Iterator<InstallStatusListener> iterator = mInstallStatusListeners.values().iterator();
            while (iterator.hasNext()) {
                InstallStatusListener listener = iterator.next();
                if (listener != null) {
                    listener.onInstallResult(pkg, statusCode, errorCode);
                }
            }
        }
    }

    private void notifyPreviewInfo(String pkg, PreviewInfo previewInfo, String listenerName) {
        if (TextUtils.isEmpty(listenerName)) {
            return;
        }
        InstallStatusListener listener = mInstallStatusListeners.get(listenerName);
        if (listener != null) {
            listener.onPreviewInfo(pkg, previewInfo);
        }
    }

    private void notifySubpackageResult(
            String pkg, String subpackage, int statusCode, int errorCode, String listenerName) {
        if (TextUtils.isEmpty(listenerName)) {
            return;
        }
        PackageInstallResultListener listener = mInstallResultListeners.get(listenerName);
        if (listener != null) {
            listener.onPackageInstallResult(pkg, subpackage, statusCode, errorCode);
        }
    }

    private void notifySubpackageProgress(
            String pkg, String subpackage, long loadSize, long totalSize, String listenerName) {
        if (TextUtils.isEmpty(listenerName)) {
            return;
        }
        PackageInstallProgressListener listener = mInstallProgressListeners.get(listenerName);
        if (listener != null) {
            listener.onPackageInstallProgress(pkg, subpackage, loadSize, totalSize);
        }
    }

    public Source getInstallSource(String pkg) {
        return pkg == null ? null : mInstallSourceMap.get(pkg);
    }

    public interface InstallStatusListener {
        void onInstallResult(String pkg, int statusCode, int errorCode);

        void onPreviewInfo(String pkg, PreviewInfo previewInfo);
    }

    public interface PackageInstallResultListener {
        void onPackageInstallResult(String pkg, String subpackage, int statusCode, int errorCode);
    }

    public interface PackageInstallProgressListener {
        void onPackageInstallProgress(String pkg, String subpackage, long loadSize, long totalSize);
    }

    private static class Holder {
        static final DistributionManager INSTANCE =
                new DistributionManager(Runtime.getInstance().getContext());
    }
}
