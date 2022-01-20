/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug.log;

import android.content.Context;

public interface DebuggerLogUtilProvider {

    public static final String NAME = "DebuggerLogUtilProvider";

    void logBreadcrumb(String msg);

    void logMessage(String msg);

    void logMessage(String msg, int level);

    void logError(String msg);

    void logException(Throwable e);

    void stop();

    void resetTraceId();

    String getTraceId();

    void init(Context context, String tranceId);
}
