/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.tools;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class AnalyzerThreadManager {

    private final Handler mMainHandler;
    private Handler mAnalyzerHandler;
    private HandlerThread mAnalyzerThread;

    private static class Holder {
        static AnalyzerThreadManager INSTANCE = new AnalyzerThreadManager();
    }

    public static AnalyzerThreadManager getInstance() {
        return Holder.INSTANCE;
    }


    private AnalyzerThreadManager() {
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    private void initAnalyzerThread() {
        if (mAnalyzerHandler == null) {
            mAnalyzerThread = new HandlerThread("thread-analyzer");
            mAnalyzerThread.start();
            mAnalyzerHandler = new Handler(mAnalyzerThread.getLooper());
        }
    }

    public void stop() {
        if (mAnalyzerHandler != null) {
            mAnalyzerHandler.removeCallbacksAndMessages(null);
        }
        if (mAnalyzerThread != null && mAnalyzerThread.isAlive()) {
            mAnalyzerThread.quitSafely();
        }
        mAnalyzerThread = null;
    }

    public Handler getMainHandler() {
        return mMainHandler;
    }

    public Handler getAnalyzerHandler() {
        if (mAnalyzerHandler == null) {
            initAnalyzerThread();
        }
        return mAnalyzerHandler;
    }
}
