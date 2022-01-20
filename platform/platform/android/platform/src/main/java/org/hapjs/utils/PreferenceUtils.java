/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hapjs.runtime.HapEngine;

public class PreferenceUtils {
    private static final String PREF_KEY_SHORTCUT_REFUSED_TIME_BY_COUNT =
            "shortcut_remind_time_by_count";
    // the time of cancel shortcut dialog from count times or
    // timing
    private static final String PREF_KEY_SHORTCUT_FORBIDDEN_TIME =
            "shortcut_forbidden_time"; // the flag not remind again during sometimes
    private static final String PREF_KEY_DEBUG_MODE = "debug_mode"; // the flag debug

    private static final String PREF_KEY_USE_RECORD = "use_record";
    private static final String USE_RECORD_EXPRESSION = ",";
    private static final int USE_RECORD_TIMES_MAX = 3;

    public static long getShortcutRefusedTimeByCount(String pkg) {
        SharedPreferences sp = getSharedPreferences(pkg);
        return sp.getLong(PREF_KEY_SHORTCUT_REFUSED_TIME_BY_COUNT, 0);
    }

    public static void setShortcutRefusedTimeByCount(String pkg, long time) {
        SharedPreferences sp = getSharedPreferences(pkg);
        sp.edit().putLong(PREF_KEY_SHORTCUT_REFUSED_TIME_BY_COUNT, time).apply();
    }

    public static long getShortcutForbiddenTime(String pkg) {
        SharedPreferences sp = getSharedPreferences(pkg);
        return sp.getLong(PREF_KEY_SHORTCUT_FORBIDDEN_TIME, 0);
    }

    public static void setShortcutForbiddenTime(String pkg, long time) {
        SharedPreferences sp = getSharedPreferences(pkg);
        sp.edit().putLong(PREF_KEY_SHORTCUT_FORBIDDEN_TIME, time).apply();
    }

    public static void addUseRecord(String pkg) {
        List<String> recordList = getUseRecord(pkg);
        recordList.add(String.valueOf(System.currentTimeMillis()));
        while (recordList.size() > USE_RECORD_TIMES_MAX) {
            recordList.remove(0);
        }
        setUseRecord(pkg, recordList);
    }

    public static List<String> getUseRecord(String pkg) {
        SharedPreferences sp = getSharedPreferences(pkg);
        String records = sp.getString(PREF_KEY_USE_RECORD, "");
        String[] split = TextUtils.split(records, USE_RECORD_EXPRESSION);
        return new ArrayList<>(Arrays.asList(split));
    }

    public static void setUseRecord(String pkg, List<String> recordList) {
        SharedPreferences sp = getSharedPreferences(pkg);
        sp.edit()
                .putString(PREF_KEY_USE_RECORD, TextUtils.join(USE_RECORD_EXPRESSION, recordList))
                .apply();
    }

    public static boolean getDebugMode(String pkg) {
        SharedPreferences sp = getSharedPreferences(pkg);
        return sp.getBoolean(PREF_KEY_DEBUG_MODE, false);
    }

    public static void setDebugMode(String pkg, boolean debug) {
        SharedPreferences sp = getSharedPreferences(pkg);
        sp.edit().putBoolean(PREF_KEY_DEBUG_MODE, debug).commit();
    }

    private static SharedPreferences getSharedPreferences(String pkg) {
        return HapEngine.getInstance(pkg).getApplicationContext().getSharedPreference();
    }
}
