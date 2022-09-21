/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors.abs;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.runtime.Runtime;

import java.util.List;

public abstract class AbsMonitor<T> implements Monitor {
    private boolean mEnable = true;
    private boolean mRunning = false;
    private Pipeline<T> mPipeline;
    private String mName;
    private final String mProcessName;

    public AbsMonitor(String name) {
        mName = name;
        mProcessName = ProcessUtils.getCurrentProcessName();
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public void setEnable(boolean enable) {
        if (!enable) {
            stop();
        }
        mEnable = enable;
    }

    @Override
    public boolean isEnabled() {
        return mEnable;
    }

    @Override
    public void start() {
        if (mRunning || !mEnable) {
            return;
        }
        onStart();
        mRunning = true;
    }

    @Override
    public void stop() {
        if (!mRunning || !mEnable) {
            return;
        }
        onStop();
        mRunning = false;
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    public void setPipeline(Pipeline<T> pipeline) {
        mPipeline = pipeline;
    }

    public Pipeline<T> getPipeline() {
        return mPipeline;
    }

    protected abstract void onStart();

    protected abstract void onStop();

    public void runOnUiThread(Runnable runnable) {
        AnalyzerThreadManager.getInstance().getMainHandler().post(runnable);
    }

    public Context getContext() {
        return Runtime.getInstance().getContext();
    }

    protected boolean isAppVisible() {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        List<ActivityManager.RunningAppProcessInfo> info = am.getRunningAppProcesses();
        if (info == null || info.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo aInfo : info) {
            if (TextUtils.equals(aInfo.processName,mProcessName) &&
                    (aInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                            aInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE)) {
                return true;
            }
        }
        return false;
    }
}
