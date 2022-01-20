/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.logging;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogHelper {

    private static Map<String, String> sSourceMap = new HashMap<>();
    private static Map<String, String> sSessionMap = new HashMap<>();

    public static void addPackage(String pkg, Source source) {
        addPackage(pkg, source, null);
    }

    public static void addPackage(String pkg, Source source, String session) {
        String sourceString = source == null ? null : source.toJson().toString();
        addPackage(pkg, sourceString, session);
    }

    public static void addPackage(String pkg, String source, String session) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        if (TextUtils.isEmpty(session)) {
            session = createSession();
        }
        sSourceMap.put(pkg, source);
        sSessionMap.put(pkg, session);
    }

    public static String getSource(String pkg) {
        return TextUtils.isEmpty(pkg) ? null : sSourceMap.get(pkg);
    }

    public static String getSession(String pkg) {
        return TextUtils.isEmpty(pkg) ? null : sSessionMap.get(pkg);
    }

    public static String createSession() {
        return UUID.randomUUID().toString();
    }
}
