/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.monitors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.model.LogData;
import org.hapjs.analyzer.model.LogPackage;
import org.hapjs.analyzer.monitors.abs.AbsMonitor;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.common.executors.Executors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.hapjs.common.utils.ProcessUtils;

import org.hapjs.render.jsruntime.SandboxProvider;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.sandbox.ILogListener;
import org.hapjs.runtime.sandbox.ISandbox;

public class LogcatMonitor extends AbsMonitor<LogPackage> {
    private static final String TAG = "LogcatMonitor";
    public static final String NAME = "logcat";
    public static final int LOG_JS = 1;
    public static final int LOG_NATIVE = 1 << 1;
    public static final int TYPE_LOG_STYLE_WEB = 0;
    public static final int TYPE_LOG_STYLE_ANDROID = 1;
    public static final String JS_TAG = "LOGCAT_CONSOLE";
    private static final int CACHE_SIZE = 500;
    private static final int MSG_SEND_ONE_LOG = 1;
    private int mLogLevel = Log.VERBOSE;
    private int mLogFlag = LOG_JS | ~LOG_NATIVE;
    private int mLogStyle = TYPE_LOG_STYLE_WEB;
    private String mFilter = "";
    private Dumper mDumper;
    private LinkedList<LogData> mCaches = new LinkedList<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SEND_ONE_LOG) {
                Pipeline<LogPackage> pipeline = getPipeline();
                if (pipeline != null) {
                    pipeline.output((LogPackage) msg.obj);
                }
            }
        }
    };

    public LogcatMonitor() {
        super(NAME);
    }

    @Override
    protected void onStart() {
        if (mDumper != null) {
            mDumper.close();
        }
        mDumper = new Dumper();
        Executors.io().execute(mDumper);
        onRuleChanged();
    }

    @Override
    protected void onStop() {
        if (mDumper != null) {
            mDumper.close();
            mDumper = null;
        }
    }

    public void setFilter(String filter) {
        mFilter = filter;
        onRuleChanged();
    }

    public void setLogLevel(int level) {
        mLogLevel = level;
        onRuleChanged();
    }

    public void setLogType(int type) {
        mLogFlag = type;
        onRuleChanged();
    }

    public int getLogType() {
        return mLogFlag;
    }

    public void setLogStyle(int style) {
        mLogStyle = style;
        onRuleChanged();
    }

    private void onRuleChanged() {
        mMainHandler.removeMessages(MSG_SEND_ONE_LOG);
        AnalyzerThreadManager.getInstance().getAnalyzerHandler().post(() -> {
            if (mCaches.isEmpty()) {
                return;
            }
            List<LogData> logDatas;
            synchronized (LogcatMonitor.this) {
                logDatas = filterData(new ArrayList<>(mCaches));
            }
            sendLogDatas(0, logDatas);
        });
    }

    private List<LogData> filterData(List<LogData> originData) {
        if (originData == null || originData.isEmpty()) {
            return new ArrayList<>();
        }
        String filter = mFilter == null ? "" : mFilter.toLowerCase();
        Iterator<LogData> iterator = originData.iterator();
        while (iterator.hasNext()) {
            LogData logData = iterator.next();
            if (logData.mType == LogPackage.LOG_TYPE_DEFAULT) {
                continue;
            }
            if (mLogStyle == TYPE_LOG_STYLE_ANDROID) {
                if (logData.mLevel < mLogLevel) {
                    iterator.remove();
                    continue;
                }
            } else {
                if (mLogLevel != Log.VERBOSE && logData.mLevel != mLogLevel) {
                    iterator.remove();
                    continue;
                }
            }
            if (!TextUtils.isEmpty(mFilter) && !logData.mContent.toLowerCase().contains(filter)) {
                iterator.remove();
                continue;
            }
            boolean isJsLog = logData.mType == LogPackage.LOG_TYPE_JS;
            if (isJsLog && (mLogFlag & LOG_JS) == 0) {
                iterator.remove();
                continue;
            }
            if (!isJsLog && (mLogFlag & LOG_NATIVE) == 0) {
                iterator.remove();
            }
        }
        return originData;
    }

    private synchronized void cacheLog(List<LogData> logDatas) {
        mCaches.addAll(logDatas);
        while (mCaches.size() >= CACHE_SIZE) {
            mCaches.removeFirst();
        }
    }

    public void clearLog() {
        mDumper.clearLogcat();
        mCaches.clear();
    }

    private void sendLogDatas(int position, List<LogData> logDatas) {
        if (logDatas != null) {
            Pipeline<LogPackage> pipeline = getPipeline();
            if (pipeline != null) {
                mMainHandler.post(() -> pipeline.output(new LogPackage(position, logDatas)));
            }
        }
    }

    private class Dumper extends AbsLogDumper {
        private ISandbox mSandbox;
        private ServiceConnection mConnection;

        public Dumper() {
            SandboxProvider sandboxProvider = ProviderManager.getDefault().getProvider(SandboxProvider.NAME);
            if (sandboxProvider != null && sandboxProvider.isSandboxEnabled()) {
                connectSandboxService();
            }
        }

        @Override
        protected void doDumpLog(List<LogData> logs) {
            cacheLog(logs);
            AnalyzerThreadManager.getInstance().getAnalyzerHandler().post(() -> {
                List<LogData> filterData = filterData(logs);
                if (!filterData.isEmpty()) {
                    Message message = mMainHandler.obtainMessage(MSG_SEND_ONE_LOG);
                    message.obj = new LogPackage(filterData);
                    mMainHandler.sendMessage(message);
                }
            });
        }

        @Override
        public void close() {
            super.close();

            if (mConnection != null) {
                Context context = Analyzer.get().getApplicationContext();
                context.unbindService(mConnection);
                mConnection = null;
            }
            if (mSandbox != null) {
                try {
                    mSandbox.setLogListener(null);
                    mSandbox = null;
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to setLogListener null", e);
                }
            }
        }

        private void connectSandboxService() {
            String currentProcessName = ProcessUtils.getCurrentProcessName();
            String sandboxName = "org.hapjs.runtime.sandbox.SandboxService$Sandbox"
                    + currentProcessName.charAt(currentProcessName.length() - 1);

            Context context = Analyzer.get().getApplicationContext();
            Intent intent = new Intent();
            intent.setClassName(context.getPackageName(), sandboxName);

            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mSandbox = ISandbox.Stub.asInterface(service);
                    try {
                        mSandbox.setLogListener(new ILogListener.Stub() {
                            public void onLog(List<LogData> logs) {
                                dumpLog(logs);
                            }
                        });
                    } catch (RemoteException e) {
                        Log.e(TAG, "failed to setLogListener", e);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            boolean bindResult = context.bindService(intent, mConnection, 0);
            if (!bindResult) {
                Log.e(TAG, "bind sandboxService failed. sandboxName=" + sandboxName);
            }
        }
    }
}
