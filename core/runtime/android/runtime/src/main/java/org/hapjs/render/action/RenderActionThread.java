/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import android.os.Handler;
import android.os.HandlerThread;

public class RenderActionThread extends HandlerThread {
    private Handler mHandler;

    RenderActionThread() {
        super("RenderActionThread");
        start();
        mHandler = new Handler(getLooper());
    }

    void doShutdown() {
        quit();
    }

    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}
