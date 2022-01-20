/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertiesUtils {
    private static final String TAG = "SystemPropertiesUtils";

    private static Method METHOD_GET;

    static {
        try {
            Class<?> klass = Class.forName("android.os.SystemProperties");
            METHOD_GET = klass.getMethod("get", String.class);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Fail to init METHOD_GET", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Fail to init METHOD_GET", e);
        }
    }

    public static String get(String key) {
        if (METHOD_GET == null) {
            return "";
        }
        try {
            return (String) METHOD_GET.invoke(null, key);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Fail to get property", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Fail to get property", e);
        }
        return "";
    }
}
