/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import static org.hapjs.debug.DebugService.ResultCode.CODE_OK;
import static org.hapjs.debug.DebugService.ResultCode.CODE_UNKNOWN_MESSAGE;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.runtime.RuntimeActivity;

public class DebugService extends Service {
    public static final int MSG_INSTALL_PACKAGE = 1;
    public static final int MSG_LAUNCH_PACKAGE = 2;
    public static final int MSG_DEBUG_PACKAGE = 3;
    public static final int MSG_UNINSTALL_PACKAGE = 4;
    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_FILE = "file";
    public static final String EXTRA_SERVER = "server";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_SHOULD_RELOAD = "shouldReload";
    public static final String EXTRA_USE_ADB = "useADB";
    public static final String EXTRA_WAIT_DEVTOOLS = "waitDevTools";
    public static final String EXTRA_SERIAL_NUMBER = "serialNumber";
    public static final String EXTRA_PLATFORM_VERSION_CODE = "platformVersionCode";
    public static final String EXTRA_WEB_DEBUG_ENABLED = "webDebugEnabled";
    public static final String EXTRA_USE_ANALYZER = "useAnalyzer";
    public static final String EXTRA_DEBUG_TARGET = "debugTarget";
    public static final String EXTRA_SENTRY_TRACE_ID = "sentryTraceId";
    private static final String TAG = "DebugService";
    private HandlerThread mThread;
    private Handler mHandler;
    private Messenger mMessenger;

    public DebugService() {
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new HandlerImpl(this, mThread.getLooper());
        mMessenger = new Messenger(mHandler);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThread.quitSafely();
    }

