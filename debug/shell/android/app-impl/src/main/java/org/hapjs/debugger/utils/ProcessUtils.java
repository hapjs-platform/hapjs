/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.os.Process;
import android.util.Log;
import java.io.IOException;
import java.util.Locale;

public class ProcessUtils {

    private static final String TAG = "ProcessUtils";
    private static String sCurrentProcessName;

    /**
     * @return The name of current process
     */
    public static String getCurrentProcessName() {
        if (sCurrentProcessName == null) {
            sCurrentProcessName = getProcessNameByPid(Process.myPid());
            if (sCurrentProcessName == null || sCurrentProcessName.isEmpty()) {
                throw new RuntimeException("Can't get current process name: " + sCurrentProcessName);
            }
        }
        return sCurrentProcessName;
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
