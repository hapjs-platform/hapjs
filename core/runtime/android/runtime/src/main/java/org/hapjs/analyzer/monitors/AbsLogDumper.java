/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.analyzer.monitors;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.analyzer.model.LogData;
import org.hapjs.analyzer.model.LogPackage;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FileUtils;

public abstract class AbsLogDumper implements Runnable {
    protected static final  String STR_DUMP_FAIL = "--- LOGCAT_CONSOLE dump log fail ! ---";
    private static final int DUMP_LOG_INTERVAL = 100;
    private static final int DUMP_LOG_BATCH_CNT = 100;
    private static final int MSG_DUMP_LOG = 0;

    private java.lang.Process mLogcatProcess;
    private boolean mIsStop;
    private List<LogData> mPendingLogs = new ArrayList<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_DUMP_LOG) {
                List<LogData> pendingLogs = new ArrayList<>();
                synchronized (AbsLogDumper.this) {
                    pendingLogs.addAll(mPendingLogs);
                    mPendingLogs.clear();
                }
                doDumpLog(pendingLogs);
            }
        }
    };

    void clearLogcat() {
        Executors.io().execute(new Runnable() {
            @Override
            public void run() {
                java.lang.Process process = null;
                try {
                    process = Runtime.getRuntime().exec("logcat -b main -c");
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // ignore
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
            }
        });
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
                    dumpFailLog();
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
        boolean isJsLog = log.contains(LogcatMonitor.JS_TAG);
        LogData logData = new LogData(logLevel, isJsLog ? LogPackage.LOG_TYPE_JS : LogPackage.LOG_TYPE_NATIVE, log);
        dumpLog(logData);
    }

    public void dumpFailLog() {
        LogData logData = new LogData(LogPackage.LOG_LEVEL_DEFAULT, LogPackage.LOG_TYPE_DEFAULT, STR_DUMP_FAIL);
        dumpLog(logData);
    }

    private synchronized void dumpLog(LogData log) {
        List<LogData> logs = new ArrayList<>();
        logs.add(log);
        dumpLog(logs);
    }

    protected synchronized void dumpLog(List<LogData> logs) {
        boolean noPending = mPendingLogs.isEmpty();
        mPendingLogs = mergeLogs(mPendingLogs, logs);
        if (noPending) {
            if (!mMainHandler.hasMessages(MSG_DUMP_LOG)) {
                mMainHandler.sendEmptyMessageDelayed(MSG_DUMP_LOG, DUMP_LOG_INTERVAL);
            }
        } else if (mPendingLogs.size() > DUMP_LOG_BATCH_CNT) {
            if (!mMainHandler.hasMessages(MSG_DUMP_LOG)) {
                mMainHandler.sendEmptyMessage(MSG_DUMP_LOG);
            }
        }
    }

    private static List<LogData> mergeLogs(List<LogData> logs1, List<LogData> logs2) {
        if (logs1 == null || logs1.isEmpty()) {
            return logs2;
        }
        if (logs2 == null || logs2.isEmpty()) {
            return logs1;
        }
        if (compareTimestamp(logs1.get(logs1.size() - 1), logs2.get(0)) <= 0) {
            logs1.addAll(logs2);
            return logs1;
        }
        if (compareTimestamp(logs2.get(logs2.size() - 1), logs1.get(0)) <= 0) {
            logs2.addAll(logs1);
            return logs2;
        }

        List<LogData> mergedLogs = new ArrayList<>();
        for (int i = 0, j = 0; i < logs1.size() || j < logs2.size(); ) {
            if (i >= logs1.size()) {
                mergedLogs.add(logs2.get(j++));
            } else if (j >= logs2.size()) {
                mergedLogs.add(logs1.get(i++));
            } else if (compareTimestamp(logs1.get(i), logs2.get(j)) <= 0) {
                mergedLogs.add(logs1.get(i++));
            } else {
                mergedLogs.add(logs2.get(j++));
            }
        }
        return mergedLogs;
    }

    private static int compareTimestamp(LogData logData1, LogData logData2) {
        return getTimestamp(logData1).compareTo(getTimestamp(logData2));
    }

    private static String getTimestamp(LogData logData) {
        int length = logData.mContent.length();
        return logData.mContent.substring(0, Math.min(length, 18));
    }

    protected abstract void doDumpLog(List<LogData> log);

    public void close() {
        mIsStop = true;
        clearLogcat();
        closeLogcatProcess();
    }

    private void closeLogcatProcess() {
        if (mLogcatProcess != null) {
            try {
                mLogcatProcess.destroy();
            } catch (Exception e) {
                // ignore
            }
        }
        mLogcatProcess = null;
    }

    private @LogPackage.LogLevel int getLogLevel(String log) {
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
}
