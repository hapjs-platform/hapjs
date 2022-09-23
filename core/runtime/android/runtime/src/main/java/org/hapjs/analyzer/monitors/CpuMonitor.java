/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.analyzer.monitors.abs.AbsTimerMonitor;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.Runtime;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class CpuMonitor extends AbsTimerMonitor<String> {
    public static final String NAME = "cpu";
    public static final String DEFAULT = "-";
    private static final String TAG = "CpuMonitor";
    private RandomAccessFile mProcStatFile;
    private RandomAccessFile mProcAppStatFile;
    private long mLastCpuTime;
    private long mLastAppCpuTime;
    private boolean mCanAccessProcStatFile;

    private static boolean canAccessProcStatFile() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isSystemApp();
    }

    private static boolean isSystemApp() {
        Context context = Runtime.getInstance().getContext();
        if (context == null) {
            return false;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo != null &&
                    packageInfo.applicationInfo != null &&
                    ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public CpuMonitor() {
        super(NAME, false, canAccessProcStatFile() ? DEFAULT_INTERVAL : 1500);
        boolean mIsSystemApp = isSystemApp();
        mCanAccessProcStatFile = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || mIsSystemApp;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCanAccessProcStatFile) {
            if (mProcStatFile == null) {
                try {
                    mProcStatFile = new RandomAccessFile("/proc/stat", "r");
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "AnalyzerPanel_LOG file not found: /proc/stat", e);
                }
            }
            if (mProcAppStatFile == null) {
                try {
                    int pid = Process.myPid();
                    mProcAppStatFile = new RandomAccessFile("/proc/" + pid + "/stat", "r");
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "AnalyzerPanel_LOG file not found: /proc/{pid}/stat", e);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProcStatFile != null) {
            try {
                mProcStatFile.close();
            } catch (IOException e) {
                // ignore
            }
            mProcStatFile = null;
        }

        if (mProcAppStatFile != null) {
            try {
                mProcAppStatFile.close();
            } catch (IOException e) {
                // ignore
            }
            mProcAppStatFile = null;
        }
    }

    @Override
    protected void loop() {
        if (!isAppVisible()) {
            return;
        }
        Pipeline<String> pipeline = getPipeline();
        if (pipeline == null) {
            return;
        }
        runOnUiThread(() -> pipeline.output(getCpuUsage()));
    }

    private String getCpuUsage() {
        if (!mCanAccessProcStatFile) {
            return getCpuUsageByTop();
        }
        return getCpuUsageByProcStat();
    }

    private String getCpuUsageByProcStat() {
        long lastCpuTime = mLastCpuTime;
        long lastAppCpuTime = mLastAppCpuTime;
        long currentCpuTime = 0;
        long currentAppCpuTime = 0;
        if (mProcStatFile != null) {
            try {
                mProcStatFile.seek(0);
                String line = mProcStatFile.readLine();
                String[] procStats = line.split("\\s+");
                long cpuTime = Long.parseLong(procStats[1]) +
                        Long.parseLong(procStats[2]) +
                        Long.parseLong(procStats[3]) +
                        Long.parseLong(procStats[4]) +
                        Long.parseLong(procStats[5]) +
                        Long.parseLong(procStats[6]) +
                        Long.parseLong(procStats[7]);
                currentCpuTime = mLastCpuTime = cpuTime;
            } catch (Exception e) {
                return DEFAULT;
            }
        }
        if (mProcAppStatFile != null) {
            try {
                mProcAppStatFile.seek(0);
                String line = mProcAppStatFile.readLine();
                String[] procAppStats = line.split("\\s+");
                long appTime = Long.parseLong(procAppStats[13]) + Long.parseLong(procAppStats[14]);
                currentAppCpuTime = mLastAppCpuTime = appTime;
            } catch (Exception e) {
                return DEFAULT;
            }
        }
        float cpuValue = 0;
        if (currentCpuTime != lastCpuTime) {
            cpuValue = 100 * (currentAppCpuTime - lastAppCpuTime) * 1f / (currentCpuTime - lastCpuTime);
            if (cpuValue < 0) {
                cpuValue = 0;
            }
        }
        return String.format(Locale.US, "%.1f%%", cpuValue);
    }


    private String getCpuUsageByTop() {
        java.lang.Process process = null;
        BufferedReader reader = null;
        try {
            process = java.lang.Runtime.getRuntime().exec("top -n 1");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            int cpuIndex = -1;
            String pid = String.valueOf(Process.myPid());
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                int tempIndex = findCPUIndex(line);
                if (tempIndex != -1) {
                    cpuIndex = tempIndex;
                    continue;
                }
                if (line.startsWith(pid)) {
                    if (cpuIndex == -1) {
                        continue;
                    }
                    String[] param = line.split("\\s+");
                    if (param.length <= cpuIndex) {
                        continue;
                    }
                    String cpu = param[cpuIndex];
                    if (cpu.endsWith("%")) {
                        cpu = cpu.substring(0, cpu.lastIndexOf('%'));
                    }
                    float cpuValue = Float.parseFloat(cpu);
                    if (cpuValue < 0) {
                        cpuValue = 0;
                    }
                    return String.format(Locale.US, "%.1f%%", cpuValue / java.lang.Runtime.getRuntime().availableProcessors());
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (process != null) {
                process.destroy();
            }
            FileUtils.closeQuietly(reader);
        }
        return DEFAULT;
    }

    private int findCPUIndex(String line) {
        if (line.contains("CPU")) {
            String[] titles = line.split("\\s+");
            for (int i = 0; i < titles.length; i++) {
                if (titles[i].contains("CPU")) {
                    return i;
                }
            }
        }
        return -1;
    }
}
