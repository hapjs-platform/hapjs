/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Process;

import org.hapjs.analyzer.monitors.abs.AbsTimerMonitor;

public class MemoryMonitor extends AbsTimerMonitor<Float> {
    public static final String NAME = "mem";
    private boolean isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    private ActivityManager mActivityManager;

    public MemoryMonitor() {
        super(NAME, false);
    }

    @Override
    protected void loop() {
        if (!isAppVisible()) {
            return;
        }
        Pipeline<Float> pipeline = getPipeline();
        if (pipeline != null) {
            runOnUiThread(() -> pipeline.output(getMemory()));
        }
    }

    private float getMemory() {
        Debug.MemoryInfo memoryInfo = null;
        //As of Android Q, for regular apps this method will only return information about the memory info for
        // the processes running as the caller's uid; no other process memory info is available and will be zero.
        // Also of Android Q the sample rate allowed by this API is significantly limited,
        // if called faster the limit you will receive the same data as the previous call.
        if (isAndroidQ) {
            memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);
        } else {
            Context context = getContext();
            if (mActivityManager == null) {
                mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            }
            Debug.MemoryInfo[] info = mActivityManager.getProcessMemoryInfo(new int[]{Process.myPid()});
            if (info != null && info.length > 0) {
                memoryInfo = info[0];
            }
        }
        if (memoryInfo != null) {
            return memoryInfo.getTotalPss() / 1024f;
        } else {
            return 0;
        }
    }
}
