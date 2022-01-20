/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.os.Trace;
import android.util.Log;

public class DebugUtils {

    public static final boolean DBG = false;
    private static final String TAG = "DebugUtils";

    private static int sNestedLevel;

    public static void record(String eventName) {
        if (!DBG) {
            return;
        }

        Log.d(TAG, eventName);
        Trace.beginSection(TAG + ":" + eventName);
        Trace.endSection();
    }

    public static void startRecord(String eventName) {
        if (!DBG) {
            return;
        }

        sNestedLevel++;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sNestedLevel; i++) {
            sb.append('-');
        }
        sb.append("sta:" + eventName);
        String msg = sb.toString();
        Log.d(TAG, msg);

        Trace.beginSection(TAG + ":" + eventName);
    }

    public static void endRecord(String eventName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sNestedLevel; i++) {
            sb.append('-');
        }
        sb.append("end:" + eventName);
        String msg = sb.toString();
        Log.d(TAG, msg);

        Trace.endSection();

        sNestedLevel--;
    }
}
