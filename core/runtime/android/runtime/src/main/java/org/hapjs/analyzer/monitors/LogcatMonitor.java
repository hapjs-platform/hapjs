/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.monitors;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.analyzer.model.LogPackage;
import org.hapjs.analyzer.monitors.abs.AbsMonitor;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.common.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LogcatMonitor extends AbsMonitor<LogPackage> {
    public static final String NAME = "logcat";
    public static final int LOG_JS = 1;
    public static final int LOG_NATIVE = 1 << 1;
    public static final int TYPE_LOG_STYLE_WEB = 0;
    public static final int TYPE_LOG_STYLE_ANDROID = 1;
    private static final String JS_TAG = "LOGCAT_CONSOLE";
    private static final int CACHE_SIZE = 500;
    private int mLogLevel = Log.VERBOSE;
    private int mLogFlag = LOG_JS | ~LOG_NATIVE;
    private int mLogStyle = TYPE_LOG_STYLE_WEB;
    private String mFilter = "";
    private Dumper mDumper;
    private Thread mThread;
    private LinkedList<LogPackage.LogData> mCaches = new LinkedList<>();
    private Handler mMainHandler = AnalyzerThreadManager.getInstance().getMainHandler();

    public LogcatMonitor() {
        super(NAME);
    }

    @Override
    protected void onStart() {
        mDumper = new Dumper();
        mThread = new Thread(mDumper);
        mThread.start();
        onRuleChanged();
    }

    @Override
    protected void onStop() {
        if (mDumper != null) {
            mDumper.close();
            mDumper = null;
        }
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
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
        AnalyzerThreadManager.getInstance().getAnalyzerHandler().post(() -> {
            if (mCaches.isEmpty()) {
                return;
            }
            List<LogPackage.LogData> logDatas = filterData(mCaches);
            sendLogDatas(0, logDatas);
        });
    }

    private List<LogPackage.LogData> filterData(List<LogPackage.LogData> originData) {
        if(originData == null || originData.isEmpty()){
            return new ArrayList<>();
        }
        List<LogPackage.LogData> logDatas = new ArrayList<>(originData.size());
        String filter = mFilter == null ? "" : mFilter.toLowerCase();
        for (LogPackage.LogData logData : originData) {
            if (mLogStyle == TYPE_LOG_STYLE_ANDROID) {
                if (logData.mLevel < mLogLevel) {
                    continue;
                }
            } else {
                if (mLogLevel != Log.VERBOSE && logData.mLevel != mLogLevel) {
                    continue;
                }
            }
            if (!TextUtils.isEmpty(mFilter) && !logData.mContent.toLowerCase().contains(filter)) {
                continue;
            }
            boolean isJsLog = logData.mContent.contains(JS_TAG);
            if (isJsLog && (mLogFlag & LOG_JS) == 0) {
                continue;
            }
            if (!isJsLog && (mLogFlag & LOG_NATIVE) == 0) {
                continue;
            }
            logDatas.add(logData);
        }
        return logDatas;
    }

    private synchronized void cacheLog(LogPackage.LogData logData) {
        if (mCaches.size() >= CACHE_SIZE) {
            mCaches.removeFirst();
        }
        mCaches.add(logData);
    }

    public void clearLog() {
        mDumper.clearLogcat();
        mCaches.clear();
    }

    private void sendLogDatas(List<LogPackage.LogData> logDatas) {
        if (logDatas != null) {
            Pipeline<LogPackage> pipeline = getPipeline();
            if (pipeline != null) {
                mMainHandler.post(() -> pipeline.output(new LogPackage(logDatas)));
            }
        }
    }

    private void sendLogDatas(int position, List<LogPackage.LogData> logDatas) {
        if (logDatas != null) {
            Pipeline<LogPackage> pipeline = getPipeline();
            if (pipeline != null) {
                mMainHandler.post(() -> pipeline.output(new LogPackage(position, logDatas)));
            }
        }
    }

    private class Dumper implements Runnable {
        private static final  String STR_DUMP_FAIL = "--------- LOGCAT_CONSOLE dump log fail !";
        private Process mLogcatProcess;
        private boolean mIsStop;

        void clearLogcat() {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("logcat -b main -c");
                process.waitFor();
            } catch (Exception e) {
                // ignore
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }

        private BufferedReader getLogReader() throws IOException {
            String command = "logcat -b main -v time --pid " + android.os.Process.myPid();
            if (mLogcatProcess != null) {
                mLogcatProcess.destroy();
            }
            mLogcatProcess = Runtime.getRuntime().exec(command);
            return new BufferedReader(new InputStreamReader(mLogcatProcess.getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                String line;
                reader = getLogReader();
                while (!mIsStop) {
                    line = reader.readLine();
                    if (line == null) {
                        dumpLog(STR_DUMP_FAIL);
                        break;
                    } else {
                        dumpLog(line);
                    }
                }
            } catch (Exception e) {
                // ignore
            } finally {
                FileUtils.closeQuietly(reader);
            }
        }

        private void dumpLog(String log) {
            int logLevel = getLogLevel(log);
            boolean isJsLog = log.contains(JS_TAG);
            LogPackage.LogData logData = new LogPackage.LogData(logLevel, isJsLog, log);
            cacheLog(logData);
            AnalyzerThreadManager.getInstance().getAnalyzerHandler().post(() -> {
                List<LogPackage.LogData> filterData = filterData(Collections.singletonList(logData));
                if (!filterData.isEmpty()) {
                    sendLogDatas(filterData);
                }
            });
        }

        private int getLogLevel(String log) {
            if (log.length() < 20) {
                return Log.VERBOSE;
            }
            char level = log.charAt(19);
            switch (level) {
                case 'V':
                    return Log.VERBOSE;
                case 'D':
                    return Log.DEBUG;
                case 'I':
                    return Log.INFO;
                case 'W':
                    return Log.WARN;
                case 'E':
                    return Log.ERROR;
            }
            return Log.VERBOSE;
        }

        void close() {
            mIsStop = true;
            clearLogcat();
            closeLogcatProcess();
        }

        void closeLogcatProcess(){
            if (mLogcatProcess != null) {
                try {
                    mLogcatProcess.destroy();
                } catch (Exception e) {
                    // ignore
                }
            }
            mLogcatProcess = null;
        }
    }
}
