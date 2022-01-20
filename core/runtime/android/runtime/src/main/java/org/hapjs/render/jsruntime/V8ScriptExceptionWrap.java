/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import com.eclipsesource.v8.V8ScriptException;

public class V8ScriptExceptionWrap extends Throwable {
    private final V8ScriptException e;

    public V8ScriptExceptionWrap(V8ScriptException e) {
        this.e = e;
        this.initCause(e.getCause());
        this.setStackTrace(e.getStackTrace());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String jsMsg = NotifyAppErrorHelper.removeMessageTag(e.getJSMessage());
        String jsStack = NotifyAppErrorHelper.removeMessageTag(e.getJSStackTrace());
        result.append(e.getFileName() + ":" + e.getLineNumber() + ": " + jsMsg);
        result.append(jsStack != null ? "\n" + jsStack : "");
        result.append("\n");
        result.append(e.getClass().getName());
        return result.toString();
    }
}
