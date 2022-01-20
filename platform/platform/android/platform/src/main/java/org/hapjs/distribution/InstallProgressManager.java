/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class InstallProgressManager {
    private static final String TAG = "InstallProgressManager";

    // 每个分包在100ms内最多通知一次进度
    private static final int PROGRESS_INTERVAL = 100;

    private static final int MSG_INSTALL_PROGRESS = 0;

    private Map<String, Long> mProgressSendedTime = new HashMap<>();
    private Map<String, MessageData> mPendingProgressMessage = new HashMap<>();

    private Map<String, Map<String, Listener>> mInstallProgressListeners = new HashMap<>();

    private Handler mMainHandler;

    private InstallProgressManager() {
        HandlerThread thread = new HandlerThread("install-progress");
        thread.start();
        mMainHandler = new ProgressHandler(thread.getLooper());
    }

    ;

    public static InstallProgressManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static String getKey(String pkg, String subpackage) {
        if (TextUtils.isEmpty(subpackage)) {
            return pkg;
        } else {
            return pkg + "-" + subpackage;
        }
    }

    public synchronized void addInstallProgressListener(
            String pkg, String subpackage, Messenger replyTo, String name) {
        String key = getKey(pkg, subpackage);
        Map<String, Listener> listeners = mInstallProgressListeners.get(key);
        if (listeners == null) {
            listeners = new HashMap<>();
            mInstallProgressListeners.put(key, listeners);
        }
        Listener l = new Listener(pkg, subpackage, replyTo);
        listeners.put(name, l);
    }

    public synchronized void removeInstallProgressListener(
            String pkg, String subpackage, String name) {
        String key = getKey(pkg, subpackage);
        Map<String, Listener> listeners = mInstallProgressListeners.get(key);
        if (listeners != null) {
            listeners.remove(name);
            if (listeners.isEmpty()) {
                mInstallProgressListeners.remove(key);
                mProgressSendedTime.remove(key);
                MessageData messageData = mPendingProgressMessage.remove(key);
                if (messageData != null) {
                    mMainHandler.removeMessages(MSG_INSTALL_PROGRESS, messageData);
                }
            }
        }
    }

    public synchronized void onInstallFinish(String pkg, String subpackage) {
        Log.d(TAG, "onInstallFinish pkg=" + pkg + ", subpackage=" + subpackage);
        String key = getKey(pkg, subpackage);
        MessageData progressData = mPendingProgressMessage.remove(key);
        if (progressData != null) {
            progressData.sendAtTime = SystemClock.uptimeMillis();
            mMainHandler.removeMessages(MSG_INSTALL_PROGRESS, progressData);
            mPendingProgressMessage.put(key, progressData);
            Message.obtain(mMainHandler, MSG_INSTALL_PROGRESS, progressData).sendToTarget();
        }
    }

    public synchronized void postOnInstallProgress(
            String pkg, String subpackage, long loadedSize, long totalSize) {
        String key = getKey(pkg, subpackage);
        Map<String, Listener> listeners = mInstallProgressListeners.get(key);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        MessageData progressData = mPendingProgressMessage.remove(key);
        if (progressData != null) {
            mMainHandler.removeMessages(MSG_INSTALL_PROGRESS, progressData);
            progressData.loadedSize = loadedSize;
            progressData.totalSize = totalSize;
        } else {
            Long lastTime = mProgressSendedTime.get(key);
            long nextTime =
                    lastTime == null
                            ? SystemClock.uptimeMillis()
                            : Math.max(SystemClock.uptimeMillis(), lastTime + PROGRESS_INTERVAL);
            progressData = new MessageData(pkg, subpackage, loadedSize, totalSize, nextTime);
        }
        mPendingProgressMessage.put(key, progressData);
        Message msg = Message.obtain(mMainHandler, MSG_INSTALL_PROGRESS, progressData);
        mMainHandler.sendMessageAtTime(msg, progressData.sendAtTime);
    }

    private void onInstallProgress(String pkg, String subpackage, long loadedSize, long totalSize) {
        Log.d(
                TAG,
                "onInstallProgress pkg="
                        + pkg
                        + ", subpackage="
                        + subpackage
                        + ", loadedSize="
                        + loadedSize
                        + ", totalSize="
                        + totalSize);
        Map<String, Listener> listeners = mInstallProgressListeners.get(getKey(pkg, subpackage));
        if (listeners != null) {
            for (Map.Entry<String, Listener> entry : listeners.entrySet()) {
                final Bundle bundle = new Bundle();
                bundle.putString(DistributionManager.EXTRA_APP, pkg);
                bundle.putString(DistributionManager.EXTRA_SUBPACKAGE, subpackage);
                bundle.putLong(DistributionManager.EXTRA_LOAD_SIZE, loadedSize);
                bundle.putLong(DistributionManager.EXTRA_TOTAL_SIZE, totalSize);
                bundle.putString(DistributionManager.EXTRA_LISTENER_NAME, entry.getKey());
                sendMessage(
                        DistributionManager.MESSAGE_PACKAGE_INSTALL_PROGRESS, bundle,
                        entry.getValue().replyTo);
            }
        }
    }

    private void sendMessage(int what, Bundle data, final Messenger messenger) {
        if (messenger == null || data == null) {
            return;
        }
        final Message message = Message.obtain();
        message.what = what;
        message.setData(data);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage", e);
        }
    }

    private static class MessageData {
        final String pkg;
        final String subpackage;
        long loadedSize;
        long totalSize;
        long sendAtTime;

        MessageData(String pkg, String subpackage, long loadedSize, long totalSize,
                    long sendAtTime) {
            this.pkg = pkg;
            this.subpackage = subpackage;
            this.loadedSize = loadedSize;
            this.totalSize = totalSize;
            this.sendAtTime = sendAtTime;
        }
    }

    private static class Listener {
        final String pkg;
        final String subpackage;
        final Messenger replyTo;

        Listener(String pkg, String subpackage, Messenger replyTo) {
            this.pkg = pkg;
            this.subpackage = subpackage;
            this.replyTo = replyTo;
        }
    }

    private static class InstanceHolder {
        private static final InstallProgressManager sInstance = new InstallProgressManager();
    }

    private class ProgressHandler extends Handler {
        public ProgressHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INSTALL_PROGRESS:
                    synchronized (InstallProgressManager.this) {
                        MessageData messageData = (MessageData) msg.obj;
                        String key = getKey(messageData.pkg, messageData.subpackage);
                        MessageData progressData = mPendingProgressMessage.remove(key);
                        if (progressData != null) {
                            onInstallProgress(
                                    progressData.pkg,
                                    progressData.subpackage,
                                    progressData.loadedSize,
                                    progressData.totalSize);
                            mProgressSendedTime.put(key, SystemClock.uptimeMillis());
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
