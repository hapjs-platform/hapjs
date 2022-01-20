/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.shortcut;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.logging.Source;
import org.hapjs.runtime.Runtime;

public class ShortcutInstaller {
    private static final String TAG = "ShortcutInstaller";
    private static final int MSG_SCHEDULE = 1;
    private static final int MSG_INSTALL = 2;
    private static final int MSG_TIMEOUT = 3;
    private static final int MSG_SUCCESS = 4;
    private static final int INSTALL_TIME_SPAN = 50;
    private static final int TIMEOUT_TIME_SPAN = 1000;
    private static final int MAX_RETRY_COUNT = 3;
    private Context mContext;
    private Handler mHandler;
    private Map<String, InstallRequest> mPendingRequests;
    private Map<String, InstallRequest> mTimingRequests;

    private ShortcutInstaller() {
        mContext = Runtime.getInstance().getContext();
        mHandler = new HandlerImpl();
        mPendingRequests = new LinkedHashMap<>();
        mTimingRequests = new HashMap<>();
    }

    public static ShortcutInstaller getInstance() {
        return Holder.sInstance;
    }

    public ResultLatch scheduleInstall(
            String pkg, String path, String params, String name, Bitmap icon, Source source) {
        InstallRequest request = new InstallRequest();
        request.pkg = pkg;
        request.path = path;
        request.params = params;
        request.name = name;
        request.icon = icon;
        request.source = source;
        request.latch = new ResultLatch();
        scheduleInstall(request);
        return request.latch;
    }

    private void scheduleInstall(InstallRequest request) {
        mHandler.obtainMessage(MSG_SCHEDULE, request).sendToTarget();
    }

    void onInstallSuccess(String pkg) {
        mHandler.obtainMessage(MSG_SUCCESS, pkg).sendToTarget();
    }

    private static class Holder {
        static final ShortcutInstaller sInstance = new ShortcutInstaller();
    }

    private static class InstallRequest {
        String requestId;
        String pkg;
        String path;
        String params;
        String name;
        Bitmap icon;
        Source source;
        ResultLatch latch;
        int retryCount;

        InstallRequest() {
            requestId = String.valueOf((long) (Math.random() * Long.MAX_VALUE));
        }
    }

    public static class ResultLatch {
        private CountDownLatch mLatch = new CountDownLatch(1);
        private boolean result;

        private ResultLatch() {
        }

        public boolean waitForResult() {
            try {
                mLatch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while waiting for result", e);
            }
            return result;
        }

        private void notifyResult(boolean result) {
            this.result = result;
            mLatch.countDown();
        }
    }

    private class HandlerImpl extends Handler {
        HandlerImpl() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCHEDULE: {
                    InstallRequest request = (InstallRequest) msg.obj;
                    if (!mPendingRequests.containsKey(request.requestId)
                            && !mTimingRequests.containsKey(request.requestId)) {
                        mPendingRequests.put(request.requestId, request);

                        // schedule install message
                        if (!hasMessages(MSG_INSTALL)) {
                            sendEmptyMessage(MSG_INSTALL);
                        }
                    } else {
                        Log.w(TAG, "Ignore repeat install schedule for " + request.requestId);
                    }
                    break;
                }
                case MSG_INSTALL: {
                    String requestId = mPendingRequests.keySet().iterator().next();
                    InstallRequest request = mPendingRequests.remove(requestId);
                    if (request == null) {
                        Log.e(
                                TAG,
                                "handleMessage: installRequest is null of which requestId is "
                                        + requestId);
                        return;
                    }
                    ShortcutService.install(
                            mContext,
                            requestId,
                            request.pkg,
                            request.path,
                            request.params,
                            request.name,
                            request.icon,
                            request.source);
                    mTimingRequests.put(requestId, request);

                    Message timeoutMessage = Message.obtain(this, MSG_TIMEOUT, request);
                    sendMessageDelayed(timeoutMessage, TIMEOUT_TIME_SPAN);

                    if (!mPendingRequests.isEmpty()) {
                        sendEmptyMessageDelayed(MSG_INSTALL, INSTALL_TIME_SPAN);
                    }
                    break;
                }
                case MSG_TIMEOUT: {
                    InstallRequest request = (InstallRequest) msg.obj;
                    mTimingRequests.remove(request.requestId);
                    if (!ShortcutManager
                            .hasShortcutInstalled(mContext, request.pkg, request.path)) {
                        int alreadyTryCount = ++request.retryCount;
                        if (alreadyTryCount < MAX_RETRY_COUNT) {
                            scheduleInstall(request);
                            Log.w(
                                    TAG,
                                    "Install for " + request.pkg + " timeout," + " already try "
                                            + alreadyTryCount);
                        } else {
                            Log.w(TAG, "Fail to install for " + request.pkg);
                            request.latch.notifyResult(false);
                        }
                    } else {
                        Log.v(TAG, "Install success, ignore timeout msg for " + request.pkg);
                        request.latch.notifyResult(true);
                    }
                    break;
                }
                case MSG_SUCCESS: {
                    String requestId = (String) msg.obj;
                    InstallRequest request = mTimingRequests.remove(requestId);
                    if (request != null) {
                        mHandler.removeMessages(MSG_TIMEOUT, request);
                        request.latch.notifyResult(true);
                    } else {
                        // 取消掉已经在重试的request
                        request = mPendingRequests.remove(requestId);
                        if (request != null) {
                            Log.w(
                                    TAG,
                                    "Cancel retry request for "
                                            + request.pkg
                                            + ", "
                                            + "already try "
                                            + request.retryCount);
                            request.latch.notifyResult(true);
                        }
                    }

                    if (request != null) {
                        // 系统Launcher已经将所有接收到的请求处理完成，并且有request在pending队列等待
                        // 则将INSTALL消息由延时消息改为立即执行的消息
                        if (mTimingRequests.size() == 0 && mPendingRequests.size() != 0) {
                            mHandler.removeMessages(MSG_INSTALL);
                            mHandler.sendEmptyMessage(MSG_INSTALL);
                            Log.v(TAG, "Cancel delay for left requests.");
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}
