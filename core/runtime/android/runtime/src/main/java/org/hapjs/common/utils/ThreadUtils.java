/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtils {

    private static volatile Handler sMainHandler;

    public static Handler getHandlerInstance() {
        if (sMainHandler == null) {
            synchronized (ThreadUtils.class) {
                if (sMainHandler == null) {
                    sMainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return sMainHandler;
    }

    public static boolean isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void checkInMainThread() {
        if (!isInMainThread()) {
            throw new IllegalStateException("Call must in main thread");
        }
    }

    public static void runOnUiThread(Runnable runnable) {
        if (isInMainThread()) {
            runnable.run();
        } else {
            getHandlerInstance().post(runnable);
        }
    }

    public static void runOnUiThreadWithDelay(Runnable runnable, int millisecond) {
        getHandlerInstance().postDelayed(runnable, millisecond);
    }
}
