/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import org.hapjs.runtime.RuntimeActivity;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final Thread.UncaughtExceptionHandler sDefaultHandler =
            Thread.getDefaultUncaughtExceptionHandler();

    private static AppCrashListener sAppCrashListener;

    public static void setAppCrashListener(AppCrashListener listener) {
        sAppCrashListener = listener;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (sAppCrashListener != null) {
            sAppCrashListener.onAppCrashed(System.getProperty(RuntimeActivity.PROP_APP), e);
        }
        if (sDefaultHandler != null) {
            sDefaultHandler.uncaughtException(t, e);
        }
    }

    public interface AppCrashListener {
        void onAppCrashed(String packageName, Throwable e);
    }
}
