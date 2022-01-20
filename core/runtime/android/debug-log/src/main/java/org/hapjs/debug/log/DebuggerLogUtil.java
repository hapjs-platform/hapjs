/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug.log;

import android.content.Context;

public class DebuggerLogUtil {

    private static volatile DebuggerLogUtilProvider sProvider;

    private static DebuggerLogUtilProvider getProvider() {
        if (sProvider == null) {
            synchronized (DebuggerLogUtil.class) {
                if (sProvider == null) {
                    sProvider = new DefaultDebuggerLogUtilImpl();
                }
            }
        }
        return sProvider;
    }

    public static void initProvider(DebuggerLogUtilProvider provider) {
        sProvider = provider;
    }

    public static void logBreadcrumb(String msg) {
        getProvider().logBreadcrumb(msg);
    }

    public static void logMessage(String msg) {
        getProvider().logMessage(msg);
    }

    public static void logMessage(String msg, int level) {
        getProvider().logMessage(msg, level);
    }

    public static void logError(String msg) {
        getProvider().logError(msg);
    }

    public static void logException(Throwable e) {
        getProvider().logException(e);
    }

    public static void stop() {
        getProvider().stop();
    }

    public static void resetTraceId() {
        getProvider().resetTraceId();
    }

    public static String getTraceId() {
        return getProvider().getTraceId();
    }

    public static void init(Context context, String traceId) {
        getProvider().init(context, traceId);
    }
}
