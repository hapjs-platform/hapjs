/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import static org.hapjs.distribution.DistributionManager.EXTRA_APP;
import static org.hapjs.distribution.DistributionManager.EXTRA_IS_BACKGROUND;
import static org.hapjs.distribution.DistributionManager.EXTRA_MIN_APP_VERSION;
import static org.hapjs.distribution.DistributionManager.EXTRA_SESSION;
import static org.hapjs.distribution.DistributionManager.EXTRA_SOURCE;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.PlatformLogManager;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.FilePackageInstaller;
import org.hapjs.cache.InstallFileFlagManager;
import org.hapjs.cache.InstallFlag;
import org.hapjs.cache.InstallFlagImpl;
import org.hapjs.cache.OneShotInstallFlag;
import org.hapjs.cache.PackageFilesValidator;
import org.hapjs.cache.PackageInstaller;
import org.hapjs.cache.PackageInstallerFactory;
import org.hapjs.cache.SrpkPackageInstallerBase;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.distribution.task.Task;
import org.hapjs.distribution.task.Task.Type;
import org.hapjs.distribution.task.TaskDispatcher;
import org.hapjs.logging.LogHelper;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.utils.SystemController;

public class DistributionService extends Service {
    protected static final int MSG_ADD_INSTALL_STATUS_LISTENER = 1;
    protected static final int MSG_SCHEDULE_INSTALL = 2;
    protected static final int MSG_CANCEL_INSTALL = 3;
    protected static final int MSG_DELAY_APPLY_UPDATE = 4;
    protected static final int MSG_APPLY_UPDATE = 5;
    protected static final int MSG_SCHEDULE_END_INSTALL = 6;
    protected static final int MSG_REMOVE_INSTALL_STATUS_LISTENER = 7;
    protected static final int MSG_ADD_INSTALL_RESULT_LISTENER = 8;
    protected static final int MSG_REMOVE_INSTALL_RESULT_LISTENER = 9;
    protected static final int MSG_ADD_INSTALL_PROGRESS_LISTENER = 10;
    protected static final int MSG_REMOVE_INSTALL_PROGRESS_LISTENER = 11;
    private static final String TAG = "DistributionService";
    private final Map<String, List<Task>> mTasks;
    private final Map<String, Messenger> mClientMessengers; // listenerName-Messenger
    private final Map<String, InstallStatus> mAppInstallStatus;
    private final Map<String, PreviewInfo> mPreviewInfos;
    private final Map<String, Map<String, Messenger>> mInstallResultListeners;
    private final Handler mHandler;
    private final Messenger mMessenger;
    private final TaskDispatcher mTaskDispatcher;
    private DistributionManager mDistributionManager;
    private DistributionProvider mDistributionProvider;

