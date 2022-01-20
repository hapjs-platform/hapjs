/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug.log;

import android.content.Context;

public class DefaultDebuggerLogUtilImpl implements DebuggerLogUtilProvider {

    @Override
    public void logBreadcrumb(String msg) {
    }

    @Override
    public void logMessage(String msg) {
    }

    @Override
    public void logMessage(String msg, int level) {
    }

    @Override
    public void logError(String msg) {
    }

    @Override
    public void logException(Throwable e) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void resetTraceId() {
    }

    @Override
    public String getTraceId() {
        return "";
    }

    @Override
    public void init(Context context, String tranceId) {
    }
}
