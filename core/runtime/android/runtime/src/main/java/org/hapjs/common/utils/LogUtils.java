/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.util.Log;
import com.eclipsesource.v8.V8ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.hapjs.render.jsruntime.V8ScriptExceptionWrap;

public class LogUtils {
    private static final String TAG = "LogUtils";

    public static String getStackTrace(Throwable e) {
        if (e instanceof V8ScriptException) {
            e = new V8ScriptExceptionWrap((V8ScriptException) e);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            e.printStackTrace(new PrintStream(out, false, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