    public DistributionService() {
        mTaskDispatcher = TaskDispatcher.getInstance();
        mTasks = new ConcurrentHashMap<>();
        mClientMessengers = new ConcurrentHashMap<>();
        mAppInstallStatus = new ConcurrentHashMap<>();
        mPreviewInfos = new ConcurrentHashMap<>();
        mInstallResultListeners = new ConcurrentHashMap<>();
        mHandler =
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Bundle args = (Bundle) msg.obj;
                        final String pkg = args.getString(EXTRA_APP);

                        if (msg.what == MSG_ADD_INSTALL_STATUS_LISTENER) {
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            Log.i(TAG, "add install status listener name=" + name);
                            addListener(name, msg.replyTo);
                        } else if (msg.what == MSG_REMOVE_INSTALL_STATUS_LISTENER) {
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            Log.i(TAG, "remove install status listener name=" + name);
                            removeListener(name);
                        } else if (msg.what == MSG_SCHEDULE_INSTALL) {
                            String path = args.getString(DistributionManager.EXTRA_PATH);
                            String subpackage =
                                    args.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            int versionCode =
                                    args.getInt(DistributionManager.EXTRA_VERSION_CODE, -1);
                            boolean isBackground = args.getBoolean(EXTRA_IS_BACKGROUND);
                            String source = args.getString(EXTRA_SOURCE);
                            String session = args.getString(EXTRA_SESSION);
                            SystemController.getInstance()
                                    .setMinAppVersion(pkg, args.getInt(EXTRA_MIN_APP_VERSION));
                            LogHelper.addPackage(pkg, source, session);
                            scheduleInstall(pkg, versionCode, path, subpackage, isBackground,
                                    false);
                        } else if (msg.what == MSG_CANCEL_INSTALL) {
                            cancelInstall(pkg);
                        } else if (msg.what == MSG_DELAY_APPLY_UPDATE) {
                            delayApplyUpdate(pkg);
                        } else if (msg.what == MSG_APPLY_UPDATE) {
                            applyUpdate(pkg);
                        } else if (msg.what == MSG_SCHEDULE_END_INSTALL) {
                            onPostPackageInstall(pkg);
                        } else if (msg.what == MSG_ADD_INSTALL_RESULT_LISTENER) {
                            String subpackage =
                                    args.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            addInstallResultListener(pkg, subpackage, msg.replyTo, name);
                        } else if (msg.what == MSG_REMOVE_INSTALL_RESULT_LISTENER) {
                            String subpackage =
                                    args.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            removeInstallResultListener(pkg, subpackage, name);
                        } else if (msg.what == MSG_ADD_INSTALL_PROGRESS_LISTENER) {
                            String subpackage =
                                    args.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            InstallProgressManager.getInstance()
                                    .addInstallProgressListener(pkg, subpackage, msg.replyTo, name);
                        } else if (msg.what == MSG_REMOVE_INSTALL_PROGRESS_LISTENER) {
                            String subpackage =
                                    args.getString(DistributionManager.EXTRA_SUBPACKAGE);
                            String name = args.getString(DistributionManager.EXTRA_LISTENER_NAME);
                            InstallProgressManager.getInstance()
                                    .removeInstallProgressListener(pkg, subpackage, name);
                        }
                    }
                };
        mMessenger = new Messenger(mHandler);
        mDistributionProvider = ProviderManager.getDefault().getProvider(DistributionProvider.NAME);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service start");
        mDistributionManager = DistributionManager.getInstance();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void addListener(String listenerName, Messenger reply) {
        if (TextUtils.isEmpty(listenerName) || reply == null) {
            return;
        }
        mClientMessengers.put(listenerName, reply);
        if (!mPreviewInfos.isEmpty()) {
            Set<String> pkgs = mPreviewInfos.keySet();
            for (String pkg : pkgs) {
                PreviewInfo previewInfo = mPreviewInfos.get(pkg);
                if (previewInfo != null) {
                    notifyPreviewInfo(pkg, previewInfo, listenerName, reply);
                }
            }
        }
        if (!mAppInstallStatus.isEmpty()) {
            Set<String> pkgs = new TreeSet<>();
            pkgs.addAll(mAppInstallStatus.keySet());
            for (String pkg : pkgs) {
                notifyLoadStatus(pkg, getInstallStatus(pkg), listenerName, reply);
            }
        }
    }

    private void removeListener(String listenerName) {
        if (TextUtils.isEmpty(listenerName)) {
            return;
        }
        mClientMessengers.remove(listenerName);
    }

    private String getKey(String pkg, String subpackage) {
        if (TextUtils.isEmpty(subpackage)) {
            return pkg;
        } else {
            return pkg + "-" + subpackage;
        }
    }

    private void addInstallResultListener(
            String pkg, String subpackage, Messenger replyTo, String name) {
        String key = getKey(pkg, subpackage);
        Map<String, Messenger> listeners = mInstallResultListeners.get(key);
        if (listeners == null) {
            listeners = new ConcurrentHashMap<>();
            mInstallResultListeners.put(key, listeners);
        }
        listeners.put(name, replyTo);
    }

    private void removeInstallResultListener(String pkg, String subpackage, String name) {
        String key = getKey(pkg, subpackage);
        Map<String, Messenger> listeners = mInstallResultListeners.get(key);
        if (listeners != null) {
            listeners.remove(name);
            if (listeners.isEmpty()) {
                mInstallResultListeners.remove(key);
            }
        }
    }

    private boolean onPrePackageInstall(String pkg) {
        return InstallFileFlagManager.createFlag(this, pkg);
    }

    private void onPostPackageInstall(final String pkg) {
        // 同步retryFail 与 allFinish状态
        if (Looper.myLooper() != mHandler.getLooper()) {
            Message msg = mHandler.obtainMessage(MSG_SCHEDULE_END_INSTALL);
            Bundle args = new Bundle();
            args.putString(DistributionManager.EXTRA_APP, pkg);
            msg.obj = args;
            msg.sendToTarget();
            return;
        }
        List<Task> tasks = mTasks.get(pkg);
        if (tasks != null && !tasks.isEmpty()) {
            InstallFlag flag = tasks.get(0).getInstallFlag();
            if (flag.isAllFinished()) {
                Log.d(TAG, "end install " + pkg + ", hasSucc:" + flag.hasSuccess());
                InstallFileFlagManager.clearFlag(this, pkg);
                mTasks.remove(pkg);
                // 拿到结果才clean，hasSucc为false会删除已安装内容
                SrpkPackageInstallerBase.cleanWhenFinish(this, pkg, flag.hasSuccess());
            }
        }
    }

    private void scheduleInstall(
            String pkg,
            int versionCode,
            String path,
            String subpackage,
            boolean isBackground,
            boolean applyUpdateOnly) {
        if (mTasks.containsKey(pkg)) {
            redispatchTasks(pkg, path, subpackage, isBackground);
        } else {
            if (onPrePackageInstall(pkg)) {
                Type type = isBackground ? Type.BACKGROUND : Type.FOREGROUND;
                CacheStorage cacheStorage = CacheStorage.getInstance(DistributionService.this);
                boolean isUpdate = cacheStorage.getCache(pkg).isUpdate();
                Task task =
                        new ScheduleInstallTask(
                                pkg, type, versionCode, path, subpackage, isUpdate, isBackground,
                                applyUpdateOnly);
                List<Task> tasks = new LinkedList<>();
                tasks.add(task);
                mTasks.put(pkg, tasks);
                mTaskDispatcher.dispatchAll(tasks);
                saveAndNotifyPackageLoadStatus(pkg,
                        new InstallStatus(InstallStatus.STATUS_INSTALLING));
            } else {
                onPostPackageInstall(pkg);
            }
        }
    }

    private void getMetaInfo(ScheduleInstallTask scheduleInstallTask) {
        String pkg = scheduleInstallTask.getPackage();
        int versionCode = scheduleInstallTask.getVersion();
        String path = scheduleInstallTask.getPath();
        String subpackage = scheduleInstallTask.getSubpackage();
        boolean isBackground = scheduleInstallTask.isBackground();
        boolean applyUpdateOnly = scheduleInstallTask.isApplyUpdateOnly();
        boolean installDelayed = !scheduleInstallTask.getInstallSemaphore().requireInstall();

        DistributionProvider provider =
                ProviderManager.getDefault().getProvider(DistributionProvider.NAME);
        AppDistributionMeta distributionMeta = null;
        try {
            distributionMeta = provider.getAppDistributionMeta(pkg, versionCode);
            if (distributionMeta == null) {
                throw new CacheException(DistributionManager.CODE_INSTALL_ERROR,
                        "fetch app info failed.");
            }
            PackageFilesValidator.addAppDistributionMeta(pkg, distributionMeta);
        } catch (CacheException e) {
            Log.e(TAG, "failed to get distributionMeta", e);
            scheduleInstallTask.getInstallFlag().increaseFinishAndCheckAll(true);
            saveAndNotifyPackageLoadStatus(
                    pkg,
                    new InstallStatus(
                            InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_ERROR,
                            e.getErrorCode(),
                            e));
            return;
        }

        Integer minAppVersion = SystemController.getInstance().getMinAppVersion(pkg);
        if (minAppVersion != null && distributionMeta.getVersion() < minAppVersion) {
            scheduleInstallTask.getInstallFlag().increaseFinishAndCheckAll(true);
            saveAndNotifyPackageLoadStatus(
                    pkg,
                    new InstallStatus(
                            InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_ERROR,
                            CacheErrorCode.PACKAGE_INCOMPATIBLE));
            SystemController.getInstance().removeMinAppVersion(pkg);
            return;
        }

        CacheStorage cacheStorage = CacheStorage.getInstance(DistributionService.this);
        boolean isUpdate = cacheStorage.getCache(pkg).isUpdate();
        scheduleBuildTask(
                scheduleInstallTask,
                distributionMeta,
                path,
                subpackage,
                isUpdate,
                isBackground,
                applyUpdateOnly,
                installDelayed);
        // 为简便起见, 上面buildTask执行后, 本ScheduleInstallTask已从mTasks中移除
        try {
            PreviewInfo previewInfo = mDistributionProvider.getPreviewInfo(pkg);
            notifyPreviewInfo(pkg, previewInfo);
        } catch (CacheException e) {
            Log.e(TAG, "failed to getPreviewInfo", e);
        }
    }

    private void scheduleBuildTask(
            ScheduleInstallTask scheduleInstallTask,
            AppDistributionMeta distributionMeta,
            String path,
            String subpackage,
            boolean isUpdate,
            boolean isBackground,
            boolean applyUpdateOnly,
            boolean installDelayed) {
        if (distributionMeta.getSubpackageInfos() == null
                || distributionMeta.getSubpackageInfos().isEmpty()) {
            scheduleBuildFullPackageTask(
                    distributionMeta, isUpdate, isBackground, applyUpdateOnly, installDelayed);
        } else {
            scheduleBuildSubpackageTask(
                    scheduleInstallTask,
                    path,
                    subpackage,
                    distributionMeta,
                    isUpdate,
                    isBackground,
                    applyUpdateOnly,
                    installDelayed);
        }
    }

    private void scheduleBuildFullPackageTask(
            AppDistributionMeta distributionMeta,
            boolean isUpdate,
            boolean isBackground,
            boolean applyUpdateOnly,
            boolean installDelayed) {
        Type type = isBackground ? Type.BACKGROUND : Type.FOREGROUND;
        PackageTask task =
                new PackageTask(distributionMeta, type, isUpdate, applyUpdateOnly, installDelayed);
        List<Task> tasks = new LinkedList<>();
        tasks.add(task);
        mTasks.put(distributionMeta.getPackage(), (List) tasks);
        mTaskDispatcher.dispatch(task);
    }

    private void scheduleBuildSubpackageTask(
            ScheduleInstallTask scheduleInstallTask,
            String path,
            String subpackage,
            AppDistributionMeta distributionMeta,
            boolean isUpdate,
            boolean isBackground,
            boolean applyUpdateOnly,
            boolean installDelayed) {
        String pkg = distributionMeta.getPackage();
        List<SubpackageInfo> needUpdateSubpackages = distributionMeta.getNeedUpdateSubpackages();
        int needUpdateCount = needUpdateSubpackages == null ? 0 : needUpdateSubpackages.size();
        Log.d(TAG, "needUpdateCount=" + needUpdateCount);

        if (needUpdateCount == 0) {
            Log.w(TAG, "nothing to install");
            // 此时需要结束scheduleTask以正确清除InstallFlag
            scheduleInstallTask.getInstallFlag().increaseFinishAndCheckAll(true);
            saveAndNotifyPackageLoadStatus(
                    pkg, new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_CANCEL));
            return;
        }

        List<SubpackageInfo> subpackageInfos = distributionMeta.getSubpackageInfos();
        SubpackageInfo targetSubpackageInfo = null;
        if (!TextUtils.isEmpty(subpackage)) {
            targetSubpackageInfo =
                    SubpackageInfo.getTargetSubpackageBySubpackageName(subpackageInfos, subpackage);
        } else {
            targetSubpackageInfo =
                    SubpackageInfo.getTargetSubpackageByPageOrPath(subpackageInfos, path);
        }
        if (targetSubpackageInfo == null) {
            Log.w(TAG, "target subpackage not found for path: " + path + ", subpackage: "
                    + subpackage);
        }

        SubpackageTask main = null;
        LinkedList<SubpackageTask> subTasks = new LinkedList<>();
        InstallSemaphore installSemaphore = new InstallSemaphore(installDelayed);
        InstallFlag installFlag = new InstallFlagImpl(subpackageInfos.size(), needUpdateCount);
        for (SubpackageInfo info : needUpdateSubpackages) {
            SubpackageTask subTask;
            if (targetSubpackageInfo != null
                    && TextUtils.equals(info.getName(), targetSubpackageInfo.getName())) {
                Type type = isBackground ? Type.BACKGROUND : Type.FOREGROUND;
                subTask =
                        new SubpackageTask(
                                distributionMeta,
                                type,
                                info,
                                isUpdate,
                                applyUpdateOnly,
                                new OneShotInstallFlag(installFlag),
                                installSemaphore);
            } else {
                Type type = isBackground ? Type.BACKGROUND : Type.FOREGROUND_PRELOAD;
                subTask =
                        new SubpackageTask(
                                distributionMeta,
                                type,
                                info,
                                isUpdate,
                                applyUpdateOnly,
                                new OneShotInstallFlag(installFlag),
                                installSemaphore);
                if (info.isBase()) {
                    main = subTask;
                }
            }
            subTasks.add(subTask);
        }

        if (main != null
                && (targetSubpackageInfo == null || !targetSubpackageInfo.isStandalone())) {
            main.setType(Type.FOREGROUND);
        }

        mTasks.put(pkg, (List) subTasks);
        mTaskDispatcher.dispatchAll((List) subTasks);
    }

    private void redispatchTasks(String pkg, String path, String subpackage, boolean isBackground) {
        Log.d(
                TAG,
                "redispatchTasks pkg="
                        + pkg
                        + ", path="
                        + path
                        + ", subpackage="
                        + subpackage
                        + ", isBackground="
                        + isBackground);
        if (!isBackground
                && mTasks.containsKey(pkg)
                && (!TextUtils.isEmpty(path) || !TextUtils.isEmpty(subpackage))) {
            List<Task> list = mTasks.get(pkg);
            if (list != null && list.size() > 0) {
                if (!(list.get(0) instanceof SubpackageTask)) {
                    list.get(0).setType(Type.FOREGROUND);
                } else {
                    SubpackageTask target = null;
                    SubpackageTask main = null;
                    List<Task> retryList = new ArrayList<>();
                    for (Iterator<Task> i = list.iterator(); i.hasNext(); ) {
                        Task task = i.next();
                        SubpackageTask origin = (SubpackageTask) task;
                        SubpackageInfo info = origin.getSubpackageInfo();

                        if (!TextUtils.isEmpty(path) && info.contain(path)
                                || !TextUtils.isEmpty(subpackage)
                                && subpackage.equals(info.getName())) {
                            origin.setType(Type.FOREGROUND);
                            target = origin;
                        } else if (info.isBase()) {
                            main = origin;
                        } else {
                            origin.setType(Type.FOREGROUND_PRELOAD);
                        }
                        // retry failed task
                        if (origin.isFailed()) {
                            i.remove();
                            SubpackageTask retryTask = origin.copy();
                            retryTask.resetInstallFlag();
                            retryList.add(retryTask);
                            Log.d(TAG,
                                    "retry task " + pkg + retryTask.getSubpackageInfo().getName());
                        }
                    }
                    if (main != null) {
                        // 目标未匹配，默认资源在主包
                        if (target == null) {
                            Log.d(
                                    TAG,
                                    "path: "
                                            + path
                                            + " can not be found in any subpackage,"
                                            + " or supackage: "
                                            + subpackage
                                            + " not exists.");
                            main.setType(Type.FOREGROUND);
                        } else {
                            main.setType(
                                    target.getSubpackageInfo().isStandalone()
                                            ? Type.FOREGROUND_PRELOAD
                                            : Type.FOREGROUND);
                        }
                    }
                    list.addAll(retryList);
                }
                mTaskDispatcher.dispatchAll(list);
                Log.d(TAG, "redispatch pkg: " + pkg + ", path: " + path + ", subpackage: "
                        + subpackage);
            }
        }
    }

    private void cancelInstall(String pkg) {
        // 取消正在安装的任务，重新安装会有并发问题，只用于存在新包名时取消旧包安装的情况
        List<Task> tasks = mTasks.get(pkg);
        if (tasks != null) {
            for (Task t : tasks) {
                t.cancel(true);
            }
            mAppInstallStatus.put(
                    pkg, new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_CANCEL));
        }
        onPostPackageInstall(pkg);
    }

    private void delayApplyUpdate(String pkg) {
        List<Task> tasks = mTasks.get(pkg);
        if (tasks != null && !tasks.isEmpty()) {
            Task task = tasks.get(0);
            if (task.getInstallSemaphore().requireDelay()) {
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_UPDATE_DELAYED));
                Log.d(TAG, "delayApplyUpdate success");
            } else {
                Log.d(TAG, "delayApplyUpdate failed");
            }
        } else {
            InstallStatus delayUpdateStatus =
                    new InstallStatus(
                            InstallStatus.STATUS_INSTALL_UPDATE_DELAYED,
                            -1,
                            DistributionManager.getInstance().getAppStatus(pkg));
            notifyLoadStatus(pkg, delayUpdateStatus);
            Log.d(TAG, "task not found. delayApplyUpdate failed");
        }
    }

    private void applyUpdate(String pkg) {
        List<Task> tasks = mTasks.get(pkg);
        if (tasks == null) {
            scheduleInstall(pkg, -1, null, null, false, true);
        }
    }

    private InstallStatus getInstallStatus(String pkg) {
        // 优先返回缓存的结果
        InstallStatus status = mAppInstallStatus.get(pkg);
        if (status != null && status.validate()) {
            return status;
        } else {
            mAppInstallStatus.remove(pkg);
        }

        if (mDistributionManager.isAppReady(pkg)) {
            return new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH, InstallStatus.RESULT_OK);
        }
        return new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH, InstallStatus.RESULT_UNKNOWN);
    }

    private void installOrUpdate(Task task, String subpackageName) {
        String pkg = task.getPackage();
        if (task.isPackageReady()) {
            Log.d(
                    TAG,
                    "skip installing for package is ready. pkg=" + pkg + ", subpackage="
                            + subpackageName);
            task.getInstallFlag().increaseFinishAndCheckAll(true);
            task.saveAndNotifyLoadStatus(
                    new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_CANCEL));
            return;
        }

        InputStream stream = null;
        try {
            File localArchive = Cache.getArchiveFile(this, pkg, subpackageName);
            if (LocalArchiveManager.isLocalArchiveVersionMatches(
                    this, task.getVersion(), pkg, subpackageName)) {
                PackageInstaller installer = task.createInstaller(localArchive);
                install(task, installer, subpackageName);
            } else if (!task.isApplyUpdateOnly()) {
                // try to install by stream
                PackageInstaller installer = null;
                stream = fetchStream(task.getDistributionMeta(), subpackageName);
                if (stream != null) {
                    installer = task.createInstaller(stream);
                } else {
                    // fallback to install by file
                    File archive = fetchFile(task.getDistributionMeta(), subpackageName);
                    installer = task.createInstaller(archive);
                }
                install(task, installer, subpackageName);
            } else {
                Log.d(TAG, "update only but local archive not found. skip.");
                task.getInstallFlag().increaseFinishAndCheckAll(false);
                task.setFailed(true);
                InstallStatus status =
                        new InstallStatus(
                                InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_ERROR,
                                CacheErrorCode.ARCHIVE_FILE_NOT_FOUND);
                task.saveAndNotifyLoadStatus(status);
            }
        } catch (CacheException e) {
            task.getInstallFlag().increaseFinishAndCheckAll(false);
            task.setFailed(true);
            InstallStatus status =
                    new InstallStatus(
                            InstallStatus.STATUS_INSTALL_FINISH, InstallStatus.RESULT_ERROR,
                            e.getErrorCode(), e);
            task.saveAndNotifyLoadStatus(status);
            Log.w(TAG, "Fail to install package: " + pkg, e);
        } catch (IOException e) {
            task.getInstallFlag().increaseFinishAndCheckAll(false);
            task.setFailed(true);
            InstallStatus status =
                    new InstallStatus(
                            InstallStatus.STATUS_INSTALL_FINISH,
                            InstallStatus.RESULT_ERROR,
                            CacheErrorCode.ARCHIVE_FILE_NOT_FOUND,
                            e);
            task.saveAndNotifyLoadStatus(status);
            Log.w(TAG, "File not found. skip.", e);
        } finally {
            FileUtils.closeQuietly(stream);
            if (!task.getInstallSemaphore().isDelayed()) {
                LocalArchiveManager.removeLocalArchive(this, pkg, subpackageName);
            }
        }
    }

    private InputStream fetchStream(AppDistributionMeta distributionMeta, String subpackageName)
            throws CacheException {
        return mDistributionProvider.fetch(distributionMeta, subpackageName);
    }

    private File fetchFile(AppDistributionMeta distributionMeta, String subpackageName)
            throws CacheException {
        File packageFile =
                Cache.getArchiveFile(this, distributionMeta.getPackage(), subpackageName);
        int result =
                mDistributionProvider.fetch(
                        distributionMeta, subpackageName, packageFile.getAbsolutePath());
        if (result != CacheErrorCode.OK) {
            packageFile.delete();
            throw new CacheException(result, "Fail to install package");
        }
        return packageFile;
    }

    private void install(Task task, PackageInstaller installer, String subpackageName)
            throws CacheException {
        final String pkg = task.getPackage();
        if (installer instanceof SrpkPackageInstallerBase) {
            SrpkPackageInstallerBase streamPackageInstaller = (SrpkPackageInstallerBase) installer;
            if (task.getInstallSemaphore().requireInstall()) {
                Log.d(TAG, "streamPackageInstaller begin stream install");
                streamPackageInstaller.prepare();
                int errorCode = -1;
                if (task instanceof SubpackageTask
                        && InstalledSubpackageManager.getInstance()
                        .checkIsNewVersion(this, pkg, task.getVersion())) {
                    errorCode = CacheErrorCode.PACKAGE_CACHE_OBSOLETE;
                }
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_STREAM, -1, errorCode));
                CacheStorage.getInstance(this).install(pkg, streamPackageInstaller);
                task.getInstallFlag().increaseFinishAndCheckAll(true);
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_OK));
            } else {
                Log.d(TAG, "streamPackageInstaller install delayed");
                task.getInstallFlag().increaseFinishAndCheckAll(false);
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_CANCEL));
                streamPackageInstaller.cancel();
                LocalArchiveManager.saveLocalArchiveVersionCode(
                        this, task.getVersion(), task.getPackage(), subpackageName);
            }
        } else if (installer instanceof FilePackageInstaller) {
            if (task.getInstallSemaphore().requireInstall()) {
                Log.d(TAG, "FilePackageInstaller begin file install");
                CacheStorage.getInstance(this).install(pkg, installer);
                task.getInstallFlag().increaseFinishAndCheckAll(true);
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_OK));
            } else {
                Log.d(TAG, "FilePackageInstaller install delayed");
                task.getInstallFlag().increaseFinishAndCheckAll(false);
                task.saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_CANCEL));
                LocalArchiveManager.saveLocalArchiveVersionCode(
                        this, task.getVersion(), task.getPackage(), subpackageName);
            }
        } else {
            throw new CacheException(DistributionManager.CODE_INSTALL_ERROR,
                    "unavailable installer");
        }
    }

    private void saveAndNotifyPackageLoadStatus(String pkg, InstallStatus status) {
        PlatformLogManager.getDefault().logAppInstallResult(pkg, status);
        mAppInstallStatus.put(pkg, status);
        if (status.isPackageTaskFinished()) {
            onPostPackageInstall(pkg);
            InstallProgressManager.getInstance().onInstallFinish(pkg, null);
            notifyLoadResult(pkg, status);
        }
        notifyLoadStatus(pkg, status);
    }

    private void saveAndNotifySubpackageLoadStatus(
            final String pkg, SubpackageTask task, InstallStatus status) {
        if (status.isPackageTaskFinished()) {
            String subpackage = task.getSubpackageInfo().getName();
            InstallProgressManager.getInstance().onInstallFinish(pkg, subpackage);
            notifyLoadResult(pkg, subpackage, status);
        }

        InstallStatus oldStatus = mAppInstallStatus.get(pkg);
        mAppInstallStatus.put(
                pkg, InstallStatus.getCurrentStatus(oldStatus, status, task.isAllTaskFinish()));
        Log.d(
                TAG,
                "saveAndNotifySubpackageLoadStatus: pkg="
                        + pkg
                        + ", subpackageName="
                        + task.getSubpackageInfo().getName()
                        + ", status: "
                        + status
                        + ", oldStatus="
                        + oldStatus);

        // report when the first CODE_INSTALL_STREAM or CODE_APPLY_UPDATE_DELAYED
        if (!isStreamingOrDelayed(oldStatus) && isStreamingOrDelayed(status)) {
            notifyLoadStatus(pkg, mAppInstallStatus.get(pkg));
        } else if (task.isAllTaskFinish()) {
            Log.d(TAG, "all task finished:" + pkg);
            onPostPackageInstall(pkg);
            notifyLoadStatus(pkg, mAppInstallStatus.get(pkg));
            notifyLoadResult(pkg, null, status);
        }
    }

    private boolean isStreamingOrDelayed(InstallStatus status) {
        return status != null && status.isStreamingOrDelayed();
    }

    private void notifyLoadStatus(final String pkg, final InstallStatus status) {
        Log.d(TAG, "notifyLoadStatus: pkg=" + pkg + ", status: " + status);
        if (mClientMessengers.isEmpty()) {
            return;
        }
        Set<String> names = mClientMessengers.keySet();
        for (String name : names) {
            Messenger messenger = mClientMessengers.get(name);
            notifyLoadStatus(pkg, status, name, messenger);
        }
    }

    private void notifyLoadStatus(
            String pkg, InstallStatus status, String listenerName, Messenger messenger) {
        if (messenger == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putInt(DistributionManager.EXTRA_STATUS_CODE, status.getExternalStatusCode());
        bundle.putInt(DistributionManager.EXTRA_ERROR_CODE, status.getErrorCode());
        bundle.putString(DistributionManager.EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionManager.MESSAGE_INSTALL_STATUS, bundle, messenger);
    }

    private void notifyPreviewInfo(final String pkg, PreviewInfo previewInfo) {
        Log.d(TAG, "notifyPreviewInfo: pkg=" + pkg + ", previewInfo=" + previewInfo);
        // Can't add null value
        if (previewInfo != null) {
            mPreviewInfos.put(pkg, previewInfo);
        }
        if (mClientMessengers.isEmpty()) {
            return;
        }
        Set<String> names = mClientMessengers.keySet();
        for (String name : names) {
            Messenger messenger = mClientMessengers.get(name);
            notifyPreviewInfo(pkg, previewInfo, name, messenger);
        }
    }

    private void notifyPreviewInfo(
            final String pkg, PreviewInfo previewInfo, String listenerName, Messenger messenger) {
        if (messenger == null) {
            return;
        }
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_APP, pkg);
        bundle.putParcelable(DistributionManager.EXTRA_PREVIEW_INFO, previewInfo);
        bundle.putString(DistributionManager.EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionManager.MESSAGE_PREVIEW_INFO, bundle, messenger);
    }

    private void notifyLoadResult(String pkg, String subpackage, InstallStatus status) {
        Log.d(
                TAG, "notifyLoadResult: pkg=" + pkg + ", subpackage=" + subpackage + ", status: "
                        + status);
        String key = getKey(pkg, subpackage);
        Map<String, Messenger> listeners = mInstallResultListeners.get(key);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Messenger> entry : listeners.entrySet()) {
            notifyLoadResult(pkg, subpackage, status, entry.getKey(), entry.getValue());
        }
    }

    private void notifyLoadResult(String pkg, InstallStatus status) {
        Log.d(TAG, "notifyLoadResult: pkg=" + pkg + ", status: " + status);
        // notify listeners of subpackages
        for (String key : mInstallResultListeners.keySet()) {
            String keyPrefix = pkg + "-";
            if (key.startsWith(keyPrefix)) {
                Map<String, Messenger> listeners = mInstallResultListeners.get(key);
                if (listeners != null && !listeners.isEmpty()) {
                    String subpackage = key.substring(keyPrefix.length());
                    for (Map.Entry<String, Messenger> entry : listeners.entrySet()) {
                        notifyLoadResult(pkg, subpackage, status, entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        notifyLoadResult(pkg, null, status);
    }

    private void notifyLoadResult(
            String pkg,
            String subpackage,
            InstallStatus status,
            String listenerName,
            Messenger messenger) {
        if (messenger == null) {
            return;
        }
        final Bundle bundle = new Bundle();
        bundle.putString(DistributionManager.EXTRA_APP, pkg);
        bundle.putString(DistributionManager.EXTRA_SUBPACKAGE, subpackage);
        bundle.putInt(DistributionManager.EXTRA_STATUS_CODE, status.getExternalStatusCode());
        bundle.putInt(DistributionManager.EXTRA_ERROR_CODE, status.getErrorCode());
        bundle.putString(DistributionManager.EXTRA_LISTENER_NAME, listenerName);
        sendMessage(DistributionManager.MESSAGE_PACKAGE_INSTALL_RESULT, bundle, messenger);
    }

    private void sendMessage(int what, Bundle data, final Messenger messenger) {
        if (messenger == null || data == null) {
            return;
        }
        final Message message = Message.obtain();
        message.what = what;
        message.setData(data);
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            messenger.send(message);
                        } catch (RemoteException e) {
                            Log.e(TAG, "sendMessage", e);
                        }
                    }
                });
    }

    private void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    public static class InstallStatus {
        static final int STATUS_INSTALLING = 0;
        static final int STATUS_INSTALL_STREAM = 1;
        static final int STATUS_INSTALL_UPDATE_DELAYED = 2;
        static final int STATUS_INSTALL_FINISH = 3;

        static final int RESULT_OK = 0;
        static final int RESULT_UNKNOWN = 1;
        static final int RESULT_CANCEL = 2;
        static final int RESULT_ERROR = 3;

        private static final long KEEP_CACHE_STATUS_MILLISECONDS = 120 * 1000;

        int statusCode;
        int resultCode;
        int errorCode;
        long time;
        Throwable throwable;

        private InstallStatus(int statusCode) {
            this(statusCode, -1);
        }

        private InstallStatus(int statusCode, int resultCode) {
            this(statusCode, resultCode, -1);
        }

        private InstallStatus(int statusCode, int resultCode, int errorCode) {
            this(statusCode, resultCode, errorCode, null);
        }

        private InstallStatus(int statusCode, int resultCode, int errorCode, Throwable throwable) {
            this.statusCode = statusCode;
            this.resultCode = resultCode;
            this.errorCode = errorCode;
            this.time = SystemClock.elapsedRealtime();
            this.throwable = throwable;
        }

        private static InstallStatus getCurrentStatus(
                InstallStatus oldStatus, InstallStatus newStatus, boolean isAllFinish) {
            if (oldStatus == null) {
                return newStatus;
            }
            if (newStatus == null) {
                return oldStatus;
            }

            int statusCode;
            if (isAllFinish) {
                statusCode = STATUS_INSTALL_FINISH;
            } else if (oldStatus.statusCode == STATUS_INSTALL_FINISH
                    || newStatus.statusCode == STATUS_INSTALL_FINISH) {
                statusCode = oldStatus.statusCode + newStatus.statusCode - STATUS_INSTALL_FINISH;
            } else {
                statusCode = Math.max(oldStatus.statusCode, newStatus.statusCode);
            }

            int resultCode = Math.max(newStatus.resultCode, oldStatus.resultCode);
            // 同样的结果，取最新的errorCode
            int errorCode =
                    newStatus.resultCode >= oldStatus.resultCode
                            ? newStatus.getErrorCode()
                            : oldStatus.getErrorCode();

            return new InstallStatus(statusCode, resultCode, errorCode);
        }

        private boolean validate() {
            return SystemClock.elapsedRealtime() - time <= KEEP_CACHE_STATUS_MILLISECONDS;
        }

        private boolean isStreamingOrDelayed() {
            return statusCode == STATUS_INSTALL_STREAM
                    || statusCode == STATUS_INSTALL_UPDATE_DELAYED;
        }

        private boolean isPackageTaskFinished() {
            return statusCode != STATUS_INSTALLING
                    && statusCode != STATUS_INSTALL_UPDATE_DELAYED
                    && statusCode != STATUS_INSTALL_STREAM;
        }

        public int getExternalStatusCode() {
            switch (statusCode) {
                case STATUS_INSTALLING:
                    return DistributionManager.CODE_INSTALLING;
                case STATUS_INSTALL_STREAM:
                    return DistributionManager.CODE_INSTALL_STREAM;
                case STATUS_INSTALL_UPDATE_DELAYED:
                    return DistributionManager.CODE_APPLY_UPDATE_DELAYED;
                case STATUS_INSTALL_FINISH:
                    switch (resultCode) {
                        case RESULT_OK:
                            return DistributionManager.CODE_INSTALL_OK;
                        case RESULT_UNKNOWN:
                            return DistributionManager.CODE_INSTALL_UNKNOWN;
                        case RESULT_CANCEL:
                            return DistributionManager.CODE_INSTALL_CANCEL;
                        case RESULT_ERROR:
                            return DistributionManager.CODE_INSTALL_ERROR;
                        default:
                            throw new IllegalArgumentException("unknown resultCode: " + resultCode);
                    }
                default:
                    throw new IllegalArgumentException("unknown statusCode: " + statusCode);
            }
        }

        public int getErrorCode() {
            return this.errorCode;
        }

        public Throwable getInstallThrowable() {
            return this.throwable;
        }

        @Override
        public String toString() {
            return "InstallStatus(statusCode="
                    + statusCode
                    + ", resultCode="
                    + resultCode
                    + ", errorCode="
                    + errorCode
                    + ", time="
                    + time
                    + ")";
        }
    }

    private class ScheduleInstallTask extends Task {
        private final String mPath;
        private final String mSubpackage;
        private final boolean mIsBackground;

        private ScheduleInstallTask(
                String pkg,
                Type type,
                int versionCode,
                String path,
                String subpackage,
                boolean isUpdate,
                boolean isBackground,
                boolean applyUpdateOnly) {
            super(
                    new AppDistributionMeta(pkg, versionCode),
                    type,
                    isUpdate,
                    applyUpdateOnly,
                    new OneShotInstallFlag(new InstallFlagImpl(1)),
                    new InstallSemaphore());
            mPath = path;
            mSubpackage = subpackage;
            mIsBackground = isBackground;
        }

        public String getPath() {
            return mPath;
        }

        public String getSubpackage() {
            return mSubpackage;
        }

        public boolean isBackground() {
            return mIsBackground;
        }

        @Override
        public void saveAndNotifyLoadStatus(InstallStatus status) {
            saveAndNotifyPackageLoadStatus(getPackage(), status);
        }

        @Override
        public void run() {
            getMetaInfo(this);
        }

        @Override
        public String toString() {
            return super.toString() + ", mPath: " + mPath;
        }
    }

    public class PackageTask extends Task {
        private PackageTask(
                AppDistributionMeta distributionMeta,
                Type type,
                boolean isUpdate,
                boolean applyUpdateOnly,
                boolean installDelayed) {
            super(
                    distributionMeta,
                    type,
                    isUpdate,
                    applyUpdateOnly,
                    new OneShotInstallFlag(new InstallFlagImpl(1)),
                    new InstallSemaphore(installDelayed));
        }

        @Override
        public void run() {
            installOrUpdate(this, null);
        }

        public void cancel() {
            if (cancel(true)) {
                saveAndNotifyLoadStatus(
                        new InstallStatus(InstallStatus.STATUS_INSTALL_FINISH,
                                InstallStatus.RESULT_CANCEL));
            }
        }

        @Override
        public void saveAndNotifyLoadStatus(InstallStatus status) {
            saveAndNotifyPackageLoadStatus(getPackage(), status);
        }

        @Override
        public boolean isPackageReady() {
            return mDistributionManager.isAppReady(getPackage());
        }

        @Override
        public PackageInstaller createInstaller(InputStream stream) throws CacheException {
            return PackageInstallerFactory.createInstaller(
                    DistributionService.this,
                    getPackage(),
                    getVersion(),
                    getDistributionMeta().getSize(),
                    null,
                    stream,
                    isUpdate(),
                    getInstallFlag());
        }

        @Override
        public PackageInstaller createInstaller(File archiveFile) {
            return new FilePackageInstaller(
                    DistributionService.this, getPackage(), archiveFile, isUpdate());
        }
    }

    public class SubpackageTask extends Task {
        private final SubpackageInfo mSubpackageInfo;

        public SubpackageTask(
                AppDistributionMeta distributionMeta,
                Type type,
                SubpackageInfo subpackageInfo,
                boolean isUpdate,
                boolean applyUpdateOnly,
                OneShotInstallFlag installFlag,
                InstallSemaphore installSemaphore) {
            super(distributionMeta, type, isUpdate, applyUpdateOnly, installFlag, installSemaphore);
            this.mSubpackageInfo = subpackageInfo;
        }

        @Override
        public void run() {
            installOrUpdate(this, mSubpackageInfo.getName());
        }

        public boolean isAllTaskFinish() {
            return getInstallFlag().isAllFinished();
        }

        @Override
        public void saveAndNotifyLoadStatus(InstallStatus status) {
            saveAndNotifySubpackageLoadStatus(getPackage(), this, status);
        }

        @Override
        public boolean isPackageReady() {
            return !mDistributionProvider
                    .needSubpackageUpdate(getPackage(), mSubpackageInfo.getName());
        }

        @Override
        public PackageInstaller createInstaller(InputStream stream) throws CacheException {
            return PackageInstallerFactory.createInstaller(
                    DistributionService.this,
                    getPackage(),
                    getVersion(),
                    mSubpackageInfo.getSize(),
                    mSubpackageInfo,
                    stream,
                    isUpdate(),
                    getInstallFlag());
        }

        @Override
        public PackageInstaller createInstaller(File archiveFile)
                throws IOException, CacheException {
            return PackageInstallerFactory.createInstaller(
                    DistributionService.this,
                    getPackage(),
                    getVersion(),
                    mSubpackageInfo.getSize(),
                    mSubpackageInfo,
                    new FileInputStream(archiveFile),
                    isUpdate(),
                    getInstallFlag());
        }

        public SubpackageInfo getSubpackageInfo() {
            return mSubpackageInfo;
        }

        @Override
        public String toString() {
            return super.toString() + ", subpackage name: " + mSubpackageInfo.getName();
        }

        public SubpackageTask copy() {
            return new SubpackageTask(
                    getDistributionMeta(),
                    getType(),
                    mSubpackageInfo,
                    isUpdate(),
                    isApplyUpdateOnly(),
                    getInstallFlag(),
                    getInstallSemaphore());
        }
    }
}
