/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors.abs;

import android.os.Handler;

import androidx.annotation.CallSuper;

import org.hapjs.analyzer.tools.AnalyzerThreadManager;

public abstract class AbsTimerMonitor<T> extends AbsMonitor<T> implements Runnable {
    protected static final int DEFAULT_INTERVAL = 1000;
    private int mInterval;
    private Handler mHandler;

    public AbsTimerMonitor(String name, boolean runOnUiThread) {
        this(name, runOnUiThread, DEFAULT_INTERVAL);
    }

    public AbsTimerMonitor(String name, boolean runOnUiThread, int interval) {
        super(name);
        mHandler = runOnUiThread ?
                AnalyzerThreadManager.getInstance().getMainHandler()
                : AnalyzerThreadManager.getInstance().getAnalyzerHandler();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }
        mInterval = interval;
    }


    @CallSuper
    @Override
    protected void onStart() {
        mHandler.post(this);
    }

    @Override
    public void run() {
        try {
            loop();
        } catch (Exception e) {
            // ignore
        }
        mHandler.postDelayed(this, mInterval);
    }

    @CallSuper
    @Override
    protected void onStop() {
        mHandler.removeCallbacks(this);
    }

    protected abstract void loop();
}
