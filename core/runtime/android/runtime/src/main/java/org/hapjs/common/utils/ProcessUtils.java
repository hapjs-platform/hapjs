/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class to hold process utility methods.
 */
public class ProcessUtils {

    private static final String TAG = "ProcessUtils";
    private static String sCurrentProcessName;
    private static Boolean sIsMainProcess;
    private static Boolean sIsAppProcess;
    private static Boolean sIsSandboxProcess;

    /**
     * Only for extends, please don't instantiate this class.
     *
     * @throws InstantiationException always when this class is instantiated.
     */
    protected ProcessUtils() throws InstantiationException {
        throw new InstantiationException("Cannot instantiate utility class");
    }

    /**
     * @return The name of current process
     */
    public static String getCurrentProcessName() {
        if (sCurrentProcessName == null) {
            sCurrentProcessName = getProcessNameByPid(Process.myPid());
            if (sCurrentProcessName == null || sCurrentProcessName.isEmpty()) {
                throw new RuntimeException(
                        "Can't get current process name: " + sCurrentProcessName);
            }
        }
        return sCurrentProcessName;
    }

    public static boolean isMainProcess(Context context) {
        if (sIsMainProcess == null) {
            sIsMainProcess = context.getPackageName().equals(getCurrentProcessName());
        }
        return sIsMainProcess;
    }

    public static boolean isAppProcess(Context context) {
        if (sIsAppProcess == null) {
            sIsAppProcess =
                    getCurrentProcessName().startsWith(context.getPackageName() + ":Launcher");
        }
        return sIsAppProcess;
    }

    public static boolean isSandboxProcess(Context context) {
        if (sIsSandboxProcess == null) {
            sIsSandboxProcess = getCurrentProcessName().startsWith(context.getPackageName() + ":Sandbox");
        }
        return sIsSandboxProcess;
    }

    /**
     * Return a process map(processName-pid), or null
     */
    public static Map<String, Integer> getAppProcesses(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos =
                am.getRunningAppProcesses();
        if (runningAppProcessInfos != null && !runningAppProcessInfos.isEmpty()) {
            Map<String, Integer> map = new HashMap<>();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfos) {
                if (processInfo.uid == Process.myUid()) {
                    map.put(processInfo.processName, processInfo.pid);
                }
            }
            map.remove(context.getPackageName());
            return map;
        }
        return null;
    }

    /**
     * Return process name by pid, or null if process with pid is not found
     */
    private static String getProcessNameByPid(int pid) {
        String path = String.format(Locale.US, "/proc/%d/cmdline", pid);
        try {
            String process = FileUtils.readFileAsString(path);
            if (process != null) {
                // Remove zero char at the tail of process
                int length = process.indexOf(0);
                if (length >= 0) {
                    process = process.substring(0, length);
                }
                return process;
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to read cmdline: " + path, e);
        }
        return null;
    }
}
