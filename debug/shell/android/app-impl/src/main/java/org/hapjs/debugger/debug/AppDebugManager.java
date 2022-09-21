/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.debug;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
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
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.debugger.DebuggerApplication;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.pm.PackageInfo;
import org.hapjs.debugger.pm.PackageManager;
import org.hapjs.debugger.utils.AppUtils;
import org.hapjs.debugger.utils.FileUtils;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

public class AppDebugManager {
    public static final int ERROR_CODE_BASE = 1000;
    public static final int ERROR_CODE_PLATFORM_NOT_CHOOSE = ERROR_CODE_BASE + 1;
    public static final int ERROR_CODE_INSTALL_GENERIC_ERROR = ERROR_CODE_BASE + 2;
    public static final int ERROR_CODE_GET_RPK_FILE_FAILED = ERROR_CODE_BASE + 3;
    public static final int ERROR_CODE_PACKAGE_INCOMPATIBLE = ERROR_CODE_BASE + 4;
    public static final int ERROR_CODE_FAIL_TO_SAVE_LOCAL_FILE = ERROR_CODE_BASE + 5;
    public static final int ERROR_CODE_FAIL_TO_CREATE_TEMP_FILE = ERROR_CODE_BASE + 6;
    public static final int ERROR_CODE_GET_PACKAGE_INFO_FAIL = ERROR_CODE_BASE + 7;
    public static final int ERROR_CODE_GET_DEBUG_SERVICE_FAIL = ERROR_CODE_BASE + 8;
    public static final int ERROR_CODE_PICK_OTHER_FILE_EXCEPT_RPK = ERROR_CODE_BASE + 9;
    public static final String RPKS_FILE_SUFFIX = ".rpks";
    public static final String RPK_FILE_SUFFIX = ".rpk";
    public static final int MSG_SERVICE_INSTALL_PACKAGE = 1;
    public static final int MSG_SERVICE_LAUNCH_PACKAGE = 2;
    public static final int MSG_SERVICE_DEBUG_PACKAGE = 3;
    public static final int MSG_SERVICE_UNINSTALL_PACKAGE = 4;
    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_FILE = "file";
    public static final String EXTRA_SERVER = "server";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_SHOULD_RELOAD = "shouldReload";
    public static final String EXTRA_USE_ADB = "useADB";
    public static final String EXTRA_USE_ANALYZER = "useAnalyzer";
    public static final String EXTRA_SERIAL_NUMBER = "serialNumber";
    public static final String EXTRA_PLATFORM_VERSION_CODE = "platformVersionCode";
    public static final String EXTRA_WAIT_DEVTOOLS = "waitDevTools";
    public static final String EXTRA_WEB_DEBUG_ENABLED = "webDebugEnabled";
    public static final String EXTRA_DEBUG_TARGET = "debugTarget";
    public static final String EXTRA_SENTRY_TRANCE_ID = "sentryTraceId";
    public static final int SERVICE_CODE_OK = 0;
    public static final int SERVICE_CODE_GENERIC_ERROR = 1;
    public static final int SERVICE_CODE_UNKNOWN_MESSAGE = 2;
    public static final int SERVICE_CODE_CERTIFICATION_MISMATCH = 100;
    private static final String TAG = "AppDebugManager";
    private static final String ACTION_BIND_DEBUG_SERVICE = "org.hapjs.intent.action.BIND_DEBUG_SERVICE";
    private static final String FILE_PROVIDER_AUTHORITY = "org.hapjs.debugger.file";
    private static final int MSG_BIND_SERVICE = 1000;
    private static final int MSG_CLOSE_SERVICE = 1001;
    private static final int MSG_SERVICE_CONNECTED = 1002;
    private static final int MSG_SERVICE_DISCONNECTED = 1003;
    private static final int MSG_UPDATE_ONLINE = 1004;
    private static final int MSG_INSTALL_LOCALLY = 1005;
    private static final int MSG_START_DEBUGGING = 1006;
    private static final int MSG_UNINSTALL_PACKAGE = 1007;
    private static final int MSG_LAUNCH_PACKAGE = 1008;
    private static final int MSG_SEARCH_SN = 1009;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final Object sLocker = new Object();
    private static volatile AppDebugManager sInstance;
    private static int sCurrentPlatformVersion = -1;
    private Context mContext;
    private HandlerThread mThread;
    private Handler mHandler;
    private Messenger mServer;
    private Messenger mClient;
    private int mState;
    private DebugListener mListener;
    private File mRetryRpkFile;
    private int mUsedPlatformVersionCode;
    private Map<String, File> mAppCache;
    private ProgressDialog mProgressDialog;
    private WeakReference<Activity> mActivityRf;
    private String traceId;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mHandler.obtainMessage(MSG_SERVICE_CONNECTED, service).sendToTarget();
            DebuggerLogUtil.logBreadcrumb("DebugService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHandler.obtainMessage(MSG_SERVICE_DISCONNECTED).sendToTarget();
            DebuggerLogUtil.logBreadcrumb("DebugService disconnected");
        }
    };

    private AppDebugManager(Context context) {
        if (context instanceof Activity) {
            mActivityRf = new WeakReference<>((Activity) context);
        }
        mContext = context.getApplicationContext();
        mAppCache = new HashMap<>();
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new HandlerImpl(mThread.getLooper());
        mClient = new Messenger(mHandler);
    }

    public static int getCurrentPlatformVersion() {
        return sCurrentPlatformVersion;
    }

    public static void setCurrentPlatformVersion(int platformVersion) {
        sCurrentPlatformVersion = platformVersion;
    }

    public static AppDebugManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sLocker) {
                AppDebugManager instance = new AppDebugManager(context);
                sInstance = instance;
            }
        }
        return sInstance;
    }

    public void setDebugListener(DebugListener listener) {
        mListener = listener;
    }

    public void setUsedPlatformVersionCode(int platformVersionCode) {
        mUsedPlatformVersionCode = platformVersionCode;
    }

    public PackageInfo getPackageInfo(String pkg) {
        File rpkFile = mAppCache.get(pkg);
        if (rpkFile != null) {
            return PackageManager.getPackageInfo(rpkFile.getPath());
        } else {
            return null;
        }
    }

    public void updateOnline(String url) {
        mHandler.obtainMessage(MSG_UPDATE_ONLINE, url).sendToTarget();
    }

    public void installLocally(Uri uri) {
        mHandler.obtainMessage(MSG_INSTALL_LOCALLY, uri).sendToTarget();
    }

    public void startDebugging(String pkg, String server, String target) {
        mHandler.obtainMessage(MSG_START_DEBUGGING, new String[] {pkg, server, target}).sendToTarget();
    }

    public void uninstall(String pkg) {
        mHandler.obtainMessage(MSG_UNINSTALL_PACKAGE, pkg).sendToTarget();
    }

    public void launchPackage(String pkg) {
        mHandler.obtainMessage(MSG_LAUNCH_PACKAGE, pkg).sendToTarget();
    }

    public void searchSerialNumber() {
        mHandler.obtainMessage(MSG_SEARCH_SN).sendToTarget();
    }

    public void unbindDebugService() {
        mHandler.obtainMessage(MSG_CLOSE_SERVICE).sendToTarget();
    }

    public void close() {
        removeProgressDialog();
        mHandler.sendEmptyMessage(MSG_CLOSE_SERVICE);
        mThread.quitSafely();
        sInstance = null;
    }

    private Messenger getServer() {
        switch (mState) {
            case STATE_DISCONNECTED:
                DebuggerLogUtil.logBreadcrumb("try to connect DebugService");
                mHandler.sendEmptyMessage(MSG_BIND_SERVICE);
                return null;
            case STATE_CONNECTING:
                return null;
            case STATE_CONNECTED:
                return mServer;
            default:
                return null;
        }
    }

    private void onUpdateOnline(final String url) {
        if (mActivityRf != null) {
            final Activity activity = mActivityRf.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgressDialog(activity);
                        Log.i(TAG, "mProgressDialog.show");
                    }
                });
                File rpkFile = downloadFile(url);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        removeProgressDialog();
                    }
                });
                if (rpkFile == null) {
                    onError(ERROR_CODE_GET_RPK_FILE_FAILED);
                    return;
                }
                mRetryRpkFile = rpkFile;
                installPackageInPlatform(rpkFile);
            }
        }
    }

    private void showProgressDialog(Context context) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage(context.getResources().getString(R.string.dlg_hint_downloading));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    public void removeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
            Log.i(TAG, "mProgressDialog.dismiss()");
        }
    }

    private void onInstallLocally(Uri uri) {
        if (isOtherFileExceptRpk(uri)) {
            onError(ERROR_CODE_PICK_OTHER_FILE_EXCEPT_RPK);
            return;
        }
        File tempFile = createTempFile();
        if (tempFile != null) {
            InputStream in = null;
            try {
                in = mContext.getContentResolver().openInputStream(uri);
                boolean success = FileUtils.saveToFile(in, tempFile);
                if (success) {
                    mRetryRpkFile = tempFile;
                    installPackageInPlatform(tempFile);
                    return;
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Fail to save local file", e);
                onError(ERROR_CODE_FAIL_TO_SAVE_LOCAL_FILE);
            } finally {
                FileUtils.closeQuietly(in);
            }
            tempFile.delete();
        } else {
            onError(ERROR_CODE_FAIL_TO_CREATE_TEMP_FILE);
        }
    }

    private boolean isOtherFileExceptRpk(Uri uri) {
        if (mActivityRf != null) {
            final Activity activity = mActivityRf.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    String filePath = FileUtils.getFilePathByUri(activity, uri);
                    if (filePath != null && !(filePath.endsWith(RPK_FILE_SUFFIX)
                            || filePath.endsWith(RPKS_FILE_SUFFIX))) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "isRpkFile error", e);
                }
            }
        }
        return false;
    }

    private boolean onStartDebugging(String pkg, String server, String target) {
        return sendDebugPackageMessage(pkg, server, target);
    }

    private void onUninstallPackage(String pkg) {
        sendUninstallPackageMessage(pkg);
    }

    private void onError(int code) {
        if (mListener != null) {
            mListener.onError(code);
        }
    }

    private void onInstallResult(String pkg, boolean result, int code) {
        if (result) {
            PreferenceUtils.setDebugRpkPath(mContext, mRetryRpkFile.getAbsolutePath());
        }
        if (mListener != null) {
            mListener.onInstallResult(pkg, result, code);
        }
    }

    private void onLaunchResult(String pkg, boolean result) {
        if (mListener != null) {
            mListener.onLaunchResult(result);
        }
    }

    private void onDebugResult(String pkg, boolean result) {
        if (mListener != null) {
            mListener.onDebugResult(result);
        }
    }

    private void onUninstallResult(String pkg, boolean result) {
        if (mListener != null) {
            mListener.onUninstallResult(result);
        }
        if (mRetryRpkFile != null) {
            installPackageInPlatform(mRetryRpkFile);
        }
    }

    private void installPackageInPlatform(File rpkFile) {
        PackageInfo pi = PackageManager.getPackageInfo(rpkFile.getPath());
        if (pi == null) {
            onError(ERROR_CODE_GET_PACKAGE_INFO_FAIL);
            return;
        }
        mAppCache.put(pi.getPackage(), rpkFile);
        if (pi.getMinPlatformVersion() > sCurrentPlatformVersion) {
            onInstallResult(pi.getPackage(), false, ERROR_CODE_PACKAGE_INCOMPATIBLE);
            return;
        }

        String pkg = pi.getPackage();
        Uri pkgUri = FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, rpkFile);
        sendInstallPackageMessage(pkg, pkgUri);
    }

    private void sendInstallPackageMessage(String pkg, Uri file) {
        String platformPackage = PreferenceUtils.getPlatformPackage(mContext);
        if (TextUtils.isEmpty(platformPackage)) {
            onError(ERROR_CODE_PLATFORM_NOT_CHOOSE);
            return;
        }

        Messenger server = getServer();
        if (server != null) {
            Message msg = Message.obtain();
            msg.what = MSG_SERVICE_INSTALL_PACKAGE;
            msg.replyTo = mClient;
            Bundle data = new Bundle();
            data.putString(EXTRA_PACKAGE, pkg);
            data.putParcelable(EXTRA_FILE, file);
            msg.setData(data);

            mContext.grantUriPermission(
                    platformPackage, file, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                server.send(msg);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Fail to update online", e);
            }
        }
        onError(ERROR_CODE_GET_DEBUG_SERVICE_FAIL);
    }

    private boolean sendLaunchPackageMessage(String pkg, boolean shouldReload) {
        Messenger server = getServer();
        if (server != null) {
            Message msg = Message.obtain();
            msg.what = MSG_SERVICE_LAUNCH_PACKAGE;
            msg.replyTo = mClient;

            Bundle data = new Bundle();
            data.putString(EXTRA_PACKAGE, pkg);
            data.putString(EXTRA_PATH, getLaunchPath());
            data.putBoolean(EXTRA_SHOULD_RELOAD, shouldReload);
            data.putBoolean(EXTRA_WEB_DEBUG_ENABLED, PreferenceUtils.isWebDebugEnabled(mContext));
            data.putBoolean(EXTRA_USE_ANALYZER, PreferenceUtils.isUseAnalyzer(mContext));
            msg.setData(data);

            try {
                server.send(msg);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Fail to update online", e);
            }
        }
        return false;
    }

    private boolean sendDebugPackageMessage(String pkg, String debugServer, String target) {
        Messenger server = getServer();
        if (server != null) {
            Message msg = Message.obtain();
            msg.what = MSG_SERVICE_DEBUG_PACKAGE;
            msg.replyTo = mClient;

            boolean isUseADB = PreferenceUtils.isUseADB(mContext);
            boolean isUseAnalyzer = PreferenceUtils.isUseAnalyzer(mContext);

            Bundle data = new Bundle();
            data.putString(EXTRA_PACKAGE, pkg);
            data.putString(EXTRA_PATH, getLaunchPath());
            data.putString(EXTRA_SERVER, debugServer);
            data.putBoolean(EXTRA_USE_ADB, isUseADB);
            data.putBoolean(EXTRA_USE_ANALYZER, isUseAnalyzer);
            data.putString(EXTRA_SERIAL_NUMBER, isUseADB ? AppUtils.getSerialNumber() : "");
            data.putInt(EXTRA_PLATFORM_VERSION_CODE, mUsedPlatformVersionCode);
            data.putBoolean(EXTRA_WAIT_DEVTOOLS, PreferenceUtils.isWaitDevTools(mContext));
            data.putBoolean(EXTRA_WEB_DEBUG_ENABLED, PreferenceUtils.isWebDebugEnabled(mContext));
            data.putString(EXTRA_DEBUG_TARGET, target);
            data.putString(EXTRA_SENTRY_TRANCE_ID, getTraceId());
            msg.setData(data);

            try {
                server.send(msg);
                DebuggerLogUtil.logMessage("DEBUGGER_SEND_DEBUG_MSG");
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Fail to update online", e);
                DebuggerLogUtil.logMessage("DEBUGGER_SEND_DEBUG_MSG_ERROR");
                DebuggerLogUtil.logException(e);
            }
        } else {
            DebuggerLogUtil.logMessage("DEBUGGER_SEND_MSG_FAILURE");
        }
        return false;
    }

    private String getLaunchPath() {
        String params = PreferenceUtils.getLaunchParams(mContext);
        if (!TextUtils.isEmpty(params)) {
            return params.startsWith("/") ? params : ("/?" + params);
        }
        return null;
    }

    private boolean sendUninstallPackageMessage(String pkg) {
        Messenger server = getServer();
        if (server != null) {
            Message msg = Message.obtain();
            msg.what = MSG_SERVICE_UNINSTALL_PACKAGE;
            msg.replyTo = mClient;

            Bundle data = new Bundle();
            data.putString(EXTRA_PACKAGE, pkg);
            msg.setData(data);

            try {
                server.send(msg);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Fail to uninstall package", e);
            }
        }
        return false;
    }

    private void bindService(List<Message> pendingMessages) {
        String platformPackage = PreferenceUtils.getPlatformPackage(mContext);
        if (TextUtils.isEmpty(platformPackage) && mListener != null) {
            mListener.onError(ERROR_CODE_PLATFORM_NOT_CHOOSE);
            return;
        }

        Intent intent = new Intent(ACTION_BIND_DEBUG_SERVICE);
        intent.setPackage(platformPackage);
        try {
            boolean success = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (success) {
                mState = STATE_CONNECTING;
                DebuggerLogUtil.logMessage("DEBUGGER_BIND_ENGINE_SUCCESS");
            } else {
                //绑定DebugService失败，需清除之前添加的message对象，不然重复执行
                if (pendingMessages != null && !pendingMessages.isEmpty()) {
                    pendingMessages.clear();
                }
                showFailHint();
                Log.e(TAG, "Fail to bind debug service");
                DebuggerLogUtil.logMessage("DEBUGGER_BIND_ENGINE_FAILURE");
            }
        } catch (SecurityException e) {
            if (pendingMessages != null && !pendingMessages.isEmpty()) {
                pendingMessages.clear();
            }
            Log.e(TAG, "Fail to bind debug service", e);
            DebuggerLogUtil.logMessage("DEBUGGER_BIND_ENGINE_ERROR");
            DebuggerLogUtil.logException(e);
        }
    }

    private void showFailHint() {
        Toast.makeText(mContext, R.string.hint_fail_to_bind_service, Toast.LENGTH_LONG).show();
    }

    private void unbindService() {
        if (mState == STATE_CONNECTED) {
            mContext.unbindService(mConnection);
            mServer = null;
            mState = STATE_DISCONNECTED;
        }
        DebuggerLogUtil.logBreadcrumb("unbind DebugService");
    }

    private File downloadFile(String url) {
        File tempFile = createTempFile();
        if (tempFile != null) {
            boolean success = HttpUtils.downloadFile(url, tempFile);
            if (success) {
                return tempFile;
            }
            tempFile.delete();
        }
        return null;
    }

    private File createTempFile() {
        try {
            return File.createTempFile("debug", ".rpk", mContext.getCacheDir());
        } catch (IOException e) {
            Log.e(TAG, "Fail to create temp file", e);
            return null;
        }
    }

    private String getTraceId() {
        return DebuggerLogUtil.getTraceId();
    }

    public List<String> getPlatformPackages() {
        List<String> platformPackages = new ArrayList<String>();
        Intent intent = new Intent();
        intent.setAction("org.hapjs.action.SCAN");
        android.content.pm.PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(intent, 0);
        if (ris != null && ris.size() > 0) {
            for (ResolveInfo ri : ris) {
                // Check if debug service exists
                String pkg = ri.activityInfo.packageName;
                Intent serviceIntent = new Intent(ACTION_BIND_DEBUG_SERVICE);
                serviceIntent.setPackage(pkg);
                if (pm.resolveService(serviceIntent, 0) != null) {
                    platformPackages.add(pkg);
                }
            }
        }
        return platformPackages;
    }

    public interface DebugListener {
        void onInstallResult(String pkg, boolean result, int code);

        void onLaunchResult(boolean result);

        void onDebugResult(boolean result);

        void onUninstallResult(boolean result);

        void onError(int code);
    }

    private class HandlerImpl extends Handler {
        private List<Message> pendingMessages;

        public HandlerImpl(Looper looper) {
            super(looper);
            pendingMessages = new ArrayList<>();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BIND_SERVICE:
                    bindService(pendingMessages);
                    break;
                case MSG_CLOSE_SERVICE:
                    unbindService();
                    break;
                case MSG_SERVICE_CONNECTED:
                    IBinder service = (IBinder) msg.obj;
                    mServer = new Messenger(service);
                    mState = STATE_CONNECTED;
                    if (!pendingMessages.isEmpty()) {
                        for (Message m : pendingMessages) {
                            handleMessage(m);
                            m.recycle();
                        }
                        pendingMessages.clear();
                    }

                    break;
                case MSG_SERVICE_DISCONNECTED:
                    mServer = null;
                    mState = STATE_DISCONNECTED;
                    break;
                case MSG_UPDATE_ONLINE: {
                    if (mState != STATE_CONNECTED) {
                        pendingMessages.add(Message.obtain(msg));
                        getServer();
                        break;
                    }

                    String url = (String) msg.obj;
                    onUpdateOnline(url);
                    break;
                }
                case MSG_INSTALL_LOCALLY: {
                    if (mState != STATE_CONNECTED) {
                        pendingMessages.add(Message.obtain(msg));
                        getServer();
                        break;
                    }

                    Uri uri = (Uri) msg.obj;
                    onInstallLocally(uri);
                    break;
                }
                case MSG_START_DEBUGGING: {
                    if (mState != STATE_CONNECTED) {
                        pendingMessages.add(Message.obtain(msg));
                        getServer();
                        DebuggerLogUtil.logMessage("DEBUGGER_UNBIND_ENGINE");
                        break;
                    }

                    String[] params = (String[]) msg.obj;
                    String pkg = params[0];
                    String server = params[1];
                    String target = params[2];
                    boolean result = onStartDebugging(pkg, server, target);
                    if (!result && mListener != null) {
                        mListener.onDebugResult(false);
                    }
                    break;
                }
                case MSG_UNINSTALL_PACKAGE: {
                    if (mState != STATE_CONNECTED) {
                        pendingMessages.add(Message.obtain(msg));
                        getServer();
                        break;
                    }

                    String pkg = ((String) msg.obj);
                    onUninstallPackage(pkg);
                    break;
                }
                case MSG_LAUNCH_PACKAGE: {
                    String pkg = ((String) msg.obj);
                    boolean shouldReload = PreferenceUtils.shouldReloadPackage(mContext);
                    sendLaunchPackageMessage(pkg, shouldReload);
                    break;
                }
                case MSG_SEARCH_SN: {
                    boolean result = HttpUtils.searchSerialNumber(mContext);
                    if (!result) {
                        Log.i(TAG, "Fail to notify npm server to update sn!");
                    }
                    break;
                }
                case MSG_SERVICE_INSTALL_PACKAGE: {
                    Bundle params = msg.getData();
                    String pkg = params.getString(EXTRA_PACKAGE);
                    boolean result = params.getBoolean(EXTRA_RESULT);
                    int code = params.getInt(EXTRA_ERROR_CODE);
                    onInstallResult(pkg, result, code);
                    break;
                }
                case MSG_SERVICE_LAUNCH_PACKAGE: {
                    Bundle params = msg.getData();
                    String pkg = params.getString(EXTRA_PACKAGE);
                    boolean result = params.getBoolean(EXTRA_RESULT);
                    onLaunchResult(pkg, result);
                    break;
                }
                case MSG_SERVICE_DEBUG_PACKAGE: {
                    Bundle params = msg.getData();
                    String pkg = params.getString(EXTRA_PACKAGE);
                    boolean result = params.getBoolean(EXTRA_RESULT);
                    onDebugResult(pkg, result);
                    break;
                }
                case MSG_SERVICE_UNINSTALL_PACKAGE: {
                    Bundle params = msg.getData();
                    String pkg = params.getString(EXTRA_PACKAGE);
                    boolean result = params.getBoolean(EXTRA_RESULT);
                    onUninstallResult(pkg, result);
                    break;
                }
                default:
                    break;
            }
        }
    }
}