    protected void onInstallPackage(Message msg) {
        Bundle params = msg.getData();
        String pkg = params.getString(EXTRA_PACKAGE);
        Uri uri = params.getParcelable(EXTRA_FILE);
        int code = installPackage(pkg, uri);
        Bundle data = new Bundle();
        data.putString(EXTRA_PACKAGE, pkg);
        data.putBoolean(EXTRA_RESULT, code == CODE_OK);
        data.putInt(EXTRA_ERROR_CODE, code);
        Message replyMsg = Message.obtain(msg);
        replyMsg.what = MSG_INSTALL_PACKAGE;
        replyMsg.setData(data);
        DebuggerLogUtil.logBreadcrumb("onInstallPackage, code=" + code);
        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to send reply message", e);
            DebuggerLogUtil.logException(e);
        }
    }

    protected int installPackage(String pkg, Uri uri) {
        return DebugRpkInstaller.installPackage(this, pkg, uri);
    }

    private void onLaunchPackage(Message msg) {
        Bundle params = msg.getData();
        String pkg = params.getString(EXTRA_PACKAGE);
        String path = params.getString(EXTRA_PATH);
        boolean shouldReload = params.getBoolean(EXTRA_SHOULD_RELOAD);
        boolean webDebugEnabled = params.getBoolean(EXTRA_WEB_DEBUG_ENABLED);
        boolean useAnalyzer = params.getBoolean(EXTRA_USE_ANALYZER);
        boolean result = launchPackage(pkg, path, shouldReload, webDebugEnabled, useAnalyzer);
        Bundle data = new Bundle();
        data.putString(EXTRA_PACKAGE, pkg);
        data.putString(EXTRA_PATH, path);
        data.putBoolean(EXTRA_RESULT, result);
        Message replyMsg = Message.obtain(msg);
        replyMsg.what = MSG_LAUNCH_PACKAGE;
        replyMsg.setData(data);
        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to send reply message", e);
        }
    }

    private boolean launchPackage(
            String pkg, String path, boolean shouldReload, boolean webDebugEnabled, boolean useAnalyzer) {
        if (TextUtils.isEmpty(pkg)) {
            Log.e(TAG, "Invalid package: " + pkg);
            return false;
        }

        path = DebugUtils.appendAnalyzerParam(pkg, path, useAnalyzer);
        Intent intent = new Intent(IntentUtils.getLaunchAction(this));
        intent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
        intent.putExtra(RuntimeActivity.EXTRA_PATH, path);
        intent.putExtra(RuntimeActivity.EXTRA_MODE, RuntimeActivity.MODE_CLEAR_TASK);
        intent.putExtra(RuntimeActivity.EXTRA_SHOULD_RELOAD, shouldReload);
        intent.putExtra(RuntimeActivity.EXTRA_WEB_DEBUG_ENABLED, webDebugEnabled);
        intent.putExtra(RuntimeActivity.EXTRA_FROM_DEBUGGER, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        return true;
    }

    private void onDebugPackage(Message msg) {
        Bundle params = msg.getData();
        String pkg = params.getString(EXTRA_PACKAGE);
        String path = params.getString(EXTRA_PATH);
        String server = params.getString(EXTRA_SERVER);
        String debugTarget = params.getString(EXTRA_DEBUG_TARGET);
        boolean waitDevTools = params.getBoolean(EXTRA_WAIT_DEVTOOLS);
        boolean useADB = params.getBoolean(EXTRA_USE_ADB);
        String serialNumber = params.getString(EXTRA_SERIAL_NUMBER);
        int platformVersionCode = params.getInt(EXTRA_PLATFORM_VERSION_CODE);
        boolean enableWebDebug = params.getBoolean(EXTRA_WEB_DEBUG_ENABLED);
        boolean useAnalyzer = params.getBoolean(EXTRA_USE_ANALYZER);
        String traceId = params.getString(EXTRA_SENTRY_TRACE_ID, "");
        boolean result =
                debugPackage(
                        pkg,
                        path,
                        server,
                        useADB,
                        serialNumber,
                        platformVersionCode,
                        waitDevTools,
                        enableWebDebug,
                        debugTarget,
                        traceId,
                        useAnalyzer);
        Bundle data = new Bundle();
        data.putString(EXTRA_PACKAGE, pkg);
        data.putString(EXTRA_PATH, path);
        data.putBoolean(EXTRA_RESULT, result);
        Message replyMsg = Message.obtain(msg);
        replyMsg.what = MSG_DEBUG_PACKAGE;
        replyMsg.setData(data);
        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to send reply message", e);
        }
    }

    private boolean debugPackage(
            String pkg,
            String path,
            String server,
            boolean useADB,
            String serialNumber,
            int platformVersionCode,
            boolean waitDevTools,
            boolean webDebugEnabled,
            String debugTarget,
            String traceId,
            boolean useAnalyzer) {
        if (TextUtils.isEmpty(pkg)) {
            Log.e(TAG, "Invalid package: " + pkg);
            return false;
        }
        Intent intent = new Intent(IntentUtils.getLaunchAction(this));
        intent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
        intent.putExtra(RuntimeActivity.EXTRA_MODE, RuntimeActivity.MODE_CLEAR_TASK);
        intent.putExtra(RuntimeActivity.EXTRA_WEB_DEBUG_ENABLED, webDebugEnabled);
        if (!TextUtils.isEmpty(server)) {
            intent.putExtra(RuntimeActivity.EXTRA_ENABLE_DEBUG, true);
            if (TextUtils.isEmpty(path)) {
                path = "/";
            }
            path =
                    DebugUtils.appendDebugParams(
                            path,
                            server,
                            pkg,
                            serialNumber,
                            useADB,
                            platformVersionCode,
                            waitDevTools,
                            debugTarget,
                            traceId);
        }
        path = DebugUtils.appendAnalyzerParam(pkg, path, useAnalyzer);
        if (!TextUtils.isEmpty(path)) {
            intent.putExtra(RuntimeActivity.EXTRA_PATH, path);
        }
        intent.putExtra(RuntimeActivity.EXTRA_FROM_DEBUGGER, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchToDebug(intent);
        return true;
    }

    void launchToDebug(Intent intent) {
        startActivity(intent);
    }

    private void onUninstallPackage(Message msg) {
        Bundle params = msg.getData();
        String pkg = params.getString(EXTRA_PACKAGE);
        uninstallPackage(pkg);
        Bundle data = new Bundle();
        data.putString(EXTRA_PACKAGE, pkg);
        data.putBoolean(EXTRA_RESULT, true);
        Message replyMsg = Message.obtain(msg);
        replyMsg.what = MSG_UNINSTALL_PACKAGE;
        replyMsg.setData(data);
        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to send reply message", e);
        }
    }

    protected void uninstallPackage(String pkg) {
        CacheStorage.getInstance(this).uninstall(pkg);
    }

    private void onReceiveUnknownMessage(Message msg) {
        Log.e(TAG, "Invalid message: " + msg.what);

        Bundle data = new Bundle();
        data.putBoolean(EXTRA_RESULT, false);
        data.putInt(EXTRA_ERROR_CODE, CODE_UNKNOWN_MESSAGE);
        Bundle params = msg.getData();
        if (params != null) {
            String pkg = params.getString(EXTRA_PACKAGE);
            if (pkg != null) {
                data.putString(EXTRA_PACKAGE, pkg);
            }
        }

        Message replyMsg = Message.obtain(msg);
        replyMsg.what = msg.what;
        replyMsg.setData(data);
        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to send reply message", e);
        }
    }

    public interface ResultCode {
        int CODE_OK = 0;
        int CODE_GENERIC_ERROR = 1;
        int CODE_UNKNOWN_MESSAGE = 2;
        int CODE_CERTIFICATION_MISMATCH = 100;
        int CODE_INSTALL_ERROR_BASE = 2000;
        int CODE_INSTALL_ERROR_DEFAULT = CODE_INSTALL_ERROR_BASE;
        int CODE_INSTALL_ERROR_INVALID_PACKAGE = CODE_INSTALL_ERROR_BASE + 1;
        int CODE_INSTALL_ERROR_INVALID_URI = CODE_INSTALL_ERROR_BASE + 2;
        int CODE_INSTALL_ERROR_FAIL_TO_CREATE_TEMP_FILE = CODE_INSTALL_ERROR_BASE + 3;
        int CODE_INSTALL_ERROR_FAIL_TO_DELETE_CACHE_DIR = CODE_INSTALL_ERROR_BASE + 4;
        int CODE_INSTALL_ERROR_FAIL_TO_CREATE_CACHE_DIR = CODE_INSTALL_ERROR_BASE + 5;
        int CODE_INSTALL_ERROR_FAIL_TO_UNZIP_SPLIT_RPK = CODE_INSTALL_ERROR_BASE + 6;
        int CODE_INSTALL_ERROR_FAIL_TO_MOVE_CACHE_FILE = CODE_INSTALL_ERROR_BASE + 7;
        int CODE_INSTALL_ERROR_FAIL_TO_SAVE_MANIFESTS = CODE_INSTALL_ERROR_BASE + 8;
        int CODE_INSTALL_ERROR_FAIL_TO_CHECK_SPLIT_FILE = CODE_INSTALL_ERROR_BASE + 9;
        int CODE_INSTALL_ERROR_ILLEGAL_INSTALLER = CODE_INSTALL_ERROR_BASE + 10;
        int CODE_INSTALL_ERROR_INSTALL_FILE_NOT_FOUND = CODE_INSTALL_ERROR_BASE + 11;
    }

    private class HandlerImpl extends DebugHandler {

        public HandlerImpl(Context context, Looper looper) {
            super(context, looper);
        }

        @Override
        public void onHandleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INSTALL_PACKAGE:
                    onInstallPackage(msg);
                    break;
                case MSG_LAUNCH_PACKAGE:
                    onLaunchPackage(msg);
                    break;
                case MSG_DEBUG_PACKAGE:
                    onDebugPackage(msg);
                    break;
                case MSG_UNINSTALL_PACKAGE:
                    onUninstallPackage(msg);
                    break;
                default:
                    onReceiveUnknownMessage(msg);
                    break;
            }
        }
    }
}
